package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class CargaInstancias {
  
  public static InstanciaProblema cargarDesdeJSON(String nombreArchivo) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(new File("instances/" + nombreArchivo), InstanciaProblema.class);
    } catch (IOException e) {
      System.err.println("Error al cargar la instancia: " + e.getMessage());
      return null;
    }
  }
  
}