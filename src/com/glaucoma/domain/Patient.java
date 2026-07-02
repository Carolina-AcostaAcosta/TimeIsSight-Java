package com.glaucoma.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Patient {
  private String id;       // Identificador del paciente i
  private int ti;          // Tiempo de llegada al sistema (en minutos de la simulación)
  private int gi;          // Nivel de gravedad (0: no glaucoma, 1: sospecha, 2: moderado, 3: avanzado, 4: agudo)
  private int di;          // Plazo crítico máximo permitido en minutos (convertido de días)

  private boolean comesFromTheER;
  @JsonProperty("isGlaucoma")
  private boolean isGlaucoma;
  @JsonProperty("isReferredToHospital")
  private boolean isReferredToHospital;
  private boolean needsFollowUp;
  private int assignedCAE;
  private int totalDays;
  
  @JsonIgnore
  private List<Appointment> route;
  
  public Patient() {
    this.route = new ArrayList<>();
  }
  
  public Patient(String id, int ti, int gi, int di, boolean comesFromTheER, boolean isGlaucoma, boolean isReferredToHospital, boolean needsFollowUp, int assignedCAE, int totalDays) {
    this.id = id;
    this.ti = ti;
    this.gi = gi;
    this.di = di;
    this.comesFromTheER = comesFromTheER;
    this.isGlaucoma = isGlaucoma;
    this.isReferredToHospital = isReferredToHospital;
    this.needsFollowUp = needsFollowUp;
    this.assignedCAE = assignedCAE;
    this.totalDays = totalDays;
    this.route = new ArrayList<>();
  }
  
  // Getters
  public String getId() { return id; }
  public int getTi() { return ti; }
  public int getGi() { return gi; }
  public int getDi() { return di; }
  public boolean isComesFromTheER() { return comesFromTheER; }
  public boolean isGlaucoma() { return isGlaucoma; }
  public boolean isNeedsFollowUp() { return needsFollowUp; }
  public boolean isReferredToHospital() { return isReferredToHospital; }
  public int getAssignedCAE() { return assignedCAE; }
  public int getTotalDays() { return totalDays; }
  
  @JsonIgnore
  public List<Appointment> getRoute() { return route; }
  
  public void generateRoute(List<Station> stations, List<Resource> resources, boolean parallel, int parallelDaysAMonth, int simulationEnd) {
    this.route = new ArrayList<>();
    int incrementalID = 1;
    
    Random rand = new Random(this.id.hashCode());
    
    Station eEmergencyRoom = stations.get(0);
    Station eTests = stations.get(1);
    Station eCAE = stations.get(2);
    Station eHospital = stations.get(3);
    Station eGlaucoma = parallel ? stations.get(4) : null;
    
    // Filtramos los doctores correspondientes a su CAE asignado
    List<Resource> doctorsCAE = resources.stream().filter(r -> r.id().startsWith("R_CAE_")).toList();
    List<Resource> myDoctorsCAE = doctorsCAE.subList((this.assignedCAE - 1) * 2, assignedCAE * 2);
    
    List<Resource> doctorsCHUC = resources.stream().filter(r -> r.id().startsWith("R_CHUC_")).toList();
    
    // Máquinas específicas del CAE asignado
    Resource rOctCAE = resources.stream().filter(r -> r.id().equals("R_OCT_CAE_" + this.assignedCAE)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_CAE_" + this.assignedCAE + " en la configuración"));
    Resource rCampCAE = resources.stream().filter(r -> r.id().equals("R_Camp_CAE_" + this.assignedCAE)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_CAE_" + this.assignedCAE + " en la configuración"));
    Resource rTonoCAE = resources.stream().filter(r -> r.id().equals("R_Tono_CAE_" + this.assignedCAE)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_CAE_" + this.assignedCAE + " en la configuración"));
    Resource rRetinoCAE = resources.stream().filter(r -> r.id().equals("R_Retino_CAE_" + this.assignedCAE)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_CAE_" + this.assignedCAE + " en la configuración"));
    
    if (this.comesFromTheER) {
      Resource rTriage = resources.stream().filter(r -> r.id().equals("R_Triaje")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Triaje en la configuración"));
      this.route.add(newAppointment(incrementalID++, eEmergencyRoom, rTriage, 20, false, simulationEnd, parallelDaysAMonth));
    } else if (this.gi > 2) {
      // Validación en el CAE por un médico, sin asistencia del paciente
      Resource docCAE = myDoctorsCAE.get(rand.nextInt(myDoctorsCAE.size()));
      this.route.add(newAppointment(incrementalID++, eCAE, docCAE, 5, false, simulationEnd, parallelDaysAMonth));
    }
    
    // 3. PRUEBAS DIAGNÓSTICAS
    this.route.add(newAppointment(incrementalID++, eTests, rTonoCAE, 5, false, simulationEnd, parallelDaysAMonth));
    this.route.add(newAppointment(incrementalID++, eTests, rRetinoCAE, 15, false, simulationEnd, parallelDaysAMonth));
    
    if (this.isGlaucoma()) { // Sospecha o diagnóstico positivo
      this.route.add(newAppointment(incrementalID++, eTests, rOctCAE, 15, false, simulationEnd, parallelDaysAMonth));
      this.route.add(newAppointment(incrementalID++, eTests, rCampCAE, 15, false, simulationEnd, parallelDaysAMonth));
    }
    
    // 4. ENRUTAMIENTO MÉDICO (Árbol de decisión)
    
    // CASO A: Agenda Paralela activa y paciente con sospecha/glaucoma
    if (parallel && this.gi > 0) {
      Resource docMonographic = resources.stream().filter(r -> r.id().equals("R_GlaucoCAE_" + this.assignedCAE)).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_GlaucoCAE_" + this.assignedCAE + " en la configuración"));
      this.route.add(newAppointment(incrementalID++, eGlaucoma, docMonographic, 15, true, simulationEnd, parallelDaysAMonth));
    }
    // CASO B: Agenda Normal (o paciente sano en agenda paralela)
    else {
      Resource docCAE = myDoctorsCAE.get(rand.nextInt(myDoctorsCAE.size()));
      
      if (this.isReferredToHospital) {
        // Sub-caso B.1: Derivado al hospital central
        this.route.add(newAppointment(incrementalID++, eCAE, docCAE, 30, false, simulationEnd, parallelDaysAMonth));
        
        Resource rTonoHUC = resources.stream().filter(r -> r.id().equals("R_Tono_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Tono_HUC en la configuración"));
        Resource rRetinoHUC = resources.stream().filter(r -> r.id().equals("R_Retino_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Retino_HUC en la configuración"));
        Resource rOctHUC = resources.stream().filter(r -> r.id().equals("R_OCT_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_OCT_HUC en la configuración"));
        Resource rCampHUC = resources.stream().filter(r -> r.id().equals("R_Camp_CHUC")).findFirst().orElseThrow(() -> new IllegalStateException("Falta el recurso R_Camp_HUC en la configuración"));
        
        this.route.add(newAppointment(incrementalID++, eTests, rTonoHUC, 5, false, simulationEnd, parallelDaysAMonth));
        this.route.add(newAppointment(incrementalID++, eTests, rRetinoHUC, 15, false, simulationEnd, parallelDaysAMonth));
        if (this.isGlaucoma) {
          this.route.add(newAppointment(incrementalID++, eTests, rOctHUC, 15, false, simulationEnd, parallelDaysAMonth));
          this.route.add(newAppointment(incrementalID++, eTests, rCampHUC, 15, false, simulationEnd, parallelDaysAMonth));
        }
        
        Resource docCHUC = doctorsCHUC.get(rand.nextInt(doctorsCHUC.size()));
        this.route.add(newAppointment(incrementalID++, eHospital, docCHUC, 30, true, simulationEnd, parallelDaysAMonth));
        
        if (this.needsFollowUp) {
          this.route.add(newAppointment(incrementalID++, eHospital, docCHUC, 15, false, simulationEnd, parallelDaysAMonth));
        }
      } else {
        // Subcaso B.2: Resuelto en el ambulatorio periférico
        this.route.add(newAppointment(incrementalID++, eCAE, docCAE, 30, true, simulationEnd, parallelDaysAMonth));
        
        if (this.needsFollowUp) {
          this.route.add(newAppointment(incrementalID++, eCAE, docCAE, 15, false, simulationEnd, parallelDaysAMonth));
        }
      }
    }
  }
  
  private Appointment newAppointment(int index, Station station, Resource resource, int duration, boolean finalDiagnosis, int simulationEnd, int parallelDaysAMonth) {
    String appointmentID = this.id + "-C" + index;
    return new Appointment(appointmentID, this, station, resource, duration, finalDiagnosis, simulationEnd, parallelDaysAMonth);
  }
}