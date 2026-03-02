
   package fr.ubordeaux.pdp.controller;

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
import fr.ubordeaux.pdp.model.Configuration;
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
  private GameCheckers game;

  private Configuration configuration;

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
      case "quit" -> new QuitCommand();
      case "load" -> new LoadCommand(this,args);
      case "save" -> new SaveCommand(this,args);
      case "pause" -> new PauseCommand();
      case "hint" -> new HintCommand();
      case "undo" -> new UndoCommand();
      case "redo" -> new RedoCommand();
      case "show" -> new ShowCommand(this, args);
      case "set" -> new SetCommand(this, args);
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

    System.out.println("");
    System.out.println("RULES:");
    System.out.println("- The board is 8x8. Each player starts with 12 pieces on the dark squares.");
    System.out.println("- Pieces move diagonally forward, one square at a time.");
    System.out.println("- To capture an opponent's piece, jump over it diagonally to the empty square behind it.");
    System.out.println("- If you can capture, you must. You can chain multiple captures in one turn.");
    System.out.println("- Reach the opponent's back row to become a King (moves diagonally in all directions).");
    System.out.println("- The player who captures all opponent's pieces (or blocks them) wins.");
    System.out.println("");
    System.out.println("To apply movements, enter for example: E1 F2");

    displayBoard();
  }

  public void executeMove(String from, String to)
  {
    game.applyMove(from, to);
    
    if(game.getState().isGameOver()) game.setState(game.checkGameOver()); 
  }

  public void displayBoard() {
    view.display(game);
  }

  public void displayConfiguration() {
    System.out.println(configuration);
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
}
  public boolean isWhiteIsAi() {
    return configuration.isWhiteIsAI();
  }

  public boolean isBlackIsAi() {
    return configuration.isBlackIsAI();
  }
}
