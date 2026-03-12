package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

/**
 * Network command: {@code server stop}.
 *
 * <p>Stops the currently running game server. All connected clients receive a
 * {@code BYE} message before the server socket is closed.
 */
public class ServerStopCommand implements Command, Helpable {

  @Override
  public void execute() {
    if (ServerStartCommand.activeServer == null
        || !ServerStartCommand.activeServer.isRunning()) {
      System.out.println("No server is currently running.");
      return;
    }

    ServerStartCommand.activeServer.stop();
    ServerStartCommand.activeServer = null;
    System.out.println("Server stopped.");
  }

  @Override
  public String getHelp() {
    return "server stop - Stops the running game server and notifies all connected clients.";
  }
}