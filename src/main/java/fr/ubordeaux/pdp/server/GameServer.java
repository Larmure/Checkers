package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
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
 * <p>Accepts client connections, registers players, creates game sessions
 * via the invitation system, and routes network commands to the appropriate
 * session.
 *
 * <h2>GUI-only mode</h2>
 *
 * <p>When {@code guiOnly} is {@code true} (started via {@code --gui --server}
 * in App):
 *
 * <ul>
 *   <li>Every {@code GAME_START} message carries {@code mode=GUI} so clients
 *       know they must render graphically.</li>
 *   <li>Cross-mode invitations (GUI ↔ CLI) are blocked at invitation time —
 *       not at connection time. All clients may connect regardless of
 *       interface.</li>
 *   <li>The {@code WELCOME} response carries {@code mode=GUI} so the client
 *       can warn the user immediately after connecting.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Created and started by
 * {@link fr.ubordeaux.pdp.controller.commands.ServerStartCommand}
 * or directly by {@link fr.ubordeaux.pdp.App}.
 */
public class GameServer {

  private static final int CLIENT_TIMEOUT_MS = 180_000;

  private final int tcpPort;
  private final String serverName;

  /**
   * Reserved for server launch mode configuration.
   * Kept because callers still pass it explicitly.
   */
  private final boolean daemon;

  /**
   * When {@code true}, all {@code GAME_START} messages carry {@code mode=GUI}.
   */
  private final boolean guiOnly;

  private final GameControllerFactory controllerFactory;
  private final GameRegistry registry = new GameRegistry();
  private final InvitationManager invitationManager = new InvitationManager();

  private Thread discoveryThread;
  private ServerSocket serverSocket;
  private ExecutorService clientPool;
  private volatile boolean running = false;

  private final Set<PrintWriter> connectedClients =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Unique constructor — all parameters are explicit to avoid boolean-argument
   * confusion.
   *
   * @param serverName name broadcast over UDP discovery
   * @param tcpPort TCP listening port
   * @param controllerFactory factory producing a fresh controller per game session
   * @param daemon server daemon flag
   * @param guiOnly {@code true} to tag all games with {@code mode=GUI}
   */
  public GameServer(String serverName, int tcpPort,
                    GameControllerFactory controllerFactory, boolean daemon, boolean guiOnly) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
    this.controllerFactory = controllerFactory;
    this.daemon = daemon;
    this.guiOnly = guiOnly;
  }

  /**
   * Binds the server socket, starts UDP discovery and the invitation sweeper,
   * then blocks accepting client connections until {@link #stop()} is called.
   *
   * @throws IOException if the port is already in use or cannot be bound
   */
  public void start() throws IOException {
    if (running) {
      System.out.println("[server] Already running on port " + tcpPort + ".");
      return;
    }

    try {
      serverSocket = new ServerSocket(tcpPort);
    } catch (java.net.BindException e) {
      throw new IOException(
          "Port " + tcpPort + " is already in use. "
              + "Stop the existing server or choose another port.", e);
    }

    running = true;
    clientPool = Executors.newCachedThreadPool();

    invitationManager.startSweeper(registry, this::handleInvitationExpiry);

    DiscoveryService discovery = new DiscoveryService(serverName, tcpPort);
    discoveryThread = new Thread(discovery, "discovery-thread");
    discoveryThread.setDaemon(true);
    discoveryThread.start();

    System.out.println("[server] Started on port " + tcpPort
        + (guiOnly ? " (GUI-only mode)" : "") + ".");
    System.out.println("[server] Discovery broadcasting on UDP 12346.");

    while (running) {
      try {
        Socket client = serverSocket.accept();
        client.setSoTimeout(CLIENT_TIMEOUT_MS);
        System.out.println("[server] Client connected: " + client.getInetAddress());
        clientPool.submit(() -> handleClient(client));
      } catch (IOException e) {
        if (running) {
          System.err.println("[server] Error accepting client: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Stops the server: notifies all clients with {@code BYE}, closes the socket,
   * shuts down the thread pool, and stops the invitation sweeper.
   */
  public void stop() {
    if (!running) {
      System.out.println("[server] Server is not running.");
      return;
    }

    running = false;
    invitationManager.stopSweeper();

    for (PrintWriter out : connectedClients) {
      try {
        out.println("BYE");
      } catch (Exception ignored) {
        // Ignore client notification failure during shutdown.
      }
    }
    connectedClients.clear();

    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      System.err.println("[server] Error closing socket: " + e.getMessage());
    }

    if (clientPool != null) {
      clientPool.shutdownNow();
    }
    if (discoveryThread != null) {
      discoveryThread.interrupt();
    }

    System.out.println("[server] Server stopped.");
  }

  /** @return {@code true} if the server is currently running */
  public boolean isRunning() {
    return running;
  }

  /** @return the TCP port the server is listening on */
  public int getPort() {
    return tcpPort;
  }

  /** @return {@code true} if this server tags all games as {@code mode=GUI} */
  public boolean isGuiOnly() {
    return guiOnly;
  }

  /**
   * Handles a newly connected client socket end-to-end: handshake → message loop.
   *
   * <p>Expected first line from the client:
   *
   * <pre>
   * REGISTER &lt;id&gt; &lt;name&gt; &lt;GUI|CLI&gt;
   * </pre>
   *
   * <p>All clients are accepted regardless of interface mode. The
   * {@code guiOnly} flag is communicated back in the {@code WELCOME} response
   * and enforced later at invitation time — not here.
   *
   * @param client the newly accepted socket
   */
  private void handleClient(Socket client) {
    try (
        BufferedReader in = new BufferedReader(
            new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(
            new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
    ) {
      connectedClients.add(out);

      String handshake = in.readLine();
      if (handshake == null || !handshake.startsWith("REGISTER ")) {
        out.println("ERROR: First message must be REGISTER <id> <name> <GUI|CLI>");
        return;
      }

      String[] parts = handshake.split("\\s+", 4);
      if (parts.length < 4) {
        out.println("ERROR: Usage: REGISTER <id> <name> <GUI|CLI>");
        return;
      }

      String playerId = parts[1];
      String playerName = parts[2];
      String interfaceMode = parts[3].trim().toUpperCase();

      if (!interfaceMode.equals("GUI") && !interfaceMode.equals("CLI")) {
        out.println("ERROR: Invalid interface mode. Expected GUI or CLI.");
        return;
      }

      PlayerSession player = registry.registerPlayer(playerId, playerName, out, interfaceMode);
      if (player == null) {
        out.println("ERROR: Player ID '" + playerId + "' is already taken.");
        return;
      }

      out.println("WELCOME " + playerId + (guiOnly ? " mode=GUI" : " mode=ANY"));
      System.out.println("[server] Registered: " + playerId
          + " (" + playerName + ") [" + interfaceMode + "]");

      tryAutoStart();
      processMessages(in, out, player);

    } catch (SocketTimeoutException e) {
      System.out.println("[server] Client timed out after 3 minutes of inactivity.");
    } catch (IOException e) {
      System.out.println("[server] Client disconnected: " + e.getMessage());
    }
  }

  /**
   * Reads and dispatches commands from a registered player until they disconnect.
   *
   * @param in client input stream
   * @param out client output stream
   * @param player registered player
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
          System.out.println("[server] Player " + player.getId() + " disconnected.");
          return;
        }

        case "STATUS" -> out.println(
            "STATUS port=" + tcpPort
                + " players=" + registry.getPlayerCount()
                + " sessions=" + registry.getActiveSessionCount()
                + (guiOnly ? " mode=GUI" : ""));

        case "PLAYERS" -> {
          if (rest.isBlank()) {
            String list = registry.getPlayersFormatted();
            out.println(list.isEmpty() ? "PLAYERS none" : "PLAYERS\n" + list);
          } else {
            PlayerSession target = registry.getPlayer(rest.trim());
            if (target == null) {
              out.println("ERROR: Player '" + rest.trim() + "' not found.");
            } else {
              out.println("PLAYER_INFO\n" + target.toDetailedString());
            }
          }
        }

        case "SCOREBOARD" -> out.println("SCOREBOARD\n" + registry.getScoreboardFormatted());

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

        case "AWAY" -> {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println("ERROR: Cannot go away while in a game.");
          } else if (player.getStatus() == PlayerSession.Status.WAITGAME) {
            out.println("ERROR: You have a pending invitation. Use DECLINE first, then AWAY.");
          } else {
            player.setStatus(PlayerSession.Status.AWAY);
            out.println("STATUS_CHANGED away");
          }
        }

        case "BACK" -> {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println("ERROR: Cannot use BACK while in a game.");
          } else {
            player.setStatus(PlayerSession.Status.IDLE);
            out.println("STATUS_CHANGED idle");
          }
        }

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

  /**
   * Handles {@code NEW <target_id>}: creates and delivers an invitation.
   *
   * <p>Invitations are only allowed between players using the same client type
   * ({@code GUI} with {@code GUI}, or {@code CLI} with {@code CLI}) so both
   * sides render the same kind of game session.
   *
   * @param out requester output stream
   * @param sender inviting player
   * @param toId target player id
   */
  private void handleNewInvitation(PrintWriter out, PlayerSession sender, String toId) {
    if (!sender.isIdle()) {
      out.println("ERROR: You must be idle to send an invitation "
          + "(current status: " + sender.getStatus().name().toLowerCase() + ").");
      return;
    }

    PlayerSession invitee = registry.getPlayer(toId);
    if (invitee == null) {
      out.println("ERROR: Player '" + toId + "' not found.");
      return;
    }

    if (!sender.getInterfaceMode().equals(invitee.getInterfaceMode())) {
      out.println("ERROR: Interface mode mismatch — you are ["
          + sender.getInterfaceMode() + "] and '" + toId + "' is ["
          + invitee.getInterfaceMode() + "]. Both players must use the same "
          + "client type (both GUI or both CLI).");
      return;
    }

    InvitationManager.CreateResult result =
        invitationManager.createInvitation(sender, toId, registry);

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;

    out.println("INVITATION_SENT PLAYER=" + invitee.getName()
        + " TIMEOUT=" + Invitation.TIMEOUT_SECONDS + "s");

    invitee.send("INVITATION_RECEIVED FROM=" + sender.getId()
        + " EXPIRES=" + Invitation.TIMEOUT_SECONDS + "s");

    System.out.println("[server] Invitation " + inv.getInvitationId()
        + ": " + sender.getId() + " → " + toId);
  }

  /** Handles {@code ACCEPT}. */
  private void handleAccept(PrintWriter out, PlayerSession acceptor) {
    InvitationManager.AcceptResult result = invitationManager.accept(acceptor.getId());
    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    PlayerSession inviter = registry.getPlayer(result.invitation.getFromPlayerId());
    if (inviter == null) {
      out.println("ERROR: The player who invited you has disconnected.");
      acceptor.setStatus(PlayerSession.Status.IDLE);
      return;
    }

    acceptor.setStatus(PlayerSession.Status.IDLE);
    inviter.setStatus(PlayerSession.Status.IDLE);
    inviter.send("INVITATION_ACCEPTED STARTING_GAME");
    startInvitedGame(inviter, acceptor);
  }

  /** Handles {@code DECLINE}. */
  private void handleDecline(PrintWriter out, PlayerSession decliner) {
    InvitationManager.DeclineResult result = invitationManager.decline(decliner.getId());
    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    PlayerSession inviter = registry.getPlayer(result.invitation.getFromPlayerId());
    decliner.setStatus(PlayerSession.Status.IDLE);
    out.println("INVITATION_DECLINED");
    if (inviter != null) {
      inviter.send("INVITATION_DECLINED BY=" + decliner.getId());
    }
  }

  /** Handles {@code CANCEL}. */
  private void handleCancel(PrintWriter out, PlayerSession canceller) {
    InvitationManager.CancelResult result = invitationManager.cancel(canceller.getId());
    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    PlayerSession invitee = registry.getPlayer(result.invitation.getToPlayerId());
    out.println("INVITATION_CANCELLED");
    if (invitee != null) {
      invitee.setStatus(PlayerSession.Status.IDLE);
      invitee.send("INVITATION_CANCELLED BY=" + canceller.getId());
    }
  }

  /** Cleans up all pending invitations when a player disconnects. */
  private void cleanupPlayerInvitations(PlayerSession player) {
    InvitationManager.CancelResult cancel = invitationManager.cancel(player.getId());
    if (cancel.isSuccess()) {
      PlayerSession invitee = registry.getPlayer(cancel.invitation.getToPlayerId());
      if (invitee != null) {
        invitee.setStatus(PlayerSession.Status.IDLE);
        invitee.send("INVITATION_CANCELLED BY=" + player.getId() + " (player disconnected)");
      }
    }

    InvitationManager.DeclineResult decline = invitationManager.decline(player.getId());
    if (decline.isSuccess()) {
      PlayerSession inviter = registry.getPlayer(decline.invitation.getFromPlayerId());
      if (inviter != null) {
        inviter.send("INVITATION_DECLINED BY=" + player.getId() + " (player disconnected)");
      }
    }
  }

  /** Called by the sweeper for each invitation that timed out. */
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
    System.out.println("[server] Invitation " + inv.getInvitationId() + " expired.");
  }

  /**
   * Starts a game session between two players who have agreed via invitation.
   *
   * <p>Appends {@code mode=GUI} to {@code GAME_START} when {@link #guiOnly}
   * is {@code true}.
   */
  private synchronized void startInvitedGame(PlayerSession inviter, PlayerSession invitee) {
    List<PlayerSession> pair = List.of(inviter, invitee);

    GameController sessionController = controllerFactory.create();
    sessionController.startNewGame(Configuration.getDefaultConfiguration());

    GameSession session = registry.createSession(pair, sessionController);

    String playerList = inviter.getId() + " " + invitee.getId();
    String modeTag = guiOnly ? " mode=GUI" : "";

    for (PlayerSession player : pair) {
      player.send("GAME_START session=" + session.getSessionId()
          + " players=" + playerList
          + " first=" + inviter.getId()
          + modeTag);
    }

    System.out.println("[server] Game started: " + session.getSessionId()
        + " between " + playerList + (guiOnly ? " [GUI]" : ""));
  }

  /**
   * Notifies the sole connected idle player that they are waiting for an opponent.
   * Auto-matching is disabled; players use {@code NEW <id>} to invite each other.
   */
  private synchronized void tryAutoStart() {
    List<PlayerSession> idlePlayers = registry.getAllPlayers().stream()
        .filter(PlayerSession::isIdle)
        .collect(Collectors.toList());

    if (idlePlayers.size() == 1) {
      idlePlayers.get(0).send(
          "WAITING You are the only player connected. "
              + "Use 'players' to list others, then 'new <id>' to invite someone.");
    }
  }
}