package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entidad de planificación de Timefold: una cita concreta de un paciente en una estación,
 * usando un recurso, cuyo minuto de inicio ({@code t}) decide el solver.
 */
@PlanningEntity
public class Appointment {

  @PlanningId
  private String id;
  private Patient patient;
  private Station station;
  private Resource resource;
  private int pij;
  private boolean isFinalDiagnosis;
  private int simulationEndMinute;
  private int parallelDaysAMonth;
  private String centerCache;
  private boolean testCache;

  @PlanningVariable(valueRangeProviderRefs = {"5minRange"})
  private Integer t;

  /**
   * Rango de minutos válidos (en bloques de 5) en los que el solver puede situar esta cita:
   * desde la llegada del paciente (redondeada hacia arriba) hasta el fin de la simulación.
   *
   * @return el rango de valores posibles para la variable de planificación {@code t}
   */
  @JsonIgnore
  @ValueRangeProvider(id = "5minRange")
  public CountableValueRange<Integer> get5minRange() {
    int roundedBeginning = ((this.patient.getTi() + 4) / 5) * 5;

    if (roundedBeginning >= simulationEndMinute) {
      return ValueRangeFactory.createIntValueRange(simulationEndMinute, simulationEndMinute + 5, 5);
    }

    return ValueRangeFactory.createIntValueRange(roundedBeginning, simulationEndMinute, 5);
  }

  // Java y Timefold necesitan un constructor vacío por defecto
  // para poder clonar las soluciones en memoria mientras calculan.
  public Appointment() {
  }

  /**
   * Crea una cita inicial (sin minuto asignado todavía) para la ruta de un paciente.
   *
   * @param id                   identificador único de la cita
   * @param patient              paciente al que pertenece la cita
   * @param station              estación en la que se realiza la cita
   * @param resource             recurso (doctor o máquina) que atiende la cita
   * @param pij                  duración de la cita, en minutos
   * @param isFinalDiagnosis     indica si esta cita es la de diagnóstico final del paciente
   * @param simulationEndMinute  minuto de simulación en el que termina el horizonte de planificación
   * @param parallelDaysAMonth   días al mes con agenda paralela del escenario
   */
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

  /**
   * Calcula el minuto en el que termina la cita.
   *
   * @return el minuto de fin, o {@code null} si el solver todavía no le ha asignado un minuto de inicio
   */
  public Integer getEndMinute() {
    if (t == null) return null;
    return t + pij;
  }

  /**
   * Indica si esta es la cita de diagnóstico final del paciente.
   *
   * @return {@code true} si es la cita de diagnóstico final
   */
  public boolean isFinalDiagnosisAppointment() {
    return isFinalDiagnosis;
  }

  /**
   * Indica si la cita corresponde a una prueba diagnóstica (estación "Pruebas Diagnósticas").
   *
   * @return {@code true} si es una prueba diagnóstica
   */
  public boolean isTest() {
    return testCache;
  }

  /**
   * Centro al que pertenece el recurso de esta cita.
   *
   * @return {@code "HUC"} si el recurso pertenece al hospital central, {@code "CAE"} en caso contrario
   */
  public String getCenter() {
    return centerCache;
  }

  /**
   * Indica si el día operativo en el que está situada la cita es un día de agenda paralela,
   * según la cantidad de días al mes configurada para el escenario.
   *
   * @return {@code true} si la cita cae en un día de agenda paralela
   */
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
