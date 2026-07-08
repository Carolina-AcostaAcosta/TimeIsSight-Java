package com.glaucoma.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.glaucoma.domain.Appointment;
import com.glaucoma.domain.WorkingCalendar;

/**
 * Define las restricciones duras y blandas que usa Timefold para evaluar la calidad
 * de una agenda ({@link com.glaucoma.domain.GlaucomaSchedule}) durante la optimización.
 */
public class GlaucomaConstraintProvider implements ConstraintProvider {

  private static final java.util.Map<String, Integer> cacheSurgeryDays = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.Set<String> DOCTORS_REPLACED_CAE =
      java.util.Set.of("R_CAE_2", "R_CAE_4", "R_CAE_6");
  private static final java.util.Set<String> DOCTORS_TRANSFERRED_CHUC =
      java.util.Set.of("R_CHUC_32", "R_CHUC_33", "R_CHUC_34");

  // Calcula (y cachea) el día de la semana en el que un doctor del CHUC tiene quirófano asignado
  private int getAssignedSurgeryDay(String resourceID) {
    return cacheSurgeryDays.computeIfAbsent(resourceID, id -> {
      if (id.startsWith("R_CHUC_")) {
        int docID = Integer.parseInt(id.replace("R_CHUC_", ""));
        return docID % 5;
      }
      return -1; // -1 si no es del CHUC
    });
  }
  
  /**
   * Declara el conjunto completo de restricciones evaluadas por el solver.
   *
   * @param factory fábrica de restricciones de Timefold
   * @return las restricciones duras y blandas del problema
   */
  @Override
  public Constraint[] defineConstraints(ConstraintFactory factory) {
    return new Constraint[]{
        sameCenterSameDayTests(factory),
        testsConsultationsPrecedence(factory),
        resourcesCapacity(factory),
        diagnosisCriticalPeriod(factory),
        minimizeDiagnosisTime(factory),
        attendOnlyAfterArrival(factory),
        sameDayAppointmentsNoSevere(factory),
        severeTransportMargin(factory),
        HUCDoctorsWeeklySurgeries(factory),
        ParallelScheduleAvailabilityLogic(factory)
    };
  }
  
  private Constraint sameCenterSameDayTests(ConstraintFactory factory) {
    return factory.forEachUniquePair(Appointment.class, Joiners.equal(Appointment::getPatient))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> c1.isTest() && c2.isTest())
        .filter((c1, c2) -> c1.getCenter().equals(c2.getCenter())) // Solo si ambas son del CAE, o ambas del HUC
        .filter((c1, c2) -> (c1.getT() / 330) != (c2.getT() / 330)) // Si están en días distintos -> penalizar
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 1000)
        .asConstraint("Pruebas del mismo centro deben ser en el mismo día");
  }
  
  private Constraint testsConsultationsPrecedence(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(testAppointment -> testAppointment.getT() != null && testAppointment.isTest())
        .join(Appointment.class,
            Joiners.equal(Appointment::getPatient),
            Joiners.equal(Appointment::getCenter)) // Cruzamos prueba CAE con consulta CAE (o HUC con HUC)
        .filter((test, consultation) -> consultation.getT() != null && !consultation.isTest())
        // Si el día de la prueba es IGUAL (mismo día) o MAYOR (después) que la consulta -> penalizar
        .filter((test, consultation) -> (test.getT() / 330) >= (consultation.getT() / 330))
        .penalize(HardSoftScore.ONE_HARD, (test, consultation) -> 1000)
        .asConstraint("Pruebas deben hacerse un día distinto y anterior a la consulta");
  }
  
  private Constraint resourcesCapacity(ConstraintFactory factory) {
    return factory.forEachUniquePair(Appointment.class,
            Joiners.equal(Appointment::getResource), // Mismo recurso
            Joiners.overlapping(Appointment::getT, Appointment::getEndMinute) // Solapamiento temporal
        )
        .penalize(HardSoftScore.ONE_HARD, ((c1, c2) -> 1000))
        .asConstraint("Capacidad de recurso excedida");
  }
  
  private Constraint diagnosisCriticalPeriod(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(appointment -> appointment.getT() != null && appointment.isFinalDiagnosisAppointment())
        // Condición de quiebre: Supera el plazo permitido di
        .filter(appointment -> {
          int totalDays = appointment.getPatient().getTotalDays();
          int arrivalRealDay = WorkingCalendar.getCalendarDay(appointment.getPatient().getTi(), totalDays);
          int appointmentRealDay = WorkingCalendar.getCalendarDay(appointment.getEndMinute(), totalDays);
          
          int realWaitingDays = appointmentRealDay - arrivalRealDay;
          int criticalPeriodDays = appointment.getPatient().getDi() / 330;
          
          return realWaitingDays > criticalPeriodDays;
        })
        .penalize(HardSoftScore.ONE_SOFT, appointment -> {
          int totalDays = appointment.getPatient().getTotalDays();
          int arrivalRealDay = WorkingCalendar.getCalendarDay(appointment.getPatient().getTi(), totalDays);
          int appointmentRealDay = WorkingCalendar.getCalendarDay(appointment.getEndMinute(), totalDays);
          int realWaitingDays = appointmentRealDay - arrivalRealDay;
          int criticalPeriodDays = appointment.getPatient().getDi() / 330;
          return realWaitingDays - criticalPeriodDays;
        })
        .asConstraint("Plazo crítico superado (Infactibilidad Médica)");
  }
  
  private Constraint minimizeDiagnosisTime(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(appointment -> appointment.getT() != null && appointment.isFinalDiagnosisAppointment())
        .filter(appointment -> appointment.getEndMinute() > appointment.getPatient().getTi())
        .penalize(HardSoftScore.ONE_SOFT, appointment -> {
          int totalDays = appointment.getPatient().getTotalDays();
          int realArrivingDay = WorkingCalendar.getCalendarDay(appointment.getPatient().getTi(), totalDays);
          int realAppointmentDay = WorkingCalendar.getCalendarDay(appointment.getEndMinute(), totalDays);
          return realAppointmentDay - realArrivingDay;
        })
        .asConstraint("Minimizar tiempo total de diagnóstico en días reales");
  }
  
  private Constraint attendOnlyAfterArrival(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(appointment -> appointment.getT() != null)
        .filter(appointment -> appointment.getT() < appointment.getPatient().getTi())
        .penalize(HardSoftScore.ONE_HARD, appointment -> appointment.getPatient().getTi() - appointment.getT())
        .asConstraint("Atención prematura antes de la llegada");
  }
  
  private Constraint sameDayAppointmentsNoSevere(ConstraintFactory factory) {
    return factory.forEachUniquePair(Appointment.class, Joiners.equal(Appointment::getPatient))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> {
          boolean isSevere = c1.getPatient().getGi() == 4;
          return !isSevere;
        })
        .filter((c1, c2) -> !(c1.isTest() && c2.isTest()))
        .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo bloque de día
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 1000)
        .asConstraint("Citas el mismo día para paciente no urgente");
  }
  
  private Constraint severeTransportMargin(ConstraintFactory factory) {
    return factory.forEachUniquePair(Appointment.class, Joiners.equal(Appointment::getPatient))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> c1.getPatient().getGi() == 4)
        .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo día
        .filter((c1, c2) -> {
          Appointment first = (c1.getT() < c2.getT()) ? c1 : c2;
          Appointment second = (first == c1) ? c2 : c1;
          return second.getT() < (first.getEndMinute() + 30); // Margen de 30 minutos de traslado
        })
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 500)
        .asConstraint("Margen de transporte insuficiente en urgencias");
  }
  
  private Constraint HUCDoctorsWeeklySurgeries(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(appointment -> appointment.getT() != null && appointment.getResource().id().startsWith("R_CHUC_"))
        .filter(appointment -> {
          int assignedSurgeryDay = getAssignedSurgeryDay(appointment.getResource().id());
          
          // Calculamos en qué día de la semana cae la cita actual
          int weekdayToday = (appointment.getT() / 330) % 5;
          
          // Si coincide su día de quirófano con la cita de consulta -> penalización dura
          return weekdayToday == assignedSurgeryDay;
        })
        .penalize(HardSoftScore.ONE_HARD, appointment -> 3000)
        .asConstraint("Médico del HUC ausente por jornada de cirugía");
  }
  
  private Constraint ParallelScheduleAvailabilityLogic(ConstraintFactory factory) {
    return factory.forEach(Appointment.class)
        .filter(appointment -> appointment.getT() != null)
        .filter(appointment -> {
          String rId = appointment.getResource().id();
          boolean isParallelDay = appointment.isParallelDay();
          
          // Caso A: Si NO es día de agenda paralela, ningún médico monográfico puede pasar consulta
          if (!isParallelDay && rId.startsWith("R_GlaucoCAE_")) {
            return true;
          }
          
          // Caso B: Si SÍ es día de agenda paralela, aplicamos las sustituciones y traslados
          if (isParallelDay) {
            // 1. Se sustituye a un médico de cada CAE (dejamos inactivos a los médicos pares de los CAE)
            if (DOCTORS_REPLACED_CAE.contains(rId)) {
              return true;
            }
            
            // 2. Tres médicos específicos del HUC (ej.: los 3 últimos, del 32 al 34) se han trasladado,
            // por lo que dejasen de estar disponibles en el Hospital Central esa jornada
            return DOCTORS_TRANSFERRED_CHUC.contains(rId);
          }
          return false;
        })
        .penalize(HardSoftScore.ONE_HARD, appointment -> 3000)
        .asConstraint("Infracción de disponibilidad por traslados de Agenda Paralela");
  }
}