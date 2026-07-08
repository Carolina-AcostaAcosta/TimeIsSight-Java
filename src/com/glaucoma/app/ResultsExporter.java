package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.glaucoma.domain.WorkingCalendar;
import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

/**
 * Exporta a JSON los resultados de una batería de configuraciones ejecutadas,
 * dentro de la carpeta {@code executions}.
 */
public class ResultsExporter {
  /**
   * Informe completo de una ejecución: metadatos del escenario y resultado de cada configuración probada.
   *
   * @param metadata    metadatos generales de la ejecución
   * @param simulations resultado de cada configuración probada
   */
  public record GeneralReport(Metadata metadata, java.util.List<ConfigurationResult> simulations) {}

  /**
   * Metadatos generales de una ejecución.
   *
   * @param patientsQuantity  cantidad de pacientes de la instancia
   * @param scheduleDays      duración del horizonte de planificación, en días
   * @param operationalMinutes minutos hábiles totales del horizonte de planificación
   */
  public record Metadata(int patientsQuantity, int scheduleDays, int operationalMinutes) {}

  /**
   * Resultado de una configuración concreta dentro de la batería.
   *
   * @param configuration                  nombre descriptivo de la configuración
   * @param parallelScheduleDays           días al mes con agenda paralela usados en la configuración
   * @param executionTimeInSeconds         tiempo de ejecución del solver, en segundos
   * @param unfeasibleCases                cantidad de infactibilidades médicas detectadas
   * @param averageDaysForDiagnosis        promedio de días reales hasta el diagnóstico final
   * @param unassignedAppointmentsTime     citas sin asignar por falta de tiempo de ejecución del solver
   * @param unassignedAppointmentsCapacity citas sin asignar por falta de capacidad del sistema
   */
  public record ConfigurationResult(
      String configuration,
      int parallelScheduleDays,
      double executionTimeInSeconds,
      int unfeasibleCases,
      double averageDaysForDiagnosis,
      int unassignedAppointmentsTime,
      int unassignedAppointmentsCapacity
  ) {}

  /**
   * Genera una ruta única (con fecha y hora) para el fichero de resultados,
   * creando la carpeta {@code executions} si no existe.
   *
   * @param patients cantidad de pacientes de la instancia
   * @param days     duración del horizonte de planificación, en días
   * @return la ruta del fichero de resultados a generar
   */
  public static String generateResultsPath(int patients, int days) {
    try {
      // 1. Crea la carpeta si no existe
      Files.createDirectories(Paths.get("executions"));

      // 2. Genera el timestamp exacto
      String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

      // 3. Formatea el string final
      return String.format("executions/results_P%d_D%d_%s.json", patients, days, dateTime);

    } catch (IOException e) {
      System.err.println("Error al crear el directorio 'executions': " + e.getMessage());
      return "Results_Error_P" + patients + ".txt"; // Fallback de seguridad
    }
  }

  /**
   * Guarda en JSON los resultados de todas las configuraciones ejecutadas.
   *
   * @param JSONResults resultado de cada configuración probada
   * @param options     opciones usadas para calcular los metadatos de la ejecución
   */
  public static void save(List<ConfigurationResult> JSONResults, OptimizerOptions options) {
    try {
      Metadata meta = new Metadata(options.patientsQuantity(), options.totalDays(), WorkingCalendar.calculateOperationalMinutes(options.totalDays()));
      GeneralReport report = new GeneralReport(meta, JSONResults);

      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(new File(generateResultsPath(options.patientsQuantity(), options.totalDays())), report);
    } catch (IOException e) {
      System.err.println("Error crítico al intentar save en el archivo: " + e.getMessage());
    }
  }
}
