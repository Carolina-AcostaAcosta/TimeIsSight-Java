package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glaucoma.app.GeneradorInstancias;

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

//    // Esta es la variable de decisión (x_ijt = 1 en el minuto t).
//    // Al principio es null, y el algoritmo se encargará de asignarle un número (minuto).
//    @PlanningVariable(valueRangeProviderRefs = "minuteRange")
//    private Integer t;
  
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
  public Cita(String id, Paciente paciente, Estacion estacion, Recurso recurso, int pij, boolean esDiagnosticoFinal, int minutoFinSimulacion) {
    this.id = id;
    this.paciente = paciente;
    this.estacion = estacion;
    this.recurso = recurso;
    this.pij = pij;
    this.esDiagnosticoFinal = esDiagnosticoFinal;
    this.t = null; // Empieza sin hora asignada
    this.minutoFinSimulacion = minutoFinSimulacion;
  }
  
  // Getters y Setters comunes
  public String getId() { return id; }
  public Paciente getPaciente() { return paciente; }
  public Estacion getEstacion() { return estacion; }
  public Recurso getRecurso() { return recurso; }
  public int getPij() { return pij; }
  
  public void setEsDiagnosticoFinal(boolean esDiagnosticoFinal) { this.esDiagnosticoFinal = esDiagnosticoFinal; }
  
  // El motor NECESITA un Getter y un Setter para la variable de planificación
  public Integer getT() { return t; }
  public void setT(Integer t) { this.t = t; }
  public void setMinutoFinSimulacion(int minutoFinSimulacion) { this.minutoFinSimulacion = minutoFinSimulacion; }
  
  // --- MÉTODOS AUXILIARES EXTRA (Nos facilitarán la vida con las restricciones) ---
  
  // Calcula automáticamente en qué minuto terminaría la cita
  public Integer getEndMinute() {
    if (t == null) return null;
    return t + pij;
  }
  
  // Comprueba si esta cita representa el diagnóstico final del paciente
  public boolean esCitaDiagnosticoFinal() {
    return esDiagnosticoFinal;
  }
}