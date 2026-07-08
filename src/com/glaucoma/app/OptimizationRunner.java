package com.glaucoma.app;

import ai.timefold.solver.core.api.solver.Solver;
import com.glaucoma.domain.GlaucomaSchedule;

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

public class OptimizationRunner {
  private record Setting(String name, boolean useParallelSchedule, int parallelDaysAMonth) {}
  private record StartupData(String choice, int patients, int days, String JSONFile) {}

  private final SolverConfigFactory solverConfigFactory = new SolverConfigFactory();
  private final ResultsAnalyzer resultsAnalyzer = new ResultsAnalyzer();

  public void run(String[] args) {
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
    Solver<GlaucomaSchedule> solver = solverConfigFactory.configureSolver(config.getExecutionTimeMinutes());
    List<ResultsExporter.ConfigurationResult> results = executeOptimizerBattery(instance, solver);
    ResultsExporter.save(results, new OptimizerOptions(false, 0, instance.patients().size(), instance.totalDays()));
  }

  private StartupData determineStartupData(CLIConfiguration config) {
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

  private ProblemInstance prepareInstance(StartupData data) {
    if (data.choice().equals("LOAD")) {
      System.out.println("\nCargando población base desde: " + data.JSONFile());
      ProblemInstance loaded = InstanceRepository.loadFromJSON(data.JSONFile());
      if (loaded == null) {
        System.out.println("Error crítico al cargar el archivo JSON. Abortando.");
      }
      return loaded;
    }

    System.out.println("\nGenerando nueva población aleatoria...");
    ProblemInstance newInstance = InstanceGenerator.generateInstance(data.patients(), data.days());

    OptimizerOptions savedOptions = new OptimizerOptions(false, 0, data.patients(), data.days());
    InstanceRepository.saveInstance(newInstance, savedOptions);
    return newInstance;
  }

  private List<ResultsExporter.ConfigurationResult> executeOptimizerBattery(ProblemInstance instance, Solver<GlaucomaSchedule> solver) {
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

      results.add(resultsAnalyzer.analyze(optimizedSchedule, currentOptions, simNumber, executionTimeMs, setting.name(), setting.parallelDaysAMonth()));
      simNumber++;
    }

    System.out.println("\n=== BATERÍA DE PRUEBAS FINALIZADA CON ÉXITO ===");
    return results;
  }

  private void configureConsoleTraceability() {
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
}
