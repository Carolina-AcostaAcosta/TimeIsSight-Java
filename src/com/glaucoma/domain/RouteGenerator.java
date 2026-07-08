package com.glaucoma.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouteGenerator {

  public List<Appointment> generateRoute(Patient patient, List<Station> stations, List<Resource> resources, boolean parallel, int parallelDaysAMonth, int simulationEnd) {
    List<Appointment> route = new ArrayList<>();
    int incrementalID = 1;

    Random rand = new Random(patient.getId().hashCode());

    Station eEmergencyRoom = stations.get(0);
    Station eTests = stations.get(1);
    Station eCAE = stations.get(2);
    Station eHospital = stations.get(3);
    Station eGlaucoma = parallel ? stations.get(4) : null;

    // Filtramos los doctores correspondientes a su CAE asignado
    List<Resource> doctorsCAE = resources.stream().filter(r -> r.id().startsWith("R_CAE_")).toList();
    List<Resource> myDoctorsCAE = doctorsCAE.subList((patient.getAssignedCAE() - 1) * 2, patient.getAssignedCAE() * 2);

    List<Resource> doctorsCHUC = resources.stream().filter(r -> r.id().startsWith("R_CHUC_")).toList();

    // Máquinas específicas del CAE asignado
    Resource rOctCAE = resources.stream().filter(r -> r.id().equals("R_OCT_CAE_" + patient.getAssignedCAE())).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_CAE_" + patient.getAssignedCAE() + " en la configuración"));
    Resource rCampCAE = resources.stream().filter(r -> r.id().equals("R_Camp_CAE_" + patient.getAssignedCAE())).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_CAE_" + patient.getAssignedCAE() + " en la configuración"));
    Resource rTonoCAE = resources.stream().filter(r -> r.id().equals("R_Tono_CAE_" + patient.getAssignedCAE())).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_CAE_" + patient.getAssignedCAE() + " en la configuración"));
    Resource rRetinoCAE = resources.stream().filter(r -> r.id().equals("R_Retino_CAE_" + patient.getAssignedCAE())).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_CAE_" + patient.getAssignedCAE() + " en la configuración"));

    if (patient.isComesFromTheER()) {
      Resource rTriage = resources.stream().filter(r -> r.id().equals("R_Triaje")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Triaje en la configuración"));
      route.add(newAppointment(patient, incrementalID++, eEmergencyRoom, rTriage, 20, false, simulationEnd, parallelDaysAMonth));
    } else if (patient.getGi() > 2) {
      // Validación en el CAE por un médico, sin asistencia del paciente
      Resource docCAE = myDoctorsCAE.get(rand.nextInt(myDoctorsCAE.size()));
      route.add(newAppointment(patient, incrementalID++, eCAE, docCAE, 5, false, simulationEnd, parallelDaysAMonth));
    }

    // 3. PRUEBAS DIAGNÓSTICAS
    route.add(newAppointment(patient, incrementalID++, eTests, rTonoCAE, 5, false, simulationEnd, parallelDaysAMonth));
    route.add(newAppointment(patient, incrementalID++, eTests, rRetinoCAE, 15, false, simulationEnd, parallelDaysAMonth));

    if (patient.isGlaucoma()) { // Sospecha o diagnóstico positivo
      route.add(newAppointment(patient, incrementalID++, eTests, rOctCAE, 15, false, simulationEnd, parallelDaysAMonth));
      route.add(newAppointment(patient, incrementalID++, eTests, rCampCAE, 15, false, simulationEnd, parallelDaysAMonth));
    }

    // 4. ENRUTAMIENTO MÉDICO (Árbol de decisión)

    // CASO A: Agenda Paralela activa y paciente con sospecha/glaucoma
    if (parallel && patient.getGi() > 0) {
      Resource docMonographic = resources.stream().filter(r -> r.id().equals("R_GlaucoCAE_" + patient.getAssignedCAE())).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_GlaucoCAE_" + patient.getAssignedCAE() + " en la configuración"));
      route.add(newAppointment(patient, incrementalID++, eGlaucoma, docMonographic, 15, true, simulationEnd, parallelDaysAMonth));
    }
    // CASO B: Agenda Normal (o paciente sano en agenda paralela)
    else {
      Resource docCAE = myDoctorsCAE.get(rand.nextInt(myDoctorsCAE.size()));

      if (patient.isReferredToHospital()) {
        // Sub-caso B.1: Derivado al hospital central
        route.add(newAppointment(patient, incrementalID++, eCAE, docCAE, 30, false, simulationEnd, parallelDaysAMonth));

        Resource rTonoHUC = resources.stream().filter(r -> r.id().equals("R_Tono_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_HUC en la configuración"));
        Resource rRetinoHUC = resources.stream().filter(r -> r.id().equals("R_Retino_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_HUC en la configuración"));
        Resource rOctHUC = resources.stream().filter(r -> r.id().equals("R_OCT_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_HUC en la configuración"));
        Resource rCampHUC = resources.stream().filter(r -> r.id().equals("R_Camp_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_HUC en la configuración"));

        route.add(newAppointment(patient, incrementalID++, eTests, rTonoHUC, 5, false, simulationEnd, parallelDaysAMonth));
        route.add(newAppointment(patient, incrementalID++, eTests, rRetinoHUC, 15, false, simulationEnd, parallelDaysAMonth));
        if (patient.isGlaucoma()) {
          route.add(newAppointment(patient, incrementalID++, eTests, rOctHUC, 15, false, simulationEnd, parallelDaysAMonth));
          route.add(newAppointment(patient, incrementalID++, eTests, rCampHUC, 15, false, simulationEnd, parallelDaysAMonth));
        }

        Resource docCHUC = doctorsCHUC.get(rand.nextInt(doctorsCHUC.size()));
        route.add(newAppointment(patient, incrementalID++, eHospital, docCHUC, 30, true, simulationEnd, parallelDaysAMonth));

        if (patient.isNeedsFollowUp()) {
          route.add(newAppointment(patient, incrementalID++, eHospital, docCHUC, 15, false, simulationEnd, parallelDaysAMonth));
        }
      } else {
        // Subcaso B.2: Resuelto en el ambulatorio periférico
        route.add(newAppointment(patient, incrementalID++, eCAE, docCAE, 30, true, simulationEnd, parallelDaysAMonth));

        if (patient.isNeedsFollowUp()) {
          route.add(newAppointment(patient, incrementalID++, eCAE, docCAE, 15, false, simulationEnd, parallelDaysAMonth));
        }
      }
    }

    return route;
  }

  private Appointment newAppointment(Patient patient, int index, Station station, Resource resource, int duration, boolean finalDiagnosis, int simulationEnd, int parallelDaysAMonth) {
    String appointmentID = patient.getId() + "-C" + index;
    return new Appointment(appointmentID, patient, station, resource, duration, finalDiagnosis, simulationEnd, parallelDaysAMonth);
  }
}
