
   package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Configuration;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.controller.commands.*;
import fr.ubordeaux.pdp.model.Internationalization;
import fr.ubordeaux.pdp.model.State;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Orchestrator of the game logic and user interactions.
 * As the Controller in the MVC (Model-View-Controller) pattern, it mediates 
 * between the {@link GameCheckers} model and the {@link GameView}.
 * It interprets user inputs as {@link Command} objects and updates the game state.
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

  /**
   * Initializes the controller with the required model and view components.
   *
   * @param view The {@link GameView} instance (View).
   */
  public GameController(GameView view) {
    this.view = view;
    Internationalization.init();
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
      case "quit" -> new QuitCommand();
      case "load" -> new LoadCommand();
      case "save" -> new SaveCommand();
      case "pause" -> new PauseCommand(blitzTimer, game);
      case "hint" -> new HintCommand();
      case "undo" -> new UndoCommand();
      case "redo" -> new RedoCommand();
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
    this.configuration =  new Configuration(configuration);
    game.addObserver(view);

    String rules = Internationalization.get("game.rules");
    System.out.println(rules);

    if (configuration.isBlitz())
    {
      startBlitzTimer();
    }

    displayBoard();
  }

  public void executeMove(String from, String to)
  {
    game.applyMove(from, to);
    
    if(configuration.isBlitz()) {
      startBlitzTimer(); 
    }
    
    if(game.getState().equals(State.FINISHED)) 
    {
      game.setState(game.checkGameOver());
      stopBlitzTimer();
    } 
  }

  public void displayBoard() {
    view.display(game);
  }

  public void displayConfiguration() {
    System.out.println(configuration);
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

  public boolean isWhiteIsAi() {
    return configuration.isWhiteIsAI();
  }

  public boolean isBlackIsAi() {
    return configuration.isBlackIsAI();
  }
}
