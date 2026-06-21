package com.glaucoma.domain;

public class Recurso {
  private String id;
  private String nombre;
  private int tiempoServicio; // En minutos (ej: 30 para ConsultaCAE, 10 para OCT)
  
  public Recurso(String id, String nombre, int tiempoServicio) {
    this.id = id;
    this.nombre = nombre;
    this.tiempoServicio = tiempoServicio;
  }
  
  public String getId() { return id; }
  public String getNombre() { return nombre; }
  public int getTiempoServicio() { return tiempoServicio; }
}