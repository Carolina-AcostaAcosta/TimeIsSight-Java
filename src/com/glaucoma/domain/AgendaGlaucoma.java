package com.glaucoma.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

@PlanningSolution
public class AgendaGlaucoma {
  
  // 1. "Problem Facts" (Datos fijos del problema que no cambian)
  private List<Paciente> listaPacientes;
  private List<Recurso> listaRecursos;
  private List<Estacion> listaEstaciones;
  
  // 3. Las Entidades de Planificación (El conjunto de citas que hay que mover en el tiempo)
  @PlanningEntityCollectionProperty
  private List<Cita> listaCitas;
  
  // 4. La puntuación (Score). Aquí es donde el motor guarda la calidad de la solución actual.
  // Almacena cuántas reglas "Hard" (obligatorias) y cuántas "Soft" (deseos) se han cumplido o roto.
  @PlanningScore
  private HardSoftScore score;
  
  // Constructor vacío obligatorio para el funcionamiento interno de Timefold
  public AgendaGlaucoma() {
  }
  
  // Constructor para que tú puedas inicializar el problema con tus datos simulados
  public AgendaGlaucoma(List<Paciente> listaPacientes, List<Recurso> listaRecursos,
                        List<Estacion> listaEstaciones, List<Cita> listaCitas) {
    this.listaPacientes = listaPacientes;
    this.listaRecursos = listaRecursos;
    this.listaEstaciones = listaEstaciones;
//        this.rangoMinutos = rangoMinutos;
    this.listaCitas = listaCitas;
  }
  
  // --- GETTERS Y SETTERS ---
  // Son necesarios para que el motor de optimización pueda leer y escribir la información
  
  public List<Paciente> getListaPacientes() { return listaPacientes; }
  public void setListaPacientes(List<Paciente> listaPacientes) { this.listaPacientes = listaPacientes; }
  
  public List<Recurso> getListaRecursos() { return listaRecursos; }
  public void setListaRecursos(List<Recurso> listaRecursos) { this.listaRecursos = listaRecursos; }
  
  public List<Estacion> getListaEstaciones() { return listaEstaciones; }
  public void setListaEstaciones(List<Estacion> listaEstaciones) { this.listaEstaciones = listaEstaciones; }
  
  public List<Cita> getListaCitas() { return listaCitas; }
  public void setListaCitas(List<Cita> listaCitas) { this.listaCitas = listaCitas; }
  
  public HardSoftScore getScore() { return score; }
  public void setScore(HardSoftScore score) { this.score = score; }
}