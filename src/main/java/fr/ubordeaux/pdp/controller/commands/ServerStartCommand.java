package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.GameControllerFactory;
import fr.ubordeaux.pdp.server.GameServer;
import fr.ubordeaux.pdp.view.HeadlessView;

/**
 * Server management command: {@code server start [PORT]}.
 *
 * <p>Starts a game server on the specified TCP port (default: 12345). On success, switches
 * the session to {@link fr.ubordeaux.pdp.server.ClientMode#SERVER}, which disables all
 * client commands ({@code join}, {@code ping}) until the server is stopped.
 *
 * <p>The server always receives a {@link GameControllerFactory} that creates a dedicated
 * {@link GameController} with a {@link HeadlessView} per game session. The local shell
 * controller is intentionally never shared with the server to avoid mixing the host
 * player's local state with server-side session state.
 *
 * <p>Category: [NETWORK — SERVER MANAGEMENT]
 */
public class ServerStartCommand implements Command, Helpable {

  private static final int DEFAULT_PORT = 12345;

  /**
   * Reference shared with {@link ServerStopCommand} so the latter can stop the server that
   * this command started.
   */
  public static GameServer activeServer = null;

  private final String[] args;
  private final ClientSession session;

  /**
   * Creates a server-start command.
   *
   * @param controller unused — kept for API compatibility with {@code Utils.COMMANDS_MAP}.
   *     The server always creates its own controllers via {@link HeadlessView}.
   * @param args optional first element is the port number as a string.
   * @param session the client session whose mode will be updated on start/stop.
   */
  public ServerStartCommand(GameController controller, String[] args, ClientSession session) {
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

    // Each game session gets its own dedicated controller — no shared state with the shell.
    GameControllerFactory factory = () -> new GameController(new HeadlessView());

    final int finalPort = port;
    boolean guiOnly = session != null && session.isGuiMode();
    activeServer = new GameServer("GameServer", finalPort, factory, false, guiOnly);
    GameServer ref = activeServer;

    Thread serverThread =
        new Thread(
            () -> {
              try {
                ref.start();
              } catch (java.io.IOException e) {
                System.out.println("Cannot start server: " + e.getMessage());
                activeServer = null;
                if (session != null) {
                  session.exitServerMode();
                }
              }
            },
            "game-server-thread");
    serverThread.setDaemon(true);
    serverThread.start();

    // Brief wait to let the server socket initialize.
    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    if (activeServer != null && activeServer.isRunning()) {
      System.out.println("Server started on port " + finalPort + ".");
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