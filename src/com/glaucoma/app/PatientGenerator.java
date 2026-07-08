package com.glaucoma.app;

import com.glaucoma.domain.Patient;
import com.glaucoma.domain.WorkingCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera pacientes aleatorios siguiendo la estratificación clínica del estudio
 * (probabilidad de glaucoma, derivación a CHUC, necesidad de seguimiento, etc.).
 */
public class PatientGenerator {
  private final Random rand = new Random();

  /**
   * Genera una lista de pacientes aleatorios para un horizonte de planificación dado.
   *
   * @param quantity  cantidad de pacientes a generar
   * @param totalDays duración del horizonte de planificación, en días
   * @return la lista de pacientes generados
   */
  public List<Patient> generatePatients(int quantity, int totalDays) {
    int totalAvailableMinutes = WorkingCalendar.calculateOperationalMinutes(totalDays);
    List<Patient> patients = new ArrayList<>();

    for (int i = 1; i <= quantity; i++) {
      String id = "P" + String.format("%04d", i);
      int arrivingLimit = Math.max(1, totalAvailableMinutes - 500);
      int ti = rand.nextInt(arrivingLimit);

      boolean comesFromTheER = rand.nextDouble() < 0.07;
      boolean isGlaucoma = rand.nextDouble() < 0.08;

      int gi = 0;
      int daysPeriod = totalDays;

      if (isGlaucoma) {
        double suspicionStratum = rand.nextDouble();
        if (suspicionStratum < 0.375) {
          double glaucomaStratum = rand.nextDouble();
          if (glaucomaStratum < 0.50) {
            gi = 2;
            daysPeriod = 90;
          } else if (glaucomaStratum < 0.85) {
            gi = 3;
            daysPeriod = 30;
          } else {
            gi = 4;
            daysPeriod = 1;
          }
        } else {
          gi = 1;
          daysPeriod = 120;
        }
      }

      boolean isReferredToHospital = rand.nextDouble() < 0.30;
      // Modificadores de gravedad si va a CHUC
      if (isReferredToHospital) {
        double CHUCStratum = rand.nextDouble();
        int giCandidate = gi;
        int periodCandidate = daysPeriod;
        if (CHUCStratum < 0.20) {
          giCandidate = 3;
          periodCandidate = 30;
        } else if (CHUCStratum < 0.25) {
          giCandidate = 4;
          periodCandidate = 1;
        }
        if (giCandidate > gi) {
          gi = giCandidate;
          daysPeriod = periodCandidate;
        }
      }

      // ¿Necesita seguimiento?
      boolean needsFollowUp;
      if (isReferredToHospital) {
        needsFollowUp = rand.nextDouble() < 0.90;
      } else {
        needsFollowUp = rand.nextDouble() < 0.66;
      }

      int diMinutes = daysPeriod * 330;

      // El CAE que se le asigna de los tres disponibles
      int assignedCAE = rand.nextInt(3) + 1;
      
      patients.add(new Patient(id, ti, gi, diMinutes, comesFromTheER, isGlaucoma, isReferredToHospital, needsFollowUp, assignedCAE, totalDays));
    }

    return patients;
  }
}
