package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PlanningEntity
public class Appointment {
  
  @PlanningId
  private String id;
  private Patient patient;
  private Station station;
  private Resource resource;
  private int pij; // Tiempo de procesamiento en minutos para este patient en esta estación
  private boolean isFinalDiagnosis;
  private int simulationEndMinute;
  private int parallelDaysAMonth;
  private String centerCache;
  private boolean testCache;
  
  @PlanningVariable(valueRangeProviderRefs = {"5minRange"})
  private Integer t;
  
  @JsonIgnore
  @ValueRangeProvider(id = "5minRange")
  public CountableValueRange<Integer> get5minRange() {
    int roundedBeginning = ((this.patient.getTi() + 4) / 5) * 5;
    
    if (roundedBeginning >= simulationEndMinute) {
      return ValueRangeFactory.createIntValueRange(simulationEndMinute, simulationEndMinute + 5, 5);
    }
    
    return ValueRangeFactory.createIntValueRange(roundedBeginning, simulationEndMinute, 5);
  }
  
  // OBLIGATORIO: Java y Timefold necesitan un constructor vacío por defecto
  // para poder clonar las soluciones en memoria mientras calculan.
  public Appointment() {
  }
  
  // Constructor que usarás tú para crear las appointments iniciales de la simulación
  public Appointment(String id, Patient patient, Station station, Resource resource, int pij, boolean isFinalDiagnosis, int simulationEndMinute, int parallelDaysAMonth) {
    this.id = id;
    this.patient = patient;
    this.station = station;
    this.resource = resource;
    this.pij = pij;
    this.isFinalDiagnosis = isFinalDiagnosis;
    this.t = null; // Empieza sin hora asignada
    this.simulationEndMinute = simulationEndMinute;
    this.parallelDaysAMonth = parallelDaysAMonth;
    this.centerCache = (resource.id().contains("HUC") || resource.id().contains("CHUC")) ? "HUC" : "CAE";
    this.testCache = station.name().equals("Pruebas Diagnósticas");
  }
  
  // Getters y Setters comunes
  public String getId() { return id; }
  public Patient getPatient() { return patient; }
  public Station getStation() { return station; }
  public Resource getResource() { return resource; }
  public int getPij() { return pij; }
  public int getParallelDaysAMonth() { return parallelDaysAMonth; }
  
  public void setFinalDiagnosis(boolean finalDiagnosis) { this.isFinalDiagnosis = finalDiagnosis; }
  public void setParallelDaysAMonth(int parallelDaysAMonth) { this.parallelDaysAMonth = parallelDaysAMonth; }
  
  // El motor NECESITA un Getter y un Setter para la variable de planificación
  public Integer getT() { return t; }
  public void setT(Integer t) { this.t = t; }
  public void setSimulationEndMinute(int simulationEndMinute) { this.simulationEndMinute = simulationEndMinute; }
  
  // --- MÉTODOS AUXILIARES EXTRA ---
  
  public Integer getEndMinute() {
    if (t == null) return null;
    return t + pij;
  }
  
  public boolean isFinalDiagnosisAppointment() {
    return isFinalDiagnosis;
  }
  
  public boolean isTest() {
    return testCache;
  }
  
  public String getCenter() {
    return centerCache;
  }
  
  public boolean isParallelDay() {
    if (this.t == null) return false;
    int today = this.t / 330;
    int monthDay = today % 20;
    
    return switch (this.parallelDaysAMonth) {
      case 2 -> monthDay == 4 || monthDay == 14;
      case 3 -> monthDay == 4 || monthDay == 9 || monthDay == 14;
      case 4 -> monthDay == 4 || monthDay == 9 || monthDay == 14 || monthDay == 19;
      default -> false;
    };
  }
}