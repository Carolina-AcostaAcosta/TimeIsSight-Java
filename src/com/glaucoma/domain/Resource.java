package com.glaucoma.domain;

/**
 * @param serviceTime En minutos (ej.: 30 para ConsultaCAE, 10 para OCT)
 */
public record Resource(String id, String name, int serviceTime) {
}