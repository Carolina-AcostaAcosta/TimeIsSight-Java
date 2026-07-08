package com.glaucoma.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorkingCalendar {
  // Cachea el mapeo de días laborables por totalDays, porque ahora se invoca desde las
  // restricciones del solver (una vez por cada Appointment de diagnóstico final evaluada), y
  // recalcularlo desde cero cada vez sería muy costoso.
  private static final java.util.Map<Integer, List<Integer>> cacheDaysMapping = new java.util.concurrent.ConcurrentHashMap<>();

  // Genera el mapeo: Índice de día de trabajo -> Día real del año (0 a 364)
  public static List<Integer> generateDaysMapping(int totalDays) {
    return cacheDaysMapping.computeIfAbsent(totalDays, WorkingCalendar::calculateDaysMapping);
  }

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
    Random randCalendar = new Random(42); // Semilla fija para que coincida el calendario en toda la ejecución

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

  public static int calculateOperationalMinutes(int totalDays) {
    return generateDaysMapping(totalDays).size() * 330; // 330 minutos útiles por día hábil
  }

  // Método útil para traducir minutos de simulación a días de calendario reales
  public static int getCalendarDay(int operationalMinute, int totalDays) {
    int operationalDay = operationalMinute / 330;
    List<Integer> map = generateDaysMapping(totalDays);
    if (operationalDay >= map.size()) return totalDays - 1;
    return map.get(operationalDay);
  }
}
