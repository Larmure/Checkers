package fr.ubordeaux.pdp.view;

import java.util.Arrays;
import java.util.Scanner;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Move;

/**
 * Concrete implementation of {@link GameView} providing an interactive text-based shell.
 * It reads user input from the standard input, parses it, and forwards it to the 
 * {@link GameController}.
 *
 * @version 1.0
 */
public class CommandLineInterface extends GameView {

  /** Flag to enable verbose. */
  private boolean verbose = false;

  /** Flag to enable debug. */
  private boolean debug = false;

  /**
   * Constructs a CommandLineInterface with specific logging levels.
   *
   * @param verbose Enable or disable verbose output.
   * @param debug Enable or disable debug output.
   */
  public CommandLineInterface(boolean verbose, boolean debug) {
    this.verbose = verbose;
    this.debug = debug;
  }

  /**
   * Renders the current state of the board in the terminal.
   */
  @Override
  public void display(GameCheckers game) {
    System.out.println("\n=== GAME BOARD ===");
    
    // On utilise le toString() du Board que tu as fourni dans tes fichiers
    // C'est ici que la Vue "lit" le modèle sans le modifier
    System.out.println(game.getBoard().toString());
    
    // Affichage des infos de tour
    String tour = game.getCurrentPlayer().toString(); // Assure-toi que Player a un toString
    System.out.println("Turn : " + tour);
    System.out.println("======================\n");
  }

  /**
   * Reacts to notifications from the observed model.
   * Typically triggers a new {@link #display()} to show updated game state.
   */
  @Override
  public void update(GameCheckers gameCheckers) {
    this.display(gameCheckers);
  }

  /**
   * Starts the main input loop. 
   * It captures user strings, splits them into commands and arguments, 
   * and delegates execution to the controller.
   */
  @Override
  public void start() {
    if (verbose) {
      System.out.println("[Info] CLI mode started with verbose output.");
    }
    if (debug) {
      System.out.println("[Debug] CLI mode started with debug output.");
    }
    
    // We must not close "System.in"
    @SuppressWarnings("resource")
    Scanner scanner = new Scanner(System.in);

    // Wait for user commands and execute them
    while (true) {
      System.out.print(">> ");

      if (!scanner.hasNextLine()) {
        break;
      }
    
      String input = scanner.nextLine().trim();
    
      if (input.isEmpty()) {
        continue;
      }
    
      try {
        // Split the input into tokens
        String[] tokens = input.split("\\s+");
        String commandName = tokens[0];     
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
        
        controller.executeCommand(commandName, args);

      } catch (Exception e) {
        System.out.println("Invalid command: " + e.getMessage());
      }
    }

  }

  /**
   * Sets the controller for this view.
   *
   * @param controller The {@link GameController} instance.
   */
  @Override
  public void setController(GameController controller) {
    this.controller = controller;
  }

  /**
   * Prompts the user for console input to select a move.
   * <p>This method blocks until a valid move is entered. It parses algebraic coordinates
   * (e.g., "A3 B4"), converts them to board indices, and validates the move against
   * the game's legal move list to ensure rules like mandatory captures are respected.
   *
   * @param game The current game instance used for coordinate conversion and rule validation.
   * @return The selected {@link Move}, or {@code null} if the user chooses to quit.
   */
  @Override
  public Move getUserMove(GameCheckers game) {
    Scanner sc = new Scanner(System.in);
    while (true) {
      System.out.println("Enter your move (e.g., A3 B4) or 'quit':");
      String input = sc.nextLine().toUpperCase().trim();

      if (input.equals("QUIT")) {
        return null;
      }

      String[] parts = input.split("\\s+");
      if (parts.length == 2) {
        try {
          // Convert algebraic notation (e.g., "A1") to internal board indices.
          int from = game.getBoard().squareToIndex(parts[0]);
          int to = game.getBoard().squareToIndex(parts[1]);

          // Verify the move against the engine's legal moves to enforce mandatory captures.
          for (Move m : game.getPossibleMoves(game.getCurrentPlayer())) {
            if (m.getFrom() == from && m.getTo() == to) {
              return m;
            }
          }
          System.out.println("Invalid move or mandatory capture missed.");
        } catch (Exception e) {
          System.out.println("Invalid format. Please use coordinates like A1 B2...");
        }
      }
    }
  }

}