package com.glaucoma.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.glaucoma.app.GeneradorInstancias;
import com.glaucoma.domain.Cita;

public class GlaucomaConstraintProvider implements ConstraintProvider {
  
  private static final java.util.Map<String, Integer> cacheDiasCirugia = new java.util.concurrent.ConcurrentHashMap<>();
  private static final java.util.Set<String> DOCTORES_SUSTITUIDOS_CAE =
      java.util.Set.of("R_CAE_2", "R_CAE_4", "R_CAE_6");
  private static final java.util.Set<String> DOCTORES_TRASLADADOS_CHUC =
      java.util.Set.of("R_CHUC_32", "R_CHUC_33", "R_CHUC_34");
  
  private int obtenerDiaCirugiaAsignado(String recursoId) {
    return cacheDiasCirugia.computeIfAbsent(recursoId, id -> {
      if (id.startsWith("R_CHUC_")) {
        int idDoc = Integer.parseInt(id.replace("R_CHUC_", ""));
        return idDoc % 5;
      }
      return -1; // -1 si no es del CHUC
    });
  }
  
  @Override
  public Constraint[] defineConstraints(ConstraintFactory factory) {
    return new Constraint[]{
        pruebasMismoCentroMismoDia(factory),
        precedenciaPruebasConsulta(factory),
        capacidadRecursos(factory),
        plazoCriticoDiagnostico(factory),
        minimizarTiempoDiagnostico(factory),
        atenderSoloTrasLlegada(factory),
        citasMismoDiaNoUrgentes(factory),
        margenTransporteUrgentes(factory),
        cirugiasSemanalesMedicosHUC(factory),
        logicaDisponibilidadAgendaParalela(factory)
    };
  }
  
  private Constraint pruebasMismoCentroMismoDia(ConstraintFactory factory) {
    return factory.forEachUniquePair(Cita.class, Joiners.equal(Cita::getPaciente))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> c1.isPrueba() && c2.isPrueba())
        .filter((c1, c2) -> c1.getCentro().equals(c2.getCentro())) // Solo si ambas son del CAE, o ambas del HUC
        .filter((c1, c2) -> (c1.getT() / 330) != (c2.getT() / 330)) // Si están en días distintos -> penalizar
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 1000)
        .asConstraint("Pruebas del mismo centro deben ser en el mismo día");
  }
  
  // 1. RESTRICCIÓN: Las pruebas diagnósticas deben ocurrir antes que la consulta
  // Matemática: Tpruebasi + pi, pruebas <= Tconsultai
  private Constraint precedenciaPruebasConsulta(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(citaPrueba -> citaPrueba.getT() != null && citaPrueba.isPrueba())
        .join(Cita.class,
            Joiners.equal(Cita::getPaciente),
            Joiners.equal(Cita::getCentro)) // Cruzamos prueba CAE con consulta CAE (o HUC con HUC)
        .filter((prueba, consulta) -> consulta.getT() != null && !consulta.isPrueba())
        // Si el día de la prueba es IGUAL (mismo día) o MAYOR (después) que la consulta -> penalizar
        .filter((prueba, consulta) -> (prueba.getT() / 330) >= (consulta.getT() / 330))
        .penalize(HardSoftScore.ONE_HARD, (prueba, consulta) -> 1000)
        .asConstraint("Pruebas deben hacerse un día distinto y anterior a la consulta");
  }
  
  // 2. RESTRICCIÓN: Capacidad de Recursos (No solapamiento en el mismo minuto t)
  // Matemática: sum(sum(pij * xijt)) <= crt
  private Constraint capacidadRecursos(ConstraintFactory factory) {
    return factory.forEachUniquePair(Cita.class,
            Joiners.equal(Cita::getRecurso), // Mismo recurso
            Joiners.overlapping(Cita::getT, Cita::getEndMinute) // Solapamiento temporal
        )
        .penalize(HardSoftScore.ONE_HARD, ((c1, c2) -> 1000))
        .asConstraint("Capacidad de recurso excedida");
  }
  
  // 3. RESTRICCIÓN (INFACTIBILIDAD): Penalización por superar el plazo crítico máximo
  private Constraint plazoCriticoDiagnostico(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(cita -> cita.getT() != null && cita.esCitaDiagnosticoFinal())
        // Condición de quiebre: Supera el plazo permitido di
        .filter(cita -> {
          int totalDias = cita.getPaciente().getTotalDias();
          int diaRealLlegada = GeneradorInstancias.obtenerDiaCalendario(cita.getPaciente().getTi(), totalDias);
          int diaRealCita = GeneradorInstancias.obtenerDiaCalendario(cita.getEndMinute(), totalDias);
          
          int diasEsperaReal = diaRealCita - diaRealLlegada;
          int diasPlazoCritico = cita.getPaciente().getDi() / 330;
          
          return diasEsperaReal > diasPlazoCritico;
        })
        .penalize(HardSoftScore.ONE_SOFT, cita -> {
          int totalDias = cita.getPaciente().getTotalDias();
          int diaRealLlegada = GeneradorInstancias.obtenerDiaCalendario(cita.getPaciente().getTi(), totalDias);
          int diaRealCita = GeneradorInstancias.obtenerDiaCalendario(cita.getEndMinute(), totalDias);
          int diasEsperaReal = diaRealCita - diaRealLlegada;
          int diasPlazoCritico = cita.getPaciente().getDi() / 330;
          return diasEsperaReal - diasPlazoCritico;
        })
        .asConstraint("Plazo crítico superado (Infactibilidad Médica)");
  }
  
  // 4. FUNCIÓN OBJETIVO: Minimizar el tiempo total de diagnóstico
  private Constraint minimizarTiempoDiagnostico(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(cita -> cita.getT() != null && cita.esCitaDiagnosticoFinal())
        .filter(cita -> cita.getEndMinute() > cita.getPaciente().getTi())
        .penalize(HardSoftScore.ONE_SOFT, cita -> {
          int totalDias = cita.getPaciente().getTotalDias();
          int diaRealLlegada = GeneradorInstancias.obtenerDiaCalendario(cita.getPaciente().getTi(), totalDias);
          int diaRealCita = GeneradorInstancias.obtenerDiaCalendario(cita.getEndMinute(), totalDias);
          return diaRealCita - diaRealLlegada;
        })
        .asConstraint("Minimizar tiempo total de diagnóstico en días reales");
  }
  
  // 5. RESTRICCIÓN ADICIONAL: Un paciente no puede ser atendido antes de llegar al hospital
  private Constraint atenderSoloTrasLlegada(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(cita -> cita.getT() != null)
        .filter(cita -> cita.getT() < cita.getPaciente().getTi())
        .penalize(HardSoftScore.ONE_HARD, cita -> cita.getPaciente().getTi() - cita.getT())
        .asConstraint("Atención prematura antes de la llegada");
  }
  
  // RESTRICCIÓN: Pacientes estables NO pueden tener dos citas el mismo día operativo
  private Constraint citasMismoDiaNoUrgentes(ConstraintFactory factory) {
    return factory.forEachUniquePair(Cita.class, Joiners.equal(Cita::getPaciente))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> {
          boolean esUrgente = c1.getPaciente().getGi() == 4;
          return !esUrgente;
        })
        .filter((c1, c2) -> !(c1.isPrueba() && c2.isPrueba()))
        .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo bloque de día
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 1000)
        .asConstraint("Citas el mismo día para paciente no urgente");
  }
  
  // RESTRICCIÓN: Pacientes urgentes pueden coincidir el mismo día pero con margen de transporte (30 min)
  private Constraint margenTransporteUrgentes(ConstraintFactory factory) {
    return factory.forEachUniquePair(Cita.class, Joiners.equal(Cita::getPaciente))
        .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
        .filter((c1, c2) -> c1.getPaciente().getGi() == 4)
        .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo día
        .filter((c1, c2) -> {
          Cita primero = (c1.getT() < c2.getT()) ? c1 : c2;
          Cita segundo = (primero == c1) ? c2 : c1;
          return segundo.getT() < (primero.getEndMinute() + 30); // Margen de 30 minutos de traslado
        })
        .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 500)
        .asConstraint("Margen de transporte insuficiente en urgencias");
  }
  
  private Constraint cirugiasSemanalesMedicosHUC(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(cita -> cita.getT() != null && cita.getRecurso().id().startsWith("R_CHUC_"))
        .filter(cita -> {
          int diaCirugiaAsignado = obtenerDiaCirugiaAsignado(cita.getRecurso().id());
          
          // Calculamos en qué día de la semana cae la cita actual
          int diaSemanaActual = (cita.getT() / 330) % 5;
          
          // Si coincide su día de quirófano con la cita de consulta -> penalización dura
          return diaSemanaActual == diaCirugiaAsignado;
        })
        .penalize(HardSoftScore.ONE_HARD, cita -> 3000)
        .asConstraint("Médico del HUC ausente por jornada de cirugía");
  }
  
  private Constraint logicaDisponibilidadAgendaParalela(ConstraintFactory factory) {
    return factory.forEach(Cita.class)
        .filter(cita -> cita.getT() != null)
        .filter(cita -> {
          String rId = cita.getRecurso().id();
          boolean esDiaParalela = cita.isDiaParalela();
          
          // Caso A: Si NO es día de agenda paralela, ningún médico monográfico puede pasar consulta
          if (!esDiaParalela && rId.startsWith("R_GlaucoCAE_")) {
            return true;
          }
          
          // Caso B: Si SÍ es día de agenda paralela, aplicamos las sustituciones y traslados
          if (esDiaParalela) {
            // 1. Se sustituye a un médico de cada CAE (dejamos inactivos a los médicos pares de los CAE)
            if (DOCTORES_SUSTITUIDOS_CAE.contains(rId)) {
              return true;
            }
            
            // 2. Tres médicos específicos del HUC (ej.: los 3 últimos, del 32 al 34) se han trasladado,
            // por lo que dejasen de estar disponibles en el Hospital Central esa jornada
            return DOCTORES_TRASLADADOS_CHUC.contains(rId);
          }
          return false;
        })
        .penalize(HardSoftScore.ONE_HARD, cita -> 3000)
        .asConstraint("Infracción de disponibilidad por traslados de Agenda Paralela");
  }
}