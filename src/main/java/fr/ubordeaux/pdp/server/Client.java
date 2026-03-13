package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.commands.HelpClientCommand;
import fr.ubordeaux.pdp.controller.commands.JoinCommand;
import fr.ubordeaux.pdp.controller.commands.PingCommand;
import fr.ubordeaux.pdp.controller.commands.QuitClientCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;
import java.util.Scanner;

/**
 * Entry point of the game network client.
 *
 * <p>Single responsibility: read user input and dispatch it to the appropriate
 * ClientCommand implementation.
 * All logic is encapsulated in the corresponding Command classes.
 *
 * <p>Command hierarchy:
 * <pre>
 * [CLIENT]          JoinCommand · PingCommand · QuitClientCommand · HelpClientCommand
 * [NETWORK SERVER]  ServerListCommand · ServerStartCommand · ServerStopCommand
 * [GAME]            Forwarded as-is to the server via {@link ClientSession#send(String)}
 * </pre>
 *
 * @see ClientSession
 */

public class Client {

  /** Shared connection state passed to all client commands. */
  private final ClientSession session = new ClientSession();

  /**
   * Starts the client's interactive command loop.
   * Reads lines from stdin, parses them, and dispatches to the correct command.
   * The loop exits when {@link QuitClientCommand#shouldExit()} returns {@code true}.
   */
  public void run() {
    Scanner scanner = new Scanner(System.in);

    System.out.println("╔══════════════════════════════════════════════════════════════╗");
    System.out.println("║                  GAME CLIENT — WELCOME                       ║");
    System.out.println("╚══════════════════════════════════════════════════════════════╝");
    System.out.println("Type 'help' to see available commands\n");

    while (true) {
      // Contextual prompt: show connected server or [local]
      System.out.print(session.isConnected()
          ? "[" + session.getCurrentServer() + "] > "
          : "[local] > ");
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        continue;
      }

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

      // Single-word client commands
      switch (word1) {

        case "join" -> {
          // Rebuild address from tokens (e.g. "join 192.168.1.1 12345" → "192.168.1.1:12345")
          String addr = word2.isBlank() ? null : word2 + (rest.isBlank() ? "" : ":" + rest);
          new JoinCommand(session, addr).execute();
        }

        case "ping" ->
          new PingCommand(session).execute();

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
   * Dispatches {@code server list / start / stop} to the matching Command class.
   *
   * @param sub  Sub-command ({@code list}, {@code start}, or {@code stop}).
   * @param args Additional arguments (e.g. a port number for {@code start}).
   */
  private void dispatchServerCommand(String sub, String args) {
    switch (sub) {

      case "list" ->
        new ServerListCommand().execute();

      case "start" -> {
        // null controller: standalone client mode.
        // For a full game server with game logic, launch GameServer directly.
        String portArg = args.isBlank() ? null : args.trim();
        String[] startArgs = portArg == null ? new String[0] : new String[] { portArg };
        new ServerStartCommand(null, startArgs).execute();
      }

      case "stop" ->
        new ServerStopCommand().execute();

      default ->
        System.out.println("Unknown server command: '" + sub + "'."
            + " Use: server list | server start [PORT] | server stop");
    }
  }

  /**
   * Main method: creates a Client instance and starts the command loop.
   *
   * @param args Command-line arguments (not used).
   */
  public static void main(String[] args) {
    new Client().run();
  }
}