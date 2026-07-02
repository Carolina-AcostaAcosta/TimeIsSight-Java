package com.glaucoma.app;

import com.glaucoma.domain.*;
import java.util.List;

public record ProblemInstance(
    List<Patient> patients,
    int totalDays // Guardamos esto para saber de cuántos días era la instancia al cargarla
) {}