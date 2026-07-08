package com.glaucoma.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Traduce entre minutos de simulación y días reales de calendario, descontando fines de
 * semana y festivos. El mapeo se cachea por horizonte de días porque se invoca desde las
 * restricciones del solver (una vez por cada {@link Appointment} de diagnóstico final evaluada),
 * y recalcularlo desde cero cada vez sería muy costoso.
 */
public class WorkingCalendar {
  private static final java.util.Map<Integer, List<Integer>> cacheDaysMapping = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Genera (o recupera del caché) el mapeo: índice de día de trabajo -> día real del año (0 a 364).
   *
   * @param totalDays duración del horizonte de planificación, en días naturales
   * @return la lista de días reales que son laborables, en orden
   */
  public static List<Integer> generateDaysMapping(int totalDays) {
    return cacheDaysMapping.computeIfAbsent(totalDays, WorkingCalendar::calculateDaysMapping);
  }

  // Calcula el mapeo de días laborables descontando fines de semana y una cantidad
  // proporcional de festivos, con semilla fija para que el calendario sea estable.
  private static List<Integer> calculateDaysMapping(int totalDays) {
    List<Integer> workingCalendarDays = new ArrayList<>();
    List<Integer> potentialWorkingDays = new ArrayList<>();

    for (int day = 0; day < totalDays; day++) {
      int weekday = day % 7;
      if (weekday != 5 && weekday != 6) {
        potentialWorkingDays.add(day);
      }
    }

    int holidaysQuantity = (int) Math.round((totalDays / 365.0) * 13);
    Random randCalendar = new Random(42);

    List<Integer> holidays = new ArrayList<>();
    while (holidays.size() < Math.min(holidaysQuantity, potentialWorkingDays.size())) {
      int maybeHoliday = potentialWorkingDays.get(randCalendar.nextInt(potentialWorkingDays.size()));
      if (!holidays.contains(maybeHoliday)) {
        holidays.add(maybeHoliday);
      }
    }

    for (int day : potentialWorkingDays) {
      if (!holidays.contains(day)) {
        workingCalendarDays.add(day);
      }
    }
    return workingCalendarDays;
  }

  /**
   * Calcula los minutos hábiles totales disponibles en el horizonte de planificación.
   *
   * @param totalDays duración del horizonte de planificación, en días naturales
   * @return los minutos hábiles totales (330 minutos por día laborable)
   */
  public static int calculateOperationalMinutes(int totalDays) {
    return generateDaysMapping(totalDays).size() * 330; // 330 minutos útiles por día hábil
  }

  /**
   * Traduce un minuto operativo de la simulación a su día real de calendario (0-364).
   *
   * @param operationalMinute minuto de simulación a traducir
   * @param totalDays         duración del horizonte de planificación, en días naturales
   * @return el día real de calendario correspondiente
   */
  public static int getCalendarDay(int operationalMinute, int totalDays) {
    int operationalDay = operationalMinute / 330;
    List<Integer> map = generateDaysMapping(totalDays);
    if (operationalDay >= map.size()) return totalDays - 1;
    return map.get(operationalDay);
  }
}
