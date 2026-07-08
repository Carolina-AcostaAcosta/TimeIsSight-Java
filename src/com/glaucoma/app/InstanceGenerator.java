package com.glaucoma.app;

import com.glaucoma.domain.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la generación de instancias y escenarios de optimización, componiendo
 * {@link PatientGenerator}, {@link ClinicResourceFactory} y {@link RouteGenerator}.
 */
public class InstanceGenerator {
  /**
   * Resultado de generar las rutas de todos los pacientes: los propios pacientes
   * (con su ruta ya asignada) y el conjunto plano de todas las citas generadas.
   *
   * @param patients     pacientes con su ruta clínica ya generada
   * @param appointments todas las citas de todos los pacientes, en una única lista
   */
  public record Planning(List<Patient> patients, List<Appointment> appointments) {}

  private static final RouteGenerator routeGenerator = new RouteGenerator();
  private static final PatientGenerator patientGenerator = new PatientGenerator();
  private static final ClinicResourceFactory clinicResourceFactory = new ClinicResourceFactory();

  /**
   * Genera una instancia nueva con pacientes aleatorios.
   *
   * @param patientsQuantity cantidad de pacientes a generar
   * @param totalDays        duración del horizonte de planificación, en días
   * @return la instancia generada
   */
  public static ProblemInstance generateInstance(int patientsQuantity, int totalDays) {
    return new ProblemInstance(patientGenerator.generatePatients(patientsQuantity, totalDays), totalDays);
  }

  /**
   * Construye el problema completo (agenda) para un escenario de optimización concreto,
   * a partir de una lista de pacientes ya existente.
   *
   * @param options  opciones del escenario a construir
   * @param patients pacientes de la instancia
   * @return la agenda lista para ser resuelta por el solver
   */
  public static GlaucomaSchedule generateProblem(OptimizerOptions options, List<Patient> patients) {
    List<Station> stations = clinicResourceFactory.createStations(options.useParallelSchedule());
    List<Resource> resources = clinicResourceFactory.createResources(options.useParallelSchedule());

    Planning planning = processAllRoutes(patients, stations, resources, options.useParallelSchedule(), options.parallelDaysAMonth());

    List<Appointment> appointments = planning.appointments;
    patients = planning.patients;

    return new GlaucomaSchedule(patients, resources, stations, appointments);
  }

  /**
   * Genera la ruta clínica de cada paciente y las asigna al propio paciente.
   *
   * @param patients             pacientes para los que generar la ruta
   * @param stations             estaciones disponibles en el escenario
   * @param resources            recursos disponibles en el escenario
   * @param useParallelSchedule  indica si el escenario usa agenda paralela
   * @param parallelDaysAMonth   días al mes con agenda paralela del escenario
   * @return los pacientes con su ruta asignada y el conjunto plano de todas las citas generadas
   */
  public static Planning processAllRoutes(List<Patient> patients, List<Station> stations,
                                          List<Resource> resources, boolean useParallelSchedule, int parallelDaysAMonth) {
    List<Appointment> allAppointmentsList = new ArrayList<>();

    for (Patient patient : patients) {
      List<Appointment> route = routeGenerator.generateRoute(patient, stations, resources, useParallelSchedule, parallelDaysAMonth, WorkingCalendar.calculateOperationalMinutes(patient.getTotalDays()));
      patient.setRoute(route);
      allAppointmentsList.addAll(route);
    }
    return new Planning(patients, allAppointmentsList);
  }
}
