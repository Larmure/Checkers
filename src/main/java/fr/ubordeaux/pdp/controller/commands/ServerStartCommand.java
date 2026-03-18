package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.GameControllerFactory;
import fr.ubordeaux.pdp.server.GameServer;
import fr.ubordeaux.pdp.view.ConsoleView;

/**
 * Server management command: {@code server start [PORT]}
 *
 * <p>Starts a game server on the specified TCP port (default: 12345). On success, switches
 * the session to {@link fr.ubordeaux.pdp.serveur.ClientMode#SERVER}, which disables all
 * client commands ({@code join}, {@code ping}) until the server is stopped.
 *
 * <p>The server receives a {@link GameControllerFactory} that creates a fresh {@link
 * GameController} per game session. If a {@code controller} is provided (unified shell
 * mode), that instance is reused for the first session; otherwise a new one is created from
 * a {@link ConsoleView}.
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

  private final GameController controller;
  private final String[] args;
  private final ClientSession session;

  /**
   * @param controller the game controller to reuse for the first session (may be {@code null}).
   * @param args optional first element is the port number as a string.
   * @param session the client session whose mode will be updated on start/stop.
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

    // Build a factory: reuse the provided controller for the first session,
    // then create fresh controllers (with their own ConsoleView) for subsequent ones.
    GameController provided = this.controller;
    GameControllerFactory factory =
          provided != null
                ? new SingleThenFreshFactory(provided)
                : () -> new GameController(new ConsoleView());

    final int finalPort = port;
    activeServer = new GameServer("GameServer", finalPort, factory);
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
    } catch (InterruptedException ignored) {
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

  /**
   * Factory that returns the provided controller on the first call, then creates a fresh one
   * (with its own {@link ConsoleView}) for all subsequent calls.
   */
  private static final class SingleThenFreshFactory implements GameControllerFactory {

    private GameController first;

    SingleThenFreshFactory(GameController first) {
      this.first = first;
    }

    @Override
    public GameController create() {
      if (first != null) {
        GameController c = first;
        first = null;
        return c;
      }
      return new GameController(new ConsoleView());
    }
  }
}