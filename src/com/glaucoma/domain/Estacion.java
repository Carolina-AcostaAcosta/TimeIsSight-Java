package com.glaucoma.domain;

public class Estacion {
  private String id;     // E1, E2, E3, E4, E5
  private String nombre; // Ej: "Triaje de urgencias", "Consulta Glaucoma"
  
  // Constructor: Sirve para crear la estación fácilmente
  public Estacion(String id, String nombre) {
    this.id = id;
    this.nombre = nombre;
  }
  
  // Getters: Permiten a otras partes del código leer estas propiedades
  public String getId() { return id; }
  public String getNombre() { return nombre; }
  
  // Un método auxiliar para ayudarnos en las restricciones más adelante
  public boolean esPrueba() {
    return id.equals("E2");
  }
  
  public boolean esConsulta() {
    return id.equals("E3") || id.equals("E4") || id.equals("E5");
  }
}