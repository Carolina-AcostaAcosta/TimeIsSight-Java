package com.glaucoma.domain;

/**
 * Recurso físico del hospital (doctor o máquina de pruebas) que atiende citas.
 *
 * @param id          identificador único del recurso (ej.: "R_CAE_1", "R_OCT_CHUC")
 * @param name        nombre descriptivo del recurso
 * @param serviceTime tiempo de servicio propio del recurso, en minutos (ej.: 30 para ConsultaCAE, 10 para OCT)
 */
public record Resource(String id, String name, int serviceTime) {
}
