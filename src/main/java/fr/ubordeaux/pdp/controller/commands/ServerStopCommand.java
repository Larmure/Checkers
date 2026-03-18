package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Server management command: {@code server stop}
 *
 * <p>Stops the running game server and notifies all connected clients with {@code BYE}.
 * Restores the session to {@link fr.ubordeaux.pdp.serveur.ClientMode#LOCAL},
 * re-enabling all client commands.
 *
 * <p>Category: [NETWORK — SERVER MANAGEMENT]
 */
public class ServerStopCommand implements Command, Helpable {

  private final ClientSession session;

  /**
   * @param session the client session whose mode will be restored to LOCAL on stop.
   */
  public ServerStopCommand(ClientSession session) {
    this.session = session;
  }

  @Override
  public void execute() {
    if (ServerStartCommand.activeServer == null
      || !ServerStartCommand.activeServer.isRunning()) {
      System.out.println("No server is currently running.");
      return;
    }

    ServerStartCommand.activeServer.stop();
    ServerStartCommand.activeServer = null;

    // Restore LOCAL mode: all commands become available again
    if (session != null) {
      session.exitServerMode();
    }
  }

  /**
   * Returns the help message for this command.
   *
   * @return a string describing how to use the server stop command
   */
  @Override
  public String getHelp() {
    return "server stop — Stops the running game server and notifies all connected clients.";
  }
}