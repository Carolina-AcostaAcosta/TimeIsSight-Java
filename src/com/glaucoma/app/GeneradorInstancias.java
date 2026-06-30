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

public class GeneradorInstancias {
  public record Planificacion(List<Paciente> pacientes, List<Cita> citas) {}
  private static final Random rand = new Random();
  private static final Logger logger = LoggerFactory.getLogger(GeneradorInstancias.class);
  
  // Genera el mapeo: Índice de día de trabajo -> Día real del año (0 a 364)
  public static List<Integer> generarMapeoDias(int totalDias) {
    List<Integer> diasCalendarioLaborables = new ArrayList<>();
    List<Integer> diasLaborablesPotenciales = new ArrayList<>();
    
    for (int d = 0; d < totalDias; d++) {
      int diaSemana = d % 7; // 0=lunes, 1=martes, ..., 5=sábado, 6=domingo
      if (diaSemana != 5 && diaSemana != 6) {
        diasLaborablesPotenciales.add(d);
      }
    }
    
    // Calculamos los 14 festivos anuales de forma proporcional al horizonte temporal
    int numFestivos = (int) Math.round((totalDias / 365.0) * 14);
    Random randCalendario = new Random(42); // Semilla fija para que coincida el calendario en toda la ejecución
    
    List<Integer> festivos = new ArrayList<>();
    while (festivos.size() < Math.min(numFestivos, diasLaborablesPotenciales.size())) {
      int posibleFestivo = diasLaborablesPotenciales.get(randCalendario.nextInt(diasLaborablesPotenciales.size()));
      if (!festivos.contains(posibleFestivo)) {
        festivos.add(posibleFestivo);
      }
    }
    
    for (int d : diasLaborablesPotenciales) {
      if (!festivos.contains(d)) {
        diasCalendarioLaborables.add(d);
      }
    }
    return diasCalendarioLaborables;
  }
  
  public static int calcularMinutosOperativos(int totalDias) {
    return generarMapeoDias(totalDias).size() * 330; // 330 minutos útiles por día hábil
  }
  
  // Método útil para que el Main traduzca minutos a días de calendario reales
  public static int obtenerDiaCalendario(int minutoOperacional, int totalDias) {
    int diaOperacional = minutoOperacional / 330;
    List<Integer> mapa = generarMapeoDias(totalDias);
    if (diaOperacional >= mapa.size()) return totalDias - 1;
    return mapa.get(diaOperacional);
  }
  
  public static int obtenerDiaCalendarioReal(int minutos) {
    int diaLaborable = minutos / 330;
    int semanasCompletas = diaLaborable / 5;
    int diasRestantesSemana = diaLaborable % 5;
    
    // Cada semana completa transcurrida aporta 7 días reales a la línea de tiempo
    return (semanasCompletas * 7) + diasRestantesSemana;
  }
  
  public static InstanciaProblema generarPoblacionBase(int cantidadPacientes, int totalDias) {
    return new InstanciaProblema(generarPacientesSimulados(cantidadPacientes, totalDias), totalDias);
  }
  
  public static AgendaGlaucoma generarProblema(OpcionesSimulacion opciones, List<Paciente> pacientes) {
    List<Estacion> estaciones = crearEstaciones(opciones.usarAgendaParalela());
    List<Recurso> recursos = crearRecursos(opciones.usarAgendaParalela());
    
    Planificacion planificacion = procesarCircuitosGlobales(pacientes, estaciones, recursos, opciones.usarAgendaParalela(), opciones.diasParalelaMes());
    
    List<Cita> citas = planificacion.citas;
    pacientes = planificacion.pacientes;
    
    return new AgendaGlaucoma(pacientes, recursos, estaciones, citas);
  }
  
  private static List<Estacion> crearEstaciones(boolean paralela) {
    List<Estacion> estaciones = new ArrayList<>();
    estaciones.add(new Estacion("E1", "Triaje de Urgencias"));
    estaciones.add(new Estacion("E2", "Pruebas Diagnósticas"));
    estaciones.add(new Estacion("E3", "Consulta CAE"));
    estaciones.add(new Estacion("E4", "Consulta CHUC"));
    if (paralela) estaciones.add(new Estacion("E5", "Consulta Glaucoma (Monográfica)"));
    return estaciones;
  }
  
  private static List<Recurso> crearRecursos(boolean paralela) {
    List<Recurso> recursos = new ArrayList<>();
    recursos.add(new Recurso("R_Triaje", "TriajeUrgencias", 20));
    
    // Creamos a los 6 doctores físicos de los CAE (3 centros x 2 doctores)
    for (int i = 1; i <= 6; i++) recursos.add(new Recurso("R_CAE_" + i, "Doctor CAE " + i, 0));
    
    // Creamos a los 34 doctores físicos del CHUC
    for (int i = 1; i <= 34; i++) recursos.add(new Recurso("R_CHUC_" + i, "Doctor CHUC " + i, 0));
    
    // Las máquinas de pruebas
    // 1 máquina de cada prueba por CAE
    for (int i = 1; i <= 3; ++i) {
      recursos.add(new Recurso("R_Camp_CAE_" + i, "Campimetría CAE " + i, 15));
      recursos.add(new Recurso("R_OCT_CAE_" + i, "OCT CAE " + i, 15));
      recursos.add(new Recurso("R_Retino_CAE_" + i, "Retinografía CAE " + i, 15));
      recursos.add(new Recurso("R_Tono_CAE_" + i, "Tonometría CAE " + i, 5));
    }
    
    // 1 máquina de cada prueba para el CHUC
    recursos.add(new Recurso("R_Camp_CHUC", "Campimetría CHUC", 15));
    recursos.add(new Recurso("R_OCT_CHUC", "OCT CHUC", 15));
    recursos.add(new Recurso("R_Retino_CHUC", "Retinografía CHUC", 15));
    recursos.add(new Recurso("R_Tono_CHUC", "Tonometría CHUC", 5));
    
    // Otros recursos específicos
    recursos.add(new Recurso("R_UrgCAE", "UrgenteCAE5plazas", 30));
    
    // En agenda paralela, asignamos tiempo monográfico
    if (paralela) {
      for (int i = 1; i <= 3; i++) recursos.add(new Recurso("R_GlaucoCAE_" + i, "Doctor Glaucoma CAE " + i, 0));
    }
    return recursos;
  }
  
  public static List<Paciente> generarPacientesSimulados(int cantidad, int totalDias) {
    int totalMinutosDisponibles = calcularMinutosOperativos(totalDias);
    List<Paciente> pacientes = new ArrayList<>();
    
    for (int i = 1; i <= cantidad; i++) {
      String id = "P" + String.format("%04d", i);
      int limiteLlegada = Math.max(1, totalMinutosDisponibles - 500);
      int ti = rand.nextInt(limiteLlegada);
      
      boolean entraPorUrgencias = rand.nextDouble() < 0.07;
      boolean esGlaucoma = rand.nextDouble() < 0.08;
      
      int gi = 0;
      int plazoDias = totalDias;
      
      if (esGlaucoma) {
        double estratoSospecha = rand.nextDouble();
        if (estratoSospecha < 0.375) {
          double estratoGlaucoma = rand.nextDouble();
          if (estratoGlaucoma < 0.50) {
            gi = 2;
            plazoDias = 90;
          } else if (estratoGlaucoma < 0.85) {
            gi = 3;
            plazoDias = 30;
          } else {
            gi = 4;
            plazoDias = 1;
          }
        } else {
          gi = 1;
          plazoDias = 120;
        }
      }
      
      boolean seDerivaAlCHUC = rand.nextDouble() < 0.30;
      // Modificadores de gravedad si va a CHUC
      if (seDerivaAlCHUC) {
        double estratoCHUC = rand.nextDouble();
        if (estratoCHUC < 0.20) {
          gi = 3;
          plazoDias = 30;
        } else if (estratoCHUC < 0.25) {
          gi = 4;
          plazoDias = 1;
        }
      }
      
      // 2. ¿Necesita seguimiento (revisiones)?
      // Según tu lógica: 90% si va al CHUC, 30% si se queda en el CAE
      boolean necesitaSeguimiento;
      if (seDerivaAlCHUC) {
        necesitaSeguimiento = rand.nextDouble() < 0.90;
      } else {
        necesitaSeguimiento = rand.nextDouble() < 0.66;
      }
      
      int diMinutos = plazoDias * 330;
      
      // El CAE que se le asigna de los tres disponibles
      int numCae = rand.nextInt(3) + 1;
      
      // El nuevo constructor de Paciente recibirá los booleanos estáticos
      pacientes.add(new Paciente(id, ti, gi, diMinutos, entraPorUrgencias, esGlaucoma, seDerivaAlCHUC, necesitaSeguimiento, numCae, totalDias));
    }
    
    return pacientes;
  }
  
  public static Planificacion procesarCircuitosGlobales(List<Paciente> pacientes, List<Estacion> estaciones,
                                                     List<Recurso> recursos, boolean usarAgendaParalela, int diasParalelaMes) {
    List<Cita> listaCitasGlobal = new ArrayList<>();
    
    for (Paciente paciente : pacientes) {
      paciente.generarCircuito(estaciones, recursos, usarAgendaParalela, diasParalelaMes, calcularMinutosOperativos(paciente.getTotalDias()));
      listaCitasGlobal.addAll(paciente.getCircuito());
    }
    return new Planificacion(pacientes, listaCitasGlobal);
  }
  
  public static void guardarInstancia(InstanciaProblema instancia, OpcionesSimulacion opt) {
    
    try {
      Files.createDirectories(Paths.get("instancias"));
      
      String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      
      String nombreArchivo = String.format("instancias/P%d_D%d_%s.json",
          opt.cantidadPacientes(), opt.totalDias(), fechaHora);
      
      new ObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValue(new File(nombreArchivo), instancia);
      
    } catch (Exception e) {
      logger.error("Error al guardar la instancia.", e);
    }
  }
}