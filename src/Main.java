import com.glaucoma.app.OptimizationRunner;

/**
 * Punto de entrada de la aplicación.
 * Delega toda la orquestación del programa en {@link OptimizationRunner}.
 */
public class Main {
  /**
   * Arranca la aplicación.
   *
   * @param args argumentos de línea de comandos (ver {@link com.glaucoma.app.CLIConfiguration})
   */
  public static void main(String[] args) {
    new OptimizationRunner().run(args);
  }
}
