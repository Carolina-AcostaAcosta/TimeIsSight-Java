package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class InstancesLoader {
  
  public static ProblemInstance loadFromJSON(String filename) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(new File("instances/" + filename), ProblemInstance.class);
    } catch (IOException e) {
      System.err.println("Error al cargar la instancia: " + e.getMessage());
      return null;
    }
  }
  
}