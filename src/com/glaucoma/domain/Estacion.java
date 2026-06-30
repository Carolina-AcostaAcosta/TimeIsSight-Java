package com.glaucoma.domain;

/**
 * @param id     E1, E2, E3, E4, E5
 * @param nombre Ej.: "Triaje de urgencias", "Consulta Glaucoma"
 */
public record Estacion(String id, String nombre) {
  public boolean esPrueba() {
    return id.equals("E2");
  }
  public boolean esConsulta() {
    return id.equals("E3") || id.equals("E4") || id.equals("E5");
  }
}