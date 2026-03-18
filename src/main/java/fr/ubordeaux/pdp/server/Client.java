package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.commands.HelpClientCommand;
import fr.ubordeaux.pdp.controller.commands.JoinCommand;
import fr.ubordeaux.pdp.controller.commands.PingCommand;
import fr.ubordeaux.pdp.controller.commands.QuitClientCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;
import fr.ubordeaux.pdp.view.ConsoleView;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Unified shell — the single entry point and sole command dispatcher.
 *
 * <p>Owns both a {@link ClientSession} (network state + mode) and a {@link GameController}
 * (game logic). The shell's {@code Scanner} is the only input loop in the program:
 * {@code controller.start()} is intentionally NOT called to prevent the view from opening
 * a second loop that would swallow network commands like {@code join} or {@code ping}.
 *
 * <p>Dispatch rules by {@link ClientMode}:
 *
 * <pre>
 * LOCAL     → game commands (GameController) + server list/start + join
 * SERVER    → server stop only; all other commands blocked
 * CONNECTED → game commands forwarded to remote server + ping + quit
 * </pre>
 *
 * @see ClientSession
 * @see ClientMode
 * @see GameController
 */
public class client {

  private final ClientSession session = new ClientSession();
  private final GameController controller;

  /** Wires the view to the controller without starting an input loop. */
  public client() {
    ConsoleView view = new ConsoleView();
    this.controller = new GameController(view);
    view.setController(controller);
  }

  /**
   * Starts the interactive command loop.
   *
   * <p>Every input line is routed through a single dispatch sequence:
   *
   * <ol>
   *   <li>{@code server <sub>} — server management commands.
   *   <li>{@code join / ping / quit / help} — client commands.
   *   <li>Everything else: LOCAL → GameController, CONNECTED → forwarded, SERVER → blocked.
   * </ol>
   */
  public void run() {
    Scanner scanner = new Scanner(System.in);

    System.out.println("╔══════════════════════════════════════════════════════════════╗");
    System.out.println("║              CHECKERS — UNIFIED SHELL                        ║");
    System.out.println("╚══════════════════════════════════════════════════════════════╝");
    System.out.println("Type 'help' to see available commands\n");

    while (true) {
      System.out.print(prompt());

      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        continue;
      }

      String[] tokens = input.split("\\s+", 3);
      String word1 = tokens[0].toLowerCase();
      String word2 = tokens.length > 1 ? tokens[1].toLowerCase() : "";
      String rest = tokens.length > 2 ? tokens[2] : "";

      if ("server".equals(word1)) {
        dispatchServerCommand(word2, rest);
        continue;
      }

      switch (word1) {
        case "join" -> {
          if (blockIf(ClientMode.SERVER,
                "Cannot join a server while hosting one. Use 'server stop' first.")) {
            break;
          }
          if (blockIf(ClientMode.CONNECTED, "Already connected. Use 'quit' to disconnect first.")) {
            break;
          }
          String addr = word2.isBlank() ? null : word2 + (rest.isBlank() ? "" : ":" + rest);
          new JoinCommand(session, addr).execute();
        }
        case "ping" -> {
          if (blockIf(ClientMode.SERVER, "Cannot ping: not in client mode.")) {
            break;
          }
          if (blockIf(ClientMode.LOCAL, "Not connected to any server. Use 'join' first.")) {
            break;
          }
          new PingCommand(session).execute();
        }
        case "help" -> new HelpClientCommand().execute();
        case "quit" -> {
          QuitClientCommand quit = new QuitClientCommand(session);
          quit.execute();
          if (quit.shouldExit()) {
            scanner.close();
            return;
          }
        }
        default -> dispatchDefault(word1, tokens);
      }
    }
  }

  /**
   * Handles any input that is not a named client or server command.
   *
   * @param commandName the first token (command keyword).
   * @param tokens all tokens from the original input line.
   */
  private void dispatchDefault(String commandName, String[] tokens) {
    switch (session.getMode()) {
      case LOCAL -> {
        String[] args =
              tokens.length > 1 ? Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];
        controller.executeCommand(commandName, args);
      }
      case CONNECTED -> session.send(String.join(" ", tokens));
      case SERVER ->
            System.out.println(
                  "[blocked] Game commands are unavailable in SERVER mode.\n"
                        + "          Use 'server stop' to return to local mode.");
    }
  }

  /**
   * Dispatches {@code server list / start / stop} after checking mode guards.
   *
   * @param sub sub-command word.
   * @param args remaining arguments (e.g. port number).
   */
  private void dispatchServerCommand(String sub, String args) {
    switch (sub) {
      case "list" -> {
        if (blockUnless(ClientMode.LOCAL, "server list is only available in LOCAL mode.")) {
          break;
        }
        new ServerListCommand().execute();
      }
      case "start" -> {
        if (blockUnless(
              ClientMode.LOCAL,
              "Cannot start a server: use 'server stop' or 'quit' to leave your current mode.")) {
          break;
        }
        String portArg = args.isBlank() ? null : args.trim();
        String[] startArgs = portArg == null ? new String[0] : new String[] {portArg};
        new ServerStartCommand(controller, startArgs, session).execute();
      }
      case "stop" -> {
        if (blockUnless(ClientMode.SERVER, "No server is running. Use 'server start [PORT]' first.")) {
          break;
        }
        new ServerStopCommand(session).execute();
      }
      default ->
            System.out.println(
                  "Unknown server command: '"
                        + sub
                        + "'.\nAvailable: server list | server start [PORT] | server stop");
    }
  }

  /**
   * Returns {@code true} and prints an error if the mode matches {@code blocked}.
   *
   * @param blocked forbidden mode.
   * @param errorMessage message displayed when blocked.
   * @return {@code true} if execution should be skipped.
   */
  private boolean blockIf(ClientMode blocked, String errorMessage) {
    if (session.getMode() == blocked) {
      System.out.println("[blocked] " + errorMessage);
      return true;
    }
    return false;
  }

  /**
   * Returns {@code true} and prints an error if the mode does NOT match {@code required}.
   *
   * @param required the only allowed mode.
   * @param errorMessage message displayed when blocked.
   * @return {@code true} if execution should be skipped.
   */
  private boolean blockUnless(ClientMode required, String errorMessage) {
    if (session.getMode() != required) {
      System.out.println("[blocked] " + errorMessage);
      return true;
    }
    return false;
  }

  /**
   * Returns a contextual prompt string reflecting the current mode.
   *
   * <pre>
   * LOCAL     →  [local] >
   * SERVER    →  [server:12345] >
   * CONNECTED →  [192.168.1.1:12345] >
   * </pre>
   */
  private String prompt() {
    return switch (session.getMode()) {
      case SERVER -> "[server:" + ServerStartCommand.activeServer.getPort() + "] > ";
      case CONNECTED -> "[" + session.getCurrentServer() + "] > ";
      default -> "[local] > ";
    };
  }

  public static void main(String[] args) {
    new client().run();
  }
}