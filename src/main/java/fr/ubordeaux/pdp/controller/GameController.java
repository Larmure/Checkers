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
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
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

  /** Dedicated thread running the main game loop for the CLI. */
  private Thread gameLoopThread;

  /** Flag controlling the lifecycle of the main game loop. */
  private volatile boolean gameLoopRunning;

  /** Flag indicating if the game ended due to time expiration in blitz mode. */
  private boolean timeExpired = false;

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
    startGameLoop();
  }

  /**
   * Starts the controller-driven game loop when the active view is interactive.
   */
  private void startGameLoop() {
    if (!(view instanceof CommandLineInterface) || gameLoopRunning) {
      return;
    }

    gameLoopRunning = true;
    gameLoopThread = new Thread(this::runGameLoop, "checkers-game-loop");
    gameLoopThread.start();
  }

  /**
   * Runs the main game loop.
   * If the current player is a human, the loop waits for input.
   * If the current player is an AI, the loop plays the AI move automatically.
   */
  private void runGameLoop() {
    CommandLineInterface cli = (CommandLineInterface) view;

    while (gameLoopRunning) {
      if (game == null) {
        sleepBriefly();
        continue;
      }

      if (game.getState() != State.IN_GAME || !(game.getCurrentPlayer() instanceof AiPlayer)) {
        String input = cli.readInput();
        if (input == null) {
          gameLoopRunning = false;
          break;
        }
        cli.handleInput(input);
        continue;
      }

      playAiTurn((AiPlayer) game.getCurrentPlayer());
    }

    stopBlitzTimer();
  }

  /**
   * Waits for the game loop to finish.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public void joinGameLoop() throws InterruptedException {
    if (gameLoopThread != null) {
      gameLoopThread.join();
    }
  }

  /**
   * Sleeps briefly when the loop has no active game to process.
   */
  private void sleepBriefly() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      gameLoopRunning = false;
    }
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
      case "hint" -> new HintCommand(this);
      case "undo" -> new UndoCommand(this, args);
      case "redo" -> new RedoCommand(this, args);
      case "show" -> new ShowCommand(this, args);
      case "set" -> new SetCommand(this, args);
      case "continue" -> new ContinueCommand(this);
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

    displayBoard();

    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    markAsSaved();

    // Trigger the AI immediately if the White player (starting player) is an AI.
    triggerAiIfNecessary();
  }

  /**
   * Executes a move in the game by applying the move to the model and checking
   * for game over conditions.
   *
   * @param from The starting position of the piece to move (e.g., "A3").
   * @param to The target position to move the piece to (e.g., "B4").
   * @param isManoury A boolean indicating whether the move is a Manoury move (capture)
   *     or a regular move.
   */
  public void executeMove(String from, String to, boolean isManoury) {
    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    game.applyMove(from, to, isManoury);

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
    handleGameOver();

    // Trigger the AI turn if the next player is controlled by the computer.
    triggerAiIfNecessary();
  }

  /**
   * Plays one move for the current AI player.
   *
   * @param aiPlayer the AI player that must play
   */
  private void playAiTurn(AiPlayer aiPlayer) {
    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    Move move = aiPlayer.getBestMove(game.getManagerUndoRedo(),
        game.getBoard(), game.getCurrentColor());

    if (move == null) {
      System.err.println("No IA move");
      game.setState(game.checkGameOver());
      handleGameOver();
      return;
    }

    String from = game.getBoard().indexToSquare(move.getFrom());
    String to = game.getBoard().indexToSquare(move.getTo());
    game.applyMove(from, to, false);
    game.setState(game.checkGameOver());
    handleGameOver();
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
  /**
   * Displays the remaining time for both players if the game is in blitz mode.
   */
  public void displayTime() {
    if (isBlitz()) {
      String template = Internationalization.get("game.time_remaining");

      int whiteTotalSeconds = game.getWhitePlayer().getPlayTime();
      int whiteMinutes = whiteTotalSeconds / 60;
      int whiteSeconds = whiteTotalSeconds % 60;
      String whiteFormattedTime = String.format("%02d:%02d", whiteMinutes, whiteSeconds);

      int blackTotalSeconds = game.getBlackPlayer().getPlayTime();
      int blackMinutes = blackTotalSeconds / 60;
      int blackSeconds = blackTotalSeconds % 60;
      String blackFormattedTime = String.format("%02d:%02d", blackMinutes, blackSeconds);

      System.out.println(String.format(template, game.getWhitePlayer().getName(),
          whiteFormattedTime));
      System.out.println(String.format(template, game.getBlackPlayer().getName(),
          blackFormattedTime));
    } else {
      System.out.println(Internationalization.get("game.time_not_blitz"));
    }
  }
  
  /**
   * Starts the blitz timer for the current game. 
   * This method initializes a new Timer that schedules a task to run every second. 
   * The task updates the game logic related to player timing and checks if the current player's 
   * time has run out. If the time is up, it stops the timer, sets the game state to FINISHED, 
   * and notifies observers of the state change.
   */
  public void startBlitzTimer() {
    stopBlitzTimer();

    if (game == null) {
      return;
    }

    blitzTimer = new Timer(true);
    blitzTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (game.getState() == State.PAUSE) {
          return;
        }
        // Update game timing logic
        game.timerPlayer();

        // Notify observers to update UI (both CLI and GUI)
        game.notifyObservers();

        int totalSeconds = game.getCurrentPlayer().getPlayTime();
        // Check if time has run out
        if (totalSeconds <= 0) {
          stopBlitzTimer();
          game.setState(State.FINISHED);
          timeExpired = true;
          handleGameOver();
          timeExpired = false;
          game.notifyObservers();
        }
      }
    }, 1000, 1000);
  }

  /**
   * Stops the blitz timer if it is currently running.
   */
  public void stopBlitzTimer() {
    if (blitzTimer != null) {
      blitzTimer.cancel();
      this.blitzTimer.purge();
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
    if (game.getState() == State.FINISHED) {
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
      lastSavedMoveCount = game.getHistory().getSize();
    }
  }

  /**
   * Handles the game over state by checking if the game has finished and displaying appropriate
   * messages to the user.
   */
  public void handleGameOver() {
    if (game.getState().equals(State.FINISHED)) {
      if (configuration.isBlitz()) {
        stopBlitzTimer();
      }
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(game.getCurrentPlayer().getName() + " "
          + Internationalization.get("game.loses"));
      System.out.println(Internationalization.get("game.start_new_game"));

      if (view instanceof GraphicalUserInterface gui) {
        gui.showGameOverAlert(game, timeExpired);
      }
    }
  }

  /**
   * Checks if the current player is an AI and triggers their turn asynchronously.
   *
   * <p>This prevents blocking the JavaFX application thread during the AI's calculation,
   * ensuring the GUI remains responsive.
   */
  public void triggerAiIfNecessary() {
    // Safeguard: Only execute this asynchronous behavior if the view is a graphical user interface.
    if (view instanceof CommandLineInterface) {
      return;
    }

    if (game.getState() == State.IN_GAME && game.getCurrentPlayer() instanceof AiPlayer) {
      AiPlayer aiPlayer = (AiPlayer) game.getCurrentPlayer();

      new Thread(() -> {
        // A brief pause to improve UX and prevent instant moves.
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }

        // Calculate the best move within the configured maximum aiTime.
        Move move = aiPlayer.getBestMove(
            game.getManagerUndoRedo(), game.getBoard(), game.getCurrentColor());

        // Switch back to the JavaFX Application Thread to safely update the UI components.

        try {
          javafx.application.Platform.runLater(() -> {
            if (move == null) {
              System.err.println("No AI move available.");
              game.setState(game.checkGameOver());
              handleGameOver();
              return;
            }

            if (configuration.isBlitz()) {
              startBlitzTimer();
            }

            String fromSquare = game.getBoard().indexToSquare(move.getFrom());
            String toSquare = game.getBoard().indexToSquare(move.getTo());

            // Apply the calculated move to the game board.
            game.applyMove(fromSquare, toSquare, false);

            game.setState(game.checkGameOver());
            handleGameOver();

            // Recursively call to check if the next player is also an AI (AI vs AI match).
            triggerAiIfNecessary();
          });
        } catch (IllegalStateException e) {
          // Ignore when JavaFX toolkit is not initialized during non-GUI tests.
        }
      }, "AI-Thinking-Thread").start();
    }
  }

  /**
   * Forwards a hint to the active view.
   *
   * @param from The starting position of the suggested move (e.g., "A3").
   * @param to The target position of the suggested move (e.g., "B4").
   */
  public void displayHint(String from, String to) {
    view.showHint(from, to);
  }
}