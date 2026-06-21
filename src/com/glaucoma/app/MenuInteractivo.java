package com.glaucoma.app;

import java.util.Scanner;

public class MenuInteractivo {
  private final Scanner scanner;
  
  public MenuInteractivo() {
    this.scanner = new Scanner(System.in);
  }
  
  // Menú de nivel 1: Carga o Nueva
  public int mostrarMenuPrincipal() {
    System.out.println("\n--- MENÚ PRINCIPAL ---");
    System.out.println("1. Crear instancia nueva");
    System.out.println("2. Cargar instancia existente");
    System.out.println("3. Salir");
    System.out.print("Elige una opción: ");
    try {
      return Integer.parseInt(scanner.nextLine());
    } catch (NumberFormatException e) { return 0; }
  }
  
  public Object[] elegirModo(int eleccion) {
    
    if (eleccion == 2) {
      System.out.print("Introduce el nombre del archivo JSON (ej: P500_A365_PAR_2.json): ");
      return new Object[]{"LOAD", scanner.nextLine()};
    } else if (eleccion == 1){
      OpcionesSimulacion ops = solicitarParametrosNuevos();
      return (ops == null) ? new Object[]{"CANCEL", null} : new Object[]{"NEW", ops};
    } else {
      return new Object[]{"EXIT", null};
    }
  }
  
  // Devuelve null si el usuario decide salir, o las opciones si decide continuar
  public OpcionesSimulacion solicitarParametrosNuevos() {
    System.out.println("\n--- CONFIGURACIÓN NUEVA INSTANCIA ---");
    System.out.println("1. Simulación estándar");
    System.out.println("2. Simulación con agenda paralela");
    System.out.println("3. Volver al menú principal");
    System.out.print("Elige una opción: ");
    String opcion = scanner.nextLine();
    
    if (opcion.equals("3")) {
      return null;
    }
    
    boolean usarAgendaParalela = false;
    int diasParalelaMes = 0;
    
    if (opcion.equals("2")) {
      usarAgendaParalela = true;
      System.out.print("¿Cuántos días al mes tendrá la agenda paralela? (2, 3 o 4): ");
      try {
        diasParalelaMes = Integer.parseInt(scanner.nextLine());
        if (diasParalelaMes < 2 || diasParalelaMes > 4) diasParalelaMes = 2;
      } catch (NumberFormatException e) {
        diasParalelaMes = 2;
      }
    } else if (!opcion.equals("1")) {
      System.out.println("Opción no reconocida. Inténtalo de nuevo.");
      return solicitarParametrosNuevos(); // Llamada recursiva si se equivoca
    }
    
    try {
      System.out.print("Introduce la cantidad de pacientes a simular (ej. 500): ");
      int cantidadPacientes = Integer.parseInt(scanner.nextLine());
      
      System.out.print("Introduce la cantidad de días del horizonte de la agenda (ej. 365): ");
      int totalDias = Integer.parseInt(scanner.nextLine());
      
      return new OpcionesSimulacion(usarAgendaParalela, diasParalelaMes, cantidadPacientes, totalDias);
    } catch (NumberFormatException e) {
      System.out.println("Error: Introduce números enteros válidos. Volviendo al menú...");
      return solicitarParametrosNuevos();
    }
  }
}