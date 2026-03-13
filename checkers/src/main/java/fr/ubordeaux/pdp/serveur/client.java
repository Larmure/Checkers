package fr.ubordeaux.pdp.serveur;

import java.util.Scanner;

import fr.ubordeaux.pdp.controller.commands.HelpClientCommand;
import fr.ubordeaux.pdp.controller.commands.JoinCommand;
import fr.ubordeaux.pdp.controller.commands.PingCommand;
import fr.ubordeaux.pdp.controller.commands.QuitClientCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;

/**
 * Entry point of the game network client.
 *
 * <p>Single responsibility: read user input and dispatch it to the appropriate
 * {@link fr.ubordeaux.pdp.controller.commands.ClientCommand} implementation.
 *
 * <p>Commands are gated by the current {@link ClientMode}:
 * <pre>
 * LOCAL     → all commands available
 * SERVER    → only server management commands: server stop/status, players, scoreboard
 *             client commands (join, ping) are BLOCKED
 * CONNECTED → client + game commands available
 *             server start is BLOCKED
 * </pre>
 *
 * @see ClientSession
 * @see ClientMode
 */
public class client {

  /** Shared connection state and mode — passed to every command. */
  private final ClientSession session = new ClientSession();

  /**
   * Starts the interactive command loop.
   * Reads lines from stdin, checks mode permissions, and dispatches to commands.
   * Exits when {@link QuitClientCommand#shouldExit()} returns {@code true}.
   */
  public void run() {
    Scanner scanner = new Scanner(System.in);

    System.out.println("╔══════════════════════════════════════════════════════════════╗");
    System.out.println("║                  GAME CLIENT — WELCOME                       ║");
    System.out.println("╚══════════════════════════════════════════════════════════════╝");
    System.out.println("Type 'help' to see available commands\n");

    while (true) {
      System.out.print(prompt());

      String input = scanner.nextLine().trim();
      if (input.isEmpty()) continue;

      // Split into at most 3 tokens: word1 [word2 [rest]]
      String[] tokens = input.split("\\s+", 3);
      String word1 = tokens[0].toLowerCase();
      String word2 = tokens.length > 1 ? tokens[1].toLowerCase() : "";
      String rest = tokens.length > 2 ? tokens[2] : "";

      // Two-word commands: server <sub-command>
      if ("server".equals(word1)) {
        dispatchServerCommand(word2, rest);
        continue;
      }

      // Single-word commands — checked against current mode
      switch (word1) {

        case "join" -> {
          if (blockIf(ClientMode.SERVER,
            "Cannot join a server while hosting one. Use 'server stop' first.")) break;
          String addr = word2.isBlank() ? null : word2 + (rest.isBlank() ? "" : ":" + rest);
          new JoinCommand(session, addr).execute();
        }

        case "ping" -> {
          if (blockIf(ClientMode.SERVER,
            "Cannot ping: client commands are disabled in SERVER mode.")) break;
          new PingCommand(session).execute();
        }

        case "help" ->
          new HelpClientCommand().execute();

        case "quit" -> {
          QuitClientCommand quit = new QuitClientCommand(session);
          quit.execute();
          if (quit.shouldExit()) {
            scanner.close();
            return;
          }
        }

        default -> {
          if (session.isConnected()) {
            // Game command: forward to the server, GameController handles it
            session.send(input);
          } else {
            System.out.println("Unknown command: '" + word1 + "'.");
            System.out.println("Type 'help' to see available commands.");
          }
        }
      }
    }
  }

  /**
   * Dispatches {@code server list / start / stop / status / players / scoreboard}.
   *
   * <p>Mode restrictions:
   * <ul>
   *   <li>{@code server start} — blocked in CONNECTED and SERVER modes.</li>
   *   <li>{@code server list}  — blocked in CONNECTED mode (already on a server).</li>
   *   <li>{@code server stop / status / players / scoreboard} — only in SERVER mode.</li>
   * </ul>
   *
   * @param sub  sub-command word.
   * @param args remaining arguments (e.g. port number).
   */
  private void dispatchServerCommand(String sub, String args) {
    switch (sub) {

      case "list" -> {
        if (blockIf(ClientMode.CONNECTED,
          "Already connected to a server. Use 'quit' to disconnect first.")) break;
        if (blockIf(ClientMode.SERVER,
          "Cannot scan for servers while hosting one.")) break;
        new ServerListCommand().execute();
      }

      case "start" -> {
        if (blockIf(ClientMode.CONNECTED,
          "Cannot start a server while connected as a client. Use 'quit' first.")) break;
        if (blockIf(ClientMode.SERVER,
          "A server is already running. Use 'server stop' first.")) break;
        String portArg = args.isBlank() ? null : args.trim();
        String[] startArgs = portArg == null ? new String[0] : new String[]{portArg};
        new ServerStartCommand(null, startArgs, session).execute();
      }

      case "stop" -> {
        if (blockUnless(ClientMode.SERVER,
          "No server is running. Use 'server start' first.")) break;
        new ServerStopCommand(session).execute();
      }

      case "status", "players", "scoreboard" -> {
        if (blockUnless(ClientMode.SERVER,
          "This command is only available in SERVER mode. Use 'server start' first.")) break;
        // Forward management commands to the server's internal handler
        // (these are handled directly by GameServer, not via TCP)
        System.out.println("Use these commands from a connected client, "
          + "or query the server process directly.");
      }

      default ->
        System.out.println("Unknown server command: '" + sub + "'.\n"
          + "Available: server list | server start [PORT] | server stop");
    }
  }

  /**
   * Prints an error and returns {@code true} if the current mode matches {@code blocked}.
   * Used to gate commands that must NOT run in a given mode.
   *
   * @param blocked     the mode in which the command is forbidden.
   * @param errorMessage message shown when the command is blocked.
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
   * Prints an error and returns {@code true} if the current mode does NOT match
   * {@code required}. Used to gate commands that must ONLY run in a given mode.
   *
   * @param required     the mode in which the command is allowed.
   * @param errorMessage message shown when the command is blocked.
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
      case SERVER    -> "[server:" + ServerStartCommand.activeServer.getPort() + "] > ";
      case CONNECTED -> "[" + session.getCurrentServer() + "] > ";
      default        -> "[local] > ";
    };
  }

  // -------------------------------------------------------------------------
  // Entry point
  // -------------------------------------------------------------------------

  public static void main(String[] args) {
    new client().run();
  }
}