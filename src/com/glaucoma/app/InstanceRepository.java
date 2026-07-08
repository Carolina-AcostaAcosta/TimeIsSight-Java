package com.glaucoma.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceRepository {
  private static final Logger logger = LoggerFactory.getLogger(InstanceRepository.class);

  public static ProblemInstance loadFromJSON(String filename) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(new File("instances/" + filename), ProblemInstance.class);
    } catch (IOException e) {
      System.err.println("Error al cargar la instancia: " + e.getMessage());
      return null;
    }
  }

  public static void saveInstance(ProblemInstance instance, OptimizerOptions options) {

    try {
      Files.createDirectories(Paths.get("instances"));

      String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

      String filename = String.format("instances/P%d_D%d_%s.json",
          options.patientsQuantity(), options.totalDays(), dateTime);

      new ObjectMapper().writerWithDefaultPrettyPrinter()
          .writeValue(new File(filename), instance);

    } catch (Exception e) {
      logger.error("Error al guardar la instancia.", e);
    }
  }
}
