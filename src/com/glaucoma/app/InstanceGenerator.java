package com.glaucoma.app;

import com.glaucoma.domain.*;

import java.util.ArrayList;
import java.util.List;

public class InstanceGenerator {
  public record Planning(List<Patient> patients, List<Appointment> appointments) {}

  private static final RouteGenerator routeGenerator = new RouteGenerator();
  private static final PatientGenerator patientGenerator = new PatientGenerator();
  private static final ClinicResourceFactory clinicResourceFactory = new ClinicResourceFactory();

  public static ProblemInstance generateInstance(int patientsQuantity, int totalDays) {
    return new ProblemInstance(patientGenerator.generatePatients(patientsQuantity, totalDays), totalDays);
  }

  public static GlaucomaSchedule generateProblem(OptimizerOptions options, List<Patient> patients) {
    List<Station> stations = clinicResourceFactory.createStations(options.useParallelSchedule());
    List<Resource> resources = clinicResourceFactory.createResources(options.useParallelSchedule());

    Planning planning = processAllRoutes(patients, stations, resources, options.useParallelSchedule(), options.parallelDaysAMonth());

    List<Appointment> appointments = planning.appointments;
    patients = planning.patients;

    return new GlaucomaSchedule(patients, resources, stations, appointments);
  }

  public static Planning processAllRoutes(List<Patient> patients, List<Station> stations,
                                          List<Resource> resources, boolean useParallelSchedule, int parallelDaysAMonth) {
    List<Appointment> allAppointmentsList = new ArrayList<>();

    for (Patient patient : patients) {
      List<Appointment> route = routeGenerator.generateRoute(patient, stations, resources, useParallelSchedule, parallelDaysAMonth, WorkingCalendar.calculateOperationalMinutes(patient.getTotalDays()));
      patient.setRoute(route);
      allAppointmentsList.addAll(route);
    }
    return new Planning(patients, allAppointmentsList);
  }
}
