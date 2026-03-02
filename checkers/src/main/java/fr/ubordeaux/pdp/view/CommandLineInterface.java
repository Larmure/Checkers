package fr.ubordeaux.pdp.view;

import java.io.IOException;
import java.util.Arrays;

import org.jline.reader.EndOfFileException;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Utils;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Concrete implementation of {@link GameView} providing an interactive
 * text-based shell.
 * It reads user input from the standard input, parses it, and forwards it to
 * the
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
   * @param debug   Enable or disable debug output.
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
    System.out.println(game.getBoard().toString());

    String tour = game.getCurrentPlayer().toString();
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

  public void handleInput(String input) {
    if (input == null || input.trim().isEmpty())
      return;

    String trimmed = input.trim();
    String[] tokens = trimmed.split("\\s+");

    if (trimmed.matches(Utils.MOVE_REGEX)) {
      controller.executeMove(tokens[0], tokens[1]);
    } else {
      String commandName = tokens[0];
      String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
      controller.executeCommand(commandName, args);
    }
  }

  /**
   * Starts the main input loop into a separated Thread.
   * It captures user strings, splits them into commands and arguments,
   * and delegates execution to the controller.
   */
  @Override
  public void start() {
    if (lineReader == null) {
      try {
        terminal = TerminalBuilder.terminal();
      } catch (IOException ex) {
        System.getLogger(CommandLineInterface.class.getName())
            .log(System.Logger.Level.ERROR, (String) null, ex);
      }
      lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }
    inputThread = new Thread(() -> {
      while (true) {
        try {
          String input = lineReader.readLine(">> ");
          handleInput(input);
        } catch (UserInterruptException | EndOfFileException e) {
          break;
        }
      }
    });
    inputThread.setDaemon(true);
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

  public void setLineReader(LineReader lineReader) {
    this.lineReader = lineReader;
  }

  public void join() throws InterruptedException {
    if (inputThread != null)
      inputThread.join();
  }

}