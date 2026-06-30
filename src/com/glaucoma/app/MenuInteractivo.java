package com.glaucoma.app;

import java.util.Scanner;

public class MenuInteractivo {
  private final Scanner scanner;
  
  public MenuInteractivo() {
    this.scanner = new Scanner(System.in);
  }
  
  public Object[] configurarDatos() {
    System.out.println("\n--- ORIGEN DE DATOS ---");
    System.out.println("1. Generar nueva instancia de pacientes");
    System.out.println("2. Cargar instancia existente desde JSON");
    System.out.println("3. Salir");
    System.out.print("Elige una opción: ");
    
    String eleccion = scanner.nextLine();
    
    if (eleccion.equals("3")) {
      return null;
    }
    
    if (eleccion.equals("2")) {
      System.out.print("Nombre del archivo JSON (ej: P500_D365_2026...json): ");
      return new Object[]{"LOAD", scanner.nextLine()};
    } else {
      int pacientes = 200;
      int dias = 45;
      
      try {
        System.out.print("Cantidad de pacientes a simular: ");
        pacientes = Integer.parseInt(scanner.nextLine());
        System.out.print("Cantidad de días del horizonte: ");
        dias = Integer.parseInt(scanner.nextLine());
      } catch (Exception e) {
        System.out.println("Entrada inválida. Usando valores por defecto (" + pacientes + "pacientes, " + dias + " días).");
      }
      return new Object[]{"NEW", pacientes, dias};
    }
  }
}