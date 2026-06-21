package com.glaucoma.app;

import com.glaucoma.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GeneradorInstancias {
  
  private static final Random rand = new Random();
  
  // Genera el mapeo: Índice de día de trabajo -> Día real del año (0 a 364)
  public static List<Integer> generarMapeoDias(int totalDias) {
    List<Integer> diasCalendarioLaborables = new ArrayList<>();
    List<Integer> diasLaborablesPotenciales = new ArrayList<>();
    
    for (int d = 0; d < totalDias; d++) {
      int diaSemana = d % 7; // 0=lunes, 1=martes, ..., 5=sábado, 6=domingo
      if (diaSemana != 5 && diaSemana != 6) {
        diasLaborablesPotenciales.add(d);
      }
    }
    
    // Calculamos los 14 festivos anuales de forma proporcional al horizonte temporal
    int numFestivos = (int) Math.round((totalDias / 365.0) * 14);
    Random randCalendario = new Random(42); // Semilla fija para que coincida el calendario en toda la ejecución
    
    List<Integer> festivos = new ArrayList<>();
    while (festivos.size() < Math.min(numFestivos, diasLaborablesPotenciales.size())) {
      int posibleFestivo = diasLaborablesPotenciales.get(randCalendario.nextInt(diasLaborablesPotenciales.size()));
      if (!festivos.contains(posibleFestivo)) {
        festivos.add(posibleFestivo);
      }
    }
    
    for (int d : diasLaborablesPotenciales) {
      if (!festivos.contains(d)) {
        diasCalendarioLaborables.add(d);
      }
    }
    return diasCalendarioLaborables;
  }
  
  public static int calcularMinutosOperativos(int totalDias) {
    return generarMapeoDias(totalDias).size() * 330; // 330 minutos útiles por día hábil
  }
  
  // Método útil para que el Main traduzca minutos a días de calendario reales
  public static int obtenerDiaCalendario(int minutoOperacional, int totalDias) {
    int diaOperacional = minutoOperacional / 330;
    List<Integer> mapa = generarMapeoDias(totalDias);
    if (diaOperacional >= mapa.size()) return totalDias - 1;
    return mapa.get(diaOperacional);
  }
  
  public static AgendaGlaucoma generarProblema(OpcionesSimulacion opciones) {
    int totalMinutos = calcularMinutosOperativos(opciones.totalDias());

//        List<Integer> rangoMinutos = new ArrayList<>();
//        for (int i = 0; i < totalMinutos; i++) rangoMinutos.add(i);
    
    List<Estacion> estaciones = crearEstaciones(opciones.usarAgendaParalela());
    List<Recurso> recursos = crearRecursos(opciones.usarAgendaParalela());
    
    List<Paciente> pacientes = generarPacientesSimulados(opciones.cantidadPacientes(), totalMinutos);
    List<Cita> citas = crearCitasParaPacientes(pacientes, estaciones, recursos, opciones.usarAgendaParalela(), opciones.totalDias());
    
    return new AgendaGlaucoma(pacientes, recursos, estaciones, citas);
  }
  
  private static List<Estacion> crearEstaciones(boolean paralela) {
    List<Estacion> lista = new ArrayList<>();
    lista.add(new Estacion("E1", "Triaje de Urgencias"));
    lista.add(new Estacion("E2", "Pruebas Diagnósticas"));
    lista.add(new Estacion("E3", "Consulta CAE"));
    lista.add(new Estacion("E4", "Consulta CHUC"));
    if (paralela) lista.add(new Estacion("E5", "Consulta Glaucoma (Monográfica)"));
    return lista;
  }
  
  private static List<Recurso> crearRecursos(boolean paralela) {
    List<Recurso> lista = new ArrayList<>();
    lista.add(new Recurso("R_Triaje", "TriajeUrgencias", 20));
    
    // Creamos a los 6 doctores físicos de los CAE (3 centros x 2 doctores)
    for (int i = 1; i <= 6; i++) lista.add(new Recurso("R_CAE_" + i, "Doctor CAE " + i, 0));
    
    // Creamos a los 2 doctores físicos del CHUC
    for (int i = 1; i <= 2; i++) lista.add(new Recurso("R_CHUC_" + i, "Doctor CHUC " + i, 0));
    
    // Las máquinas de pruebas
    lista.add(new Recurso("R_Camp", "Campimetría", 15));
    lista.add(new Recurso("R_OCT", "OCT", 10));
    lista.add(new Recurso("R_Retino", "Retinografía", 10));
    lista.add(new Recurso("R_Tono", "Tonometría", 3));
    
    // Otros recursos específicos
    lista.add(new Recurso("R_UrgCAE", "UrgenteCAE5plazas", 30));
    lista.add(new Recurso("R_ValCAE", "ValidaciónCAE", 10));
    
    // En agenda paralela, asignamos tiempo monográfico
    if (paralela) {
      for (int i = 1; i <= 3; i++) lista.add(new Recurso("R_GlaucoCAE_" + i, "Doctor Glaucoma CAE " + i, 0));
    }
    return lista;
  }
  
  private static List<Paciente> generarPacientesSimulados(int cantidad, int totalMinutosDisponibles) {
    List<Paciente> lista = new ArrayList<>();
    
    for (int i = 1; i <= cantidad; i++) {
      String id = "P" + String.format("%04d", i);
      int limiteLlegada = Math.max(1, totalMinutosDisponibles - 500);
      int ti = rand.nextInt(limiteLlegada);
      
      boolean entraPorUrgencias = rand.nextDouble() < 0.15; // Asumimos 15% entrada por urgencias
      boolean esGlaucoma = rand.nextDouble() < 0.08; // 8% de los pacientes derivados
      
      int gi = 0;
      int plazoDias = 120; // Por defecto: sospecha / normal
      
      if (esGlaucoma) {
        double estratoGlaucoma = rand.nextDouble();
        if (estratoGlaucoma < 0.50) { gi = 1; plazoDias = 90; } // Leve
        else if (estratoGlaucoma < 0.85) { gi = 2; plazoDias = 30; } // Moderado
        else { gi = 3; plazoDias = 1; } // Avanzado/Agudo
      }
      
      // Derivación CAE (70%) vs CHUC (30%)
      String circuito = (rand.nextDouble() < 0.70) ? "CAE" : "CHUC";
      
      // Modificadores de gravedad si va a CHUC y NO es glaucoma diagnosticado
      if (!esGlaucoma && circuito.equals("CHUC")) {
        double estratoCHUC = rand.nextDouble();
        if (estratoCHUC < 0.20) { gi = 2; plazoDias = 30; } // 20% Preferente
        else if (estratoCHUC < 0.25) { gi = 3; plazoDias = 1; } // 5% Urgente
      }
      
      // Añadimos prefijo al circuito si entra por urgencias para procesarlo luego
      if (entraPorUrgencias) circuito = "URG_" + circuito;
      
      int diMinutos = plazoDias * 330;
      lista.add(new Paciente(id, ti, gi, diMinutos, circuito));
    }
    return lista;
  }
  
  private static List<Cita> crearCitasParaPacientes(List<Paciente> pacientes, List<Estacion> estaciones,
                                                    List<Recurso> recursos, boolean paralela, int totalDias) {
    int finSimulacionCalculado = calcularMinutosOperativos(totalDias);
    List<Cita> listaCitas = new ArrayList<>();
    int idCita = 1;
    
    // Extraer estaciones para fácil acceso
    Estacion eUrgencias = estaciones.get(0);
    Estacion ePruebas = estaciones.get(1);
    Estacion eCae = estaciones.get(2);
    Estacion eChuc = estaciones.get(3);
    Estacion eGlaucoma = paralela ? estaciones.get(4) : null;
    
    // Buscadores de recursos (doctores físicos)
    List<Recurso> doctoresCAE = recursos.stream().filter(r -> r.getId().startsWith("R_CAE")).toList();
    List<Recurso> doctoresCHUC = recursos.stream().filter(r -> r.getId().startsWith("R_CHUC")).toList();
    List<Recurso> doctoresGlaucoma = paralela ? recursos.stream().filter(r -> r.getId().startsWith("R_GlaucoCAE")).toList() : null;
    
    // Buscadores de recursos (máquinas y triaje)
    Recurso rTriaje = recursos.stream().filter(r -> r.getId().equals("R_Triaje")).findFirst().get();
    Recurso rOct = recursos.stream().filter(r -> r.getId().equals("R_OCT")).findFirst().get();
    Recurso rCamp = recursos.stream().filter(r -> r.getId().equals("R_Camp")).findFirst().get();
    Recurso rTono = recursos.stream().filter(r -> r.getId().equals("R_Tono")).findFirst().get();
    Recurso rRetino = recursos.stream().filter(r -> r.getId().equals("R_Retino")).findFirst().get();
    
    for (Paciente p : pacientes) {
      // 1. TRIAJE (Solo para pacientes que entran por la vía de Urgencias)
      if (p.getCircuito().startsWith("URG_")) {
        listaCitas.add(new Cita("C-" + idCita++, p, eUrgencias, rTriaje, 20, false, finSimulacionCalculado));
      }
      
      // 2. PRUEBAS DIAGNÓSTICAS
      // Todos los pacientes de la agenda se hacen las pruebas básicas
      listaCitas.add(new Cita("C-" + idCita++, p, ePruebas, rTono, 3, false, finSimulacionCalculado));
      listaCitas.add(new Cita("C-" + idCita++, p, ePruebas, rRetino, 10, false, finSimulacionCalculado));
      
      // Solo el 8% (los identificados con sospecha/diagnóstico de glaucoma) consumen OCT y Campimetría
      if (p.getGi() > 0) {
        listaCitas.add(new Cita("C-" + idCita++, p, ePruebas, rOct, 10, false, finSimulacionCalculado));
        listaCitas.add(new Cita("C-" + idCita++, p, ePruebas, rCamp, 15, false, finSimulacionCalculado));
      }
      
      // 3. ENRUTAMIENTO MÉDICO (Árbol de decisión real)
      
      // CASO A: Agenda Paralela activa y el paciente pertenece al 8% de Glaucoma
      if (paralela && p.getGi() > 0) {
        // Se desvían directamente a la consulta monográfica liberando el CAE general
        Recurso docMonografico = doctoresGlaucoma.get(rand.nextInt(doctoresGlaucoma.size()));
        listaCitas.add(new Cita("C-" + idCita++, p, eGlaucoma, docMonografico, 15, true, finSimulacionCalculado)); // Diagnóstico final aquí (15 min)
      }
      // CASO B: Agenda Normal (O pacientes no-glaucoma en agenda paralela)
      else {
        // El 100% de estos pacientes van obligatoriamente primero al CAE (Primera consulta = 30 min)
        Recurso docCAE = doctoresCAE.get(rand.nextInt(doctoresCAE.size()));
        
        // Determinamos el destino según tu estadística: 30% se derivan, 70% se resuelven en el CAE
        boolean seDerivaAlCHUC = rand.nextDouble() < 0.30;
        
        if (seDerivaAlCHUC) {
          // SUB-CASO B.1: Se deriva (30%). La cita del CAE consumió tiempo pero NO dio el diagnóstico definitivo
          listaCitas.add(new Cita("C-" + idCita++, p, eCae, docCAE, 30, false, finSimulacionCalculado)); // esDiagnosticoFinal = false
          
          // El paciente viaja al hospital central (CHUC) para otra primera consulta de 30 min
          Recurso docCHUC = doctoresCHUC.get(rand.nextInt(doctoresCHUC.size()));
          listaCitas.add(new Cita("C-" + idCita++, p, eChuc, docCHUC, 30, true, finSimulacionCalculado)); // ¡Aquí SÍ obtiene su diagnóstico final!
          
          // Agenda de seguimiento: El 90% de los pacientes del CHUC se quedan en el sistema de revisiones
          if (rand.nextDouble() < 0.90) {
            // Ocupa un hueco de consulta sucesiva (15 min) con el mismo pool de doctores del CHUC
            listaCitas.add(new Cita("C-" + idCita++, p, eChuc, docCHUC, 15, false, finSimulacionCalculado)); // Seguimiento (no cuenta para T_diag)
          }
        } else {
          // SUB-CASO B.2: No se deriva (70%). El paciente se resuelve por completo en la periferia
          listaCitas.add(new Cita("C-" + idCita++, p, eCae, docCAE, 30, true, finSimulacionCalculado)); // ¡Su diagnóstico final se dio en el CAE!
          
          // Agenda de seguimiento: El 30% de los pacientes del CAE se quedan en el sistema de revisiones
          if (rand.nextDouble() < 0.30) {
            // Ocupa un hueco de consulta sucesiva (15 min) con el mismo pool de doctores del CHUC
            listaCitas.add(new Cita("C-" + idCita++, p, eCae, docCAE, 15, false, finSimulacionCalculado)); // Seguimiento (no cuenta para T_diag)
          }
        }
      }
    }
    return listaCitas;
  }
  
  public static void guardarInstancia(AgendaGlaucoma agenda, OpcionesSimulacion opt) {
    InstanciaProblema contenedor = new InstanciaProblema(
        agenda.getListaPacientes(),
        agenda.getListaRecursos(),
        agenda.getListaEstaciones(),
        agenda.getListaCitas()
    );
    
    try {
      Files.createDirectories(Paths.get("instancias"));
      
      String nombreArchivo = String.format("instancias/P%d_A%d_%s_%d.json",
          opt.cantidadPacientes(), opt.totalDias(),
          (opt.usarAgendaParalela() ? "PAR" : "STD"),
          opt.diasParalelaMes());
      
      new ObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValue(new File(nombreArchivo), contenedor);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}