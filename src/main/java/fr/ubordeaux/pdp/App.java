package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.ShellCommandRouter;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
  private static boolean verbose = Utils.DEFAULT_VERBOSE;

  /** Flag to enable debug mode. */
  private static boolean debug = Utils.DEFAULT_DEBUG;

  /** Flag to enable blitz mode. */
  private static boolean blitz = Utils.DEFAULT_BLITZ;

  /** Flag to set the time limit for each player's turn. */
  private static int time = Utils.DEFAULT_TIME;

  /** Flag to enable contest mode. */
  private static boolean contest = Utils.DEFAULT_CONTEST;

  /** Flag to set the board size. */
  private static int size = Utils.DEFAULT_BOARD_SIZE;

  /** Flag to enable white AI. */
  private static boolean whiteAi = false;

  /** Flag to enable black AI. */
  private static boolean blackAi = false;

  /** Flag to set the time limit for AI moves. */
  private static long aiTime = Ai.DEFAULT_MAX_TIME_MS;

  /** Flag to set the AI mode. */
  public static String aiMode = Utils.DEFAULT_AI_MODE;

  /** Flag to set the AI search depth. */
  private static int aiDepth = Ai.DEFAULT_DEPTH;

  /** Flag to set the selection mode for MCTS. */
  private static SelectionMode selectionMode = Mcts.DEFAULT_SELECTION_MODE;

  /**
   * Entry point of the application. Delegates logic to run() and handles exit
   * codes.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    int status = run(args);

    if (status == EXIT_INFO) {
      System.exit(0);
    } else if (status == EXIT_ERROR) {
      System.exit(1);
    }

    GameView view;

    if (status == EXIT_GUI) {
      view = new GraphicalUserInterface();
      GameController controller = new GameController(view);
      controller.start();
      controller.startNewGame(new Configuration(
          blitz, time, contest, size, verbose, debug,
          whiteAi, blackAi, aiTime, aiMode, aiDepth, selectionMode));
    } else {
      CommandLineInterface cli = new CommandLineInterface(verbose, debug);
      view = cli;

      GameController controller = new GameController(view);
      ClientSession session = new ClientSession();

      session.setController(controller);
      ShellCommandRouter router = new ShellCommandRouter(controller, session);
      cli.setRouter(router);

      ClientMode mode = session.getMode();
      boolean effectiveWhiteAi;
      boolean effectiveBlackAi;

      if (mode != ClientMode.LOCAL) {
        effectiveWhiteAi = false;
        effectiveBlackAi = false;
      } else {
        effectiveWhiteAi = whiteAi;
        effectiveBlackAi = blackAi;
      }

      controller.start();
      controller.startNewGame(new Configuration(
          blitz, time, contest, size, verbose, debug,
          effectiveWhiteAi, effectiveBlackAi, aiTime, aiMode, aiDepth, selectionMode));

      try {
        controller.joinGameLoop();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        System.exit(0);
      }
    }
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
    blitz = Utils.DEFAULT_BLITZ;
    time = Utils.DEFAULT_TIME;
    contest = configManager.isContest();
    size = configManager.getSize();
    debug = configManager.isDebug();

    // Options definition
    Options options = new Options();
    options.addOption("h", "help", false, Internationalization.get("opt.help"));
    options.addOption("V", "version", false, Internationalization.get("opt.version"));
    options.addOption("v", "verbose", false, Internationalization.get("opt.verbose"));
    options.addOption("d", "debug", false, Internationalization.get("opt.debug"));
    options.addOption("b", "blitz", false, Internationalization.get("opt.blitz"));
    options.addOption("t", "time", true, Internationalization.get("opt.time"));
    options.addOption("g", "gui", false, Internationalization.get("opt.gui"));
    // options.addOption("a", "ai", true, Internationalization.get("opt.ai"));
    Option aiOption = Option.builder("a")
        .longOpt("ai")
        .desc(Internationalization.get("opt.ai"))
        .hasArg()
        .optionalArg(true)
        .build();
    options.addOption(aiOption);
    options.addOption("c", "contest", true, "enable contest mode");
    options.addOption("s", "size", true, "set board size (8|10|12)");
    options.addOption("at", "ai-time", true, "set AI time limit in seconds");
    options.addOption("am", "ai-mode", true, "set AI mode (minimax|alphabeta|iterative|mcts)");
    options.addOption("ad", "ai-depth", true, "set AI search depth");
    options.addOption("as", "ai-mcts-selection", true, "set MCTS selection mode (uct|ml)");

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);

      if (!cmd.getArgList().isEmpty()) {
        throw new ParseException(Internationalization.get("app.error.unrecognized_arg")
            + cmd.getArgList());
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
        System.out.println(Internationalization.get("opt.verbose.status"));
        verbose = true;
      }

      if (cmd.hasOption("d")) {
        System.out.println(Internationalization.get("opt.debug.status"));
        debug = true;
      }

      if (cmd.hasOption("g")) {
        System.out.println(Internationalization.get("app.gui.launch"));
        return EXIT_GUI;
      }

      if (cmd.hasOption("b")) {
        System.out.println(Internationalization.get("opt.blitz.status"));
        blitz = true;
      }

      if (cmd.hasOption("t")) {
        time = Integer.parseInt(cmd.getOptionValue("t"));
        System.out.println(Internationalization.get("opt.time.status", time));
      }

      if (cmd.hasOption("c")) {
        System.out.println(Internationalization.get("opt.contest.status"));
        contest = true;
      }

      if (cmd.hasOption("s")) {
        size = Integer.parseInt(cmd.getOptionValue("s"));
        System.out.println(Internationalization.get("opt.size.status") + size + ".");
      }

      if (cmd.hasOption("a")) {
        String color = cmd.getOptionValue("a", "default").toUpperCase();
        if (color == null) {
          color = "W";
        }

        color = color.toUpperCase();
        switch (color) {
          case "W":
            whiteAi = true;
            break;
          case "B":
            blackAi = true;
            break;
          case "A":
            whiteAi = true;
            blackAi = true;
            break;
          case "":
            whiteAi = true;
            break;
          default:
            System.err.println(Internationalization.get("app.warn.invalid_ai_color") + color);
            whiteAi = true;
            break;
        }
      }

      if (cmd.hasOption("at")) {
        aiTime = Integer.parseInt(cmd.getOptionValue("at"));
        aiTime *= 1000; // Convert seconds to milliseconds
        System.out.println(Internationalization.get("opt.ai.time.status", aiTime));
      }

      if (cmd.hasOption("am")) {
        aiMode = cmd.getOptionValue("am");
        System.out.println(Internationalization.get("opt.ai.mode.status", aiMode));
      }

      if (cmd.hasOption("ad")) {
        aiDepth = Integer.parseInt(cmd.getOptionValue("ad"));
        System.out.println(Internationalization.get("opt.ai.depth.status", aiDepth));
      }

      if (cmd.hasOption("as")) {
        String selection = cmd.getOptionValue("as").toUpperCase();
        try {
          selectionMode = SelectionMode.valueOf(selection);
          System.out.println(Internationalization.get("opt.ai.selection.status", selectionMode));
        } catch (IllegalArgumentException e) {
          System.err.println(Internationalization.get("app.warn.invalid_ai_selection")
              + selection);
          selectionMode = Mcts.DEFAULT_SELECTION_MODE;
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
   * Checks if blitz mode is enabled.
   *
   * @return true if blitz is on.
   */
  public static boolean isBlitz() {
    return blitz;
  }

  /**
   * Returns the time limit for blitz mode.
   *
   * @return the time limit in seconds.
   */
  public static int getSize() {
    return size;
  }

  /**
   * Returns the time limit for blitz mode.
   *
   * @return the time limit in seconds.
   */
  public static int getTime() {
    return time;
  }

  /**
   * Checks if contest mode is enabled.
   *
   * @return true if contest mode is on.
   */
  public static boolean isContest() {
    return contest;
  }

  /**
   * Resets the global state. Essential for isolated unit tests.
   */
  public static void reset() {
    verbose = false;
    debug = false;
    blitz = false;
    time = Utils.DEFAULT_TIME;
    contest = false;
    size = Utils.DEFAULT_BOARD_SIZE;
    whiteAi = false;
    blackAi = false;
    aiTime = Ai.DEFAULT_MAX_TIME_MS;
    aiMode = Utils.DEFAULT_AI_MODE;
    aiDepth = Ai.DEFAULT_DEPTH;
    selectionMode = Mcts.DEFAULT_SELECTION_MODE;
  }
}