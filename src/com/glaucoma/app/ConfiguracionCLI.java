package com.glaucoma.app;

public class ConfiguracionCLI {
  private boolean mostrarAyuda = false;
  private boolean usarMenuInteractivo = false;
  
  private boolean nuevaInstancia = false;
  private int pacientes = 0;
  private int dias = 0;
  private long tiempoEjecucionMinutos = 60L;
  
  private boolean cargarInstancia = false;
  private String archivoEntrada = null;
  
  public ConfiguracionCLI(String[] args) {
    // Si no hay argumentos, el comportamiento por defecto es el menú
    if (args == null || args.length == 0) {
      usarMenuInteractivo = true;
      return;
    }
    
    try {
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "-h":
          case "--help":
            mostrarAyuda = true;
            return; // Si pide ayuda, no procesamos más
          
          case "-n":
            nuevaInstancia = true;
            // Leemos los dos parámetros siguientes
            pacientes = Integer.parseInt(args[++i]);
            dias = Integer.parseInt(args[++i]);
            break;
            
          case "-e":
            cargarInstancia = true;
            // Leemos el nombre del archivo
            archivoEntrada = args[++i];
            break;
            
          case "-t":
            tiempoEjecucionMinutos = Long.parseLong(args[++i]);
            break;
            
          default:
            System.err.println("Argumento desconocido: " + args[i]);
            mostrarAyuda = true;
            return;
        }
      }
    } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
      System.err.println("Error: Faltan parámetros o el formato numérico es incorrecto.");
      mostrarAyuda = true;
    }
  }
  
  // --- GETTERS ---
  public boolean isMostrarAyuda() { return mostrarAyuda; }
  public boolean isUsarMenuInteractivo() { return usarMenuInteractivo; }
  public boolean isNuevaInstancia() { return nuevaInstancia; }
  public int getPacientes() { return pacientes; }
  public int getDias() { return dias; }
  public boolean isCargarInstancia() { return cargarInstancia; }
  public String getArchivoEntrada() { return archivoEntrada; }
  public long getTiempoEjecucionMinutos() { return tiempoEjecucionMinutos; }
}