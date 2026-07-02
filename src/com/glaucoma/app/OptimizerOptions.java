package com.glaucoma.app;

public record OptimizerOptions(
    boolean useParallelSchedule,
    int parallelDaysAMonth,
    int patientsQuantity,
    int totalDays
) {}