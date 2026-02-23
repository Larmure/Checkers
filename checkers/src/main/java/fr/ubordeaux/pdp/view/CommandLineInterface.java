package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import java.util.Arrays;
import java.util.Scanner;

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
  public void display() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Reacts to notifications from the observed model.
   * Typically triggers a new {@link #display()} to show updated game state.
   */
  @Override
  public void update() {
    throw new UnsupportedOperationException("Not supported yet.");
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

}