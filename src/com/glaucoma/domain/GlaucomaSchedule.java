package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class GlaucomaSchedule {
  
  // 1. "Problem Facts" (Datos fijos del problema que no cambian)
  private List<Patient> patientsList;
  private List<Resource> resourcesList;
  private List<Station> stationsList;
  
  // 3. Las Entidades de Planificación (El conjunto de appointments que hay que mover en el tiempo)
  @PlanningEntityCollectionProperty
  private List<Appointment> appointmentsList;
  
  // 4. La puntuación (Score). Aquí es donde el motor guarda la calidad de la solución actual.
  // Almacena cuántas reglas "Hard" (obligatorias) y cuántas "Soft" (deseos) se han cumplido o roto.
  @PlanningScore
  private HardSoftScore score;
  
  // Constructor vacío obligatorio para el funcionamiento interno de Timefold
  public GlaucomaSchedule() {
  }
  
  // Constructor para que tú puedas inicializar el problema con tus datos simulados
  public GlaucomaSchedule(List<Patient> patientsList, List<Resource> resourcesList,
                          List<Station> stationsList, List<Appointment> appointmentsList) {
    this.patientsList = patientsList;
    this.resourcesList = resourcesList;
    this.stationsList = stationsList;
//        this.rangoMinutos = rangoMinutos;
    this.appointmentsList = appointmentsList;
  }
  
  // --- GETTERS Y SETTERS ---
  // Son necesarios para que el motor de optimización pueda leer y escribir la información
  
  public List<Patient> getPatientsList() { return patientsList; }
  public void setPatientsList(List<Patient> patientsList) { this.patientsList = patientsList; }
  
  public List<Resource> getListaRecursos() { return resourcesList; }
  public void setResourcesList(List<Resource> resourcesList) { this.resourcesList = resourcesList; }
  
  public List<Station> getStationsList() { return stationsList; }
  public void setStationsList(List<Station> stationsList) { this.stationsList = stationsList; }
  
  public List<Appointment> getAppointmentsList() { return appointmentsList; }
  public void setAppointmentsList(List<Appointment> appointmentsList) { this.appointmentsList = appointmentsList; }
  
  public HardSoftScore getScore() { return score; }
  public void setScore(HardSoftScore score) { this.score = score; }
}