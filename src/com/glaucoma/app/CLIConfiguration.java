package com.glaucoma.app;

public class CLIConfiguration {
  private boolean showHelp = false;
  private boolean useInteractiveMenu = false;
  
  private boolean newInstance = false;
  private int patients = 0;
  private int days = 0;
  private long executionTimeMinutes = 60L;
  
  private boolean loadInstance = false;
  private String loadFile = null;
  
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
            return; // Si pide ayuda, no procesamos más
          
          case "-n":
            newInstance = true;
            // Leemos los dos parámetros siguientes
            patients = Integer.parseInt(args[++i]);
            days = Integer.parseInt(args[++i]);
            break;
            
          case "-e":
            loadInstance = true;
            // Leemos el name del archivo
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
}