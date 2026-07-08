package com.glaucoma.domain;

/**
 * Estación del circuito clínico por la que pasa un paciente (triaje, pruebas, consulta...).
 *
 * @param id   E1, E2, E3, E4, E5
 * @param name Ej.: "Triaje de urgencias", "Consulta Glaucoma"
 */
public record Station(String id, String name) {
  /**
   * Indica si esta estación es la de pruebas diagnósticas.
   *
   * @return {@code true} si es la estación de pruebas diagnósticas (E2)
   */
  public boolean isTest() {
    return id.equals("E2");
  }

  /**
   * Indica si esta estación es una consulta médica (CAE, CHUC o Glaucoma).
   *
   * @return {@code true} si es una estación de consulta (E3, E4 o E5)
   */
  public boolean isConsultation() {
    return id.equals("E3") || id.equals("E4") || id.equals("E5");
  }
}
