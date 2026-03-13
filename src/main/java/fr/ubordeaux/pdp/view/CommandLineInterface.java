package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.BashStyleCompleter;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import java.io.IOException;
import java.util.Arrays;
import org.jline.reader.EndOfFileException;
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
 * @version 1.1
 */
public class CommandLineInterface extends GameView {
  /** Thread for handling user input. */
  private Thread inputThread;

  /** Flag to enable verbose. */
  private boolean verbose = false;

  /** Flag to enable debug. */
  private boolean debug = false;

  /** The terminal for user interaction. */
  private Terminal terminal;

  /** The line reader for handling user input. */
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
    System.out.println(Internationalization.get("game.board_title"));

    // On utilise le toString() du Board que tu as fourni dans tes fichiers
    // C'est ici que la Vue "lit" le modèle sans le modifier
    System.out.println(game.getBoard().toString());

    // Affichage des infos de tour
    String tour = game.getCurrentPlayer().getName();
    System.out.println(String.format(Internationalization.get("game.turn"), tour));

    if (controller.isBlitz()) {
      controller.displayTime();
    }
    System.out.println("======================\n");
  }

  /**
   * Reacts to notifications from the observed model.
   */
  @Override
  public void update(GameCheckers gameCheckers) {
    this.display(gameCheckers);
  }

  /**
   * Handles user input from the terminal.
   * It distinguishes between move commands (e.g., "B2 C3") and other commands
   * (e.g., "show", "help") and delegates execution to the controller
   *
   * @param input The raw input string entered by the user.
   */
  public void handleInput(String input) {
    if (input == null || input.trim().isEmpty()) {
      return;
    }

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
      lineReader = LineReaderBuilder.builder()
          .terminal(terminal)
          .completer(new BashStyleCompleter(Utils.COMMANDS_MAP.keySet()))
          .build();

      lineReader.setVariable(LineReader.BELL_STYLE, "visible");
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

  /**
   * Sets the LineReader for this view.
   *
   * @param lineReader The LineReader instance to be used for reading user input
   */
  public void setLineReader(LineReader lineReader) {
    this.lineReader = lineReader;
  }

  /**
   * Waits for the input thread to finish. This is useful for testing purposes to
   * ensure that all input processing
   * is completed before assertions are made.
   *
   * @throws InterruptedException if the current thread is interrupted while
   *                              waiting.
   */
  public void join() throws InterruptedException {
    if (inputThread != null) {
      inputThread.join();
    }
  }

}