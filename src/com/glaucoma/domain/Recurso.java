package com.glaucoma.domain;

/**
 * @param tiempoServicio En minutos (ej.: 30 para ConsultaCAE, 10 para OCT)
 */
public record Recurso(String id, String nombre, int tiempoServicio) {
}