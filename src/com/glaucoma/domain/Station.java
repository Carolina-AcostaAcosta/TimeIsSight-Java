package com.glaucoma.domain;

/**
 * @param id     E1, E2, E3, E4, E5
 * @param name Ej.: "Triaje de urgencias", "Consulta Glaucoma"
 */
public record Station(String id, String name) {
  public boolean isTest() {
    return id.equals("E2");
  }
  public boolean isConsultation() {
    return id.equals("E3") || id.equals("E4") || id.equals("E5");
  }
}