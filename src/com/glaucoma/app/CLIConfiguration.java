package com.glaucoma.app;

/**
 * Analiza y almacena las opciones recibidas por línea de comandos al arrancar el programa.
 * Si no se reciben argumentos, activa el menú interactivo por defecto.
 */
public class CLIConfiguration {
  private boolean showHelp = false;
  private boolean useInteractiveMenu = false;

  private boolean newInstance = false;
  private int patients = 0;
  private int days = 0;
  private long executionTimeMinutes = 60L;

  private boolean loadInstance = false;
  private String loadFile = null;

  /**
   * Analiza los argumentos recibidos y configura el estado de la instancia.
   * Ante cualquier argumento desconocido o parámetro faltante, activa la ayuda.
   *
   * @param args argumentos de línea de comandos
   */
  public CLIConfiguration(String[] args) {
    // Si no hay argumentos, el comportamiento por defecto es el menú
    if (args == null || args.length == 0) {
      useInteractiveMenu = true;
      return;
    }

    try {
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "-h":
          case "--help":
            showHelp = true;
            return;

          case "-n":
            newInstance = true;
            patients = Integer.parseInt(args[++i]);
            days = Integer.parseInt(args[++i]);
            break;

          case "-e":
            loadInstance = true;
            loadFile = args[++i];
            break;

          case "-t":
            executionTimeMinutes = Long.parseLong(args[++i]);
            break;

          default:
            System.err.println("Argumento desconocido: " + args[i]);
            showHelp = true;
            return;
        }
      }
    } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
      System.err.println("Error: Faltan parámetros o el formato numérico es incorrecto.");
      showHelp = true;
    }
  }

  // --- GETTERS ---
  public boolean isShowHelp() { return showHelp; }
  public boolean isUseInteractiveMenu() { return useInteractiveMenu; }
  public boolean isNewInstance() { return newInstance; }
  public int getPatients() { return patients; }
  public int getDays() { return days; }
  public boolean isLoadInstance() { return loadInstance; }
  public String getLoadFile() { return loadFile; }
  public long getExecutionTimeMinutes() { return executionTimeMinutes; }

  /**
   * Imprime por consola las instrucciones de uso del programa.
   */
  public static void showHelp() {
    System.out.println("\nUso del simulador:");
    System.out.println("  java -jar target/TimeIsSight-1.0-SNAPSHOT.jar [-h | --help] [-n <pacientes> <días>] [-e <archivo>] [-t <tiempo (min)>]");
    System.out.println("\nOpciones:");
    System.out.println("  Sin argumentos\tAbre el menú interactivo paso a paso.");
    System.out.println("  -h, --help    \tMuestra este mensaje de ayuda.");
    System.out.println("  -n <pac> <días>\tGenera una NUEVA instancia con la cantidad de pacientes y días especificados.");
    System.out.println("                \tEjemplo: -n 500 365");
    System.out.println("  -e <archivo>  \tCarga una instancia EXISTENTE desde la carpeta 'instances'.");
    System.out.println("                \tEjemplo: -e P500_D365_20260530_215242.json");
    System.out.println("  -t <tiempo (min)>\tAsigna un tiempo máximo de ejecución para el solver dentro de cada escenario en minutos.");
    System.out.println("                \tEjemplo: -t 120\n");
  }
}
