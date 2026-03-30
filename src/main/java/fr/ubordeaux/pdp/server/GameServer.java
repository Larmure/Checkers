package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Multi-client TCP game server.
 *
 * <p>Accepts client connections, registers players, creates game sessions,
 * and routes network commands to the appropriate session.
 */
public class GameServer {

  private static final int DEFAULT_PORT = 12345;
  private static final int CLIENT_TIMEOUT_MS = 180_000;
  /** Client inactivity timeout: 3 minutes. */

  private final int tcpPort;
  private final String serverName;
  private final boolean daemon;
  private final GameControllerFactory controllerFactory;
  private final GameRegistry registry = new GameRegistry();

  private Thread discoveryThread;
  private ServerSocket serverSocket;
  private ExecutorService clientPool;
  private volatile boolean running = false;

  private final Set<PrintWriter> connectedClients =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Creates a new game server.
   *
   * @param serverName the broadcast server name
   * @param tcpPort the TCP listening port
   * @param controllerFactory factory used to create a new controller for each session
   * @param daemon whether the server runs in daemon mode
   */
  public GameServer(
        String serverName, int tcpPort, GameControllerFactory controllerFactory, boolean daemon) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
    this.controllerFactory = controllerFactory;
    this.daemon = daemon;
  }

  /**
   * Creates a new non-daemon game server.
   *
   * @param serverName the broadcast server name
   * @param tcpPort the TCP listening port
   * @param controllerFactory factory used to create a new controller for each session
   */
  public GameServer(String serverName, int tcpPort, GameControllerFactory controllerFactory) {
    this(serverName, tcpPort, controllerFactory, false);
  }

  /**
   * Starts the server socket and the discovery service.
   *
   * @throws IOException if the server cannot bind to the TCP port
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
   * Stops the server and closes all active resources.
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
      } catch (Exception ignored) {
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
   * Returns whether the server is currently running.
   *
   * @return {@code true} if the server is running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the TCP port used by the server.
   *
   * @return the listening TCP port
   */
  public int getPort() {
    return tcpPort;
  }

  /**
   * Handles a newly connected client socket.
   *
   * @param client the client socket
   */
  private void handleClient(Socket client) {
    try (
          BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
          PrintWriter out =
                new PrintWriter(
                      new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)) {

      connectedClients.add(out);

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

      tryAutoStart();

      processMessages(in, out, player);

    } catch (SocketTimeoutException e) {
      System.out.println("Client timed out after 3 minute of inactivity.");
    } catch (IOException e) {
      System.out.println("Client disconnected: " + e.getMessage());
    }
  }

  /**
   * Processes all messages received from a connected player.
   *
   * @param in the client input stream
   * @param out the client output stream
   * @param player the registered player
   * @throws IOException if reading from the socket fails
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
              out.println("MOVE_OK " + rest);
            }
          }
        }
        default -> out.println("ERROR: Unknown command '" + cmd + "'.");
      }
    }
  }

  /**
   * Automatically starts a game when at least two idle players are available.
   */
  private synchronized void tryAutoStart() {
    List<PlayerSession> idlePlayers =
          registry.getAllPlayers().stream()
                .filter(PlayerSession::isIdle)
                .collect(Collectors.toList());

    if (idlePlayers.size() < 2) {
      if (idlePlayers.size() == 1) {
        idlePlayers.get(0).send("WAITING Waiting for another player to join...");
      }
      return;
    }

    List<PlayerSession> pair = idlePlayers.subList(0, 2);
    GameController sessionController = controllerFactory.create();

    sessionController.startNewGame(Configuration.getDefaultConfiguration());

    GameSession session = registry.createSession(pair, sessionController);

    String playerList = pair.get(0).getId() + " " + pair.get(1).getId();
    String firstId = pair.get(0).getId();

    for (PlayerSession p : pair) {
      p.send(
            "GAME_START session="
                  + session.getSessionId()
                  + " players="
                  + playerList
                  + " first="
                  + firstId);
    }

    System.out.println(
          "Auto-started game: " + session.getSessionId() + " between " + playerList);
  }

  /**
   * Creates a new game session with the given participants.
   *
   * @param out the requester output stream
   * @param requester the player requesting the game
   * @param participantIds the players to include in the session
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

    GameController sessionController = controllerFactory.create();
    sessionController.startNewGame(Configuration.getDefaultConfiguration());

    GameSession session = registry.createSession(participants, sessionController);

    String playerList = String.join(" ", participantIds);
    String firstId = participants.get(0).getId();

    for (PlayerSession p : participants) {
      p.send(
            "GAME_START session="
                  + session.getSessionId()
                  + " players="
                  + playerList
                  + " first="
                  + firstId);
    }

    System.out.println("Game session started: " + session.getSessionId());
  }

  /**
   * Starts the server in standalone mode.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    int port = DEFAULT_PORT;
    boolean daemonMode = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-s", "--server" -> {
          if (i + 1 < args.length) {
            try {
              port = Integer.parseInt(args[++i]);
            } catch (NumberFormatException ignored) {
              port = DEFAULT_PORT;
            }
          }
        }
        case "-d", "--daemon" -> daemonMode = true;
        default -> {
        }
      }
    }

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