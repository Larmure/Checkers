package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.GameRegistry;
import fr.ubordeaux.pdp.server.GameServer;

/**
 * Server management command: {@code server status}.
 *
 * <p>Displays the server's TCP port, the number of connected clients,
 * and the number of active game sessions.
 */
public class ServerStatusCommand implements Command, Helpable {

  private final int port;
  private final GameRegistry registry;

  /**
   * Constructor used for real execution.
   *
   * @param port the TCP port the server is listening on
   * @param registry the shared player and session registry
   */
  public ServerStatusCommand(int port, GameRegistry registry) {
    this.port = port;
    this.registry = registry;
  }

  /**
   * Constructor used only for help/metadata.
   */
  public ServerStatusCommand() {
    this.port = 0;
    this.registry = null;
  }

  @Override
  public void execute() {
    if (registry == null) {
      System.out.println("Server status unavailable.");
      return;
    }

    System.out.printf(
        "Server status: port=%d  clients=%d  games=%d%n",
        port,
        registry.getPlayerCount(),
        registry.getActiveSessionCount());
  }

  @Override
  public String getHelp() {
    return "server status — Shows the server port, number of connected clients,"
        + " and number of active game sessions.";
  }
}