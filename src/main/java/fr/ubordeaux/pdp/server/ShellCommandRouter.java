package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.commands.HelpClientCommand;
import fr.ubordeaux.pdp.controller.commands.JoinCommand;
import fr.ubordeaux.pdp.controller.commands.PingCommand;
import fr.ubordeaux.pdp.controller.commands.QuitClientCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;
import fr.ubordeaux.pdp.model.tools.Utils;

/**
 * Single dispatch point for all user input.
 *
 * <p>The {@link fr.ubordeaux.pdp.view.CommandLineInterface} reads a raw input line and
 * delegates it here. This router decides which layer handles the command:
 *
 * <ul>
 *   <li>Move (regex match) → {@link GameController#executeMove} or forwarded if connected.
 *   <li>Client commands ({@code join}, {@code ping}, {@code quit}, {@code help}) →
 *       {@link ClientSession}.
 *   <li>Server commands ({@code server list/start/stop}) → server command classes.
 *   <li>Game commands ({@code new}, {@code undo}, {@code save}, etc.) →
 *       {@link GameController#executeCommand}.
 *   <li>Connected mode — unknown commands forwarded raw to the remote server.
 * </ul>
 */
public class ShellCommandRouter {

  private final GameController controller;
  private final ClientSession session;

  /**
   * Creates a router for shell commands.
   *
   * @param controller the local game controller.
   * @param session the current client session.
   */
  public ShellCommandRouter(GameController controller, ClientSession session) {
    this.controller = controller;
    this.session = session;
  }

  /**
   * Routes a raw input line to the correct handler.
   *
   * @param input the raw line entered by the user.
   */
  public void route(String input) {
    if (input == null || input.trim().isEmpty()) {
      return;
    }

    String trimmed = input.trim();

    if (isMove(trimmed)) {
      handleMove(trimmed);
      return;
    }

    dispatchCommand(trimmed);
  }

  /**
   * Returns {@code true} if the input matches the move regex (e.g. {@code E1 F2}).
   *
   * @param input trimmed input line.
   */
  private boolean isMove(String input) {
    return input.matches(Utils.MOVE_REGEX);
  }

  /**
   * Handles a move command.
   *
   * <p>If connected, sends {@code MOVE from-to} to the remote server.
   *  Otherwise,  executes locally via the controller.
   *
   * @param input trimmed move string (e.g. {@code "E1 F2"}).
   */
  private void handleMove(String input) {
    String[] tokens = input.split("\\s+");
    if (session.getMode() == ClientMode.CONNECTED) {
      session.send("MOVE " + tokens[0] + "-" + tokens[1]);
    } else {
      controller.executeMove(tokens[0], tokens[1],false);
    }
  }

  /**
   * Dispatches a non-move input line to the appropriate handler.
   *
   * @param input trimmed input line.
   */
  private void dispatchCommand(String input) {
    String[] tokens = input.split("\\s+", 3);
    String command = tokens[0].toLowerCase();

    switch (command) {
      case "server" -> handleServerCommand(tokens);
      case "join" -> handleJoin(tokens);
      case "ping" -> handlePing();
      case "quit" -> handleQuit();
      case "help" -> new HelpClientCommand().execute();
      default -> handleLocalOrRemote(input, tokens);
    }
  }

  /**
   * Handles {@code server list / start / stop} after checking mode guards.
   *
   * @param tokens split tokens from the input line.
   */
  private void handleServerCommand(String[] tokens) {
    String sub = tokens.length > 1 ? tokens[1].toLowerCase() : "";
    String args = tokens.length > 2 ? tokens[2] : "";

    switch (sub) {
      case "list" -> {
        if (blockUnless(ClientMode.LOCAL, "server list is only available in LOCAL mode.")) {
          return;
        }
        new ServerListCommand().execute();
      }
      case "start" -> {
        if (blockUnless(ClientMode.LOCAL,
            "Cannot start: use 'server stop' or 'quit' first.")) {
          return;
        }
        String portArg = args.isBlank() ? null : args.trim();
        String[] startArgs = portArg == null ? new String[0] : new String[]{portArg};
        new ServerStartCommand(controller, startArgs, session).execute();
      }
      case "stop" -> {
        if (blockUnless(ClientMode.SERVER, "No server running. Use 'server start' first.")) {
          return;
        }
        new ServerStopCommand(session).execute();
      }
      default ->
          System.out.println(
              "Unknown server command: '" + sub + "'.\n"
                  + "Available: server list | server start [PORT] | server stop");
    }
  }

  /**
   * Handles the {@code join} command.
   *
   * @param tokens split tokens (tokens[1] optionally contains the address).
   */
  private void handleJoin(String[] tokens) {
    if (blockIf(ClientMode.SERVER, "Cannot join while hosting. Use 'server stop' first.")) {
      return;
    }
    if (blockIf(ClientMode.CONNECTED, "Already connected. Use 'quit' to disconnect.")) {
      return;
    }
    String addr = tokens.length > 1 ? tokens[1] : null;
    new JoinCommand(session, addr).execute();
  }

  /** Handles the {@code ping} command. */
  private void handlePing() {
    if (blockIf(ClientMode.SERVER, "Cannot ping: not in client mode.")) {
      return;
    }
    if (blockIf(ClientMode.LOCAL, "Not connected. Use 'join' first.")) {
      return;
    }
    new PingCommand(session).execute();
  }

  /** Handles the {@code quit} command. */
  private void handleQuit() {
    QuitClientCommand quit = new QuitClientCommand(session);
    quit.execute();
    if (quit.shouldExit()) {
      System.exit(0);
    }
  }

  /**
   * Handles game commands locally or forwards them to the remote server if connected.
   *
   * <p>In SERVER mode, only management commands ({@code status}, {@code players},
   * {@code scoreboard}) are forwarded; all other game commands are blocked.
   *
   * @param raw the original trimmed input line.
   * @param tokens split tokens from the input line.
   */
  private void handleLocalOrRemote(String raw, String[] tokens) {
    switch (session.getMode()) {
      case CONNECTED -> session.send(raw);
      case SERVER -> {
        if (isServerManagementCommand(tokens[0].toLowerCase())) {
          session.send(raw);
        } else {
          System.out.println("""
                             [blocked] Game commands unavailable in SERVER mode.
                                       Use 'server stop' to return to local mode.""");
        }
      }
      case LOCAL -> {
        String commandName = tokens[0];
        String[] args =
            tokens.length > 1
                ? java.util.Arrays.copyOfRange(tokens, 1, tokens.length)
                : new String[0];
        controller.executeCommand(commandName, args);
      }
      default -> throw new IllegalStateException("Unexpected mode: " + session.getMode());
    }
  }

  /**
   * Returns {@code true} if the command is allowed in SERVER mode.
   *
   * @param cmd lowercase command keyword.
   */
  private boolean isServerManagementCommand(String cmd) {
    return cmd.equals("status") || cmd.equals("players") || cmd.equals("scoreboard");
  }

  /**
   * Blocks and prints an error if the current mode matches {@code blocked}.
   *
   * @param blocked the forbidden mode.
   * @param message the error message.
   * @return {@code true} if execution should stop.
   */
  private boolean blockIf(ClientMode blocked, String message) {
    if (session.getMode() == blocked) {
      System.out.println("[blocked] " + message);
      return true;
    }
    return false;
  }

  /**
   * Blocks and prints an error if the current mode does NOT match {@code required}.
   *
   * @param required the only allowed mode.
   * @param message the error message.
   * @return {@code true} if execution should stop.
   */
  private boolean blockUnless(ClientMode required, String message) {
    if (session.getMode() != required) {
      System.out.println("[blocked] " + message);
      return true;
    }
    return false;
  }
}