package com.glaucoma.app;

public class ConfiguracionCLI {
  private final String archivoSalida;
  
  public ConfiguracionCLI(String[] args) {
    if (args.length == 0) {
      System.err.println("Error: Debes proporcionar el nombre del archivo de salida como argumento.");
      System.err.println("Ejemplo de uso: java Main resultados_glaucoma.txt");
      System.exit(1);
    }
    this.archivoSalida = args[0];
    System.out.println("Archivo de salida configurado: " + this.archivoSalida);
  }
  
  public String getArchivoSalida() {
    return archivoSalida;
  }
}