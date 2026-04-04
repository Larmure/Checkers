package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.BashStyleCompleter;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.server.ShellCommandRouter;
import java.io.IOException;
import java.util.Arrays;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Concrete implementation of {@link GameView} providing an interactive text-based shell.
 *
 * <p>This class has a single responsibility: read input lines and forward them to
 * {@link ShellCommandRouter}. All dispatch logic (move vs. game command vs. network command)
 * lives in the router, keeping this class focused on I/O only.
 *
 * <p>If no {@link ShellCommandRouter} is injected (standalone game mode), input is forwarded
 * directly to the {@link GameController}.
 *
 * @version 2.0
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
  private Thread inputThread;

  /**
   * Optional router — injected when running with network support.
   * If {@code null}, input is forwarded directly to the controller.
   */
  private ShellCommandRouter router;

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
   * Injects the shell command router.
   *
   * <p>Call this before {@link #start()} when running with network support so that
   * {@code join}, {@code ping}, and {@code server} commands are handled correctly.
   *
   * @param router the router that dispatches all input.
   */
  public void setRouter(ShellCommandRouter router) {
    this.router = router;
  }

  /**
   * Displays the current game state in the terminal.
   *
   * <p>Shows the board, the current player's turn, and the remaining time
   * in blitz mode.
   *
   * @param game the current game to display
   */
  @Override
  public void display(GameCheckers game) {
    System.out.println(Internationalization.get("game.board_title"));
    System.out.println(game.getBoard().toString());

    String currentPlayerName = game.getCurrentPlayer().getName();
    System.out.println(
        String.format(Internationalization.get("game.turn"), currentPlayerName));

    if (controller.isBlitz()) {
      controller.displayTime();
    }

    System.out.println("======================\n");
  }

  /**
   * Updates the view when the observed game state changes.
   *
   * @param gameCheckers the updated game instance
   */
  @Override
  public void update(GameCheckers gameCheckers) {
    display(gameCheckers);
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

    router.route(input.trim());
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