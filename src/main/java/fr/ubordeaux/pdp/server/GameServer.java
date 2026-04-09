package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Multi-client TCP game server.
 *
 * <p>Accepts client connections, registers players, creates game sessions
 * via the invitation system, and routes network commands to the appropriate
 * session.
 *
 * <h2>Thread-safety</h2>
 *
 * <p>A {@link ReentrantLock} ({@code lifecycleLock}) makes {@link #start()} and
 * {@link #stop()} mutually exclusive and atomic with respect to each other.
 * Without this lock, two concurrent callers could both pass the
 * {@code if (running)} guard and either double-bind the port or double-close
 * the socket.
 *
 * <p>{@code running} is also declared {@code volatile} so the
 * accept-loop thread sees the write from {@link #stop()} without holding the lock.
 *
 * <p>{@code connectedClients} uses a {@link ConcurrentHashMap}-backed set so
 * {@link #stop()} can iterate and write to it concurrently with client handler
 * threads that add/remove entries.
 *
 * <h2>GUI-only mode</h2>
 *
 * <p>When {@code guiOnly} is {@code true} (started via {@code --gui --server}):
 *
 * <ul>
 *   <li>Every {@code GAME_START} message carries {@code mode=GUI}.</li>
 *   <li>Cross-mode invitations (GUI ↔ CLI) are blocked at invitation time.</li>
 *   <li>The {@code WELCOME} response carries {@code mode=GUI}.</li>
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
  private final boolean daemon;
  private final boolean guiOnly;
  private final GameControllerFactory controllerFactory;


  /**
   * Ensures {@link #start()} and {@link #stop()} are mutually exclusive.
   *
   * <p>Both methods perform a check-then-act on {@code running}. Without this
   * lock a concurrent pair of calls could both pass the guard, leading to a
   * double-bind or a double-close.
   */
  private final ReentrantLock lifecycleLock = new ReentrantLock();
  private final GameRegistry registry = new GameRegistry();
  private final InvitationManager invitationManager = new InvitationManager();

  private Thread discoveryThread;
  private ServerSocket serverSocket;
  private ExecutorService clientPool;

  /**
   * Visible to the accept-loop thread without holding {@code lifecycleLock}.
   * Writes are still performed under the lock to keep the check-then-act atomic.
   */
  private volatile boolean running = false;

  private final Set<PrintWriter> connectedClients =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Unique constructor — all parameters are explicit to avoid boolean-argument confusion.
   *
   * @param serverName       name broadcast over UDP discovery
   * @param tcpPort          TCP listening port
   * @param controllerFactory factory producing a fresh controller per game session
   * @param daemon           server daemon flag
   * @param guiOnly          {@code true} to tag all games with {@code mode=GUI}
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
   * <p>The check on {@code running} and the subsequent assignment are performed
   * under {@link #lifecycleLock} to prevent a concurrent {@link #stop()} from
   * interleaving. The lock is released before blocking on {@code accept()} so
   * that {@link #stop()} can proceed.
   *
   * @throws IOException if the port is already in use or cannot be bound
   */
  public void start() throws IOException {
    lifecycleLock.lock();
    try {
      if (running) {
        System.out.println(Internationalization.get("server.main.already_running", tcpPort));
        return;
      }

      try {
        serverSocket = new ServerSocket(tcpPort);
      } catch (java.net.BindException e) {
        throw new IOException(
            Internationalization.get("server.main.port_in_use", tcpPort), e);
      }

      running = true;
      clientPool = Executors.newCachedThreadPool();

      invitationManager.startSweeper(registry, this::handleInvitationExpiry);

      DiscoveryService discovery = new DiscoveryService(serverName, tcpPort);
      discoveryThread = new Thread(discovery, "discovery-thread");
      discoveryThread.setDaemon(true);
      discoveryThread.start();

      String suffix = guiOnly
          ? Internationalization.get("server.main.gui_only_suffix")
          : "";
      System.out.println(Internationalization.get("server.main.started", tcpPort, suffix));
      System.out.println(Internationalization.get("server.main.discovery_started"));

    } finally {
      lifecycleLock.unlock();
    }

    while (running) {
      try {
        Socket client = serverSocket.accept();
        client.setSoTimeout(CLIENT_TIMEOUT_MS);
        System.out.println(Internationalization.get(
            "server.main.client_connected", client.getInetAddress()));
        clientPool.submit(() -> handleClient(client));
      } catch (IOException e) {
        if (running) {
          System.err.println(Internationalization.get(
              "server.main.accept_error", e.getMessage()));
        }
      }
    }
  }

  /**
   * Stops the server: notifies all clients with {@code BYE}, closes the socket,
   * shuts down the thread pool, and stops the invitation sweeper.
   *
   * <p>Idempotent: a second call while the server is already stopped is a no-op,
   * and the check-then-act is atomic under {@link #lifecycleLock}.
   */
  public void stop() {
    lifecycleLock.lock();
    try {
      if (!running) {
        System.out.println(Internationalization.get("server.main.not_running"));
        return;
      }
      running = false;
    } finally {
      lifecycleLock.unlock();
    }

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
      System.err.println(Internationalization.get(
          "server.main.close_socket_error", e.getMessage()));
    }

    if (clientPool != null) {
      clientPool.shutdownNow();
    }
    if (discoveryThread != null) {
      discoveryThread.interrupt();
    }

    System.out.println(Internationalization.get("server.main.stopped"));
  }


  /**
   * Handles the NEW invitation command and delivers an invitation to a target player.
   *
   * <p>The sender's idle check and the invitation creation are both performed inside
   * {@link InvitationManager#createInvitation}, which is {@code synchronized},
   * so no additional locking is needed here.
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
        out.println(Internationalization.get("server.main.error.first_message_register"));
        return;
      }

      String[] parts = handshake.split("\\s+", 3);
      if (parts.length < 3) {
        out.println(Internationalization.get("server.main.error.register_usage"));
        return;
      }

      String playerId = parts[1];
      String playerName = parts[2].split("\\s+")[0];

      PlayerSession player = registry.registerPlayer(playerId, playerName, out);
      if (player == null) {
        out.println(Internationalization.get("server.main.error.id_taken", playerId));
        return;
      }

      String modeTag = guiOnly
          ? Internationalization.get("server.main.mode_tag_gui")
          : Internationalization.get("server.main.mode_tag_any");
      out.println(Internationalization.get("server.main.welcome", playerId, modeTag));
      System.out.println(Internationalization.get(
          "server.main.registered", playerId, playerName));

      tryAutoStart();

      String line;
      while ((line = in.readLine()) != null) {
        handleCommand(out, player, line.trim());
      }

      System.out.println(Internationalization.get(
          "server.main.client_disconnected", playerId));
      cleanupPlayerInvitations(player);
      registry.removePlayer(playerId);

    } catch (IOException e) {
      System.err.println(Internationalization.get(
          "server.main.client_io_error", e.getMessage()));
    } finally {
      try {
        client.close();
      } catch (IOException ignored) {
        // Ignore close failure.
      }
    }
  }

  /**
   * Dispatches a single command received from a client.
   *
   * <p>For commands that involve a compound status check-then-set (e.g. {@code AWAY},
   * {@code BACK}), the player's lock is held for the entire read-modify sequence to
   * prevent a race with a concurrent game-end or invitation callback.
   */
  private void handleCommand(PrintWriter out, PlayerSession player, String line)  {
    if (line.isBlank()) {
      return;
    }

    String[] tokens = line.split("\\s+", 2);
    String cmd = tokens[0].toUpperCase();
    String rest = tokens.length > 1 ? tokens[1] : "";

    switch (cmd) {

      case "PLAYERS" -> {
        String formatted = registry.getPlayersFormatted();
        out.println(Internationalization.get(
            "server.main.players_response",
            formatted.isBlank()
                ? Internationalization.get("server.main.players_empty")
                : formatted));
      }

      case "STATUS" -> {
        String target = rest.trim();
        if (target.isBlank()) {
          out.println(Internationalization.get("server.main.player_info_response", player));
        } else {
          PlayerSession targetPlayer = registry.getPlayer(target);
          if (targetPlayer == null) {
            out.println(Internationalization.get("server.main.error.player_not_found", target));
          } else {
            out.println(Internationalization.get(
                "server.main.player_info_response", targetPlayer));
          }
        }
      }

      case "SCOREBOARD" -> out.println(
          Internationalization.get("server.main.scoreboard_response",
              registry.getScoreboardFormatted()));

      case "NEW" -> {
        if (rest.isBlank()) {
          out.println(Internationalization.get("server.main.error.new_usage"));
          break;
        }
        handleNewInvitation(out, player, rest.trim());
      }

      case "ACCEPT" -> handleAccept(out, player);
      case "DECLINE" -> handleDecline(out, player);
      case "CANCEL" -> handleCancel(out, player);

      case "AWAY" -> {
        player.lock();
        try {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println(Internationalization.get("server.main.error.cannot_go_away_ingame"));
          } else if (player.getStatus() == PlayerSession.Status.WAITGAME) {
            out.println(Internationalization.get(
                "server.main.error.pending_invitation_decline_first"));
          } else {
            player.setStatus(PlayerSession.Status.AWAY);
            out.println(Internationalization.get("server.main.status_changed_away"));
          }
        } finally {
          player.unlock();
        }
      }

      case "BACK" -> {
        player.lock();
        try {
          if (player.getStatus() == PlayerSession.Status.INGAME) {
            out.println(Internationalization.get("server.main.error.cannot_back_ingame"));
          } else {
            player.setStatus(PlayerSession.Status.IDLE);
            out.println(Internationalization.get("server.main.status_changed_idle"));
          }
        } finally {
          player.unlock();
        }
      }

      case "MOVE" -> {
        GameSession session = registry.getSessionForPlayer(player.getId());
        if (session == null) {
          out.println(Internationalization.get("server.main.error.not_in_game"));
        } else {
          String error = session.handleMove(player, rest);
          if (error != null) {
            out.println(error);
          } else {
            out.println(Internationalization.get("server.main.move_ok", rest));
          }
        }
      }

      default -> out.println(Internationalization.get(
          "server.main.error.unknown_command", cmd));
    }
  }

  /**
   * Handles {@code NEW <target_id>}: creates and delivers an invitation.
   *
   * <p>The sender's idle check and the invitation creation are both performed inside
   * {@link InvitationManager#createInvitation}, which is {@code synchronized},
   * so no additional locking is needed here.
   */
  private void handleNewInvitation(PrintWriter out, PlayerSession sender, String toId) {
    if (!sender.isIdle()) {
      out.println(Internationalization.get(
          "server.main.error.must_be_idle_to_invite",
          sender.getStatus().name().toLowerCase()));
      return;
    }

    PlayerSession invitee = registry.getPlayer(toId);
    if (invitee == null) {
      out.println(Internationalization.get("server.main.error.player_not_found", toId));
      return;
    }

    InvitationManager.CreateResult result =
        invitationManager.createInvitation(sender, toId, registry);

    if (!result.isSuccess()) {
      out.println("ERROR: " + result.error);
      return;
    }

    Invitation inv = result.invitation;

    out.println(Internationalization.get(
        "server.main.invitation_sent",
        invitee.getName(),
        Invitation.TIMEOUT_SECONDS));
    invitee.send(Internationalization.get(
        "server.main.invitation_received",
        sender.getId(),
        Invitation.TIMEOUT_SECONDS));

    System.out.println(Internationalization.get(
        "server.main.invitation_logged",
        inv.getInvitationId(),
        sender.getId(),
        toId));
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
      out.println(Internationalization.get("server.main.error.inviter_disconnected"));
      acceptor.setStatus(PlayerSession.Status.IDLE);
      return;
    }

    acceptor.setStatus(PlayerSession.Status.IDLE);
    inviter.setStatus(PlayerSession.Status.IDLE);
    inviter.send(Internationalization.get("server.main.invitation_accepted_starting_game"));
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
    out.println(Internationalization.get("server.main.invitation_declined"));
    if (inviter != null) {
      inviter.send(Internationalization.get(
          "server.main.invitation_declined_by", decliner.getId()));
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
    out.println(Internationalization.get("server.main.invitation_cancelled"));
    if (invitee != null) {
      invitee.setStatus(PlayerSession.Status.IDLE);
      invitee.send(Internationalization.get(
          "server.main.invitation_cancelled_by", canceller.getId()));
    }
  }

  /** Cleans up all pending invitations when a player disconnects. */
  private void cleanupPlayerInvitations(PlayerSession player) {
    InvitationManager.CancelResult cancel = invitationManager.cancel(player.getId());
    if (cancel.isSuccess()) {
      PlayerSession invitee = registry.getPlayer(cancel.invitation.getToPlayerId());
      if (invitee != null) {
        invitee.setStatus(PlayerSession.Status.IDLE);
        invitee.send(Internationalization.get(
            "server.main.invitation_cancelled_disconnected", player.getId()));
      }
    }

    InvitationManager.DeclineResult decline = invitationManager.decline(player.getId());
    if (decline.isSuccess()) {
      PlayerSession inviter = registry.getPlayer(decline.invitation.getFromPlayerId());
      if (inviter != null) {
        inviter.send(Internationalization.get(
            "server.main.invitation_declined_disconnected", player.getId()));
      }
    }
  }

  /** Called by the sweeper for each invitation that timed out. */
  private void handleInvitationExpiry(Invitation inv, GameRegistry reg) {
    PlayerSession inviter = reg.getPlayer(inv.getFromPlayerId());
    PlayerSession invitee = reg.getPlayer(inv.getToPlayerId());

    if (invitee != null && invitee.getStatus() == PlayerSession.Status.WAITGAME) {
      invitee.setStatus(PlayerSession.Status.IDLE);
      invitee.send(Internationalization.get(
          "server.main.invitation_expired_from", inv.getFromPlayerId()));
    }
    if (inviter != null) {
      inviter.send(Internationalization.get(
          "server.main.invitation_expired_to", inv.getToPlayerId()));
    }
    System.out.println(Internationalization.get(
        "server.main.invitation_expired_logged", inv.getInvitationId()));
  }

  /**
   * Starts a game session between two players who have agreed via invitation.
   *
   * <p>{@code synchronized} ensures that two concurrent {@code ACCEPT} calls
   * (theoretically possible if the invitation system is bypassed by a bug)
   * cannot start two sessions for the same pair.
   */
  private synchronized void startInvitedGame(PlayerSession inviter, PlayerSession invitee) {
    List<PlayerSession> pair = List.of(inviter, invitee);

    GameController sessionController = controllerFactory.create();
    sessionController.startNewGame(Configuration.getDefaultConfiguration());

    GameSession session = registry.createSession(pair, sessionController);

    String playerList = inviter.getId() + " " + invitee.getId();
    String modeTag = guiOnly
        ? Internationalization.get("server.main.mode_tag_gui")
        : "";

    for (PlayerSession p : pair) {
      p.send(Internationalization.get(
          "server.main.game_start",
          session.getSessionId(),
          playerList,
          inviter.getId(),
          modeTag));
    }

    String guiSuffix = guiOnly
        ? Internationalization.get("server.main.game_started_gui_suffix")
        : "";
    System.out.println(Internationalization.get(
        "server.main.game_started_logged",
        session.getSessionId(),
        playerList,
        guiSuffix));
  }

  /**
   * Notifies the sole connected idle player that they are waiting for an opponent.
   */
  private synchronized void tryAutoStart() {
    List<PlayerSession> idlePlayers = registry.getAllPlayers().stream()
        .filter(PlayerSession::isIdle)
        .collect(Collectors.toList());

    if (idlePlayers.size() == 1) {
      idlePlayers.get(0).send(
          Internationalization.get("server.main.waiting_single_player"));
    }
  }

  /**
   * Indicates whether the server is currently running.
   *
   * @return {@code true} if the server is currently running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * Returns the TCP port the server is listening on.
   *
   * @return the listening TCP port
   */
  public int getPort() {
    return tcpPort;
  }

  /**
   * Indicates whether this server accepts only GUI clients.
   *
   * @return {@code true} if this server tags all games as {@code mode=GUI}
   */
  public boolean isGuiOnly() {
    return guiOnly;
  }

  /**
   * Returns the TCP listening port of this server.
   *
   * @return the TCP listening port
   */
  public int getTcpPort() {
    return tcpPort;
  }

  /**
   * Returns the shared game registry used by this server.
   *
   * @return the server game registry
   */
  public GameRegistry getRegistry() {
    return registry;
  }
}