package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PlanningEntity
public class Cita {
  
  @PlanningId
  private String id;
  private Paciente paciente;
  private Estacion estacion;
  private Recurso recurso;
  private int pij; // Tiempo de procesamiento en minutos para este paciente en esta estación
  private boolean esDiagnosticoFinal;
  private int minutoFinSimulacion;
  private int diasParalelaMes;
  
  @PlanningVariable(valueRangeProviderRefs = {"rango5min", "rango10min", "rango15min"})
  private Integer t;
  
  @JsonIgnore
  @ValueRangeProvider(id = "rango5min")
  public CountableValueRange<Integer> getRango5() {
    if (this.pij % 5 != 0) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 5, 5);
    }
    
    int inicioRedondeado = ((this.paciente.getTi() + 4) / 5) * 5;
    
    if (inicioRedondeado >= minutoFinSimulacion) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 5, 5);
    }
    
    return ValueRangeFactory.createIntValueRange(inicioRedondeado, minutoFinSimulacion, 5);
  }
  
  @JsonIgnore
  @ValueRangeProvider(id = "rango10min")
  public CountableValueRange<Integer> getRango10() {
    
    if (this.pij % 10 != 0) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 10, 10);
    }
    
    int inicioRedondeado = ((this.paciente.getTi() + 9) / 10) * 10;
    
    if (inicioRedondeado >= minutoFinSimulacion) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 10, 10);
    }
    
    return ValueRangeFactory.createIntValueRange(inicioRedondeado, minutoFinSimulacion, 10);
  }
  
  @JsonIgnore
  @ValueRangeProvider(id = "rango15min")
  public CountableValueRange<Integer> getRango15() {
    
    if (this.pij % 15 != 0) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 15, 15);
    }
    
    int inicioRedondeado = ((this.paciente.getTi() + 14) / 15) * 15;
    
    if (inicioRedondeado >= minutoFinSimulacion) {
      return ValueRangeFactory.createIntValueRange(minutoFinSimulacion, minutoFinSimulacion + 15, 15);
    }
    
    return ValueRangeFactory.createIntValueRange(inicioRedondeado, minutoFinSimulacion, 15);
  }
  
  // OBLIGATORIO: Java y Timefold necesitan un constructor vacío por defecto
  // para poder clonar las soluciones en memoria mientras calculan.
  public Cita() {
  }
  
  // Constructor que usarás tú para crear las citas iniciales de la simulación
  public Cita(String id, Paciente paciente, Estacion estacion, Recurso recurso, int pij, boolean esDiagnosticoFinal, int minutoFinSimulacion, int diasParalelaMes) {
    this.id = id;
    this.paciente = paciente;
    this.estacion = estacion;
    this.recurso = recurso;
    this.pij = pij;
    this.esDiagnosticoFinal = esDiagnosticoFinal;
    this.t = null; // Empieza sin hora asignada
    this.minutoFinSimulacion = minutoFinSimulacion;
    this.diasParalelaMes = diasParalelaMes;
  }
  
  // Getters y Setters comunes
  public String getId() { return id; }
  public Paciente getPaciente() { return paciente; }
  public Estacion getEstacion() { return estacion; }
  public Recurso getRecurso() { return recurso; }
  public int getPij() { return pij; }
  public int getDiasParalelaMes() { return diasParalelaMes; }
  
  public void setEsDiagnosticoFinal(boolean esDiagnosticoFinal) { this.esDiagnosticoFinal = esDiagnosticoFinal; }
  public void setDiasParalelaMes(int diasParalelaMes) { this.diasParalelaMes = diasParalelaMes; }
  
  // El motor NECESITA un Getter y un Setter para la variable de planificación
  public Integer getT() { return t; }
  public void setT(Integer t) { this.t = t; }
  public void setMinutoFinSimulacion(int minutoFinSimulacion) { this.minutoFinSimulacion = minutoFinSimulacion; }
  
  // --- MÉTODOS AUXILIARES EXTRA ---
  
  public Integer getEndMinute() {
    if (t == null) return null;
    return t + pij;
  }
  
  public boolean esCitaDiagnosticoFinal() {
    return esDiagnosticoFinal;
  }
  
  public boolean isPrueba() {
    return estacion.nombre().equals("Pruebas Diagnósticas");
  }
  
  public String getCentro() {
    if (recurso.id().contains("HUC") || recurso.id().contains("CHUC")) return "HUC";
    return "CAE";
  }
  
  public boolean isDiaParalela() {
    if (this.t == null) return false;
    int diaActual = this.t / 330;
    int diaDelMes = diaActual % 20;
    
    return switch (this.diasParalelaMes) {
      case 2 -> diaDelMes == 4 || diaDelMes == 14;
      case 3 -> diaDelMes == 4 || diaDelMes == 9 || diaDelMes == 14;
      case 4 -> diaDelMes == 4 || diaDelMes == 9 || diaDelMes == 14 || diaDelMes == 19;
      default -> false;
    };
  }
}