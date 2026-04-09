package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/** Tests for {@link GameServer}. */
class GameServerTest {

  @Test
  void constructor_setsPortAndServerStartsStopped() {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    assertEquals(23456, server.getPort());
    assertTrue(!server.isRunning());
    assertTrue(!server.isGuiOnly());
  }

  @Test
  void stop_whenServerIsNotRunning_keepsStateStopped() {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    server.stop();

    assertTrue(!server.isRunning());
  }

  @Test
  void stop_whenRunning_notifiesClientsAndClosesResources() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    StringWriter clientBuffer = new StringWriter();
    PrintWriter clientOut = new PrintWriter(clientBuffer, true);
    ServerSocket serverSocket = new ServerSocket(0);
    ExecutorService clientPool = Executors.newSingleThreadExecutor();

    Thread discoveryThread = new Thread(() -> {
      try {
        Thread.sleep(10_000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    discoveryThread.start();

    setField(server, "running", true);
    setField(server, "serverSocket", serverSocket);
    setField(server, "clientPool", clientPool);
    setField(server, "discoveryThread", discoveryThread);

    Set<PrintWriter> connectedClients = getConnectedClients(server);
    connectedClients.add(clientOut);

    server.stop();

    discoveryThread.join(1000);
    assertTrue(clientBuffer.toString().contains("BYE"));
    assertTrue(serverSocket.isClosed());
    assertTrue(clientPool.isShutdown());
    assertTrue(!discoveryThread.isAlive());
    assertTrue(!server.isRunning());
    assertTrue(connectedClients.isEmpty());
  }

  @Test
  void start_whenAlreadyRunning_returnsImmediately() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    setField(server, "running", true);

    server.start();

    assertTrue(server.isRunning());
  }

  @Test
  void start_whenStartedNormally_setsRunningAndCanBeStopped() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        0,
        defaultFactory(),
        false,
        false);

    Thread startThread = new Thread(() -> {
      try {
        server.start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    startThread.start();
    waitForRunning(server);

    assertTrue(server.isRunning());

    int actualPort = getServerSocket(server).getLocalPort();
    try (Socket client = new Socket("127.0.0.1", actualPort);
        PrintWriter clientOut = new PrintWriter(
            new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
        BufferedReader clientIn = new BufferedReader(
            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
      clientOut.println("REGISTER p1 Alice");
      assertTrue(clientIn.readLine().startsWith("WELCOME p1"));
      client.shutdownOutput();
      waitUntilPlayerRemoved(server, "p1");
    }

    server.stop();
    startThread.join(2_000);

    assertFalse(startThread.isAlive());
    assertFalse(server.isRunning());
  }

  @Test
  void getTcpPortAndRegistryExposeConstructorValues() {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        true);

    assertEquals(23456, server.getTcpPort());
    assertNotNull(server.getRegistry());
    assertTrue(server.isGuiOnly());
  }

  @Test
  void tryAutoStart_withMultipleIdlePlayersDoesNotSendWaitingMessage() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    StringWriter p2Buffer = new StringWriter();
    registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));
    registry.registerPlayer("p2", "Bob", new PrintWriter(p2Buffer, true));

    invokeTryAutoStart(server);

    assertTrue(p1Buffer.toString().isEmpty());
    assertTrue(p2Buffer.toString().isEmpty());
  }

  @Test
  void handleCommand_playersWhenNoPlayers_returnsNonePlaceholder() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = new PlayerSession("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "PLAYERS");

    assertTrue(buffer.toString().contains("(none)"));
  }

  @Test
  void handleCommand_statusWithTarget_returnsTargetInfo() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);
    registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    invokeHandleCommand(server, out, player, "STATUS p2");

    assertTrue(buffer.toString().contains("PLAYER_INFO"));
    assertTrue(buffer.toString().contains("Bob"));
  }

  @Test
  void handleCommand_newWhenSenderIsNotIdle_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession sender = registry.registerPlayer("p1", "Alice", out);
    registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    sender.setStatus(PlayerSession.Status.AWAY);

    invokeHandleCommand(server, out, sender, "NEW p2");

    assertTrue(buffer.toString().contains("must be idle"));
  }

  @Test
  void handleCommand_newWhenTargetIsAway_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession sender = registry.registerPlayer("p1", "Alice", out);
    PlayerSession target = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    target.setStatus(PlayerSession.Status.AWAY);

    invokeHandleCommand(server, out, sender, "NEW p2");

    assertTrue(buffer.toString().contains("away and cannot receive invitations"));
  }

  @Test
  void handleCommand_newWhenTargetIsInGame_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession sender = registry.registerPlayer("p1", "Alice", out);
    PlayerSession target = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    registry.createSession(
        List.of(target, registry.registerPlayer("p3", "Carol", new PrintWriter(new StringWriter(), true))),
        new RecordingGameController());

    invokeHandleCommand(server, out, sender, "NEW p2");

    assertTrue(buffer.toString().contains("already in a game"));
  }

  @Test
  void handleCommand_awayMarksIdlePlayerAway() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "AWAY");

    assertEquals(PlayerSession.Status.AWAY, player.getStatus());
    assertTrue(buffer.toString().contains("STATUS_CHANGED away"));
  }

  @Test
  void handleCommand_awayRejectsInGamePlayer() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);
    PlayerSession opponent = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    registry.createSession(List.of(player, opponent), new RecordingGameController());

    invokeHandleCommand(server, out, player, "AWAY");

    assertEquals(PlayerSession.Status.INGAME, player.getStatus());
    assertTrue(buffer.toString().contains("Cannot go away while in a game"));
  }

  @Test
  void handleCommand_backReturnsIdlePlayerToIdle() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "BACK");

    assertEquals(PlayerSession.Status.IDLE, player.getStatus());
    assertTrue(buffer.toString().contains("STATUS_CHANGED idle"));
  }

  @Test
  void handleCommand_backRejectsInGamePlayer() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);
    PlayerSession opponent = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    registry.createSession(List.of(player, opponent), new RecordingGameController());

    invokeHandleCommand(server, out, player, "BACK");

    assertEquals(PlayerSession.Status.INGAME, player.getStatus());
    assertTrue(buffer.toString().contains("Cannot use BACK while in a game"));
  }

  @Test
  void handleCommand_moveWithActiveSessionReturnsMoveOk() throws Exception {
    RecordingGameController controller = new RecordingGameController();
    GameServer server = new GameServer(
        "TestServer",
        23456,
        () -> controller,
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);
    PlayerSession opponent = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    registry.createSession(List.of(player, opponent), controller);

    invokeHandleCommand(server, out, player, "MOVE A3-B4");

    assertTrue(buffer.toString().contains("MOVE_OK A3-B4"));
    assertEquals("A3", controller.lastFrom);
    assertEquals("B4", controller.lastTo);
    assertFalse(controller.lastIsManoury);
    assertEquals(1, controller.executeMoveCalls);
    assertEquals(PlayerSession.Status.INGAME, player.getStatus());
  }

  @Test
  void handleCommand_moveWithIllegalMoveReturnsError() throws Exception {
    RecordingGameController controller = new RecordingGameController();
    controller.moveException = new IllegalArgumentException("forbidden move");
    GameServer server = new GameServer(
        "TestServer",
        23456,
        () -> controller,
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);
    PlayerSession opponent = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));
    registry.createSession(List.of(player, opponent), controller);

    invokeHandleCommand(server, out, player, "MOVE A3-B4");

    assertTrue(buffer.toString().contains("Illegal move"));
    assertTrue(buffer.toString().contains("forbidden move"));
  }

  @Test
  void handleCommand_acceptStartsGameAndSendsMessages() throws Exception {
    RecordingGameController controller = new RecordingGameController();
    GameServer server = new GameServer(
        "TestServer",
        23456,
        () -> controller,
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);

    invokeHandleCommand(server, aliceOut, alice, "NEW bob");
    invokeHandleCommand(server, bobOut, bob, "ACCEPT");

    assertEquals(1, controller.startNewGameCalls);
    assertEquals(1, registry.getActiveSessionCount());
    assertNotNull(registry.getSessionForPlayer("alice"));
    assertEquals(PlayerSession.Status.INGAME, alice.getStatus());
    assertEquals(PlayerSession.Status.INGAME, bob.getStatus());
    assertTrue(aliceBuffer.toString().contains("INVITATION_ACCEPTED STARTING_GAME"));
    assertTrue(aliceBuffer.toString().contains("GAME_START session="));
    assertTrue(bobBuffer.toString().contains("GAME_START session="));
  }

  @Test
  void handleCommand_acceptInGuiOnlyModeTagsGameStart() throws Exception {
    RecordingGameController controller = new RecordingGameController();
    GameServer server = new GameServer(
        "TestServer",
        23456,
        () -> controller,
        false,
        true);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);

    invokeHandleCommand(server, aliceOut, alice, "NEW bob");
    invokeHandleCommand(server, bobOut, bob, "ACCEPT");

    assertTrue(aliceBuffer.toString().contains("mode=GUI"));
    assertTrue(bobBuffer.toString().contains("mode=GUI"));
  }

  @Test
  void handleCommand_acceptDeclineCancelWithoutPendingInvitationReturnErrors() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter bobBuffer = new StringWriter();
    StringWriter aliceBuffer = new StringWriter();
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);

    invokeHandleCommand(server, bobOut, bob, "ACCEPT");
    invokeHandleCommand(server, bobOut, bob, "DECLINE");
    invokeHandleCommand(server, aliceOut, alice, "CANCEL");

    assertTrue(bobBuffer.toString().contains("No pending invitation found for you"));
    assertTrue(aliceBuffer.toString().contains("no pending outgoing invitation"));
  }

  @Test
  void handleCommand_declineClearsInvitationAndNotifiesInviter() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);

    invokeHandleCommand(server, aliceOut, alice, "NEW bob");
    invokeHandleCommand(server, bobOut, bob, "DECLINE");

    assertEquals(PlayerSession.Status.IDLE, bob.getStatus());
    assertTrue(bobBuffer.toString().contains("INVITATION_DECLINED"));
    assertTrue(aliceBuffer.toString().contains("INVITATION_DECLINED BY=bob"));
  }

  @Test
  void handleCommand_cancelClearsOutgoingInvitationAndNotifiesInvitee() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);

    invokeHandleCommand(server, aliceOut, alice, "NEW bob");
    invokeHandleCommand(server, aliceOut, alice, "CANCEL");

    assertEquals(PlayerSession.Status.IDLE, bob.getStatus());
    assertTrue(aliceBuffer.toString().contains("INVITATION_CANCELLED"));
    assertTrue(bobBuffer.toString().contains("INVITATION_CANCELLED BY=alice"));
  }

  @Test
  void handleInvitationExpiry_notifiesWaitingPlayers() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    PlayerSession bob = registry.registerPlayer("bob", "Bob", bobOut);

    InvitationManager manager = getInvitationManager(server);
    InvitationManager.CreateResult result = manager.createInvitation(alice, "bob", registry);

    invokeHandleInvitationExpiry(server, result.invitation, registry);

    assertEquals(PlayerSession.Status.IDLE, bob.getStatus());
    assertTrue(aliceBuffer.toString().contains("INVITATION_EXPIRED TO=bob"));
    assertTrue(bobBuffer.toString().contains("INVITATION_EXPIRED FROM=alice"));
  }

  @Test
  void handleInvitationExpiry_whenInviteeIsMissing_stillNotifiesInviter() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    registry.registerPlayer("bob", "Bob", bobOut);

    InvitationManager manager = getInvitationManager(server);
    InvitationManager.CreateResult result = manager.createInvitation(alice, "bob", registry);
    registry.removePlayer("bob");

    invokeHandleInvitationExpiry(server, result.invitation, registry);

    assertTrue(aliceBuffer.toString().contains("INVITATION_EXPIRED TO=bob"));
    assertTrue(bobBuffer.toString().isEmpty());
  }

  @Test
  void cleanupPlayerInvitations_cancelsOutgoingAndDeclinesIncomingOnDisconnect() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter aliceBuffer = new StringWriter();
    StringWriter bobBuffer = new StringWriter();
    StringWriter carolBuffer = new StringWriter();
    PrintWriter aliceOut = new PrintWriter(aliceBuffer, true);
    PrintWriter bobOut = new PrintWriter(bobBuffer, true);
    PrintWriter carolOut = new PrintWriter(carolBuffer, true);
    PlayerSession alice = registry.registerPlayer("alice", "Alice", aliceOut);
    registry.registerPlayer("bob", "Bob", bobOut);
    PlayerSession carol = registry.registerPlayer("carol", "Carol", carolOut);

    InvitationManager manager = getInvitationManager(server);
    manager.createInvitation(alice, "bob", registry);
    manager.createInvitation(carol, "alice", registry);

    invokeCleanupPlayerInvitations(server, alice);

    assertTrue(bobBuffer.toString().contains("INVITATION_CANCELLED BY=alice (player disconnected)"));
    assertTrue(carolBuffer.toString().contains("INVITATION_DECLINED BY=alice (player disconnected)"));
  }

  @Test
  void handleClient_withShortRegisterHandshake_returnsUsageError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    ByteArrayInputStream in = new ByteArrayInputStream("REGISTER p1\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("ERROR: Usage: REGISTER <id> <name>"));
    assertEquals(0, getRegistry(server).getPlayerCount());
  }

  @Test
  void handleClient_withDuplicateId_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);
    registry.registerPlayer("p1", "Existing", new PrintWriter(new StringWriter(), true));

    ByteArrayInputStream in = new ByteArrayInputStream("REGISTER p1 Alice\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("ID 'p1' is already taken"));
    assertEquals(1, registry.getPlayerCount());
  }

  @Test
  void handleClient_registersGuiOnlyPlayerAndSendsGuiWelcome() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        true);

    ByteArrayInputStream in = new ByteArrayInputStream("REGISTER p1 Alice\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("WELCOME p1 mode=GUI"));
    assertTrue(output.contains("WAITING"));
  }

  @Test
  void start_whenPortAlreadyInUse_throwsIOException() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      int usedPort = occupied.getLocalPort();

      GameServer server = new GameServer(
          "TestServer",
          usedPort,
          defaultFactory(),
          false,
          false);

      IOException ex = assertThrows(IOException.class, server::start);
      assertTrue(ex.getMessage().contains("already in use"));
      assertTrue(!server.isRunning());
    }
  }

  @Test
  void tryAutoStart_withOneIdlePlayer_sendsWaitingMessage() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    GameRegistry registry = getRegistry(server);
    StringWriter p1Buffer = new StringWriter();
    registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));

    invokeTryAutoStart(server);

    assertTrue(p1Buffer.toString().contains("WAITING"));
    assertEquals(0, registry.getActiveSessionCount());
  }

  @Test
  void handleCommand_players_returnsRegisteredPlayersList() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);

    PlayerSession p1 = registry.registerPlayer("p1", "Alice", out);
    registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    invokeHandleCommand(server, out, p1, "PLAYERS");

    String output = buffer.toString();
    assertTrue(output.contains("PLAYERS"));
    assertTrue(output.contains("Alice"));
    assertTrue(output.contains("Bob"));
  }

  @Test
  void handleCommand_status_withoutTarget_returnsOwnInfo() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "STATUS");

    String output = buffer.toString();
    assertTrue(output.contains("PLAYER_INFO"));
    assertTrue(output.contains("Alice"));
  }

  @Test
  void handleCommand_scoreboard_returnsFormattedScores() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);

    PlayerSession p1 = registry.registerPlayer("p1", "Alice", out);
    PlayerSession p2 = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    p1.recordWin();
    p2.recordLoss();

    invokeHandleCommand(server, out, p1, "SCOREBOARD");

    String output = buffer.toString();
    assertTrue(output.contains("SCOREBOARD"));
    assertTrue(output.contains("Alice"));
    assertTrue(output.contains("Bob"));
  }

  @Test
  void handleCommand_newWithoutTarget_returnsUsageError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "NEW");

    assertTrue(buffer.toString().contains("ERROR: Usage: NEW <player_id>"));
  }

  @Test
  void handleCommand_newWithUnknownTarget_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "NEW unknown");

    assertTrue(buffer.toString().contains("ERROR: Player 'unknown' not found."));
  }

  @Test
  void handleCommand_moveWithoutSession_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "MOVE A3-B4");

    assertTrue(
        buffer.toString().contains("ERROR: Not in a game. Use NEW to invite a player."));
  }

  @Test
  void handleCommand_unknownCommand_returnsError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);
    GameRegistry registry = getRegistry(server);

    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    invokeHandleCommand(server, out, player, "UNKNOWN_CMD");

    assertTrue(buffer.toString().contains("ERROR: Unknown command 'UNKNOWN_CMD'."));
  }

  @Test
  void handleClient_withInvalidHandshake_returnsProtocolError() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    ByteArrayInputStream in = new ByteArrayInputStream("HELLO\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("ERROR: First message must be REGISTER <id> <name>"));
    assertEquals(0, getRegistry(server).getPlayerCount());
  }

  @Test
  void handleClient_registersPlayerAndSendsWelcome() throws Exception {
    GameServer server = new GameServer(
        "TestServer",
        23456,
        defaultFactory(),
        false,
        false);

    ByteArrayInputStream in = new ByteArrayInputStream("REGISTER p1 Alice\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("WELCOME p1"));
    assertTrue(output.contains("WAITING"));
    assertNull(getRegistry(server).getPlayer("p1"));
  }

  private static InvitationManager getInvitationManager(GameServer server) throws Exception {
    Field field = GameServer.class.getDeclaredField("invitationManager");
    field.setAccessible(true);
    return (InvitationManager) field.get(server);
  }

  private static ServerSocket getServerSocket(GameServer server) throws Exception {
    Field field = GameServer.class.getDeclaredField("serverSocket");
    field.setAccessible(true);
    return (ServerSocket) field.get(server);
  }

  private static GameRegistry getRegistry(GameServer server) throws Exception {
    Field field = GameServer.class.getDeclaredField("registry");
    field.setAccessible(true);
    return (GameRegistry) field.get(server);
  }

  private static void invokeTryAutoStart(GameServer server) throws Exception {
    Method method = GameServer.class.getDeclaredMethod("tryAutoStart");
    method.setAccessible(true);
    method.invoke(server);
  }

  private static void invokeHandleCommand(
      GameServer server,
      PrintWriter out,
      PlayerSession player,
      String line)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod(
        "handleCommand",
        PrintWriter.class,
        PlayerSession.class,
        String.class);
    method.setAccessible(true);
    method.invoke(server, out, player, line);
  }

  private static void invokeHandleClient(GameServer server, Socket client)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod("handleClient", Socket.class);
    method.setAccessible(true);
    method.invoke(server, client);
  }

  private static void invokeHandleInvitationExpiry(
      GameServer server,
      Invitation invitation,
      GameRegistry registry)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod(
        "handleInvitationExpiry",
        Invitation.class,
        GameRegistry.class);
    method.setAccessible(true);
    method.invoke(server, invitation, registry);
  }

  private static void invokeCleanupPlayerInvitations(GameServer server, PlayerSession player)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod(
        "cleanupPlayerInvitations",
        PlayerSession.class);
    method.setAccessible(true);
    method.invoke(server, player);
  }

  private static void waitForRunning(GameServer server) throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      if (server.isRunning()) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static void waitUntilPlayerRemoved(GameServer server, String playerId)
      throws InterruptedException, Exception {
    GameRegistry registry = getRegistry(server);
    for (int i = 0; i < 100; i++) {
      if (registry.getPlayer(playerId) == null) {
        return;
      }
      Thread.sleep(20);
    }
  }

  @SuppressWarnings("unchecked")
  private static Set<PrintWriter> getConnectedClients(GameServer server)
      throws Exception {
    Field field = GameServer.class.getDeclaredField("connectedClients");
    field.setAccessible(true);
    return (Set<PrintWriter>) field.get(server);
  }

  private static void setField(GameServer server, String fieldName, Object value)
      throws Exception {
    Field field = GameServer.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(server, value);
  }

  private static GameControllerFactory defaultFactory() {
    return () -> new GameController(new HeadlessView());
  }

  private static final class RecordingGameController extends GameController {

    private int startNewGameCalls = 0;
    private int executeMoveCalls = 0;
    private String lastFrom;
    private String lastTo;
    private boolean lastIsManoury; // This line is retained for context
    private RuntimeException moveException;

    private RecordingGameController() {
      super(new HeadlessView());
    }

    @Override
    public void startNewGame(Configuration configuration) {
      startNewGameCalls++;
    }

    @Override
    public void executeMove(String from, String to, boolean isManoury) {
      executeMoveCalls++;
      if (moveException != null) {
        throw moveException;
      }
      lastFrom = from;
      lastTo = to;
      lastIsManoury = isManoury;
    }
  }

  private static final class StubSocket extends Socket {
    private final ByteArrayInputStream in;
    private final ByteArrayOutputStream out;

    private StubSocket(ByteArrayInputStream in, ByteArrayOutputStream out) {
      this.in = in;
      this.out = out;
    }

    @Override
    public java.io.InputStream getInputStream() {
      return in;
    }

    @Override
    public java.io.OutputStream getOutputStream() {
      return out;
    }
  }
}