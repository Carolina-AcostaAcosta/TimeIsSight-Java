package com.glaucoma.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

/**
 * Paciente del sistema, con sus datos clínicos y de llegada, y la ruta de citas
 * que debe seguir dentro del escenario de optimización (asignada por {@link RouteGenerator}).
 */
public class Patient {
  /** Identificador del paciente. */
  private String id;
  /** Tiempo de llegada al sistema, en minutos de la simulación. */
  private int ti;
  /** Nivel de gravedad (0: no glaucoma, 1: sospecha, 2: moderado, 3: avanzado, 4: agudo). */
  private int gi;
  /** Plazo crítico máximo permitido, en minutos (convertido a partir de días). */
  private int di;

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

  /** Constructor vacío requerido por Jackson para deserializar desde JSON. */
  public Patient() {
    this.route = new ArrayList<>();
  }

  /**
   * Crea un paciente con todos sus datos clínicos y de llegada.
   *
   * @param id                   identificador del paciente
   * @param ti                   tiempo de llegada al sistema, en minutos de la simulación
   * @param gi                   nivel de gravedad (0 a 4)
   * @param di                   plazo crítico máximo permitido, en minutos
   * @param comesFromTheER       indica si el paciente llega derivado de urgencias
   * @param isGlaucoma           indica si hay sospecha o diagnóstico de glaucoma
   * @param isReferredToHospital indica si el paciente está derivado al hospital central (CHUC)
   * @param needsFollowUp        indica si el paciente necesita revisiones de seguimiento
   * @param assignedCAE          centro de atención especializada (CAE) asignado al paciente
   * @param totalDays            duración del horizonte de planificación, en días
   */
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

  /**
   * Asigna la ruta de citas del paciente.
   *
   * @param route ruta de citas generada, normalmente por {@link RouteGenerator}
   */
  public void setRoute(List<Appointment> route) { this.route = route; }
}
