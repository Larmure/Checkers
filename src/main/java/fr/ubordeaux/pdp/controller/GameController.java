package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.controller.commands.ContinueCommand;
import fr.ubordeaux.pdp.controller.commands.HelpCommand;
import fr.ubordeaux.pdp.controller.commands.HintCommand;
import fr.ubordeaux.pdp.controller.commands.LoadCommand;
import fr.ubordeaux.pdp.controller.commands.NewCommand;
import fr.ubordeaux.pdp.controller.commands.PauseCommand;
import fr.ubordeaux.pdp.controller.commands.QuitCommand;
import fr.ubordeaux.pdp.controller.commands.RedoCommand;
import fr.ubordeaux.pdp.controller.commands.SaveCommand;
import fr.ubordeaux.pdp.controller.commands.SetCommand;
import fr.ubordeaux.pdp.controller.commands.ShowCommand;
import fr.ubordeaux.pdp.controller.commands.UndoCommand;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.view.GameView;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Orchestrator of the game logic and user interactions.
 *
 * <p>As the Controller in the MVC (Model-View-Controller) pattern, it mediates
 * between the {@link GameCheckers} model and the {@link GameView}. It interprets
 * user inputs as {@link Command} objects and updates the game state.
 *
 * @version 2.0
 */
public class GameController {

  /** The graphical or text-based interface used by the player. */
  private final GameView view;

  /** The core game engine containing rules and board state. */
  private GameCheckers game;

  /** The configuration options for the game. */
  private Configuration configuration;

  /** The timer for managing the blitz mode. */
  private Timer blitzTimer;

  /** History size at the time of the last save. */
  private int lastSavedMoveCount = 0;

  /**
   * Initializes the controller with the required model and view components.
   *
   * @param view the {@link GameView} instance
   */
  public GameController(GameView view) {
    this.view = view;
  }

  /**
   * Binds the view to this controller and launches the main application loop.
   */
  public void start() {
    view.setController(this);
    view.start();
  }

  /**
   * Dispatches a user command to its corresponding logic.
   *
   * @param commandName the name of the command
   * @param args optional arguments associated with the command
   */
  public void executeCommand(String commandName, String[] args) {
    Command command = buildCommand(commandName, args);

    if (command != null) {
      command.execute();
    }
  }

  /**
   * Builds a command object from the given command name and arguments.
   *
   * @param commandName the name of the command
   * @param args the command arguments
   * @return the matching {@link Command}, or {@code null} if unknown
   */
  private Command buildCommand(String commandName, String[] args) {
    return switch (commandName.toLowerCase()) {
      case "new" -> new NewCommand(this, args);
      case "help" -> new HelpCommand(args);
      case "quit" -> new QuitCommand(this);
      case "pause" -> new PauseCommand(blitzTimer, game);
      case "load" -> new LoadCommand(this, args);
      case "save" -> new SaveCommand(this, args);
      case "hint" -> new HintCommand();
      case "undo" -> new UndoCommand(this, args);
      case "redo" -> new RedoCommand(this, args);
      case "show" -> new ShowCommand(this, args);
      case "set" -> new SetCommand(this, args);
      case "continue" -> new ContinueCommand(game);
      default -> {
        System.out.println("Unknown command: " + commandName);
        yield null;
      }
    };
  }

  /**
   * Business logic trigger to initialize a fresh game session.
   *
   * @param configuration the configuration options for the new game
   */
  public void startNewGame(Configuration configuration) {
    System.out.println("Initializing new game with options: " + configuration);
    this.game = new GameCheckers(configuration);
    this.configuration = new Configuration(configuration);
    game.addObserver(view);

    System.out.println(Internationalization.get("game.rules"));

    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    displayBoard();
    markAsSaved();
  }

  /**
   * Executes a move in the game by applying the move to the model and checking
   * for game over conditions.
   *
   * @param from the starting position of the piece to move
   * @param to the target position to move the piece to
   */
  public void executeMove(String from, String to) {
    if (game == null || configuration == null) {
      System.out.println("No game is currently running.");
      return;
    }

    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    game.applyMove(from, to);
    game.setState(game.checkGameOver());

    if (game.getState().equals(State.FINISHED)) {
      if (configuration.isBlitz()) {
        stopBlitzTimer();
      }
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(
            game.getCurrentPlayer().getName() + " " + Internationalization.get("game.loses"));
      System.out.println(Internationalization.get("game.start_new_game"));
    }
  }

  /**
   * Displays the current state of the game board by delegating to the view.
   */
  public void displayBoard() {
    if (game == null) {
      System.out.println("No game is currently running.");
      return;
    }
    view.display(game);
  }

  /**
   * Displays the move history of the current game.
   */
  public void displayHistory() {
    if (game == null || game.getHistory() == null) {
      System.out.println("No game history available.");
      return;
    }
    System.out.println(game.getHistory().historyString());
  }

  /**
   * Displays the current game configuration settings.
   */
  public void displayConfiguration() {
    if (configuration == null) {
      System.out.println("No configuration available.");
      return;
    }
    System.out.println(configuration);
  }

  /**
   * Displays the remaining time for the current player if the game is in blitz mode.
   */
  public void displayTime() {
    if (game == null) {
      System.out.println("No game is currently running.");
      return;
    }

    if (isBlitz()) {
      int totalSeconds = game.getCurrentPlayer().getPlayTime();
      String formattedTime = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
      String template = Internationalization.get("game.time_remaining");

      System.out.println(
            String.format(template, game.getCurrentPlayer().getName(), formattedTime));
    } else {
      System.out.println(Internationalization.get("game.time_not_blitz"));
    }
  }

  /**
   * Starts the blitz timer for the current game.
   */
  private void startBlitzTimer() {
    stopBlitzTimer();

    blitzTimer = new Timer(true);
    blitzTimer.scheduleAtFixedRate(
          new TimerTask() {
            @Override
            public void run() {
              game.timerPlayer();

              if (game.getCurrentPlayer().getPlayTime() <= 0) {
                stopBlitzTimer();
                System.out.println(
                      "\n"
                            + Internationalization.get("game.time_up")
                            + game.getCurrentPlayer().getName());
                game.setState(State.FINISHED);
              }
            }
          },
          1000,
          1000);
  }

  /**
   * Stops the blitz timer if it is currently running.
   */
  public void stopBlitzTimer() {
    if (blitzTimer != null) {
      blitzTimer.cancel();
      blitzTimer = null;
    }
  }

  /**
   * Checks if verbose mode is enabled in the current configuration.
   *
   * @return {@code true} if verbose mode is enabled, {@code false} otherwise
   */
  public boolean isVerbose() {
    return configuration != null && configuration.isVerbose();
  }

  /**
   * Checks if debug mode is enabled in the current configuration.
   *
   * @return {@code true} if debug mode is enabled, {@code false} otherwise
   */
  public boolean isDebug() {
    return configuration != null && configuration.isDebug();
  }

  /**
   * Checks if the current game configuration is set to blitz mode.
   *
   * @return {@code true} if the game is in blitz mode, {@code false} otherwise
   */
  public boolean isBlitz() {
    return configuration != null && configuration.isBlitz();
  }

  /**
   * Checks if the white player is controlled by an AI.
   *
   * @return {@code true} if the white player is AI-controlled, {@code false} otherwise
   */
  public boolean isWhiteAi() {
    return configuration != null && configuration.iswhiteAi();
  }

  /**
   * Checks if the black player is controlled by an AI.
   *
   * @return {@code true} if the black player is AI-controlled, {@code false} otherwise
   */
  public boolean isBlackAi() {
    return configuration != null && configuration.isblackAi();
  }

  /**
   * Sets the debug mode in the current configuration.
   *
   * @param debug the new debug setting
   */
  public void setDebug(boolean debug) {
    if (configuration != null) {
      this.configuration = new Configuration(configuration, isVerbose(), debug);
    }
  }

  /**
   * Sets the verbose mode in the current configuration.
   *
   * @param verbose the new verbose setting
   */
  public void setVerbose(boolean verbose) {
    if (configuration != null) {
      this.configuration = new Configuration(configuration, verbose, isDebug());
    }
  }

  /**
   * Returns the current game instance.
   *
   * @return the current game instance
   */
  public GameCheckers getGame() {
    return game;
  }

  /**
   * Sets the current game instance and configuration based on a loaded game.
   *
   * @param loadedGame the loaded game state
   * @param cfg the configuration associated with the loaded game
   */
  public void setGame(GameCheckers loadedGame, Configuration cfg) {
    this.game = loadedGame;
    this.configuration = new Configuration(cfg);
    this.game.addObserver(view);
    displayBoard();
    markAsSaved();
  }

  /**
   * Plays a predefined move sequence for tests or demonstrations.
   */
  /**
   * Joue une séquence de coups prédéfinie pour des tests ou une démo.
   */
  public void playPredefinedSequence() {
    String[][] moves = {
        { "c1", "d2" }, { "f4", "e3" }, { "d2", "f4" }, { "g5", "e3" },
        { "b2", "c1" }, { "e3", "d2" }, { "c1", "e3" }, { "f2", "b2" },
        { "a1", "c3" }, { "f6", "e5" }, { "c3", "d4" }, { "e5", "c3" },
        { "b4", "d2" }, { "g3", "f2" }, { "d2", "e3" }, { "f2", "d4" },
        { "c5", "e3" }, { "g1", "f2" }, { "e3", "g1" }, { "h2", "g3" },
        { "g1", "h2" }, { "h4", "g5" }, { "h2", "e5" }, { "g5", "f4" },
        { "e5", "g3" }, { "f8", "e7" }, { "c7", "d6" }, { "e7", "c5" },
        { "b6", "d4" }, { "g7", "f8" }, { "d4", "e5" }, { "f8", "e7" },
        { "e5", "f4" }, { "h6", "g5" }, { "f4", "h6" }, { "h8", "g7" },
        { "h6", "d6" }
    };

    System.out.println("Début de la séquence d'automatisation des coups...");

    for (String[] move : moves) {
      String from = move[0];
      String to = move[1];

      System.out.println("Coup joué : " + from + "-" + to);

      executeMove(from, to);

      if (game.getState() == State.FINISHED) {
        System.out.println("La partie s'est terminée avant la fin de la séquence.");
        break;
      }
    }

    System.out.println("Séquence terminée.");
  }


  /**
   * Undoes the last n moves in the game.
   *
   * @param n the number of moves to undo
   */
  public void undoGame(int n) {
    if (game == null) {
      return;
    }

    for (int i = 0; i < n; i++) {
      game.undoManage();
    }
  }

  /**
   * Redoes the last n undone moves in the game.
   *
   * @param n the number of moves to redo
   */
  public void redoGame(int n) {
    if (game == null) {
      return;
    }

    for (int i = 0; i < n; i++) {
      game.redoManage();
    }
  }

  /**
   * Checks if the current game state has modifications since the last save.
   *
   * @return {@code true} if there are unsaved moves
   */
  public boolean hasUnsavedChanges() {
    if (game == null || game.getHistory() == null) {
      return false;
    }

    return game.getHistory().getSize() != lastSavedMoveCount;
  }

  /**
   * Updates the save tracker to the current history size.
   */
  public void markAsSaved() {
    if (game != null && game.getHistory() != null) {
      lastSavedMoveCount = game.getHistory().getSize();
    }
  }
}