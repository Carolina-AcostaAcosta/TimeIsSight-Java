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

public class ResultsExporter {
  public record GeneralReport(Metadata metadata, java.util.List<ConfigurationResult> simulations) {}
  public record Metadata(int patientsQuantity, int scheduleDays, int operationalMinutes) {}
  public record ConfigurationResult(
      String configuration,
      int parallelScheduleDays,
      double executionTimeInSeconds,
      int unfeasibleCases,
      double averageDaysForDiagnosis,
      int unassignedAppointmentsTime,
      int unassignedAppointmentsCapacity
  ) {}
  
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