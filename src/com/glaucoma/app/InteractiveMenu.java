package com.glaucoma.app;

import java.util.Scanner;

public class InteractiveMenu {
  private final Scanner scanner;
  
  public InteractiveMenu() {
    this.scanner = new Scanner(System.in);
  }
  
  public Object[] configureData() {
    System.out.println("\n--- ORIGEN DE DATOS ---");
    System.out.println("1. Generar nueva instancia de patients");
    System.out.println("2. Cargar instancia existente desde JSON");
    System.out.println("3. Salir");
    System.out.print("Elige una opción: ");
    
    String choice = scanner.nextLine();
    
    if (choice.equals("3")) {
      return null;
    }
    
    if (choice.equals("2")) {
      System.out.print("Nombre del archivo JSON (ej: P500_D365_2026...json): ");
      return new Object[]{"LOAD", scanner.nextLine()};
    } else {
      int patients = 200;
      int days = 45;
      
      try {
        System.out.print("Cantidad de patients a simular: ");
        patients = Integer.parseInt(scanner.nextLine());
        System.out.print("Cantidad de días del horizonte: ");
        days = Integer.parseInt(scanner.nextLine());
      } catch (Exception e) {
        System.out.println("Entrada inválida. Usando valores por defecto (" + patients + "patients, " + days + " días).");
      }
      return new Object[]{"NEW", patients, days};
    }
  }
}