package com.glaucoma.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import com.glaucoma.domain.Cita;

public class GlaucomaConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                precedenciaPruebasConsulta(factory),
                capacidadRecursos(factory),
                plazoCriticoDiagnostico(factory),
                minimizarTiempoDiagnostico(factory),
                atenderSoloTrasLlegada(factory),
                citasMismoDiaNoUrgentes(factory),
                margenTransporteUrgentes(factory)
        };
    }

    // 1. RESTRICCIÓN: Las pruebas diagnósticas deben ocurrir antes que la consulta
    // Matemática: Tpruebasi + pi,pruebas <= Tconsultai
    private Constraint precedenciaPruebasConsulta(ConstraintFactory factory) {
        return factory.forEach(Cita.class)
                .filter(cita -> cita.getEstacion().esPrueba()) // Filtramos solo citas de pruebas (E2)
                .join(Cita.class,
                        Joiners.equal(Cita::getPaciente) // Que pertenezcan al mismo paciente
                )
                .filter((prueba, consulta) -> consulta.getEstacion().esConsulta()) // La segunda cita es la consulta
                // Condición de quiebre: Si la consulta empieza ANTES de que termine la prueba
                .filter((prueba, consulta) -> consulta.getT() != null && prueba.getEndMinute() > consulta.getT())
                .penalize(HardSoftScore.ONE_HARD) // Rompe la viabilidad de la agenda
                .asConstraint("Precedencia Pruebas-Consulta");
    }

    // 2. RESTRICCIÓN: Capacidad de Recursos (No solapamiento en el mismo minuto t)
    // Matemática: sum(sum(pij * xijt)) <= crt
    private Constraint capacidadRecursos(ConstraintFactory factory) {
        return factory.forEach(Cita.class)
                .filter(cita -> cita.getT() != null)
                // Cruzamos cada cita con cualquier otra cita...
                .join(Cita.class,
                        Joiners.equal(Cita::getRecurso), //... Que use el mismo recurso médico
                        //... Y cuyos horarios de atención se solapen en el tiempo
                        Joiners.overlapping(Cita::getT, Cita::getEndMinute)
                )
                // Evitamos comparar una cita consigo misma
                .filter((cita1, cita2) -> !cita1.getId().equals(cita2.getId()))
                .penalize(HardSoftScore.ONE_HARD) // Dos pacientes no pueden usar el mismo recurso a la vez
                .asConstraint("Capacidad de recurso excedida");
    }

    // 3. RESTRICCIÓN (INFACTIBILIDAD): Penalización por superar el plazo crítico máximo
    private Constraint plazoCriticoDiagnostico(ConstraintFactory factory) {
        return factory.forEach(Cita.class)
                .filter(Cita::esCitaDiagnosticoFinal)
                .filter(cita -> cita.getT() != null)
                // CORRECCIÓN: Nos aseguramos de que la cita ocurra DESPUÉS de la llegada
                .filter(cita -> cita.getEndMinute() > cita.getPaciente().getTi())
                // Condición de quiebre: Supera el plazo permitido di
                .filter(cita -> (cita.getEndMinute() - cita.getPaciente().getTi()) > cita.getPaciente().getDi())
                .penalize(HardSoftScore.ONE_SOFT, cita -> {
                    int tiempoTotal = cita.getEndMinute() - cita.getPaciente().getTi();
                    return tiempoTotal - cita.getPaciente().getDi();
                })
                .asConstraint("Plazo crítico superado (Infactibilidad Médica)");
    }

    // 4. FUNCIÓN OBJETIVO: Minimizar el tiempo total de diagnóstico
    private Constraint minimizarTiempoDiagnostico(ConstraintFactory factory) {
        return factory.forEach(Cita.class)
                .filter(Cita::esCitaDiagnosticoFinal)
                .filter(cita -> cita.getT() != null)
                // CORRECCIÓN: Solo penalizamos si el tiempo transcurrido es lógico (positivo)
                .filter(cita -> cita.getEndMinute() > cita.getPaciente().getTi())
                .penalize(HardSoftScore.ONE_SOFT, cita -> cita.getEndMinute() - cita.getPaciente().getTi())
                .asConstraint("Minimizar tiempo total de diagnóstico");
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
                    boolean esUrgente = c1.getPaciente().getGi() == 3 || c1.getPaciente().getCircuito().startsWith("URG_");
                    return !esUrgente;
                })
                .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo bloque de día
                .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 1000)
                .asConstraint("Citas el mismo día para paciente no urgente");
    }

    // RESTRICCIÓN: Pacientes urgentes pueden coincidir el mismo día pero con margen de transporte (30 min)
    private Constraint margenTransporteUrgentes(ConstraintFactory factory) {
        return factory.forEachUniquePair(Cita.class, Joiners.equal(Cita::getPaciente))
                .filter((c1, c2) -> c1.getT() != null && c2.getT() != null)
                .filter((c1, c2) -> c1.getPaciente().getGi() == 3 || c1.getPaciente().getCircuito().startsWith("URG_"))
                .filter((c1, c2) -> (c1.getT() / 330) == (c2.getT() / 330)) // Mismo día
                .filter((c1, c2) -> {
                    Cita primero = (c1.getT() < c2.getT()) ? c1 : c2;
                    Cita segundo = (primero == c1) ? c2 : c1;
                    return segundo.getT() < (primero.getEndMinute() + 30); // Margen de 30 minutos de traslado
                })
                .penalize(HardSoftScore.ONE_HARD, (c1, c2) -> 500)
                .asConstraint("Margen de transporte insuficiente en urgencias");
    }
}