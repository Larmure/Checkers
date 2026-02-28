package fr.ubordeaux.pdp.view;

import java.util.Arrays;
import java.util.Scanner;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Utils;
import fr.ubordeaux.pdp.model.Internationalization;

/**
 * Concrete implementation of {@link GameView} providing an interactive text-based shell.
 * It reads user input from the standard input, parses it, and forwards it to the 
 * {@link GameController}.
 *
 * @version 1.0
 */
public class CommandLineInterface extends GameView {

  private Thread inputThread;

  /** Flag to enable verbose. */
  private boolean verbose = false;

  /** Flag to enable debug. */
  private boolean debug = false;

  private boolean blitz = false;

  /**
   * Constructs a CommandLineInterface with specific logging levels.
   *
   * @param verbose Enable or disable verbose output.
   * @param debug Enable or disable debug output.
   * @param blitz Enable or disable blitz mode.
   */
  public CommandLineInterface(boolean verbose, boolean debug, boolean blitz) {
    this.verbose = verbose;
    this.debug = debug;
    this.blitz = blitz;

    Internationalization.init();
  }

  /**
   * Renders the current state of the board in the terminal.
   */
  @Override
  public void display(GameCheckers game) {
    System.out.println(Internationalization.get("game.board_title"));
    
    // On utilise le toString() du Board que tu as fourni dans tes fichiers
    // C'est ici que la Vue "lit" le modèle sans le modifier
    System.out.println(game.getBoard().toString());
    
    // Affichage des infos de tour
    String tour = game.getCurrentPlayer().getName(); 
    System.out.println(String.format(Internationalization.get("game.turn"), tour));

    if(blitz)
    {
      int totalSeconds = game.getCurrentPlayer().getPlayTime();
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;

      String formattedTime = String.format("%02d:%02d", minutes, seconds);

      String template = Internationalization.get("game.time_remaining");

      System.out.println(String.format(template, game.getCurrentPlayer().getName(), formattedTime));
    }
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
   * Starts the main input loop into a separated Thread. 
   * It captures user strings, splits them into commands and arguments, 
   * and delegates execution to the controller.
   */
  @Override
  public void start() {
    inputThread = new Thread(() -> {
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

          if (input.matches(Utils.MOVE_REGEX)) {
            controller.executeMove(tokens[0], tokens[1]);
          } else {
            String commandName = tokens[0];     
            String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
            controller.executeCommand(commandName, args);
          }

        } catch (Exception e) {
          System.out.println("Invalid input: " + e.getMessage());
        }
      }
    });
    inputThread.start();
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

  public void join() throws InterruptedException {
    if (inputThread != null) inputThread.join();
  }

}