package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glaucoma.domain.AgendaGlaucoma;
import java.io.File;
import java.io.IOException;

public class CargaInstancias {
  
  public static AgendaGlaucoma cargarDesdeJSON(String nombreArchivo) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      // Lee el archivo JSON y lo convierte al objeto InstanciaProblema
      InstanciaProblema problema = mapper.readValue(new File("instancias/" + nombreArchivo), InstanciaProblema.class);
      
      // Retorna un nuevo objeto AgendaGlaucoma usando los datos cargados
      return new AgendaGlaucoma(
          problema.pacientes(),
          problema.recursos(),
          problema.estaciones(),
          problema.citas()
      );
    } catch (IOException e) {
      System.err.println("Error al cargar la instancia: " + e.getMessage());
      return null;
    }
  }
  
  // Método para inferir las OpcionesSimulacion basándonos en los datos del problema
  public static OpcionesSimulacion inferirOpciones(AgendaGlaucoma agenda) {
    // En un caso real, podrías guardar las opciones en el propio JSON,
    // pero aquí podemos inferirlas contando los pacientes o viendo el horizonte
    int cantidadPacientes = agenda.getListaPacientes().size();
    
    // Asumimos 365 días como estándar si no se puede inferir otra cosa
    return new OpcionesSimulacion(false, 0, cantidadPacientes, 365);
  }
}