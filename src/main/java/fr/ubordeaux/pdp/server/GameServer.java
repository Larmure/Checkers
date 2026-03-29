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
  private final Invitationmanager invitationManager = new Invitationmanager();

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

    // Start the invitation expiry sweeper
    invitationManager.startSweeper(registry, this::handleInvitationExpiry);

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

    invitationManager.stopSweeper();

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
   * <p>Supported commands:
   * <ul>
   *   <li>{@code PING} — latency check.
   *   <li>{@code QUIT} — graceful disconnect.
   *   <li>{@code STATUS} — server statistics.
   *   <li>{@code PLAYERS [id]} — list players or show details for a specific player.
   *   <li>{@code SCOREBOARD} — wins/losses/draws leaderboard.
   *   <li>{@code NEW <target_id>} — send a game invitation.
   *   <li>{@code ACCEPT} — accept a pending invitation.
   *   <li>{@code DECLINE} — decline a pending invitation.
   *   <li>{@code CANCEL} — cancel an outgoing invitation.
   *   <li>{@code AWAY} — mark yourself as away (no invitations accepted).
   *   <li>{@code BACK} — return to idle status.
   *   <li>{@code MOVE <from-to>} — make a move in an active game.
   * </ul>
   *
   * @param in     the client input stream
   * @param out    the client output stream
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
          cleanupPlayerInvitations(player);
          registry.removePlayer(player.getId());
          connectedClients.remove(out);
          System.out.println("Player " + player.getId() + " disconnected.");
          return;
        }
        case "STATUS" -> {
          int playerCount = registry.getPlayerCount();
          int sessionCount = registry.getActiveSessionCount();
          out.println(
              "STATUS port=" + tcpPort + " players=" + playerCount
                  + " sessions=" + sessionCount);
        }
        case "PLAYERS" -> {
          if (rest.isBlank()) {
            // List all players
            String list = registry.getPlayersFormatted();
            out.println(list.isEmpty() ? "PLAYERS none" : "PLAYERS\n" + list);
          } else {
            // Detail for one player
            String targetId = rest.trim();
            PlayerSession target = registry.getPlayer(targetId);
            if (target == null) {
              out.println("ERROR: Player '" + targetId + "' not found.");
            } else {
              out.println("PLAYER_INFO\n" + target.toDetailedString());
            }
          }
        }
        case "SCOREBOARD" -> out.println("SCOREBOARD\n" + registry.getScoreboardFormatted());

        // ----------------------------------------------------------------
        // Invitation commands
        // ----------------------------------------------------------------
        case "NEW" -> {
          if (rest.isBlank()) {
            out.println("ERROR: Usage: NEW <player_id>");
            break;
          }
          handleNewInvitation(out, player, rest.trim());
        }
        case "ACCEPT" -> handleAccept(out, player);
        case "DECLINE" -> handleDecline(out, player);
        case "CANCEL" -> handleCancel(out, player);

        // ----------------------------------------------------------------
        // Presence commands
        // ----------------------------------------------------------------
        case "AWAY" -> {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println("ERROR: Cannot go away while in a game.");
          } else if (player.getStatus() == PlayerSession.Status.WAITGAME) {
            out.println("ERROR: You have a pending invitation. "
                + "Use DECLINE first, then AWAY.");
          } else {
            player.setStatus(PlayerSession.Status.AWAY);
            out.println("STATUS_CHANGED away");
            System.out.println("Player " + player.getId() + " is now away.");
          }
        }
        case "BACK" -> {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println("ERROR: Cannot use BACK while in a game.");
          } else {
            player.setStatus(PlayerSession.Status.IDLE);
            out.println("STATUS_CHANGED idle");
            System.out.println("Player " + player.getId() + " is back (idle).");
          }
        }

        // ----------------------------------------------------------------
        // In-game move
        // ----------------------------------------------------------------
        case "MOVE" -> {
          GameSession session = registry.getSessionForPlayer(player.getId());
          if (session == null) {
            out.println("ERROR: Not in a game. Use NEW to invite a player.");
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

  // -------------------------------------------------------------------------
  // Invitation handlers
  // -------------------------------------------------------------------------

  /**
   * Handles a {@code NEW <target_id>} command: sends a game invitation.
   *
   * @param out    requester's output stream.
   * @param sender the player sending the invitation.
   * @param toId   the target player's ID.
   */
  private void handleNewInvitation(PrintWriter out, PlayerSession sender, String toId) {
    if (!sender.isIdle()) {
      out.println("ERROR: You must be idle to send an invitation "
          + "(current status: " + sender.getStatus().name().toLowerCase() + ").");
      return;
    }

    Invitationmanager.CreateResult result =
        invitationManager.createInvitation(sender, toId, registry);

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;
    PlayerSession invitee = registry.getPlayer(toId);

    // Notify inviter
    out.println("INVITATION_SENT PLAYER=" + invitee.getName()
        + " TIMEOUT=" + Invitation.TIMEOUT_SECONDS + "s");

    // Notify invitee
    invitee.send("INVITATION_RECEIVED FROM=" + sender.getId()
        + " EXPIRES=" + Invitation.TIMEOUT_SECONDS + "s");

    System.out.println("Invitation " + inv.getInvitationId()
        + ": " + sender.getId() + " → " + toId);
  }

  /**
   * Handles an {@code ACCEPT} command: starts the game if pre-conditions hold.
   *
   * @param out      acceptor's output stream.
   * @param acceptor the player accepting the invitation.
   */
  private void handleAccept(PrintWriter out, PlayerSession acceptor) {
    Invitationmanager.AcceptResult result = invitationManager.accept(acceptor.getId());

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;
    PlayerSession inviter = registry.getPlayer(inv.getFromPlayerId());

    if (inviter == null) {
      out.println("ERROR: The player who invited you has disconnected.");
      acceptor.setStatus(PlayerSession.Status.IDLE);
      return;
    }

    // Restore both players to IDLE before starting the session
    acceptor.setStatus(PlayerSession.Status.IDLE);
    inviter.setStatus(PlayerSession.Status.IDLE);

    // Notify inviter that the invitation was accepted and the game is starting
    inviter.send("INVITATION_ACCEPTED STARTING_GAME");

    // Start the game session
    startInvitedGame(inviter, acceptor);
  }

  /**
   * Handles a {@code DECLINE} command.
   *
   * @param out      decliner's output stream.
   * @param decliner the player declining.
   */
  private void handleDecline(PrintWriter out, PlayerSession decliner) {
    Invitationmanager.DeclineResult result = invitationManager.decline(decliner.getId());

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;
    PlayerSession inviter = registry.getPlayer(inv.getFromPlayerId());

    // Restore invitee status
    decliner.setStatus(PlayerSession.Status.IDLE);
    out.println("INVITATION_DECLINED");

    // Notify inviter
    if (inviter != null) {
      inviter.send("INVITATION_DECLINED BY=" + decliner.getId());
    }

    System.out.println("Invitation " + inv.getInvitationId() + " declined by " + decliner.getId());
  }

  /**
   * Handles a {@code CANCEL} command.
   *
   * @param out       canceller's output stream.
   * @param canceller the player cancelling their outgoing invitation.
   */
  private void handleCancel(PrintWriter out, PlayerSession canceller) {
    Invitationmanager.CancelResult result = invitationManager.cancel(canceller.getId());

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;
    PlayerSession invitee = registry.getPlayer(inv.getToPlayerId());

    out.println("INVITATION_CANCELLED");

    // Restore invitee to IDLE and notify them
    if (invitee != null) {
      invitee.setStatus(PlayerSession.Status.IDLE);
      invitee.send("INVITATION_CANCELLED BY=" + canceller.getId());
    }

    System.out.println("Invitation " + inv.getInvitationId()
        + " cancelled by " + canceller.getId());
  }

  /**
   * Cleans up any pending invitations (sent or received) when a player disconnects.
   *
   * @param player the disconnecting player.
   */
  private void cleanupPlayerInvitations(PlayerSession player) {
    // Cancel outgoing invitation
    Invitationmanager.CancelResult cancel = invitationManager.cancel(player.getId());
    if (cancel.isSuccess()) {
      PlayerSession invitee = registry.getPlayer(cancel.invitation.getToPlayerId());
      if (invitee != null) {
        invitee.setStatus(PlayerSession.Status.IDLE);
        invitee.send("INVITATION_CANCELLED BY=" + player.getId() + " (player disconnected)");
      }
    }

    // Decline any incoming invitation (so the inviter is unblocked)
    Invitationmanager.DeclineResult decline = invitationManager.decline(player.getId());
    if (decline.isSuccess()) {
      PlayerSession inviter = registry.getPlayer(decline.invitation.getFromPlayerId());
      if (inviter != null) {
        inviter.send("INVITATION_DECLINED BY=" + player.getId() + " (player disconnected)");
      }
    }
  }

  /**
   * Called by the {@link Invitationmanager} sweeper when an invitation expires.
   *
   * @param inv      the expired invitation.
   * @param registry the player registry.
   */
  private void handleInvitationExpiry(Invitation inv, GameRegistry registry) {
    PlayerSession inviter = registry.getPlayer(inv.getFromPlayerId());
    PlayerSession invitee = registry.getPlayer(inv.getToPlayerId());

    if (invitee != null && invitee.getStatus() == PlayerSession.Status.WAITGAME) {
      invitee.setStatus(PlayerSession.Status.IDLE);
      invitee.send("INVITATION_EXPIRED FROM=" + inv.getFromPlayerId());
    }
    if (inviter != null) {
      inviter.send("INVITATION_EXPIRED TO=" + inv.getToPlayerId() + " (no response)");
    }

    System.out.println("Invitation " + inv.getInvitationId() + " expired.");
  }

  /**
   * Creates a game session for two players who agreed via an invitation.
   *
   * @param inviter  player who sent the invitation (plays first).
   * @param invitee  player who accepted the invitation.
   */
  private synchronized void startInvitedGame(PlayerSession inviter, PlayerSession invitee) {
    List<PlayerSession> pair = List.of(inviter, invitee);
    GameController sessionController = controllerFactory.create();
    sessionController.startNewGame(
        fr.ubordeaux.pdp.model.core.Configuration.getDefaultConfiguration());

    GameSession session = registry.createSession(pair, sessionController);

    String playerList = inviter.getId() + " " + invitee.getId();

    for (PlayerSession p : pair) {
      p.send("GAME_START session=" + session.getSessionId()
          + " players=" + playerList
          + " first=" + inviter.getId());
    }

    System.out.println("Invited game started: " + session.getSessionId()
        + " between " + playerList);
  }

  /**
   * Notifies a newly connected player to wait for an invitation if they are alone.
   *
   * <p>Auto-matching is disabled in invitation mode. Players must use {@code NEW <id>}
   * to invite specific opponents.
   */
  private synchronized void tryAutoStart() {
    List<PlayerSession> idlePlayers =
        registry.getAllPlayers().stream()
            .filter(PlayerSession::isIdle)
            .collect(Collectors.toList());

    if (idlePlayers.size() == 1) {
      idlePlayers.get(0).send(
          "WAITING You are the only player connected. "
              + "Use 'players' to list others, then 'new <id>' to invite someone.");
    }
  }

  /**
   * @deprecated Replaced by the invitation flow ({@link #handleNewInvitation}).
   *             Kept for reference only and no longer called.
   */
  @Deprecated
  private void handleNewGame(PrintWriter out, PlayerSession requester, String[] participantIds) {
    out.println("ERROR: Direct game creation is replaced by the invitation system. "
        + "Use 'new <player_id>' to invite a player.");
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