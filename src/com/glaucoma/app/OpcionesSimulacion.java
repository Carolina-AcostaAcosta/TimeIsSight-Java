package com.glaucoma.app;

public record OpcionesSimulacion(
    boolean usarAgendaParalela,
    int diasParalelaMes,
    int cantidadPacientes,
    int totalDias
) {}