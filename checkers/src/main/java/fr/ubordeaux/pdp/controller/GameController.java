package fr.ubordeaux.pdp.controller;

import java.util.Timer;
import java.util.TimerTask;

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

/**
 * Orchestrator of the game logic and user interactions.
 * As the Controller in the MVC (Model-View-Controller) pattern, it mediates
 * between the {@link GameCheckers} model and the {@link GameView}.
 * It interprets user inputs as {@link Command} objects and updates the game
 * state.
 *
 * @version 1.0
 */
public class GameController {

  /** The graphical or text-based interface used by the player. */
  private final GameView view;

  /** The core game engine containing rules and board state. */
  private GameCheckers game;

  private Configuration configuration;

  private Timer blitzTimer;

  /** History size at the time of the last save.*/
  private int lastSavedMoveCount = 0;

  /**
   * Initializes the controller with the required model and view components.
   *
   * @param view The {@link GameView} instance (View).
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
   * This method uses a modern switch expression to map command names to
   * {@link Command} implementations.
   *
   * @param commandName The name of the command (e.g., "new", "quit").
   * @param args        Optional arguments associated with the command.
   */
  public void executeCommand(String commandName, String[] args) {
    Command command = switch (commandName.toLowerCase()) {
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
        System.out.println("Unknown command: " 
          + commandName);
        yield null;
      }
    };

    if (command != null) {
      command.execute();
    }
  }

  /**
   * Business logic trigger to initialize a fresh game session.
   * 
   * @param blitz   Whether the blitz mode (fast-paced) is enabled.
   * @param contest Whether the contest mode (tournament rules) is enabled.
   * @param time    The time limit per player in seconds (0 for no limit).
   * @param size    The board dimension (standard is 8).
   */
  public void startNewGame(Configuration configuration) {
    System.out.println("Initializing new game with options: " + configuration);
    this.game = new GameCheckers(configuration);
    this.configuration = new Configuration(configuration);
    game.addObserver(view);

    String rules = Internationalization.get("game.rules");
    System.out.println(rules);

    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    displayBoard();
    markAsSaved();
  }

  public void executeMove(String from, String to) {
    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    game.applyMove(from, to);

    game.setState(game.checkGameOver());
    if (game.getState().equals(State.FINISHED)) {
      if (configuration.isBlitz())
        stopBlitzTimer();
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(game.getCurrentPlayer().getName() + " " + Internationalization.get("game.loses"));
      System.out.println(Internationalization.get("game.start_new_game"));
    }
    
  }

  public void displayBoard() {
    view.display(game);
  }

  public void displayConfiguration() {
    System.out.println(configuration);
  }

  public void displayTime() {
    if (isBlitz()) {
      int totalSeconds = game.getCurrentPlayer().getPlayTime();
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;

      String formattedTime = String.format("%02d:%02d", minutes, seconds);

      String template = Internationalization.get("game.time_remaining");

      System.out.println(String.format(template, game.getCurrentPlayer().getName(), formattedTime));
    } else {
      System.out.println(Internationalization.get("game.time_not_blitz"));
    }
  }

  private void startBlitzTimer() {
    stopBlitzTimer();
    blitzTimer = new Timer(true); // daemon = s'arrête avec le programme
    blitzTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        // 1. Mise à jour de la logique uniquement (pas d'affichage constant)
        game.timerPlayer();

        int totalSeconds = game.getCurrentPlayer().getPlayTime();

        // 2. On intervient dans la console UNIQUEMENT si le temps est écoulé
        if (totalSeconds <= 0) {
          stopBlitzTimer();
          System.out.println("\n" + Internationalization.get("game.time_up") + game.getCurrentPlayer().getName());
          game.setState(State.FINISHED);
        }
      }
    }, 1000, 1000);
  }

  public void stopBlitzTimer() {
    if (blitzTimer != null) {
      blitzTimer.cancel();
      blitzTimer = null;
    }
  }

  public boolean isVerbose() {
    return configuration.isVerbose();
  }

  public boolean isDebug() {
    return configuration.isDebug();
  }

  public void setDebug(boolean debug) {
    this.configuration = new Configuration(configuration, isVerbose(), debug);
  }

  public void setVerbose(boolean verbose) {
    this.configuration = new Configuration(configuration, verbose, isDebug());
  }

  public GameCheckers getGame() {
    return game;
  }

  public void setGame(GameCheckers loadedGame, Configuration cfg) {
    
    this.game = loadedGame;
    this.configuration = new Configuration(cfg);
    this.game.addObserver(view);
    displayBoard();
    markAsSaved();
  }

  public boolean isWhiteIsAi() {
    return configuration.isWhiteIsAI();
  }

  public boolean isBlackIsAi() {
    return configuration.isBlackIsAI();
  }

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

  public void undoGame(int n) {
    for (int i = 0; i < n; i++) {
      game.undoManage();
    }
  }

  public void redoGame(int n) {
    for (int i = 0; i < n; i++) {
      game.redoManage();
    }
  }

  public boolean isBlitz() {
    return configuration.isBlitz();
  }

  /**
   * Checks if the current game state has modifications since the last save.
   * It compares the current history size with the history size at the last save.
   *
   * @return true if there are unsaved moves.
   */
  public boolean hasUnsavedChanges() {
    if (game == null || game.getHistory() == null) {
      return false;
    }
    int currentSize = game.getHistory().getSize();
    return currentSize != lastSavedMoveCount;
  }

  /**
   * Updates the save tracker to the current history size.
   */
  public void markAsSaved() {
    if (game != null && game.getHistory() != null) {
      this.lastSavedMoveCount = game.getHistory().getSize();
    }
  }
}