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

  @Override
  public void showHint(String from, String to) {
    System.out.println(Internationalization.get("hint.execute") + " " + from + " " + to + "\n");
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
    String[] tokens;

    if (trimmed.matches(Utils.MOVE_REGEX)) {
      tokens = trimmed.split("\\s+");
      controller.executeMove(tokens[0], tokens[1], false);

    } else if (trimmed.matches(Utils.MANOURY_REGEX)) {
      tokens = trimmed.split("-");
      controller.executeMove(tokens[0], tokens[1], true);

    } else {
      tokens = trimmed.split("\\s+");

      String commandName = tokens[0];
      String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
      controller.executeCommand(commandName, args);
    }
  }

  /**
    * Initializes the terminal and the line reader used by the controller game loop.
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
  }

  /**
   * Blocks until the user enters a line, then returns it to the controller loop.
   *
   * @return the entered line, or {@code null} if the input stream is closed
   */
  public String readInput() {
    try {
      return lineReader.readLine(">> "); // This will block until the user enters a line
    } catch (UserInterruptException | EndOfFileException e) {
      return null;
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
   * Sets the LineReader for this view.
   *
   * @param lineReader The LineReader instance to be used for reading user input
   */
  public void setLineReader(LineReader lineReader) {
    this.lineReader = lineReader;
  }
}