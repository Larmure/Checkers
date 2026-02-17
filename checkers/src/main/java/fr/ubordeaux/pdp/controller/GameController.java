package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;


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
  private final GameCheckers game;

  /**
   * Initializes the controller with the required model and view components.
   *
   * @param game The {@link GameCheckers} instance (Model).
   * @param view The {@link GameView} instance (View).
   */
  public GameController(GameCheckers game, GameView view) {
    this.view = view;
    this.game = game;
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
      case "quit" -> new QuitCommand();
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
  public void startNewGame(boolean blitz, boolean contest, int time, int size) {
    System.out.println("Initializing new game with options: " 
        + (blitz ? "Blitz " : "") 
        + (contest ? "Contest " : "") 
        + (time > 0 ? "Time=" + time + "min " : "") 
        + (size != 8 ? "Size=" + size : ""));
  }
}
