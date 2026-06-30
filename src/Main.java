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

public class Main {
  private record Escenario(String nombre, boolean usaAgendaParalela, int diasParalelaMes) {}
  private record DatosArranque(String accion, int pacientes, int dias, String archivoJson) {}
  
  public static void main(String[] args) {
    configurarTrazabilidadConsola();
    System.out.println("=== SISTEMA DE SIMULACIÓN: DIAGNÓSTICO DE GLAUCOMA ===");
    ConfiguracionCLI config = new ConfiguracionCLI(args);
    
    if (config.isMostrarAyuda()) {
      imprimirAyuda();
      return;
    }
    
    // FASE 1: Obtener qué quiere hacer el usuario
    DatosArranque datos = determinarDatosArranque(config);
    if (datos == null) return; // El usuario eligió salir en el menú
    
    // FASE 2: Preparar la población base (Cargar o Crear)
    InstanciaProblema instancia = prepararInstancia(datos);
    if (instancia == null) return; // Hubo un error crítico en la carga
    
    // FASE 3: Ejecutar la batería de pruebas
    Solver<AgendaGlaucoma> solver = configurarSolver(config.getTiempoEjecucionMinutos());
    List<ExportadorResultados.SimulacionResult> resultados = ejecutarBateriaSimulaciones(instancia, solver);
    ExportadorResultados.guardar(resultados, new OpcionesSimulacion(false, 0, instancia.pacientes().size(), instancia.totalDias()));
  }
  
  private static DatosArranque determinarDatosArranque(ConfiguracionCLI config) {
    if (!config.isUsarMenuInteractivo()) {
      if (config.isNuevaInstancia()) {
        return new DatosArranque("NEW", config.getPacientes(), config.getDias(), "");
      } else if (config.isCargarInstancia()) {
        return new DatosArranque("LOAD", 0, 0, config.getArchivoEntrada());
      }
    }
    
    // Menú Interactivo
    MenuInteractivo menu = new MenuInteractivo();
    Object[] resultado = menu.configurarDatos();
    
    if (resultado == null) {
      System.out.println("Saliendo del programa...");
      return null;
    }
    
    String accion = (String) resultado[0];
    if (accion.equals("LOAD")) {
      return new DatosArranque("LOAD", 0, 0, (String) resultado[1]);
    } else {
      return new DatosArranque("NEW", (int) resultado[1], (int) resultado[2], "");
    }
  }
  
  private static InstanciaProblema prepararInstancia(DatosArranque datos) {
    if (datos.accion().equals("LOAD")) {
      System.out.println("\nCargando población base desde: " + datos.archivoJson());
      InstanciaProblema cargada = CargaInstancias.cargarDesdeJSON(datos.archivoJson());
      if (cargada == null) {
        System.out.println("Error crítico al cargar el archivo JSON. Abortando.");
      }
      return cargada;
    }
    
    System.out.println("\nGenerando nueva población aleatoria...");
    InstanciaProblema nueva = GeneradorInstancias.generarPoblacionBase(datos.pacientes(), datos.dias());
    
    OpcionesSimulacion optGuardado = new OpcionesSimulacion(false, 0, datos.pacientes(), datos.dias());
    GeneradorInstancias.guardarInstancia(nueva, optGuardado);
    return nueva;
  }
  
  private static List<ExportadorResultados.SimulacionResult> ejecutarBateriaSimulaciones(InstanciaProblema instancia, Solver<AgendaGlaucoma> solver) {
    List<Escenario> escenariosPrueba = List.of(
        new Escenario("ESTÁNDAR (Sin Agenda Paralela)", false, 0),
        new Escenario("PARALELA (2 Días/Mes)", true, 2),
        new Escenario("PARALELA (3 Días/Mes)", true, 3),
        new Escenario("PARALELA (4 Días/Mes)", true, 4)
    );
    
    int cantidadPacientes = instancia.pacientes().size();
    int totalDiasSimulacion = instancia.totalDias();
    
    System.out.println("\n--- INICIANDO BATERÍA DE " + escenariosPrueba.size() + " SIMULACIONES ---");
    
    int numSimulacion = 1;
    List<ExportadorResultados.SimulacionResult> resultados = new ArrayList<>();
    for (Escenario escenario : escenariosPrueba) {
      System.out.println("\n=======================================================");
      System.out.println("[Prueba " + numSimulacion + "/4] Ejecutando: " + escenario.nombre());
      System.out.println("=======================================================");
      
      OpcionesSimulacion opcionesActuales = new OpcionesSimulacion(
          escenario.usaAgendaParalela(), escenario.diasParalelaMes(), cantidadPacientes, totalDiasSimulacion
      );
      
      AgendaGlaucoma problemaFinal = GeneradorInstancias.generarProblema(opcionesActuales, instancia.pacientes());
      
      System.out.println("Motor Timefold iniciado. Optimizando circuitos...");
      long inicioTiempo = System.currentTimeMillis();
      AgendaGlaucoma agendaOptimizada = solver.solve(problemaFinal);
      long tiempoEjecucionMs = System.currentTimeMillis() - inicioTiempo;
      
      // Asumiendo que procesarYGuardarResultados acepta archivoSalida como String
      resultados.add(procesarResultados(agendaOptimizada, opcionesActuales, numSimulacion, tiempoEjecucionMs, escenario));
      numSimulacion++;
    }
    
    System.out.println("\n=== BATERÍA DE PRUEBAS FINALIZADA CON ÉXITO ===");
    return resultados;
  }
  
  private static ExportadorResultados.SimulacionResult procesarResultados(AgendaGlaucoma agendaOptimizada, OpcionesSimulacion opciones,
                                                                          int numSimulacion, long tiempoEjecucionMs, Escenario escenario) {
    int casosInfactibles = 0;
    long sumaDiasEspera = 0;
    int totalDiagnosticosGlaucoma = 0;
    int citasEnLimbo = 0;
    
    for (Cita cita : agendaOptimizada.getListaCitas()) {
      if (cita.getT() == null) {
        citasEnLimbo++;
        continue;
      }
      
      if (cita.esCitaDiagnosticoFinal() && cita.getEndMinute() != null && cita.getPaciente().getGi() > 0) {
        totalDiagnosticosGlaucoma++;
        
        // Traducimos los minutos de simulación a días reales del calendario (0-364)
        int diaCalInicio = GeneradorInstancias.obtenerDiaCalendario(cita.getPaciente().getTi(), opciones.totalDias());
        int diaCalFin = GeneradorInstancias.obtenerDiaCalendario(cita.getEndMinute(), opciones.totalDias());
        
        int diasEsperaReal = diaCalFin - diaCalInicio;
        sumaDiasEspera += diasEsperaReal;
        
        // El plazo crítico máximo permitido expresado en días
        int diasPlazoCritico = cita.getPaciente().getDi() / 330;
        
        if (diasEsperaReal > diasPlazoCritico) {
          casosInfactibles++;
        }
      }
    }
    
    double mediaEsperaDias = totalDiagnosticosGlaucoma > 0 ? (double) sumaDiasEspera / totalDiagnosticosGlaucoma : 0.0;
    
    int citasSinAsignarPorTiempo = 0;
    int citasSinAsignarPorCapacidad = 0;
    int variablesNoInicializadas = agendaOptimizada.getScore().initScore();
    
    if (variablesNoInicializadas < 0) {
      citasSinAsignarPorTiempo = Math.abs(variablesNoInicializadas);
      citasSinAsignarPorCapacidad = citasEnLimbo - citasSinAsignarPorTiempo;
    } else {
      citasSinAsignarPorCapacidad = citasEnLimbo;
    }
    
    System.out.println("-> Cantidad de pacientes: " + agendaOptimizada.getListaPacientes().size());
    System.out.println("-> Tamaño de la agenda: " + opciones.totalDias());
    System.out.println("-> Citas fuera de agenda: " + citasEnLimbo);
    System.out.println("-> Citas fuera de agenda por falta de tiempo de ejecución: " + citasSinAsignarPorTiempo);
    System.out.println("-> Citas fuera de agenda por falta de capacidad: " + citasSinAsignarPorCapacidad);
    System.out.println("-> Infactibilidades médicas: " + casosInfactibles);
    System.out.println("[Simulación " + numSimulacion + "] Completada con éxito.");
    
    return new ExportadorResultados.SimulacionResult(
        escenario.nombre(),
        escenario.diasParalelaMes(),
        tiempoEjecucionMs / 1000.0,
        casosInfactibles,
        mediaEsperaDias,
        citasSinAsignarPorTiempo,
        citasSinAsignarPorCapacidad
    );
  }
  
  private static void configurarTrazabilidadConsola() {
    try {
      Files.createDirectories(Paths.get("output"));
      
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String rutaArchivo = "output/console_register_" + timestamp + ".log";
      
      PrintStream fileOut = new PrintStream(new FileOutputStream(rutaArchivo));

      System.setOut(fileOut);
      System.setErr(fileOut);
      
      System.out.println("=== INICIO DE TRAZABILIDAD: " + timestamp + " ===");
      
    } catch (Exception e) {
      System.err.println("No se pudo configurar la redirección de logs: " + e.getMessage());
    }
  }
  
  private static void imprimirAyuda() {
    System.out.println("\nUso del simulador:");
    System.out.println("  java -jar SimuladorGlaucoma.jar [-h | --help] [-n <pacientes> <días>] [-e <archivo>] [-t <tiempo (min)>]");
    System.out.println("\nOpciones:");
    System.out.println("  Sin argumentos\tAbre el menú interactivo paso a paso.");
    System.out.println("  -h, --help    \tMuestra este mensaje de ayuda.");
    System.out.println("  -n <pac> <días>\tGenera una NUEVA instancia con la cantidad de pacientes y días especificados.");
    System.out.println("                \tEjemplo: -n 500 365");
    System.out.println("  -e <archivo>  \tCarga una instancia EXISTENTE desde la carpeta 'instancias'.");
    System.out.println("                \tEjemplo: -e P500_D365_20260629_215242.json");
    System.out.println("  -t <tiempo (min)>\tAsigna un tiempo máximo de ejecución para el solver dentro de cada simulación en minutos.");
    System.out.println("                \tEjemplo: -t 120\n");
  }
  
  // Configuración limpia del motor metaheurístico
  private static Solver<AgendaGlaucoma> configurarSolver(long tiempoMaximo) {
    // Fase de construcción rápida - Agregamos esto para que la construcción de las citas iniciales no consuma todo el tiempo
    // FIRST_FIT coloca la cita en el primer hueco viable que encuentra
    ConstructionHeuristicPhaseConfig faseConstruccion = new ConstructionHeuristicPhaseConfig()
        .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT);
    
    // Fase de búsqueda local
    // Tenemos que instanciarlo, ya que al configurar la fase de construcción esta desaparece
    // Al inicializarla vacía, el solver usará sus algoritmos por defecto: Búsqueda tabú y Late Acceptance
    LocalSearchPhaseConfig faseBusquedaLocal = new LocalSearchPhaseConfig();
    
    ScoreDirectorFactoryConfig configPuntuacion = new ScoreDirectorFactoryConfig()
        .withConstraintProviderClass(GlaucomaConstraintProvider.class)
        .withInitializingScoreTrend("ONLY_DOWN/ONLY_DOWN");
    
    // Configuración general del solver
    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(AgendaGlaucoma.class)
        .withEntityClasses(Cita.class)
        .withScoreDirectorFactory(configPuntuacion)
        .withTerminationConfig(new TerminationConfig().withMinutesSpentLimit(tiempoMaximo))
        .withPhases(faseConstruccion, faseBusquedaLocal);
    return SolverFactory.<AgendaGlaucoma>create(solverConfig).buildSolver();
  }
}