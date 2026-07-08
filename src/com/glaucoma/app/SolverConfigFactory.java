package com.glaucoma.app;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import com.glaucoma.domain.Appointment;
import com.glaucoma.domain.GlaucomaSchedule;
import com.glaucoma.solver.GlaucomaConstraintProvider;

public class SolverConfigFactory {

  // Configuración limpia del motor metaheurístico
  public Solver<GlaucomaSchedule> configureSolver(long maxTime) {
    // Fase de construcción rápida - Agregamos esto para que la construcción de las appointments iniciales no consuma todo el tiempo
    // FIRST_FIT coloca la cita en el primer hueco viable que encuentra
    ConstructionHeuristicPhaseConfig constructionPhase = new ConstructionHeuristicPhaseConfig()
        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT);

    // Fase de búsqueda local
    // Tenemos que instanciarlo, ya que al configurar la fase de construcción esta desaparece
    // Al inicializarla vacía, el solver usará sus algoritmos por defecto: Búsqueda tabú y Late Acceptance
    LocalSearchPhaseConfig localSearchPhase = new LocalSearchPhaseConfig();

    ScoreDirectorFactoryConfig scoreConfig = new ScoreDirectorFactoryConfig()
        .withConstraintProviderClass(GlaucomaConstraintProvider.class)
        .withInitializingScoreTrend("ONLY_DOWN/ONLY_DOWN");

    // Configuración general del solver
    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(GlaucomaSchedule.class)
        .withEntityClasses(Appointment.class)
        .withScoreDirectorFactory(scoreConfig)
        .withTerminationConfig(new TerminationConfig().withMinutesSpentLimit(maxTime))
        .withPhases(constructionPhase, localSearchPhase);
    return SolverFactory.<GlaucomaSchedule>create(solverConfig).buildSolver();
  }
}
