package com.glaucoma.domain;

public class Paciente {
  private String id;       // Identificador del paciente i
  private int ti;          // Tiempo de llegada al sistema (en minutos de la simulación)
  private int gi;          // Nivel de gravedad (0: sospecha, 1: moderado, 2: avanzado, 3: agudo)
  private int di;          // Plazo crítico máximo permitido en minutos (convertido de días)
  private String circuito; // Circuito asignado (Ci)
  
  public Paciente(String id, int ti, int gi, int di, String circuito) {
    this.id = id;
    this.ti = ti;
    this.gi = gi;
    this.di = di;
    this.circuito = circuito;
  }
  
  // Getters
  public String getId() { return id; }
  public int getTi() { return ti; }
  public int getGi() { return gi; }
  public int getDi() { return di; }
  public String getCircuito() { return circuito; }
}