package com.glaucoma.app;

import com.glaucoma.domain.*;
import java.util.List;

public record InstanciaProblema(
    List<Paciente> pacientes,
    List<Recurso> recursos,
    List<Estacion> estaciones,
    List<Cita> citas
) {}