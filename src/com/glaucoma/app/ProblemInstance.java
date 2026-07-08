package com.glaucoma.app;

import com.glaucoma.domain.*;
import java.util.List;

/**
 * Población base de un problema: el conjunto de pacientes y el horizonte temporal en el que
 * se generó, independiente del escenario de optimización que se ejecute sobre ella.
 *
 * @param patients  pacientes de la instancia
 * @param totalDays días del horizonte de planificación con el que se generó la instancia
 *                  (se conserva para saber de cuántos días era al volver a cargarla)
 */
public record ProblemInstance(
    List<Patient> patients,
    int totalDays
) {}
