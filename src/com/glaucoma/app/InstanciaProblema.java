package com.glaucoma.app;

import com.glaucoma.domain.*;
import java.util.List;

public record InstanciaProblema(
    List<Paciente> pacientes,
    int totalDias // Guardamos esto para saber de cuántos días era la instancia al cargarla
) {}