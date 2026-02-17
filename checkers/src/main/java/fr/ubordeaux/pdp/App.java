package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import fr.ubordeaux.pdp.view.GameView;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main class for the Checkers game. Handles command line arguments and
 * initializes the game modes.
 *
 * @version 1.0
 */
public class App {

  /** Exit code for success. */
  public static final int EXIT_SUCCESS = 0;

  /** Exit code for Help or Version. */
  public static final int EXIT_INFO = 1;

  /** Exit code error. */
  public static final int EXIT_ERROR = 2;

  /** Flag to enable verbose. */
  private static boolean verbose = false;

  /** Flag to enable debug mode. */
  private static boolean debug = false;

  /**
   * Entry point of the application. Delegates logic to run() and handles exit
   * codes.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    int status = run(args);

    // Status handling
    if (status == EXIT_INFO) {
      System.exit(0);
    } else if (status == EXIT_ERROR) {
      System.exit(1);
    }

    // Status EXIT_SUCCESS means continue execution normally
    GameCheckers game = new GameCheckers();
    GameView view = new CommandLineInterface(verbose, debug);
    GameController controller = new GameController(game, view);
    controller.start();
  }

  /**
   * Parses arguments and sets global flags. This method is separated for unit
   * testing purposes to
   * avoid System.exit().
   *
   * @param args command line arguments: -h/--help, -V/--version, -v/--verbose,
   *             -d/--debug.
   * @return EXIT_SUCCESS to continue, EXIT_INFO to stop (info displayed),
   *         EXIT_ERROR for error.
   */
  public static int run(String[] args) {
    ConfigManager configManager = new ConfigManager();
    configManager.load();
    verbose = configManager.isVerbose();
    // Options definition
    Options options = new Options();
    options.addOption("h", "help", false, "display help");
    options.addOption("V", "version", false, "display version");
    options.addOption("v", "verbose", false, "increase verbosity");
    options.addOption("d", "debug", false, "display debug messages");
    options.addOption("b", "blitz", false, "enable blitz mode");
    options.addOption("t", "time", true, "set time limit in minutes");

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("checkers", options);
        return EXIT_INFO;
      }

      if (cmd.hasOption("V")) {
        System.out.println("checkers version 1.0");
        return EXIT_INFO;
      }

      if (cmd.hasOption("v")) {
        verbose = true;
      }

      if (cmd.hasOption("d")) {
        debug = true;
        System.out.println("Debug mode enabled.");
      }

      if (verbose) {
        System.out.println("Verbose mode enabled.");
      }
      System.out.println("Welcome to Checkers!");
      return EXIT_SUCCESS;

    } catch (ParseException e) {
      System.err.println("Error: " + e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("checkers", options);
      return EXIT_ERROR;
    }
  }

  /**
   * Checks if verbose mode is enabled.
   *
   * @return true if verbose is on.
   */
  public static boolean isVerbose() {
    return verbose;
  }

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug is on.
   */
  public static boolean isDebug() {
    return debug;
  }

  /**
   * Resets the global state. Essential for isolated unit tests.
   */
  public static void reset() {
    verbose = false;
    debug = false;
  }
}