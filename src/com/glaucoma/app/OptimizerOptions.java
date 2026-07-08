package com.glaucoma.app;

/**
 * Opciones que definen un escenario concreto de optimización.
 *
 * @param useParallelSchedule indica si el escenario usa agenda paralela de Glaucoma
 * @param parallelDaysAMonth  cantidad de días al mes con agenda paralela (0 si no aplica)
 * @param patientsQuantity    cantidad de pacientes del escenario
 * @param totalDays           duración del horizonte de planificación, en días
 */
public record OptimizerOptions(
    boolean useParallelSchedule,
    int parallelDaysAMonth,
    int patientsQuantity,
    int totalDays
) {}
