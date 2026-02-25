package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Internationalization;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import fr.ubordeaux.pdp.view.GameView;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

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

  /** Exit code for GUI. */
  public static final int EXIT_GUI = 3;

  /** Flag to enable verbose. */
  private static boolean verbose = false;

  /** Flag to enable debug mode. */
  private static boolean debug = false;

  /** Flag to enable white AI. */
  private static boolean whiteIsAI = false;

  /** Flag to enable black AI. */
  private static boolean blackIsAI = false;

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
    } else if (status == EXIT_GUI) {
      //TODO
    }
    
    // Status EXIT_SUCCESS means continue execution normally
    GameCheckers game = new GameCheckers(whiteIsAI, blackIsAI);
    System.out.println("Player White type: " + game.getWhitePlayer().getClass().getSimpleName());
    System.out.println("Player Black type: " + game.getBlackPlayer().getClass().getSimpleName());
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
    Internationalization.init();
    ConfigManager configManager = new ConfigManager();
    configManager.load();
    verbose = configManager.isVerbose();
    // Options definition
    Options options = new Options();
    options.addOption("h", "help", false, Internationalization.get("opt.help"));
    options.addOption("V", "version", false, Internationalization.get("opt.version"));
    options.addOption("v", "verbose", false, Internationalization.get("opt.verbose"));
    options.addOption("d", "debug", false, Internationalization.get("opt.debug"));
    options.addOption("b", "blitz", false, Internationalization.get("opt.blitz"));
    options.addOption("t", "time", true, Internationalization.get("opt.time"));
    options.addOption("g", "gui", false, Internationalization.get("opt.gui"));
    //options.addOption("a", "ai", true, Internationalization.get("opt.ai"));
    Option aiOption = Option.builder("a")
      .longOpt("ai")
      .desc(Internationalization.get("opt.ai"))
      .hasArg()
      .optionalArg(true)
      .build();
    options.addOption(aiOption);
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);

      if (!cmd.getArgList().isEmpty()) {
        throw new ParseException(Internationalization.get("app.error.unrecognized_arg") + cmd.getArgList());
      }

      if (cmd.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("checkers", options);
        return EXIT_INFO;
      }

      if (cmd.hasOption("V")) {
        System.out.println(Internationalization.get("app.version"));
        return EXIT_INFO;
      }

      if (cmd.hasOption("v")) {
        verbose = true;
      }

      if (cmd.hasOption("d")) {
        debug = true;
      }

      if (cmd.hasOption("g")) {
        System.out.println(Internationalization.get("app.gui.launch"));
        return EXIT_GUI;
      }

      if (cmd.hasOption("a")) {
        String color = cmd.getOptionValue("a", "default").toUpperCase();
        if (color == null) {
          color = "W";
        }

        color = color.toUpperCase();
        if (color.equals("W")) whiteIsAI = true;
        else if (color.equals("B")) blackIsAI = true;
        else if (color.equals("A")) {
          whiteIsAI = true;
          blackIsAI = true;
        } else {
          whiteIsAI = true; 
        }
      }
      System.out.println(Internationalization.get("app.welcome"));
      return EXIT_SUCCESS;

    } catch (ParseException e) {
      String message;

      if (e instanceof UnrecognizedOptionException) {
          String opt = ((UnrecognizedOptionException) e).getOption();
          message = Internationalization.get("app.error.unrecognized_option") + opt;
      } else if (e instanceof MissingArgumentException) {
          org.apache.commons.cli.Option optionObj = ((MissingArgumentException) e).getOption();
          String optName = optionObj.getOpt(); 
          message = Internationalization.get("app.error.missing_arg") + optName;
      } else {
          message = e.getMessage(); 
      }
      System.err.println(message);
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