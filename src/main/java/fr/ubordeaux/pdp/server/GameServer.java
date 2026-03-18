package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.GameView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-client TCP game server.
 *
 * <p>Responsibilities (network layer only):
 *
 * <ul>
 *   <li>Accepts concurrent client connections via a cached thread pool.
 *   <li>Maintains the player registry and active game sessions ({@link GameRegistry}).
 *   <li>Routes {@code MOVE} commands to the correct {@link GameSession}.
 *   <li>Broadcasts {@code OPPONENT_MOVE} to the other players in the session.
 *   <li>Validates that moves come from the correct player (anti-cheat).
 *   <li>Handles management commands: {@code STATUS}, {@code PLAYERS}, {@code SCOREBOARD},
 *       {@code NEW}.
 * </ul>
 *
 * <p>All game logic is delegated to {@link GameController} instances produced by the
 * injected {@link GameControllerFactory}. Shared state is protected by {@link GameRegistry}.
 *
 * <p>Command-line flags:
 *
 * <pre>
 *   -s [PORT]  /  --server [PORT]   start in server mode on PORT (default 12345)
 *   -d         /  --daemon          headless mode (no interactive console)
 * </pre>
 */
public class GameServer {

  private static final int DEFAULT_PORT = 12345;
  /** Inactivity timeout for connected clients: 1 minute. */
  private static final int CLIENT_TIMEOUT_MS = 60_000;

  private final int tcpPort;
  private final String serverName;
  private final boolean daemon;
  private final GameControllerFactory controllerFactory;
  private final GameRegistry registry = new GameRegistry();

  private Thread discoveryThread;
  private ServerSocket serverSocket;
  private ExecutorService clientPool;
  private volatile boolean running = false;

  /**
   * @param serverName name broadcast via UDP discovery.
   * @param tcpPort TCP listening port.
   * @param controllerFactory factory producing a fresh {@link GameController} per session.
   * @param daemon if {@code true}, the server runs headless without an interactive console.
   */
  public GameServer(
        String serverName,
        int tcpPort,
        GameControllerFactory controllerFactory,
        boolean daemon) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
    this.controllerFactory = controllerFactory;
    this.daemon = daemon;
  }

  /** Convenience constructor for non-daemon mode. */
  public GameServer(String serverName, int tcpPort, GameControllerFactory factory) {
    this(serverName, tcpPort, factory, false);
  }

  /**
   * Backward-compatible constructor that wraps a single controller in a factory.
   *
   * @param serverName name broadcast via UDP.
   * @param tcpPort TCP listening port.
   * @param controller the controller shared across all sessions (no isolation).
   */
  public GameServer(String serverName, int tcpPort, GameController controller) {
    this(serverName, tcpPort, () -> controller, false);
  }

  /**
   * Starts the TCP server and the UDP discovery service.
   *
   * @throws IOException with a descriptive message if the port is already in use.
   */
  public void start() throws IOException {
    if (running) {
      System.out.println("Server already running on port " + tcpPort + ".");
      return;
    }

    try {
      serverSocket = new ServerSocket(tcpPort);
    } catch (java.net.BindException e) {
      throw new IOException(
            "Port "
                  + tcpPort
                  + " is already in use. Stop the existing server or choose another port.",
            e);
    }

    running = true;
    clientPool =
          Executors.newCachedThreadPool(
                r -> {
                  Thread t = new Thread(r);
                  t.setDaemon(true);
                  return t;
                });

    DiscoveryService discovery = new DiscoveryService(serverName, tcpPort);
    discoveryThread = new Thread(discovery, "discovery-thread");
    discoveryThread.setDaemon(true);
    discoveryThread.start();

    System.out.println("Server '" + serverName + "' started on port " + tcpPort + ".");
    System.out.println("UDP discovery broadcasting on port 12346.");
    if (daemon) {
      System.out.println("Running in daemon (headless) mode.");
    }

    while (running) {
      try {
        Socket client = serverSocket.accept();
        client.setSoTimeout(CLIENT_TIMEOUT_MS);
        System.out.println("New connection: " + client.getInetAddress());
        clientPool.submit(() -> handleClient(client));
      } catch (IOException e) {
        if (running) {
          System.err.println("Accept error: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Stops the server:
   *
   * <ol>
   *   <li>Sends {@code BYE} to every connected player.
   *   <li>Ends all active game sessions.
   *   <li>Closes the server socket.
   *   <li>Shuts down the thread pool and the discovery service.
   * </ol>
   */
  public void stop() {
    if (!running) {
      System.out.println("Server is not running.");
      return;
    }
    running = false;

    registry.getAllPlayers().forEach(p -> p.send("BYE"));
    registry.getActiveSessions().forEach(s -> s.end(null));

    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      System.err.println("Error closing socket: " + e.getMessage());
    }

    if (clientPool != null) {
      clientPool.shutdownNow();
    }
    if (discoveryThread != null) {
      discoveryThread.interrupt();
    }

    System.out.println("Server stopped.");
  }

  /** @return {@code true} if the server is currently accepting connections. */
  public boolean isRunning() {
    return running;
  }

  /** @return the TCP port the server is listening on. */
  public int getPort() {
    return tcpPort;
  }

  /**
   * Handles the full lifecycle of one client connection:
   * registration handshake → command loop → cleanup.
   */
  private void handleClient(Socket socket) {
    PlayerSession player = null;
    try (
          BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          PrintWriter out =
                new PrintWriter(
                      new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)) {
      player = awaitRegistration(in, out);
      if (player == null) {
        return;
      }

      System.out.println("Registered: " + player.getId() + " (" + player.getName() + ")");
      out.println("WELCOME " + player.getId());

      String line;
      while ((line = in.readLine()) != null) {
        System.out.println("[" + player.getId() + "] << " + line);
        try {
          processMessage(player, out, line.trim());
        } catch (ClientQuitException e) {
          break;
        }
      }
    } catch (SocketTimeoutException e) {
      System.out.println(
            "Timeout: " + (player != null ? player.getId() : socket.getInetAddress()));
    } catch (IOException e) {
      System.out.println(
            "Disconnected: " + (player != null ? player.getId() : socket.getInetAddress()));
    } finally {
      if (player != null) {
        registry.removePlayer(player.getId());
        System.out.println("Removed player: " + player.getId());
      }
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }

  /**
   * Reads the mandatory {@code REGISTER} handshake from a new connection.
   *
   * <p>Expected format: {@code REGISTER <id> <name>}
   *
   * @return the registered {@link PlayerSession}, or {@code null} on failure.
   */
  private PlayerSession awaitRegistration(BufferedReader in, PrintWriter out) throws IOException {
    String line = in.readLine();
    if (line == null) {
      return null;
    }

    String[] parts = line.trim().split("\\s+", 3);
    if (parts.length < 3 || !"REGISTER".equalsIgnoreCase(parts[0])) {
      out.println("ERROR: First message must be 'REGISTER <id> <name>'.");
      return null;
    }

    PlayerSession player = registry.registerPlayer(parts[1], parts[2], out);
    if (player == null) {
      out.println("ERROR: Player ID '" + parts[1] + "' is already taken.");
      return null;
    }
    return player;
  }

  /**
   * Routes one client message to the appropriate handler.
   *
   * <p>Protocol commands ({@code PING}, {@code QUIT}) are handled inline. Management
   * and game commands are delegated to dedicated methods.
   *
   * @throws ClientQuitException when the client sends {@code QUIT}.
   */
  private void processMessage(PlayerSession player, PrintWriter out, String line) {
    if (line.isEmpty()) {
      return;
    }

    String[] tokens = line.split("\\s+", 2);
    String command = tokens[0].toUpperCase();
    String args = tokens.length > 1 ? tokens[1] : "";

    switch (command) {
      case "PING" -> out.println("PONG TIME=0ms");
      case "QUIT" -> {
        out.println("BYE");
        throw new ClientQuitException();
      }
      case "STATUS" -> handleStatus(out);
      case "PLAYERS" -> handlePlayers(out);
      case "SCOREBOARD" -> handleScoreboard(out);
      case "NEW" -> handleNew(player, out, args);
      case "MOVE" -> handleMove(player, out, args);
      default -> out.println("ERROR: Unknown command '" + command + "'.");
    }
  }

  /**
   * {@code STATUS} — reports port, connected clients, and active games.
   */
  private void handleStatus(PrintWriter out) {
    out.println(
          "STATUS port="
                + tcpPort
                + " clients="
                + registry.getPlayerCount()
                + " games="
                + registry.getActiveSessionCount());
  }

  /**
   * {@code PLAYERS} — lists each player's ID, name, and status.
   */
  private void handlePlayers(PrintWriter out) {
    StringBuilder sb = new StringBuilder("PLAYERS\n");
    for (PlayerSession player : registry.getAllPlayers()) {
      sb.append(
            String.format(
                  "  %-10s %-15s %s%n",
                  player.getId(), player.getName(), player.getStatus().name().toLowerCase()));
    }
    out.println(sb.toString().trim());
  }

  /**
   * {@code SCOREBOARD} — lists wins, losses, and games played, sorted by wins descending.
   */
  private void handleScoreboard(PrintWriter out) {
    StringBuilder sb = new StringBuilder("SCOREBOARD\n");
    sb.append(String.format("  %-10s %-15s %5s %5s %5s%n", "ID", "NAME", "WINS", "LOSS", "GAMES"));
    sb.append("  ").append("-".repeat(48)).append("\n");

    registry.getAllPlayers().stream()
          .sorted((a, b) -> b.getWins() - a.getWins())
          .forEach(
                p ->
                      sb.append(
                            String.format(
                                  "  %-10s %-15s %5d %5d %5d%n",
                                  p.getId(), p.getName(), p.getWins(), p.getLosses(), p.getGamesPlayed())));

    out.println(sb.toString().trim());
  }

  /**
   * {@code NEW <PLAYER_ID> [PLAYER_ID2 ...]} — creates a game session.
   *
   * <p>The requester is always included as the first participant. All listed players
   * must exist and be in {@code IDLE} status.
   */
  private void handleNew(PlayerSession requester, PrintWriter out, String args) {
    if (args.isBlank()) {
      out.println("ERROR: Usage: NEW <PLAYER_ID> [PLAYER_ID2 ...]");
      return;
    }
    if (!requester.isIdle()) {
      out.println("ERROR: You are already in a game.");
      return;
    }

    List<PlayerSession> participants = new ArrayList<>();
    participants.add(requester);

    for (String id : args.trim().split("\\s+")) {
      if (id.equals(requester.getId())) {
        continue;
      }
      PlayerSession target = registry.getPlayer(id);
      if (target == null) {
        out.println("ERROR: Player '" + id + "' does not exist.");
        return;
      }
      if (!target.isIdle()) {
        out.println(
              "ERROR: Player '"
                    + id
                    + "' is not available ("
                    + target.getStatus().name().toLowerCase()
                    + ").");
        return;
      }
      participants.add(target);
    }

    if (participants.size() < 2) {
      out.println("ERROR: Specify at least one opponent.");
      return;
    }

    GameController controller = controllerFactory.create();
    GameSession session = registry.createSession(participants, controller);

    String playerList =
          participants.stream()
                .map(PlayerSession::getId)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

    for (PlayerSession p : participants) {
      p.send(
            "GAME_START session="
                  + session.getSessionId()
                  + " players="
                  + playerList
                  + " first="
                  + session.getCurrentPlayer().getId());
    }

    out.println("OK game=" + session.getSessionId() + " players=" + participants.size());
    System.out.println("Session created: " + session);
  }

  /**
   * {@code MOVE <from-to>} — forwards a move to the player's active game session.
   *
   * <p>Anti-cheat: {@link GameSession#handleMove} verifies the turn order and move
   * legality before routing the {@code OPPONENT_MOVE} notification.
   */
  private void handleMove(PlayerSession player, PrintWriter out, String args) {
    GameSession session = registry.getSessionForPlayer(player.getId());
    if (session == null) {
      out.println("ERROR: You are not in an active game. Use NEW to start one.");
      return;
    }

    String error = session.handleMove(player, args);
    if (error != null) {
      out.println(error);
    } else {
      out.println("OK");
    }
  }

  /**
   * Internal signal thrown when a client sends {@code QUIT}, used as a non-local exit
   * from the command loop inside {@link #handleClient}.
   */
  private static final class ClientQuitException extends RuntimeException {

    ClientQuitException() {
      super(null, null, true, false);
    }
  }

  /**
   * Entry point.
   *
   * <p>Supported flags:
   *
   * <ul>
   *   <li>{@code -s [PORT]} / {@code --server [PORT]} — start on PORT (default 12345).
   *   <li>{@code -d} / {@code --daemon} — headless mode.
   * </ul>
   */
  public static void main(String[] args) {
    String name = "GameServer-1";
    int port = DEFAULT_PORT;
    boolean isDaemon = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-s", "--server" -> {
          if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
            try {
              port = Integer.parseInt(args[++i]);
            } catch (NumberFormatException ignored) {
            }
          }
        }
        case "-d", "--daemon" -> isDaemon = true;
        default -> {
          try {
            port = Integer.parseInt(args[i]);
          } catch (NumberFormatException e) {
            name = args[i];
          }
        }
      }
    }

    final boolean finalDaemon = isDaemon;
    GameControllerFactory factory =
          () -> {
            GameView view =
                  finalDaemon
                        ? new fr.ubordeaux.pdp.view.HeadlessView()
                        : new fr.ubordeaux.pdp.view.ConsoleView();
            return new GameController(view);
          };

    GameServer server = new GameServer(name, port, factory, isDaemon);
    Runtime.getRuntime()
          .addShutdownHook(
                new Thread(
                      () -> {
                        System.out.println("\nShutting down...");
                        server.stop();
                      }));

    try {
      server.start();
    } catch (IOException e) {
      System.err.println("Failed to start: " + e.getMessage());
    }
  }
}