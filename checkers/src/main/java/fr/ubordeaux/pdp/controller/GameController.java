package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.GameOption;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.model.Utils;
import jdk.jshell.execution.Util;


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

  /**
   * Initializes the controller with the required model and view components.
   *
   * @param game The {@link GameCheckers} instance (Model).
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
      case "quit" -> new QuitCommand();
      case "load" -> new LoadCommand();
      case "save" -> new SaveCommand();
      case "pause" -> new PauseCommand();
      case "hint" -> new HintCommand();
      case "undo" -> new UndoCommand();
      case "redo" -> new RedoCommand();
      case "show" -> new ShowCommand();
      case "set" -> new SetCommand();
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
  public void startNewGame(GameOption gameOption) {
    System.out.println("Initializing new game with options: " + gameOption); 

    this.game = new GameCheckers(gameOption);
    game.addObserver(view);

    view.display(game);
  }

  public void executeMove(String from, String to)
  {
    game.applyMove(from, to);

    view.display(game);

    if(game.getState().isGameOver()) game.setState(game.checkGameOver()); 
  }
}
