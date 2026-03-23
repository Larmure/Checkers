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
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Orchestrator of the game logic and user interactions.
 * As the Controller in the MVC (Model-View-Controller) pattern, it mediates
 * between the {@link GameCheckers} model and the {@link GameView}.
 * It interprets user inputs as {@link Command} objects and updates the game
 * state.
 *
 * <p>This version also supports server-related commands such as *
 * {@code server list}, {@code server start}, and {@code server stop}.
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
      case "continue" -> new ContinueCommand(this);
      case "server" -> resolveServerCommand(args);
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
   * Resolves a server subcommand from the provided arguments.
   *
   * @param args the arguments passed after {@code server}
   * @return the matching command, or {@code null} if the input is invalid
   */

  private Command resolveServerCommand(String[] args) {
    if (args == null || args.length == 0) {
      System.out.println("Usage: server list | server start [PORT] | server stop");
      return null;
    }

    String subCommand = args[0].toLowerCase();
    String[] subArgs = new String[args.length - 1];
    System.arraycopy(args, 1, subArgs, 0, subArgs.length);

    return switch (subCommand) {
      case "list" -> new ServerListCommand();
      case "start" -> new ServerStartCommand(this, subArgs);
      case "stop" -> new ServerStopCommand();
      default -> {
        System.out.println("Unknown server command: " + subCommand
            + ". Use: list | start [PORT] | stop");
        yield null;
      }
    };
  }

  /**
   * Business logic trigger to initialize a fresh game session.
   *
   * @param configuration The configuration options for the new game.
   */
  public void startNewGame(Configuration configuration) {
    System.out.println("Initializing new game with options: " + configuration);
    this.game = new GameCheckers(configuration);
    this.configuration = new Configuration(configuration);
    game.addObserver(view);

    String rules = Internationalization.get("game.rules");
    System.out.println(rules);

    displayBoard();

    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    markAsSaved();
  }

  /**
   * Executes a move in the game by applying the move to the model and checking for game over 
   * conditions. If the game is in blitz mode, it starts the blitz timer. 
   * If the move results in a game over state, it stops the blitz timer (if applicable) and 
   * displays appropriate messages to the user.
   *
   * @param from The starting position of the piece to move (e.g., "A3").
   * @param to The target position to move the piece to (e.g., "B4").
   */
  public void executeMove(String from, String to) {
    if (configuration.isBlitz()) {
      startBlitzTimer();
    }

    game.applyMove(from, to);

    game.setState(game.checkGameOver());
    handleGameOver();
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
    game.applyMove(from, to);
    game.setState(game.checkGameOver());
    handleGameOver();
  }

  /**
   * Displays the current state of the game board by delegating to the view.
   */
  public void displayBoard() {
    view.display(game);
  }

  /**
   * Displays the move history of the current game by retrieving it from the model and printing it 
   * to the console.
   * It uses the historyString method of the History class to format the output.
   */
  public void displayHistory() {
    System.out.println(game.getHistory().historyString());
  }

  /**
   * Displays the current game configuration settings by printing the Configuration object to the 
   * console.
   */
  public void displayConfiguration() {
    System.out.println(configuration);
  }

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
   * and notifies the user that their time has expired.
   */
  public void startBlitzTimer() {
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
          System.out.println("\n" + Internationalization.get("game.time_up")
              + game.getCurrentPlayer().getName());
          game.setState(State.FINISHED);
        }
      }
    }, 1000, 1000);
  }

  /**
   * Stops the blitz timer if it is currently running. This method is used to cancel the timer when 
   * the game is paused or finished, ensuring that no further timer tasks are executed.
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
   * @return true if verbose mode is enabled, false otherwise.
   */
  public boolean isVerbose() {
    return configuration.isVerbose();
  }

  /**
   * Checks if debug mode is enabled in the current configuration.
   *
   * @return true if debug mode is enabled, false otherwise.
   */
  public boolean isDebug() {
    return configuration.isDebug();
  }

  /**
   * Sets the debug mode in the current configuration by creating a new Configuration object with 
   * the updated debug value while preserving other settings.
   *
   * @param debug The debug setting to be applied to the new Configuration.
   */
  public void setDebug(boolean debug) {
    this.configuration = new Configuration(configuration, isVerbose(), debug);
  }

  /**
   * Sets the verbose mode in the current configuration by creating a new Configuration object with
   * the updated verbose value while preserving other settings.
   *
   * @param verbose The verbose setting to be applied to the new Configuration.
   */
  public void setVerbose(boolean verbose) {
    this.configuration = new Configuration(configuration, verbose, isDebug());
  }

  /**
   * Returns the current game instance.
   *
   * @return The current game instance.
   */
  public GameCheckers getGame() {
    return game;
  }

  /**
   * Sets the current game instance and configuration based on a loaded game. This method is used 
   * when loading a saved game state, allowing the controller to update its internal references to
   * the game and configuration, and to refresh the view accordingly.
   *
   * @param loadedGame The GameCheckers instance representing the loaded game state.
   * @param cfg Instance representing the settings associated with the loaded game.
   */
  public void setGame(GameCheckers loadedGame, Configuration cfg) {

    this.game = loadedGame;
    this.configuration = new Configuration(cfg);
    this.game.addObserver(view);
    displayBoard();
    markAsSaved();
  }

  /**
   * Checks if the white player is controlled by an AI based on the current configuration.
   *
   * @return true if the white player is controlled by AI, false otherwise.
   */
  public boolean iswhiteAi() {
    return configuration.iswhiteAi();
  }

  /**
   * Checks if the black player is controlled by an AI based on the current configuration.
   *
   * @return true if the black player is controlled by AI, false otherwise.
   */
  public boolean isblackAi() {
    return configuration.isblackAi();
  }

  /**
   * Undoes the last n moves in the game by invoking the undoManage method on the game 
   * instance n times. 
   *
   * @param n The number of moves to undo.
   */
  public void undoGame(int n) {
    for (int i = 0; i < n; i++) {
      game.undoManage();
    }
  }

  /**
   * Redoes the last n undone moves in the game by invoking the redoManage method on the game 
   * instance n times.
   *
   * @param n The number of moves to redo.
   */
  public void redoGame(int n) {
    for (int i = 0; i < n; i++) {
      game.redoManage();
    }
  }

  /**
   * Checks if the current game configuration is set to blitz mode, which affects the timing 
   * mechanics of the game.
   *
   * @return true if the game is in blitz mode, false otherwise.
   */
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
    }
  }
}