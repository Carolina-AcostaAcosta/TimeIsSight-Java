package com.glaucoma.app;

import com.glaucoma.domain.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceGenerator {
  public record Planning(List<Patient> patients, List<Appointment> appointments) {}
  private static final Random rand = new Random();
  private static final Logger logger = LoggerFactory.getLogger(InstanceGenerator.class);
  // Cachea el mapeo de días laborables por totalDays, porque ahora se invoca desde las
  // restricciones del solver (una vez por cada Appointment de diagnóstico final evaluada), y
  // recalcularlo desde cero cada vez sería muy costoso.
  private static final java.util.Map<Integer, List<Integer>> cacheDaysMapping = new java.util.concurrent.ConcurrentHashMap<>();
  
  // Genera el mapeo: Índice de día de trabajo -> Día real del año (0 a 364)
  public static List<Integer> generateDaysMapping(int totalDays) {
    return cacheDaysMapping.computeIfAbsent(totalDays, InstanceGenerator::calculateDaysMapping);
  }
  
  private static List<Integer> calculateDaysMapping(int totalDays) {
    List<Integer> workingCalendarDays = new ArrayList<>();
    List<Integer> potentialWorkingDays = new ArrayList<>();
    
    for (int day = 0; day < totalDays; day++) {
      int weekday = day % 7;
      if (weekday != 5 && weekday != 6) {
        potentialWorkingDays.add(day);
      }
    }
    
    int holidaysQuantity = (int) Math.round((totalDays / 365.0) * 13);
    Random randCalendar = new Random(42); // Semilla fija para que coincida el calendario en toda la ejecución
    
    List<Integer> holidays = new ArrayList<>();
    while (holidays.size() < Math.min(holidaysQuantity, potentialWorkingDays.size())) {
      int maybeHoliday = potentialWorkingDays.get(randCalendar.nextInt(potentialWorkingDays.size()));
      if (!holidays.contains(maybeHoliday)) {
        holidays.add(maybeHoliday);
      }
    }
    
    for (int day : potentialWorkingDays) {
      if (!holidays.contains(day)) {
        workingCalendarDays.add(day);
      }
    }
    return workingCalendarDays;
  }
  
  public static int calculateOperationalMinutes(int totalDays) {
    return generateDaysMapping(totalDays).size() * 330; // 330 minutos útiles por día hábil
  }
  
  // Método útil para que el Main traduzca minutos a días de calendario reales
  public static int getCalendarDay(int operationalMinute, int totalDays) {
    int operationalDay = operationalMinute / 330;
    List<Integer> map = generateDaysMapping(totalDays);
    if (operationalDay >= map.size()) return totalDays - 1;
    return map.get(operationalDay);
  }
  
  public static ProblemInstance generateInstance(int patientsQuantity, int totalDays) {
    return new ProblemInstance(generatePatients(patientsQuantity, totalDays), totalDays);
  }
  
  public static GlaucomaSchedule generateProblem(OptimizerOptions options, List<Patient> patients) {
    List<Station> stations = createStations(options.useParallelSchedule());
    List<Resource> resources = createResources(options.useParallelSchedule());
    
    Planning planning = processAllRoutes(patients, stations, resources, options.useParallelSchedule(), options.parallelDaysAMonth());
    
    List<Appointment> appointments = planning.appointments;
    patients = planning.patients;
    
    return new GlaucomaSchedule(patients, resources, stations, appointments);
  }
  
  private static List<Station> createStations(boolean parallel) {
    List<Station> stations = new ArrayList<>();
    stations.add(new Station("E1", "Triaje de Urgencias"));
    stations.add(new Station("E2", "Pruebas Diagnósticas"));
    stations.add(new Station("E3", "Consulta CAE"));
    stations.add(new Station("E4", "Consulta CHUC"));
    if (parallel) stations.add(new Station("E5", "Consulta Glaucoma (Monográfica)"));
    return stations;
  }
  
  private static List<Resource> createResources(boolean parallel) {
    List<Resource> resources = new ArrayList<>();
    resources.add(new Resource("R_Triaje", "TriajeUrgencias", 20));
    
    // Creamos a los 6 doctores físicos de los CAE (3 centros x 2 doctores)
    for (int i = 1; i <= 6; i++) resources.add(new Resource("R_CAE_" + i, "Doctor CAE " + i, 0));
    
    // Creamos a los 34 doctores físicos del CHUC
    for (int i = 1; i <= 34; i++) resources.add(new Resource("R_CHUC_" + i, "Doctor CHUC " + i, 0));
    
    // Las máquinas de pruebas
    // 1 máquina de cada prueba por CAE
    for (int i = 1; i <= 3; ++i) {
      resources.add(new Resource("R_Camp_CAE_" + i, "Campimetría CAE " + i, 15));
      resources.add(new Resource("R_OCT_CAE_" + i, "OCT CAE " + i, 15));
      resources.add(new Resource("R_Retino_CAE_" + i, "Retinografía CAE " + i, 15));
      resources.add(new Resource("R_Tono_CAE_" + i, "Tonometría CAE " + i, 5));
    }
    
    // 1 máquina de cada prueba para el CHUC
    resources.add(new Resource("R_Camp_CHUC", "Campimetría CHUC", 15));
    resources.add(new Resource("R_OCT_CHUC", "OCT CHUC", 15));
    resources.add(new Resource("R_Retino_CHUC", "Retinografía CHUC", 15));
    resources.add(new Resource("R_Tono_CHUC", "Tonometría CHUC", 5));
    
    // En agenda parallel, asignamos tiempo monográfico
    if (parallel) {
      for (int i = 1; i <= 3; i++) resources.add(new Resource("R_GlaucoCAE_" + i, "Doctor Glaucoma CAE " + i, 0));
    }
    return resources;
  }
  
  public static List<Patient> generatePatients(int quantity, int totalDays) {
    int totalAvailableMinutes = calculateOperationalMinutes(totalDays);
    List<Patient> patients = new ArrayList<>();
    
    for (int i = 1; i <= quantity; i++) {
      String id = "P" + String.format("%04d", i);
      int arrivingLimit = Math.max(1, totalAvailableMinutes - 500);
      int ti = rand.nextInt(arrivingLimit);
      
      boolean comesFromTheER = rand.nextDouble() < 0.07;
      boolean isGlaucoma = rand.nextDouble() < 0.08;
      
      int gi = 0;
      int daysPeriod = totalDays;
      
      if (isGlaucoma) {
        double suspicionStratum = rand.nextDouble();
        if (suspicionStratum < 0.375) {
          double glaucomaStratum = rand.nextDouble();
          if (glaucomaStratum < 0.50) {
            gi = 2;
            daysPeriod = 90;
          } else if (glaucomaStratum < 0.85) {
            gi = 3;
            daysPeriod = 30;
          } else {
            gi = 4;
            daysPeriod = 1;
          }
        } else {
          gi = 1;
          daysPeriod = 120;
        }
      }
      
      boolean isReferredToHospital = rand.nextDouble() < 0.30;
      // Modificadores de gravedad si va a CHUC
      if (isReferredToHospital) {
        double CHUCStratum = rand.nextDouble();
        int giCandidate = gi;
        int periodCandidate = daysPeriod;
        if (CHUCStratum < 0.20) {
          giCandidate = 3;
          periodCandidate = 30;
        } else if (CHUCStratum < 0.25) {
          giCandidate = 4;
          periodCandidate = 1;
        }
        if (giCandidate > gi) {
          gi = giCandidate;
          daysPeriod = periodCandidate;
        }
      }
      
      // 2. ¿Necesita seguimiento (revisiones)?
      // Según tu lógica: 90% si va al CHUC, 30% si se queda en el CAE
      boolean needsFollowUp;
      if (isReferredToHospital) {
        needsFollowUp = rand.nextDouble() < 0.90;
      } else {
        needsFollowUp = rand.nextDouble() < 0.66;
      }
      
      int diMinutes = daysPeriod * 330;
      
      // El CAE que se le asigna de los tres disponibles
      int assignedCAE = rand.nextInt(3) + 1;
      
      // El nuevo constructor de Patient recibirá los booleanos estáticos
      patients.add(new Patient(id, ti, gi, diMinutes, comesFromTheER, isGlaucoma, isReferredToHospital, needsFollowUp, assignedCAE, totalDays));
    }
    
    return patients;
  }
  
  private static final RouteGenerator routeGenerator = new RouteGenerator();

  public static Planning processAllRoutes(List<Patient> patients, List<Station> stations,
                                          List<Resource> resources, boolean useParallelSchedule, int parallelDaysAMonth) {
    List<Appointment> allAppointmentsList = new ArrayList<>();

    for (Patient patient : patients) {
      List<Appointment> route = routeGenerator.generateRoute(patient, stations, resources, useParallelSchedule, parallelDaysAMonth, calculateOperationalMinutes(patient.getTotalDays()));
      patient.setRoute(route);
      allAppointmentsList.addAll(route);
    }
    return new Planning(patients, allAppointmentsList);
  }
  
  public static void saveInstance(ProblemInstance instance, OptimizerOptions options) {
    
    try {
      Files.createDirectories(Paths.get("instances"));
      
      String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      
      String filename = String.format("instances/P%d_D%d_%s.json",
          options.patientsQuantity(), options.totalDays(), dateTime);
      
      new ObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValue(new File(filename), instance);
      
    } catch (Exception e) {
      logger.error("Error al guardar la instancia.", e);
    }
  }
}