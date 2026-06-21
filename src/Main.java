import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.glaucoma.app.*;
import com.glaucoma.domain.AgendaGlaucoma;
import com.glaucoma.domain.Cita;
import com.glaucoma.solver.GlaucomaConstraintProvider;

public class Main {
  public static void main(String[] args) {
    System.out.println("=== SISTEMA DE SIMULACIÓN: DIAGNÓSTICO DE GLAUCOMA ===");
    ConfiguracionCLI config = new ConfiguracionCLI(args);
    MenuInteractivo menu = new MenuInteractivo();
    Solver<AgendaGlaucoma> solver = configurarSolver();
    
    int numSimulacion = 1;
    boolean ejecutando = true;
    
    while (ejecutando) {
      // Muestra: 1. Nueva instancia, 2. Cargar instancia, 3. Salir
      int eleccion = menu.mostrarMenuPrincipal();
      
      if (eleccion == 3) {
        ejecutando = false;
        continue;
      }
      
      // Procesa la elección recogiendo los datos necesarios
      Object[] resultado = menu.elegirModo(eleccion);
      String accion = (String) resultado[0];
      
      // Si en el submenú de "Nueva Instancia" eligió la opción de volver (3), accion será "CANCEL"
      if (accion.equals("CANCEL")) {
        continue; // Regresa al principio del bucle y vuelve a pintar el Menú Principal
      }
      
      AgendaGlaucoma problemaInicial = null;
      OpcionesSimulacion opciones = null;
      
      // 4. Preparación de datos (Bifurcación limpia)
      if (accion.equals("LOAD")) {
        String nombreArchivo = (String) resultado[1];
        System.out.println("\n[Simulación " + numSimulacion + "] Cargando instancia desde: " + nombreArchivo);
        
        problemaInicial = CargaInstancias.cargarDesdeJSON(nombreArchivo);
        if (problemaInicial == null) {
          System.out.println("Error al cargar la instancia. Volviendo al menú principal...");
          continue;
        }
        opciones = CargaInstancias.inferirOpciones(problemaInicial);
        
      } else if (accion.equals("NEW")) {
        opciones = (OpcionesSimulacion) resultado[1];
        System.out.println("\n[Simulación " + numSimulacion + "] Generando nueva instancia aleatoria...");
        
        problemaInicial = GeneradorInstancias.generarProblema(opciones);
        
        // Guardamos de forma automática en formato JSON en la carpeta /instancias
        GeneradorInstancias.guardarInstancia(problemaInicial, opciones);
      }
      
      // 5. Ejecución del Solver (Común para ambas fuentes de datos)
      if (problemaInicial != null) {
        System.out.println("[Simulación " + numSimulacion + "] Optimizando con Timefold. Por favor, espera...");
        long inicioTiempo = System.currentTimeMillis();
        AgendaGlaucoma agendaOptimizada = solver.solve(problemaInicial);
        long tiempoEjecucionMs = System.currentTimeMillis() - inicioTiempo;
        
        // 6. Analizar y procesar la solución óptima resultante
        procesarYGuardarResultados(agendaOptimizada, opciones, config, numSimulacion, tiempoEjecucionMs);
        
        numSimulacion++;
      }
    }
    
    System.out.println("\nSaliendo del simulador... ¡Hasta pronto!");
  }
  
  private static void procesarYGuardarResultados(AgendaGlaucoma agendaOptimizada, OpcionesSimulacion opciones,
                                                 ConfiguracionCLI config, int numSimulacion, long tiempoEjecucionMs) {
    int casosInfactibles = 0;
    long sumaDiasEspera = 0;
    int totalDiagnosticosGlaucoma = 0;
    int citasAsignadas = 0;
    int citasEnLimbo = 0;
    
    for (Cita cita : agendaOptimizada.getListaCitas()) {
      if (cita.getT() == null) {
        citasEnLimbo++;
        continue;
      }
      
      if (cita.esCitaDiagnosticoFinal() && cita.getEndMinute() != null) {
        citasAsignadas++;
        if (cita.getPaciente().getGi() > 0) {
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
    }
    
    double mediaEsperaDias = totalDiagnosticosGlaucoma > 0 ? (double) sumaDiasEspera / totalDiagnosticosGlaucoma : 0.0;
    int totalMinutos = GeneradorInstancias.calcularMinutosOperativos(opciones.totalDias());
    
    // Exportación de resultados al archivo de salida plano .txt
    ExportadorResultados.guardar(config.getArchivoSalida(), numSimulacion, tiempoEjecucionMs,
        opciones, totalMinutos, casosInfactibles, mediaEsperaDias);
    
    System.out.println("¡Alerta! Citas que el solver no pudo acomodar por falta de capacidad: " + citasEnLimbo);
    System.out.println("[Simulación " + numSimulacion + "] Completada con éxito. Resultados guardados en: " + config.getArchivoSalida());
  }
  
  // Configuración limpia del motor metaheurístico
  private static Solver<AgendaGlaucoma> configurarSolver() {
    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(AgendaGlaucoma.class)
        .withEntityClasses(Cita.class)
        .withConstraintProviderClass(GlaucomaConstraintProvider.class)
        .withTerminationConfig(new TerminationConfig().withMinutesSpentLimit(5L));
    return SolverFactory.<AgendaGlaucoma>create(solverConfig).buildSolver();
  }
}