package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li>Handles management commands: {@code STATUS}, {@code PLAYERS}, {@code SCOREBOARD},
 *       {@code NEW}.
 * </ul>
 *
 * <p>All game logic is delegated to {@link GameController} instances produced by the
 * injected {@link GameControllerFactory}. Each game session gets its own fresh controller
 * so that game state is fully isolated between concurrent sessions.
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

  /** Output streams of currently connected clients — used by {@link #stop()} to send BYE. */
  private final Set<PrintWriter> connectedClients =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Creates a new game server instance.
   *
   * @param serverName name broadcast via UDP discovery.
   * @param tcpPort TCP listening port.
   * @param controllerFactory factory producing a fresh {@link GameController} per session.
   * @param daemon if {@code true}, the server runs headless without an interactive console.
   */
  public GameServer(
      String serverName, int tcpPort, GameControllerFactory controllerFactory, boolean daemon) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
    this.controllerFactory = controllerFactory;
    this.daemon = daemon;
  }

  /**
   * Convenience constructor for non-daemon mode.
   *
   * @param serverName name broadcast via UDP discovery.
   * @param tcpPort TCP listening port.
   * @param controllerFactory factory producing a fresh {@link GameController} per session.
   */
  public GameServer(String serverName, int tcpPort, GameControllerFactory controllerFactory) {
    this(serverName, tcpPort, controllerFactory, false);
  }

  /**
   * Starts the TCP server and the UDP discovery service.
   *
   * @throws IOException with an explicit message if the port is already in use.
   */
  public void start() throws IOException {
    if (running) {
      System.out.println("Server is already running on port " + tcpPort + ".");
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
    clientPool = Executors.newCachedThreadPool();

    DiscoveryService discovery = new DiscoveryService(serverName, tcpPort);
    discoveryThread = new Thread(discovery, "discovery-thread");
    discoveryThread.setDaemon(true);
    discoveryThread.start();

    System.out.println("Server '" + serverName + "' started on port " + tcpPort + ".");
    System.out.println("Discovery broadcasting on UDP 12346.");

    while (running) {
      try {
        Socket client = serverSocket.accept();
        client.setSoTimeout(CLIENT_TIMEOUT_MS);
        System.out.println("Client connected: " + client.getInetAddress());
        clientPool.submit(() -> handleClient(client));
      } catch (IOException e) {
        if (running) {
          System.err.println("Error accepting client: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Stops the server: sends {@code BYE} to all connected players, closes the server socket,
   * and interrupts the discovery thread.
   */
  public void stop() {
    if (!running) {
      System.out.println("Server is not running.");
      return;
    }

    running = false;

    for (PrintWriter out : connectedClients) {
      try {
        out.println("BYE");
      } catch (Exception e) {
        // Ignore client notification failures during shutdown.
      }
    }
    connectedClients.clear();

    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      System.err.println("Error closing server socket: " + e.getMessage());
    }

    if (clientPool != null) {
      clientPool.shutdownNow();
    }

    if (discoveryThread != null) {
      discoveryThread.interrupt();
    }

    System.out.println("Server stopped.");
  }

  /**
   * Returns whether the server is currently accepting connections.
   *
   * @return {@code true} if the server is currently accepting connections
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the TCP port the server is listening on.
   *
   * @return the TCP listening port
   */
  public int getPort() {
    return tcpPort;
  }

  /**
   * Handles one connected client: reads lines, routes network protocol commands
   * (PING, QUIT, STATUS, PLAYERS, SCOREBOARD, NEW, MOVE) and forwards game commands
   * to the appropriate {@link GameSession}.
   */
  private void handleClient(Socket client) {
    try (
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out =
            new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)) {

      connectedClients.add(out);

      // Handshake: first message must be REGISTER <id> <name>
      String handshake = in.readLine();
      if (handshake == null || !handshake.startsWith("REGISTER ")) {
        out.println("ERROR: First message must be REGISTER <id> <name>");
        return;
      }

      String[] parts = handshake.split("\\s+", 3);
      if (parts.length < 3) {
        out.println("ERROR: Usage: REGISTER <id> <name>");
        return;
      }

      String playerId = parts[1];
      String playerName = parts[2];

      PlayerSession player = registry.registerPlayer(playerId, playerName, out);
      if (player == null) {
        out.println("ERROR: Player ID '" + playerId + "' is already taken.");
        return;
      }

      out.println("WELCOME " + playerId);
      System.out.println("Player registered: " + playerId + " (" + playerName + ")");

      processMessages(in, out, player);

    } catch (SocketTimeoutException e) {
      System.out.println("Client timed out after 1 minute of inactivity.");
    } catch (IOException e) {
      System.out.println("Client disconnected: " + e.getMessage());
    }
  }

  /**
   * Main message loop for a registered player.
   *
   * @param in reader from the client socket.
   * @param out writer to the client socket.
   * @param player the registered player session.
   * @throws IOException if the socket is closed unexpectedly.
   */
  private void processMessages(BufferedReader in, PrintWriter out, PlayerSession player)
      throws IOException {

    String line;
    while ((line = in.readLine()) != null) {
      System.out.println("[" + player.getId() + "] " + line);
      String[] tokens = line.trim().split("\\s+", 2);
      String cmd = tokens[0].toUpperCase();
      String rest = tokens.length > 1 ? tokens[1] : "";

      switch (cmd) {
        case "PING" -> {
          long t0 = System.currentTimeMillis();
          long elapsed = System.currentTimeMillis() - t0;
          out.println("PONG TIME=" + elapsed + "ms");
        }
        case "QUIT" -> {
          out.println("BYE");
          registry.removePlayer(player.getId());
          connectedClients.remove(out);
          System.out.println("Player " + player.getId() + " disconnected.");
          return;
        }
        case "STATUS" -> {
          int playerCount = registry.getPlayerCount();
          int sessionCount = registry.getActiveSessionCount();
          out.println(
              "STATUS port=" + tcpPort + " players=" + playerCount + " sessions=" + sessionCount);
        }
        case "PLAYERS" -> {
          String list = registry.getPlayersFormatted();
          out.println(list.isEmpty() ? "PLAYERS none" : "PLAYERS\n" + list);
        }
        case "SCOREBOARD" -> out.println("SCOREBOARD\n" + registry.getScoreboardFormatted());
        case "NEW" -> {
          if (rest.isBlank()) {
            out.println("ERROR: Usage: NEW <player_id> [player_id2 ...]");
            break;
          }
          String[] participants = rest.split("\\s+");
          handleNewGame(out, player, participants);
        }
        case "MOVE" -> {
          GameSession session = registry.getSessionForPlayer(player.getId());
          if (session == null) {
            out.println("ERROR: Not in a game. Use NEW to start one.");
          } else {
            String error = session.handleMove(player, rest);
            if (error != null) {
              out.println(error);
            } else {
              out.println("OK");
            }
          }
        }
        default -> out.println("ERROR: Unknown command '" + cmd + "'.");
      }
    }
  }

  /**
   * Creates a new game session between the requesting player and the listed participants.
   *
   * @param out writer to the requesting client.
   * @param requester the player who issued the NEW command.
   * @param participantIds IDs of all players to include (requester + others).
   */
  private void handleNewGame(PrintWriter out, PlayerSession requester, String[] participantIds) {
    java.util.List<PlayerSession> participants = new java.util.ArrayList<>();
    for (String pid : participantIds) {
      PlayerSession p = registry.getPlayer(pid);
      if (p == null) {
        out.println("ERROR: Player '" + pid + "' not found.");
        return;
      }
      if (!p.isIdle()) {
        out.println("ERROR: Player '" + pid + "' is already in a game.");
        return;
      }
      participants.add(p);
    }

    // Each session gets a fresh controller — isolated game state, no shared mutable state.
    GameController sessionController = controllerFactory.create();
    GameSession session = registry.createSession(participants, sessionController);

    String playerList = String.join(" ", participantIds);
    String firstId = participants.get(0).getId();
    for (PlayerSession p : participants) {
      p.send("GAME_START session=" + session.getSessionId()
          + " players=" + playerList + " first=" + firstId);
    }

    System.out.println("Game session started: " + session.getSessionId());
  }

  /**
   * Entry point for standalone server mode.
   *
   * <p>Flags: {@code -s [PORT]} / {@code --server}, {@code -d} / {@code --daemon}.
   */
  public static void main(String[] args) {
    int port = DEFAULT_PORT;
    boolean daemonMode = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-s", "--server" -> {
          if (i + 1 < args.length) {
            String portArg = args[++i];
            try {
              port = Integer.parseInt(args[++i]);
            } catch (NumberFormatException e) {
              System.out.println("Invalid port '" + args[i]
                    + "'. Using default port: " + DEFAULT_PORT);
              port = DEFAULT_PORT;
            }
          }
        }
        case "-d", "--daemon" -> daemonMode = true;
        default -> {

        }
      }
    }

    // Always use HeadlessView server-side: no terminal output needed.
    GameControllerFactory factory = () -> new GameController(new HeadlessView());
    GameServer server = new GameServer("GameServer", port, factory, daemonMode);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  System.out.println("\nShutting down server...");
                  server.stop();
                }));

    try {
      server.start();
    } catch (IOException e) {
      System.err.println("Failed to start server: " + e.getMessage());
    }
  }
}