package com.glaucoma.app;

import com.glaucoma.domain.Appointment;
import com.glaucoma.domain.GlaucomaSchedule;

public class ResultsAnalyzer {

  public ResultsExporter.ConfigurationResult analyze(GlaucomaSchedule optimizedSchedule, OptimizerOptions options,
                                                      int simulationNumber, long executionTimeMs, String settingName, int parallelDaysAMonth) {
    int unfeasibleCases = 0;
    long waitingDaysSum = 0;
    int totalGlaucomaDiagnosis = 0;
    int limboAppointments = 0;

    for (Appointment appointment : optimizedSchedule.getAppointmentsList()) {
      if (appointment.getT() == null) {
        limboAppointments++;
        continue;
      }

      if (appointment.isFinalDiagnosisAppointment() && appointment.getEndMinute() != null && appointment.getPatient().getGi() > 0) {
        totalGlaucomaDiagnosis++;

        // Traducimos los minutos de simulación a días reales del calendario (0-364)
        int beginningCalendarDay = InstanceGenerator.getCalendarDay(appointment.getPatient().getTi(), options.totalDays());
        int endCalendarDay = InstanceGenerator.getCalendarDay(appointment.getEndMinute(), options.totalDays());

        int realWaitingDays = endCalendarDay - beginningCalendarDay;
        waitingDaysSum += realWaitingDays;

        // El plazo crítico máximo permitido expresado en días
        int criticalPeriodDays = appointment.getPatient().getDi() / 330;

        if (realWaitingDays > criticalPeriodDays) {
          unfeasibleCases++;
        }
      }
    }

    double averageWaitingDays = totalGlaucomaDiagnosis > 0 ? (double) waitingDaysSum / totalGlaucomaDiagnosis : 0.0;

    int unassignedAppointmentsTime = 0;
    int unassignedAppointmentsCapacity;
    int uninitializedVariables = optimizedSchedule.getScore().initScore();

    if (uninitializedVariables < 0) {
      unassignedAppointmentsTime = Math.abs(uninitializedVariables);
      unassignedAppointmentsCapacity = limboAppointments - unassignedAppointmentsTime;
    } else {
      unassignedAppointmentsCapacity = limboAppointments;
    }

    System.out.println("-> Cantidad de patients: " + optimizedSchedule.getPatientsList().size());
    System.out.println("-> Tamaño de la agenda: " + options.totalDays());
    System.out.println("-> Citas fuera de agenda: " + limboAppointments);
    System.out.println("-> Citas fuera de agenda por falta de tiempo de ejecución: " + unassignedAppointmentsTime);
    System.out.println("-> Citas fuera de agenda por falta de capacidad: " + unassignedAppointmentsCapacity);
    System.out.println("-> Infactibilidades médicas: " + unfeasibleCases);
    System.out.println("[Simulación " + simulationNumber + "] Completada con éxito.");

    return new ResultsExporter.ConfigurationResult(
        settingName,
        parallelDaysAMonth,
        executionTimeMs / 1000.0,
        unfeasibleCases,
        averageWaitingDays,
        unassignedAppointmentsTime,
        unassignedAppointmentsCapacity
    );
  }
}
