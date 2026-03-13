package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.serveur.ClientSession;
import fr.ubordeaux.pdp.serveur.GameServer;

/**
 * Server management command: {@code server start [PORT]}
 *
 * <p>Starts a game server on the specified TCP port (default: 12345).
 * On success, switches the session to {@link fr.ubordeaux.pdp.serveur.ClientMode#SERVER},
 * which disables all client commands ({@code join}, {@code ping}) until the server is stopped.
 *
 * <p>Displays a clear error message if the port is already in use.
 *
 * <p>Category: [NETWORK — SERVER MANAGEMENT]
 */
public class ServerStartCommand implements Command, Helpable {

  private static final int DEFAULT_PORT = 12345;

  /**
   * Reference shared with {@link ServerStopCommand} so the latter can stop
   * the server that this command started.
   */
  public static GameServer activeServer = null;

  private final GameController controller;
  private final String[] args;
  private final ClientSession session;

  /**
   * @param controller the game controller to inject into the server (may be {@code null}
   *                   in standalone client mode).
   * @param args       optional first element is the port number as a string.
   * @param session    the client session whose mode will be updated on start.
   */
  public ServerStartCommand(GameController controller, String[] args, ClientSession session) {
    this.controller = controller;
    this.args = args;
    this.session = session;
  }

  @Override
  public void execute() {
    if (activeServer != null && activeServer.isRunning()) {
      System.out.println("A server is already running on port " + activeServer.getPort() + ".");
      return;
    }

    int port = DEFAULT_PORT;
    if (args != null && args.length > 0 && !args[0].isBlank()) {
      try {
        port = Integer.parseInt(args[0].trim());
      } catch (NumberFormatException e) {
        System.out.println("Invalid port '" + args[0] + "'. Using default: " + DEFAULT_PORT);
      }
    }

    final int finalPort = port;
    activeServer = new GameServer("GameServer", finalPort, controller);
    GameServer ref = activeServer;

    Thread serverThread = new Thread(() -> {
      try {
        ref.start();
      } catch (java.io.IOException e) {
        System.out.println("Cannot start server: " + e.getMessage());
        activeServer = null;
        // Restore LOCAL mode if startup failed
        if (session != null) {
          session.exitServerMode();
        }
      }
    }, "game-server-thread");
    serverThread.setDaemon(true);
    serverThread.start();

    // Brief wait to let the server socket initialize
    try {
      Thread.sleep(300);
    } catch (InterruptedException ignored) {
    }

    if (activeServer != null && activeServer.isRunning()) {
      System.out.println("Server started on port " + finalPort + ".");
      // Switch to SERVER mode: client commands are now disabled
      if (session != null) {
        session.enterServerMode();
      }
    }
  }

  @Override
  public String getHelp() {
    return "server start [PORT] — Starts a game server on PORT (default: 12345).\n"
      + "                       Client commands (join, ping) are disabled while the server runs.";
  }
}