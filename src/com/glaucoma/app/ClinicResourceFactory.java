package com.glaucoma.app;

import com.glaucoma.domain.Resource;
import com.glaucoma.domain.Station;

import java.util.ArrayList;
import java.util.List;

public class ClinicResourceFactory {

  public List<Station> createStations(boolean parallel) {
    List<Station> stations = new ArrayList<>();
    stations.add(new Station("E1", "Triaje de Urgencias"));
    stations.add(new Station("E2", "Pruebas Diagnósticas"));
    stations.add(new Station("E3", "Consulta CAE"));
    stations.add(new Station("E4", "Consulta CHUC"));
    if (parallel) stations.add(new Station("E5", "Consulta Glaucoma (Monográfica)"));
    return stations;
  }

  public List<Resource> createResources(boolean parallel) {
    List<Resource> resources = new ArrayList<>();
    resources.add(new Resource("R_Triaje", "TriajeUrgencias", 20));

    // Creamos a los 6 doctores físicos de los CAE (3 centros x 2 doctores)
    for (int i = 1; i <= 6; i++) resources.add(new Resource("R_CAE_" + i, "Doctor CAE " + i, 0));

    // Creamos a los 34 doctores físicos del CHUC
    for (int i = 1; i <= 34; i++) resources.add(new Resource("R_CHUC_" + i, "Doctor CHUC " + i, 0));

    // Las máquinas de pruebas
    // 1 máquina de cada prueba por CAE
    for (int i = 1; i <= 3; ++i) {
      resources.add(new Resource("R_Camp_CAE_" + i, "Campimetría CAE " + i, 15));
      resources.add(new Resource("R_OCT_CAE_" + i, "OCT CAE " + i, 15));
      resources.add(new Resource("R_Retino_CAE_" + i, "Retinografía CAE " + i, 15));
      resources.add(new Resource("R_Tono_CAE_" + i, "Tonometría CAE " + i, 5));
    }

    // 1 máquina de cada prueba para el CHUC
    resources.add(new Resource("R_Camp_CHUC", "Campimetría CHUC", 15));
    resources.add(new Resource("R_OCT_CHUC", "OCT CHUC", 15));
    resources.add(new Resource("R_Retino_CHUC", "Retinografía CHUC", 15));
    resources.add(new Resource("R_Tono_CHUC", "Tonometría CHUC", 5));

    // En agenda parallel, asignamos tiempo monográfico
    if (parallel) {
      for (int i = 1; i <= 3; i++) resources.add(new Resource("R_GlaucoCAE_" + i, "Doctor Glaucoma CAE " + i, 0));
    }
    return resources;
  }
}
