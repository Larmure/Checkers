package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.ShellCommandRouter;
import fr.ubordeaux.pdp.controller.bridge.ContestAnalysisBridge;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.LogisticRegressionTrainer;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.GameControllerFactory;
import fr.ubordeaux.pdp.server.GameServer;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.view.HeadlessView;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import java.io.IOException;
import java.util.List;
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

  /** Exit code for training. */
  public static final int EXIT_TRAINING = 4;

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
  private static String aiMode = Utils.DEFAULT_AI_MODE;

  /** Flag to set the white AI mode. */
  private static String whiteAiMode = Utils.DEFAULT_AI_MODE;

  /** Flag to set the black AI mode. */
  private static String blackAiMode = Utils.DEFAULT_AI_MODE;

  /** Flag to set the AI search depth. */
  private static int aiDepth = Ai.DEFAULT_DEPTH;

  /** Flag to set the selection mode for MCTS. */
  private static SelectionMode selectionMode = Mcts.DEFAULT_SELECTION_MODE;

  /** Flag to set the evaluation function for Minimax-family AI. */
  private static String minimaxScoring = Utils.DEFAULT_MINIMAX_SCORING;

  /** Flag indicating whether GUI mode was requested on the CLI. */
  private static boolean guiMode = false;
  /** Flag to start in server mode. */
  private static boolean serverMode = false;

  /** TCP port for server mode. */
  private static int serverPort = 12345;

  /** Flag to start server in daemon mode. */
  private static boolean daemonMode = false;

  /** Flag to set the number of games for training. */
  private static int numGames = LogisticRegressionTrainer.DEFAULT_NUM_GAMES;

  /** Optional save file path provided as positional CLI argument. */
  private static String startupSaveFile = null;

  /**
   * Entry point of the application. Delegates logic to run() and handles exit
   * codes.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    int status = run(args);

    switch (status) {
      case EXIT_INFO:
        System.exit(0);
        break;
      case EXIT_ERROR:
        System.exit(1);
        break;
      case EXIT_TRAINING:
        LogisticRegressionTrainer.lauchTraining(numGames);
        System.exit(0);
        break;
      default:
        break;
    }

    if (serverMode) {
      GameControllerFactory factory = () -> new GameController(new HeadlessView());
      GameServer server = new GameServer("GameServer", serverPort, factory, daemonMode, guiMode);

      Runtime.getRuntime().addShutdownHook(
          new Thread(
              () -> {
                System.out.println("\nShutting down server...");
                server.stop();
              }));

      if (daemonMode) {
        System.out.println("Server running in daemon mode.");
      } else {
        System.out.println("Server running in server mode.");
      }

      Thread serverThread = new Thread(
          () -> {
            try {
              server.start();
            } catch (IOException e) {
              System.err.println("Failed to start server: " + e.getMessage());
            }
          },
          "game-server-main");
      serverThread.setDaemon(false);
      serverThread.start();
      return;
    }

    if (status == EXIT_SUCCESS && ContestAnalysisBridge.runIfRequested(contest, startupSaveFile)) {
      return;
    }

    GameView view;

    ClientSession session = new ClientSession();
    session.setGuiMode(guiMode);
    if (status == EXIT_GUI) {
      view = new GraphicalUserInterface(new Configuration(blitz, time, contest,
          size, verbose, debug, whiteAi, blackAi, aiTime,
          whiteAiMode, blackAiMode, aiDepth, selectionMode, minimaxScoring), serverMode, session);
      view = new GraphicalUserInterface(
          new Configuration(
              blitz,
              time,
              contest,
              size,
              verbose,
              debug,
              whiteAi,
              blackAi,
              aiTime,
              aiMode,
              aiDepth,
              selectionMode),
          serverMode,
          session);
    } else {
      view = new CommandLineInterface(verbose, debug);
    }
    GameController controller = new GameController(view);

    session.setController(controller);
    ShellCommandRouter router = new ShellCommandRouter(controller, session);

    if (view instanceof CommandLineInterface) {
      ((CommandLineInterface) view).setRouter(router);
    }

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

    boolean loadedFromCliArg = false;
    if (startupSaveFile != null && !startupSaveFile.isBlank()) {
      controller.executeCommand("load", new String[] { startupSaveFile });
      loadedFromCliArg = controller.getGame() != null;
    }

    if (status != EXIT_GUI) {
      if (!loadedFromCliArg) {
        controller.startNewGame(new Configuration(blitz, time, contest,
            size, verbose, debug, effectiveWhiteAi, effectiveBlackAi, aiTime,
            whiteAiMode, blackAiMode, aiDepth, selectionMode, minimaxScoring));
      }
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
    startupSaveFile = null;

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
    Option serverOption = Option.builder()
        .longOpt("server")
        .hasArg()
        .optionalArg(true)
        .desc("start server on optional port")
        .build();

    Option daemonOption = Option.builder()
        .longOpt("daemon")
        .desc("start server in headless mode")
        .build();

    options.addOption(serverOption);
    options.addOption(daemonOption);
    Option aiOption = Option.builder("a")
        .longOpt("ai")
        .desc(Internationalization.get("opt.ai"))
        .hasArg()
        .optionalArg(true)
        .build();
    options.addOption(aiOption);
    Option contestOption = Option.builder("c")
        .longOpt("contest")
        .desc("enable contest mode")
        .hasArg()
        .optionalArg(true)
        .build();
    options.addOption(contestOption);
    options.addOption("s", "size", true, "set board size (8|10|12)");
    options.addOption("at", "ai-time", true, "set AI time limit in seconds");
    options.addOption("am", "ai-mode", true, "set AI mode (minimax|alphabeta|iterative|mcts)");
    options.addOption("wam", "white-ai-mode", true,
        "set white AI mode (minimax|alphabeta|iterative|mcts)");
    options.addOption("bam", "black-ai-mode", true,
        "set black AI mode (minimax|alphabeta|iterative|mcts)");
    options.addOption("ad", "ai-minimax-depth", true,
        "set Minimax search depth (if omitted, depth is auto-selected from ai-time)");
    options.addOption("as", "ai-mcts-selection", true, "set MCTS selection mode (uct|ml)");
    options.addOption("ams", "ai-minimax-scoring", true,
        "set Minimax scoring (simple|advanced|max)");
    options.addOption("tr", "train", true, "train a ML selection function");

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, normalizeArgs(args));

      List<String> positionalArgs = cmd.getArgList();
      if (positionalArgs.size() > 1) {
        throw new ParseException(Internationalization.get("app.error.unrecognized_arg")
            + positionalArgs);
      }
      if (positionalArgs.size() == 1) {
        startupSaveFile = positionalArgs.get(0).trim();
      }

      if (cmd.hasOption("server")) {
        serverMode = true;
        String portValue = cmd.getOptionValue("server");
        if (portValue != null) {
          try {
            serverPort = Integer.parseInt(portValue);
          } catch (NumberFormatException e) {
            System.err.println("Invalid server port: " + portValue);
            return EXIT_ERROR;
          }
        }
      }

      if (cmd.hasOption("daemon")) {
        daemonMode = true;
        serverMode = true;
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
        guiMode = true;
        if (!serverMode) {
          System.out.println(Internationalization.get("app.gui.launch"));
        } else {
          System.out.println("Server will host GUI-only games.");
        }
      }

      if (cmd.hasOption("b")) {
        System.out.println(Internationalization.get("opt.blitz.status"));
        blitz = true;
      }

      if (cmd.hasOption("t")) {
        try {
          time = Integer.parseInt(cmd.getOptionValue("t"));
          System.out.println(Internationalization.get("opt.time.status", time));
        } catch (NumberFormatException e) {
          System.out.println(Internationalization.get("app.warn.invalid_number",
              cmd.getOptionValue("t")));
          System.out.println(Internationalization.get("app.warn.changed",
              "time", Utils.DEFAULT_TIME));

          time = Utils.DEFAULT_TIME;
        }
      }

      if (cmd.hasOption("c")) {
        System.out.println(Internationalization.get("opt.contest.status"));
        contest = true;

        String contestArg = cmd.getOptionValue("c");
        if (contestArg != null && !contestArg.isBlank()) {
          String value = contestArg.trim();
          if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            startupSaveFile = value;
          }
        }
      }

      if (cmd.hasOption("s")) {
        try {
          size = Integer.parseInt(cmd.getOptionValue("s"));
          System.out.println(Internationalization.get("opt.size.status") + size + ".");
        } catch (NumberFormatException e) {
          System.out.println(Internationalization.get("app.warn.invalid_number",
              cmd.getOptionValue("s")));
          System.out.println(Internationalization.get("app.warn.changed", "size",
              Utils.DEFAULT_BOARD_SIZE));
          size = Utils.DEFAULT_BOARD_SIZE;
        }
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
        try {
          aiTime = Long.parseLong(cmd.getOptionValue("at")) * 1000;
          System.out.println(Internationalization.get("opt.ai.time.status", aiTime / 1000));
        } catch (NumberFormatException e) {
          System.out.println(Internationalization.get("app.warn.invalid_number",
              cmd.getOptionValue("at")));
          System.out.println(Internationalization.get("app.warn.changed",
              "AI time", Ai.DEFAULT_MAX_TIME_MS / 1000));
          aiTime = Ai.DEFAULT_MAX_TIME_MS;
        }
      }

      if (cmd.hasOption("am")) {
        aiMode = cmd.getOptionValue("am");
        whiteAiMode = aiMode;
        blackAiMode = aiMode;
        System.out.println(Internationalization.get("opt.ai.mode.status", aiMode));
      }

      if (cmd.hasOption("wam")) {
        whiteAiMode = cmd.getOptionValue("wam");
        System.out.println("White AI mode set to " + whiteAiMode + ".");
      }

      if (cmd.hasOption("bam")) {
        blackAiMode = cmd.getOptionValue("bam");
        System.out.println("Black AI mode set to " + blackAiMode + ".");
      }

      if (cmd.hasOption("ad") || cmd.hasOption("ai-minimax-depth")) {
        String depthArg = cmd.getOptionValue("ad");
        if (depthArg == null) {
          depthArg = cmd.getOptionValue("ai-minimax-depth");
        }
        try {
          aiDepth = Integer.parseInt(depthArg);
          System.out.println(Internationalization.get("opt.ai.depth.status", aiDepth));
        } catch (NumberFormatException e) {
          System.out.println(Internationalization.get("app.warn.invalid_number",
              depthArg));
          System.out.println(Internationalization.get("app.warn.changed",
              "AI depth", Ai.DEFAULT_DEPTH));
          aiDepth = Ai.DEFAULT_DEPTH;
        }
      } else {
        aiDepth = Ai.suggestDepthFromTime(aiTime);
      }

      if (cmd.hasOption("ai-minimax-scoring")) {
        String scoring = cmd.getOptionValue("ai-minimax-scoring").toLowerCase();
        if (Utils.VALID_MINIMAX_SCORINGS.contains(scoring)) {
          minimaxScoring = scoring;
          System.out.println("Minimax scoring set to " + minimaxScoring + ".");
        } else {
          System.out.println("Warning: Invalid Minimax scoring: " + scoring);
          System.out.println("Value of minimax scoring changed to: "
              + Utils.DEFAULT_MINIMAX_SCORING);
          minimaxScoring = Utils.DEFAULT_MINIMAX_SCORING;
        }
      }

      if (cmd.hasOption("as")) {
        String selection = cmd.getOptionValue("as").toUpperCase();
        try {
          selectionMode = SelectionMode.valueOf(selection);
          System.out.println(Internationalization.get("opt.ai.selection.status", selectionMode));
        } catch (IllegalArgumentException e) {
          System.out.println(Internationalization.get("app.warn.invalid_ai_selection")
              + selection);
          selectionMode = Mcts.DEFAULT_SELECTION_MODE;
        }
      }

      if (cmd.hasOption("tr")) {
        try {
          numGames = Integer.parseInt(cmd.getOptionValue("tr"));
          System.out.println(Internationalization.get("opt.train.status", numGames));
        } catch (NumberFormatException e) {
          System.out.println(Internationalization.get("app.warn.invalid_number",
              cmd.getOptionValue("tr")));
          System.out.println(Internationalization.get("app.warn.changed",
              "number of games", LogisticRegressionTrainer.DEFAULT_NUM_GAMES));
          numGames = LogisticRegressionTrainer.DEFAULT_NUM_GAMES;
        }
        return EXIT_TRAINING;
      }

      System.out.println(Internationalization.get("app.welcome"));
      if (serverMode) {
        return EXIT_SUCCESS;
      }
      return guiMode ? EXIT_GUI : EXIT_SUCCESS;

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
    guiMode = false;
    time = Utils.DEFAULT_TIME;
    contest = false;
    size = Utils.DEFAULT_BOARD_SIZE;
    whiteAi = false;
    blackAi = false;
    aiTime = Ai.DEFAULT_MAX_TIME_MS;
    aiMode = Utils.DEFAULT_AI_MODE;
    whiteAiMode = Utils.DEFAULT_AI_MODE;
    blackAiMode = Utils.DEFAULT_AI_MODE;
    aiDepth = Ai.DEFAULT_DEPTH;
    selectionMode = Mcts.DEFAULT_SELECTION_MODE;
    minimaxScoring = Utils.DEFAULT_MINIMAX_SCORING;
    startupSaveFile = null;
    serverMode = false;
    serverPort = 12345;
    daemonMode = false;
  }

  /**
   * Returns the save file provided as positional startup argument, if any.
   *
   * @return startup save file path or null
   */
  public static String getStartupSaveFile() {
    return startupSaveFile;
  }

  /**
   * Returns the maximum time allowed for AI moves.
   *
   * @return AI time limit in milliseconds.
   */
  public static long getAiTime() {
    return aiTime;
  }

  /**
   * Returns the configured search depth for AI algorithms.
   *
   * @return AI search depth.
   */
  public static int getAiDepth() {
    return aiDepth;
  }

  /**
   * Checks whether white is controlled by an AI player.
   *
   * @return true if white AI is enabled.
   */
  public static boolean isWhiteAi() {
    return whiteAi;
  }

  /**
   * Checks whether black is controlled by an AI player.
   *
   * @return true if black AI is enabled.
   */
  public static boolean isBlackAi() {
    return blackAi;
  }

  /**
   * Returns the configured Minimax scoring function.
   *
   * @return Minimax scoring function name.
   */
  public static String getMinimaxScoring() {
    return minimaxScoring;
  }

  /**
   * Returns the selection strategy used by the MCTS AI.
   *
   * @return active MCTS selection mode.
   */
  public static SelectionMode getSelectionMode() {
    return selectionMode;
  }

  /**
   * Normalizes legacy CLI aliases before parsing options.
   *
   * @param args original command line arguments
   * @return normalized arguments compatible with Commons CLI
   */
  private static String[] normalizeArgs(String[] args) {
    String[] normalized = args.clone();
    for (int i = 0; i < normalized.length; i++) {
      if ("-contest".equals(normalized[i])) {
        normalized[i] = "--contest";
      }
    }
    return normalized;
  }

  /**
   * Returns the configured AI mode for white player.
   *
   * @return white AI mode.
   */
  public static String getWhiteAiMode() {
    return whiteAiMode;
  }

  /**
   * Returns the configured AI mode for black player.
   *
   * @return black AI mode.
   */
  public static String getBlackAiMode() {
    return blackAiMode;
  }

  /**
   * Returns the configured AI mode for both players (if set via --ai) or the default AI mode.  
   *
   * @return AI mode.
   */
  public static String getAiMode() {
    return aiMode;
  }
}