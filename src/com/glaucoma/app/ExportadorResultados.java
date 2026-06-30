package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class ExportadorResultados {
  public record InformeGlobal(Metadata metadata, java.util.List<SimulacionResult> simulaciones) {}
  public record Metadata(int total_pacientes, int dias_agenda, int minutos_operativos) {}
  public record SimulacionResult(
      String configuracion,
      int agenda_paralela_dias,
      double tiempo_ejecucion_segundos,
      int casos_infactibles,
      double tiempo_medio_diagnostico_dias,
      int citas_sin_asignar_tiempo,
      int citas_sin_asignar_capacidad
  ) {}
  
  public static String generarRutaResultados(int pacientes, int dias) {
    try {
      // 1. Crea la carpeta si no existe
      Files.createDirectories(Paths.get("executions"));
      
      // 2. Genera el timestamp exacto
      String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      
      // 3. Formatea el string final
      return String.format("executions/results_P%d_D%d_%s.json", pacientes, dias, fechaHora);
      
    } catch (IOException e) {
      System.err.println("Error al crear el directorio 'executions': " + e.getMessage());
      return "Results_Error_P" + pacientes + ".txt"; // Fallback de seguridad
    }
  }
  
  public static void guardar(List<SimulacionResult> resultadosJson, OpcionesSimulacion opciones) {
    try {
      Metadata meta = new Metadata(opciones.cantidadPacientes(), opciones.totalDias(), GeneradorInstancias.calcularMinutosOperativos(opciones.totalDias()));
      InformeGlobal informe = new InformeGlobal(meta, resultadosJson);
      
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(new File(generarRutaResultados(opciones.cantidadPacientes(), opciones.totalDias())), informe);
    } catch (IOException e) {
      System.err.println("Error crítico al intentar guardar en el archivo: " + e.getMessage());
    }
  }
}