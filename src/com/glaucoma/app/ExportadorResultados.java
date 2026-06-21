package com.glaucoma.app;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ExportadorResultados {
  
  public static void guardar(String archivo, int numSim, long tiempoMs, OpcionesSimulacion opciones,
                             int totalMinutos, int casosInfactibles, double mediaEspera) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(archivo, true))) {
      writer.println("Simulación " + numSim);
      writer.println("1. Tiempo de ejecución: " + (tiempoMs / 1000.0) + " segundos");
      writer.println("2. Tamaño de la instancia: " + opciones.cantidadPacientes() + " pacientes");
      writer.println("3. Tamaño de la agenda: " + totalMinutos + " minutos operativos distribuidos en " + opciones.totalDias() + " días de calendario");
      
      if (opciones.usarAgendaParalela()) {
        writer.println("4. Configuración: Con Agenda Paralela (" + opciones.diasParalelaMes() + " días al mes)");
      } else {
        writer.println("4. Configuración: Sin Agenda Paralela");
      }
      
      writer.println("5. Casos no factibles (superan plazo crítico): " + casosInfactibles);
      writer.printf("6. Media de tiempo de espera para el diagnóstico: %.2f días de calendario\n", mediaEspera);
      writer.println("--------------------------------------------------");
    } catch (IOException e) {
      System.err.println("Error crítico al intentar guardar en el archivo de texto: " + e.getMessage());
    }
  }
}