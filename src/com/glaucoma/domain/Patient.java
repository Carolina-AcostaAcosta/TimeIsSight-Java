package com.glaucoma.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

public class Patient {
  private String id;       // Identificador del paciente i
  private int ti;          // Tiempo de llegada al sistema (en minutos de la simulación)
  private int gi;          // Nivel de gravedad (0: no glaucoma, 1: sospecha, 2: moderado, 3: avanzado, 4: agudo)
  private int di;          // Plazo crítico máximo permitido en minutos (convertido de días)

  private boolean comesFromTheER;
  @JsonProperty("isGlaucoma")
  @JsonAlias({"glaucoma", "isGlaucoma"})
  private boolean isGlaucoma;
  @JsonProperty("isReferredToHospital")
  @JsonAlias({"referredToHospital", "isReferredToHospital"})
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

  public void setRoute(List<Appointment> route) { this.route = route; }
}