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

/**
 * Orquesta la ejecución completa del programa: obtiene la configuración de arranque
 * (por CLI o menú interactivo), prepara la instancia del problema y ejecuta la batería
 * de configuraciones de optimización, exportando los resultados al finalizar.
 */
public class OptimizationRunner {
  /** Configuración de un escenario dentro de la batería de pruebas. */
  private record Setting(String name, boolean useParallelSchedule, int parallelDaysAMonth) {}
  /** Datos recogidos en la fase de arranque: qué hacer y con qué parámetros. */
  private record StartupData(String choice, int patients, int days, String JSONFile) {}

  private final SolverConfigFactory solverConfigFactory = new SolverConfigFactory();
  private final ResultsAnalyzer resultsAnalyzer = new ResultsAnalyzer();

  /**
   * Ejecuta el programa de principio a fin.
   *
   * @param args argumentos de línea de comandos (ver {@link CLIConfiguration})
   */
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

  /**
   * Decide qué acción de arranque tomar (nueva instancia o carga) a partir de la CLI,
   * o mediante el menú interactivo si no se recibieron argumentos suficientes.
   *
   * @param config configuración recibida por línea de comandos
   * @return los datos de arranque, o {@code null} si el usuario decidió salir en el menú
   */
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

  /**
   * Carga o genera la instancia del problema según los datos de arranque obtenidos.
   *
   * @param data datos de arranque (qué hacer y con qué parámetros)
   * @return la instancia lista para usarse, o {@code null} si hubo un error crítico al cargarla
   */
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

  /**
   * Ejecuta el solver sobre la misma instancia con cada una de las configuraciones
   * de la batería (estándar y agenda paralela con distintos días al mes).
   *
   * @param instance instancia del problema sobre la que se ejecuta cada configuración
   * @param solver   solver ya configurado, reutilizado para todas las configuraciones
   * @return el resultado de cada configuración ejecutada
   */
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

  /**
   * Redirige la salida estándar y de error a un fichero de log con marca de tiempo,
   * dentro de la carpeta {@code output}.
   */
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
