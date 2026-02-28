package fr.ubordeaux.pdp.view;

import java.io.IOException;
import java.util.Arrays;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Utils;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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

  private Terminal terminal;
  private LineReader lineReader;

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
   * Starts the main input loop into a separated Thread. 
   * It captures user strings, splits them into commands and arguments, 
   * and delegates execution to the controller.
   */
  @Override
  public void start() {
    try {
      terminal = TerminalBuilder.terminal();
    } catch (IOException ex) {
      System.getLogger(CommandLineInterface.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
    }
    lineReader = LineReaderBuilder.builder().terminal(terminal).build();


    inputThread = new Thread(() -> {
      if (verbose) System.out.println("[Info] CLI mode started with verbose output.");
      if (debug) System.out.println("[Debug] CLI mode started with debug output.");

      while (true) {
        String input;
        try {
            input = lineReader.readLine(">> ").trim(); // JLine gère les flèches et Ctrl+R
        } catch (org.jline.reader.EndOfFileException | org.jline.reader.UserInterruptException e) {
            break;
        }

        if (input.isEmpty()) {
          continue;
        }

        try {
          String[] tokens = input.split("\\s+");
          if (input.matches(Utils.MOVE_REGEX)) {
            System.out.println("MOVE : " + tokens[0] + "-" + tokens[1]);
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