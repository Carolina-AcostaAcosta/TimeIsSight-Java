import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import com.glaucoma.app.*;
import com.glaucoma.domain.*;
import com.glaucoma.solver.GlaucomaConstraintProvider;

import java.util.List;
import java.util.ArrayList;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class Main {
  private record Setting(String name, boolean useParallelSchedule, int parallelDaysAMonth) {}
  private record StartupData(String choice, int patients, int days, String JSONFile) {}
  
  public static void main(String[] args) {
    Logger timefoldLogger = (Logger) LoggerFactory.getLogger("ai.timefold.solver");
    timefoldLogger.setLevel(Level.WARN);
    configureConsoleTraceability();
    System.out.println("=== SISTEMA DE SIMULACIÓN: DIAGNÓSTICO DE GLAUCOMA ===");
    CLIConfiguration config = new CLIConfiguration(args);
    
    if (config.isShowHelp()) {
      CLIConfiguration.showHelp();
      return;
    }
    
    // FASE 1: Obtener qué quiere hacer el usuario
    StartupData data = determineStartupData(config);
    if (data == null) return; // El usuario eligió salir en el menú
    
    // FASE 2: Preparar la población base (Cargar o Crear)
    ProblemInstance instance = prepareInstance(data);
    if (instance == null) return; // Hubo un error crítico en la carga
    
    // FASE 3: Ejecutar la batería de pruebas
    Solver<GlaucomaSchedule> solver = configureSolver(config.getExecutionTimeMinutes());
    List<ResultsExporter.ConfigurationResult> results = executeOptimizerBattery(instance, solver);
    ResultsExporter.save(results, new OptimizerOptions(false, 0, instance.patients().size(), instance.totalDays()));
  }
  
  private static StartupData determineStartupData(CLIConfiguration config) {
    if (!config.isUseInteractiveMenu()) {
      if (config.isNewInstance()) {
        return new StartupData("NEW", config.getPatients(), config.getDays(), "");
      } else if (config.isLoadInstance()) {
        return new StartupData("LOAD", 0, 0, config.getLoadFile());
      }
    }
    
    // Menú Interactivo
    InteractiveMenu menu = new InteractiveMenu();
    Object[] results = menu.configureData();
    
    if (results == null) {
      System.out.println("Saliendo del programa...");
      return null;
    }
    
    String choice = (String) results[0];
    if (choice.equals("LOAD")) {
      return new StartupData("LOAD", 0, 0, (String) results[1]);
    } else {
      return new StartupData("NEW", (int) results[1], (int) results[2], "");
    }
  }
  
  private static ProblemInstance prepareInstance(StartupData data) {
    if (data.choice().equals("LOAD")) {
      System.out.println("\nCargando población base desde: " + data.JSONFile());
      ProblemInstance loaded = InstanceLoader.loadFromJSON(data.JSONFile());
      if (loaded == null) {
        System.out.println("Error crítico al cargar el archivo JSON. Abortando.");
      }
      return loaded;
    }
    
    System.out.println("\nGenerando nueva población aleatoria...");
    ProblemInstance newInstance = InstanceGenerator.generateInstance(data.patients(), data.days());
    
    OptimizerOptions savedOptions = new OptimizerOptions(false, 0, data.patients(), data.days());
    InstanceGenerator.saveInstance(newInstance, savedOptions);
    return newInstance;
  }
  
  private static List<ResultsExporter.ConfigurationResult> executeOptimizerBattery(ProblemInstance instance, Solver<GlaucomaSchedule> solver) {
    List<Setting> settingBattery = List.of(
        new Setting("ESTÁNDAR (Sin Agenda Paralela)", false, 0),
        new Setting("PARALELA (2 Días/Mes)", true, 2),
        new Setting("PARALELA (3 Días/Mes)", true, 3),
        new Setting("PARALELA (4 Días/Mes)", true, 4)
    );
    
    int patientsQuantity = instance.patients().size();
    int simulationTotalDays = instance.totalDays();
    
    System.out.println("\n--- INICIANDO BATERÍA DE " + settingBattery.size() + " ASIGNACIONES ---");
    
    int simNumber = 1;
    List<ResultsExporter.ConfigurationResult> results = new ArrayList<>();
    for (Setting setting : settingBattery) {
      System.out.println("\n=======================================================");
      System.out.println("[Prueba " + simNumber + "/4] Ejecutando: " + setting.name());
      System.out.println("=======================================================");
      
      OptimizerOptions currentOptions = new OptimizerOptions(
          setting.useParallelSchedule(), setting.parallelDaysAMonth(), patientsQuantity, simulationTotalDays
      );
      
      GlaucomaSchedule finalProblem = InstanceGenerator.generateProblem(currentOptions, instance.patients());
      
      System.out.println("Motor Timefold iniciado. Optimizando circuitos...");
      long startTime = System.currentTimeMillis();
      GlaucomaSchedule optimizedSchedule = solver.solve(finalProblem);
      long executionTimeMs = System.currentTimeMillis() - startTime;
      
      // Asumiendo que procesarYGuardarResultados acepta archivoSalida como String
      results.add(processResults(optimizedSchedule, currentOptions, simNumber, executionTimeMs, setting));
      simNumber++;
    }
    
    System.out.println("\n=== BATERÍA DE PRUEBAS FINALIZADA CON ÉXITO ===");
    return results;
  }
  
  private static ResultsExporter.ConfigurationResult processResults(GlaucomaSchedule optimizedSchedule, OptimizerOptions options,
                                                                    int simulationNumber, long executionTimeMs, Setting setting) {
    int unfeasibleCases = 0;
    long waitingDaysSum = 0;
    int totalGlaucomaDiagnosis = 0;
    int limboAppointments = 0;
    
    for (Appointment appointment : optimizedSchedule.getAppointmentsList()) {
      if (appointment.getT() == null) {
        limboAppointments++;
        continue;
      }
      
      if (appointment.isFinalDiagnosisAppointment() && appointment.getEndMinute() != null && appointment.getPatient().getGi() > 0) {
        totalGlaucomaDiagnosis++;
        
        // Traducimos los minutos de simulación a días reales del calendario (0-364)
        int beginningCalendarDay = InstanceGenerator.getCalendarDay(appointment.getPatient().getTi(), options.totalDays());
        int endCalendarDay = InstanceGenerator.getCalendarDay(appointment.getEndMinute(), options.totalDays());
        
        int realWaitingDays = endCalendarDay - beginningCalendarDay;
        waitingDaysSum += realWaitingDays;
        
        // El plazo crítico máximo permitido expresado en días
        int criticalPeriodDays = appointment.getPatient().getDi() / 330;
        
        if (realWaitingDays > criticalPeriodDays) {
          unfeasibleCases++;
        }
      }
    }
    
    double averageWaitingDays = totalGlaucomaDiagnosis > 0 ? (double) waitingDaysSum / totalGlaucomaDiagnosis : 0.0;
    
    int unassignedAppointmentsTime = 0;
    int unassignedAppointmentsCapacity;
    int uninitializedVariables = optimizedSchedule.getScore().initScore();
    
    if (uninitializedVariables < 0) {
      unassignedAppointmentsTime = Math.abs(uninitializedVariables);
      unassignedAppointmentsCapacity = limboAppointments - unassignedAppointmentsTime;
    } else {
      unassignedAppointmentsCapacity = limboAppointments;
    }
    
    System.out.println("-> Cantidad de patients: " + optimizedSchedule.getPatientsList().size());
    System.out.println("-> Tamaño de la agenda: " + options.totalDays());
    System.out.println("-> Citas fuera de agenda: " + limboAppointments);
    System.out.println("-> Citas fuera de agenda por falta de tiempo de ejecución: " + unassignedAppointmentsTime);
    System.out.println("-> Citas fuera de agenda por falta de capacidad: " + unassignedAppointmentsCapacity);
    System.out.println("-> Infactibilidades médicas: " + unfeasibleCases);
    System.out.println("[Simulación " + simulationNumber + "] Completada con éxito.");
    
    return new ResultsExporter.ConfigurationResult(
        setting.name(),
        setting.parallelDaysAMonth(),
        executionTimeMs / 1000.0,
        unfeasibleCases,
        averageWaitingDays,
        unassignedAppointmentsTime,
        unassignedAppointmentsCapacity
    );
  }
  
  private static void configureConsoleTraceability() {
    try {
      Files.createDirectories(Paths.get("output"));
      
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String filePath = "output/console_register_" + timestamp + ".log";
      
      PrintStream fileOut = new PrintStream(new FileOutputStream(filePath));

      System.setOut(fileOut);
      System.setErr(fileOut);
      
      System.out.println("=== INICIO DE TRAZABILIDAD: " + timestamp + " ===");
      
    } catch (Exception e) {
      System.err.println("No se pudo configurar la redirección de logs: " + e.getMessage());
    }
  }
  
  // Configuración limpia del motor metaheurístico
  private static Solver<GlaucomaSchedule> configureSolver(long maxTime) {
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