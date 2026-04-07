package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GameServer}.
 */
class GameServerTest {

  @Test
  void constructor_setsPortAndServerStartsStopped() {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    assertEquals(23456, server.getPort());
    assertTrue(!server.isRunning());
  }

  @Test
  void stop_whenServerIsNotRunning_keepsStateStopped() {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    server.stop();

    assertTrue(!server.isRunning());
  }

  @Test
  void stop_whenRunning_notifiesClientsAndClosesResources() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

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
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    setField(server, "running", true);

    server.start();

    assertTrue(server.isRunning());
  }

  @Test
  void start_whenPortAlreadyInUse_throwsIOException() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      int usedPort = occupied.getLocalPort();
      GameControllerFactory factory = defaultFactory();
      GameServer server = new GameServer("TestServer", usedPort, factory);

      IOException ex = assertThrows(IOException.class, server::start);
      assertTrue(ex.getMessage().contains("already in use"));
      assertTrue(!server.isRunning());
    }
  }

  @Test
  void start_acceptsClientAndProcessesRegisterQuit_thenStopsCleanly() throws Exception {
    int port;
    try (ServerSocket probe = new ServerSocket(0)) {
      port = probe.getLocalPort();
    }

    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", port, factory);

    Thread serverThread = new Thread(() -> {
      try {
        server.start();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, "gameserver-start-test-thread");

    serverThread.start();

    waitForCondition(server::isRunning, 2000);
    assertTrue(server.isRunning());

    try (Socket client = new Socket("127.0.0.1", port);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {

      out.println("REGISTER pStart Alice");

      String line1 = in.readLine();

      assertNotNull(line1);
      assertTrue(line1.startsWith("WELCOME pStart"));

      out.println("QUIT");
      List<String> serverLines = new ArrayList<>();
      for (int i = 0; i < 3; i++) {
        String line = in.readLine();
        if (line == null) {
          break;
        }
        serverLines.add(line);
        if ("BYE".equals(line)) {
          break;
        }
      }
      assertTrue(serverLines.contains("BYE"));
    } finally {
      server.stop();
    }

    serverThread.join(2000);
    assertTrue(!serverThread.isAlive());
    assertTrue(!server.isRunning());
  }

  @Test
  void main_whenPortIsAlreadyUsed_printsStartupFailureAndReturns() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      int usedPort = occupied.getLocalPort();

      java.io.ByteArrayOutputStream errBuffer = new java.io.ByteArrayOutputStream();
      PrintStream originalErr = System.err;
      System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));

      try {
        GameServer.main(new String[] { "--server", String.valueOf(usedPort) });
      } finally {
        System.setErr(originalErr);
      }

      String errOutput = errBuffer.toString(StandardCharsets.UTF_8);
      assertTrue(errOutput.contains("Failed to start server:"));
      assertTrue(errOutput.contains("already in use"));
    }
  }

  @Test
  void tryAutoStart_withOneIdlePlayer_sendsWaitingMessage() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    GameRegistry registry = getRegistry(server);
    StringWriter p1Buffer = new StringWriter();
    registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));

    invokePrivate(server, "tryAutoStart");

    assertTrue(p1Buffer.toString().contains("WAITING Waiting for another player to join..."));
    assertEquals(0, registry.getActiveSessionCount());
  }

  @Test
  void tryAutoStart_withTwoIdlePlayers_createsSessionAndStartsController() throws Exception {
    AtomicInteger createCount = new AtomicInteger(0);
    GameControllerFactory factory = () -> {
      createCount.incrementAndGet();
      return new GameController(new HeadlessView());
    };

    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    StringWriter p2Buffer = new StringWriter();

    registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));
    registry.registerPlayer("p2", "Bob", new PrintWriter(p2Buffer, true));

    invokePrivate(server, "tryAutoStart");

    assertEquals(1, registry.getActiveSessionCount());
    assertNotNull(registry.getSessionForPlayer("p1"));
    assertNotNull(registry.getSessionForPlayer("p2"));

    assertTrue(p1Buffer.toString().contains("GAME_START"));
    assertTrue(p2Buffer.toString().contains("GAME_START"));
    assertTrue(p1Buffer.toString().contains("players=p1 p2"));

    assertEquals(1, createCount.get());
  }

  @Test
  void handleNewGame_withUnknownPlayer_returnsErrorAndDoesNotCreateSession() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter requesterBuffer = new StringWriter();
    PlayerSession requester = registry.registerPlayer("p1", "Alice", new PrintWriter(requesterBuffer, true));

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);

    invokeHandleNewGame(server, out, requester, new String[] { "unknown" });

    assertTrue(outBuffer.toString().contains("ERROR: Player 'unknown' not found."));
    assertEquals(0, registry.getActiveSessionCount());
    assertNull(registry.getSessionForPlayer("p1"));
  }

  @Test
  void handleNewGame_withValidParticipants_createsSessionAndNotifiesPlayers() throws Exception {
    AtomicInteger createCount = new AtomicInteger(0);
    GameControllerFactory factory = () -> {
      createCount.incrementAndGet();
      return new GameController(new HeadlessView());
    };

    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    StringWriter p2Buffer = new StringWriter();

    PlayerSession p1 = registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));
    registry.registerPlayer("p2", "Bob", new PrintWriter(p2Buffer, true));

    StringWriter requesterOutBuffer = new StringWriter();
    PrintWriter requesterOut = new PrintWriter(requesterOutBuffer, true);

    invokeHandleNewGame(server, requesterOut, p1, new String[] { "p1", "p2" });

    assertEquals(1, registry.getActiveSessionCount());
    assertNotNull(registry.getSessionForPlayer("p1"));
    assertNotNull(registry.getSessionForPlayer("p2"));

    assertTrue(p1Buffer.toString().contains("GAME_START"));
    assertTrue(p2Buffer.toString().contains("GAME_START"));

    assertEquals(1, createCount.get());
  }

  @Test
  void processMessages_handlesPingStatusAndUnknownCommand() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    String commands = String.join("\n", "PING", "STATUS", "UNKNOWN_CMD") + "\n";
    BufferedReader in = new BufferedReader(new StringReader(commands));

    invokeProcessMessages(server, in, out, player);

    String serverOutput = outBuffer.toString();
    assertTrue(serverOutput.contains("PONG TIME="));
    assertTrue(serverOutput.contains("STATUS port=23456 players=1 sessions=0"));
    assertTrue(serverOutput.contains("ERROR: Unknown command 'UNKNOWN_CMD'."));
  }

  @Test
  void processMessages_moveWithoutSession_returnsError() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    BufferedReader in = new BufferedReader(new StringReader("MOVE A3-B4\n"));
    invokeProcessMessages(server, in, out, player);

    assertTrue(outBuffer.toString().contains("ERROR: Not in a game. Use NEW to start one."));
  }

  @Test
  void processMessages_newWithoutParticipants_returnsUsageError() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    BufferedReader in = new BufferedReader(new StringReader("NEW\n"));
    invokeProcessMessages(server, in, out, player);

    assertTrue(outBuffer.toString().contains("ERROR: Usage: NEW <player_id> [player_id2 ...]"));
  }

  @Test
  void processMessages_newWithUnknownParticipant_returnsError() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    BufferedReader in = new BufferedReader(new StringReader("NEW unknown\n"));
    invokeProcessMessages(server, in, out, player);

    assertTrue(outBuffer.toString().contains("ERROR: Player 'unknown' not found."));
  }

  @Test
  void processMessages_moveWithActiveSession_successReturnsMoveOkAndNotifiesOpponent()
      throws Exception {
    GameController controller = new GameController(new HeadlessView());
    controller.startNewGame(fr.ubordeaux.pdp.model.core.Configuration.getDefaultConfiguration());

    GameServer server = new GameServer("TestServer", 23456, () -> controller);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    StringWriter p2Buffer = new StringWriter();
    PlayerSession p1 = registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));
    PlayerSession p2 = registry.registerPlayer("p2", "Bob", new PrintWriter(p2Buffer, true));

    registry.createSession(List.of(p1, p2), controller);

    BufferedReader in = new BufferedReader(new StringReader("MOVE B6-C5\n"));
    invokeProcessMessages(server, in, new PrintWriter(p1Buffer, true), p1);

    assertTrue(p1Buffer.toString().contains("MOVE_OK B6-C5"));
    assertTrue(p2Buffer.toString().contains("OPPONENT_MOVE B6-C5"));
  }

  @Test
  void processMessages_moveWithActiveSession_invalidFormatReturnsError() throws Exception {
    GameController controller = new GameController(new HeadlessView());
    controller.startNewGame(fr.ubordeaux.pdp.model.core.Configuration.getDefaultConfiguration());

    GameServer server = new GameServer("TestServer", 23456, () -> controller);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    PlayerSession p1 = registry.registerPlayer("p1", "Alice", new PrintWriter(p1Buffer, true));
    PlayerSession p2 = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    registry.createSession(List.of(p1, p2), controller);

    BufferedReader in = new BufferedReader(new StringReader("MOVE BADFORMAT\n"));
    invokeProcessMessages(server, in, new PrintWriter(p1Buffer, true), p1);

    assertTrue(
        p1Buffer
            .toString()
            .contains("ERROR: Invalid move format. Expected FROM-TO (e.g. e2-e4)."));
  }

  @Test
  void processMessages_playersReturnsRegisteredPlayersList() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    PrintWriter p1Out = new PrintWriter(p1Buffer, true);
    PlayerSession p1 = registry.registerPlayer("p1", "Alice", p1Out);

    registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    BufferedReader in = new BufferedReader(new StringReader("PLAYERS\n"));
    invokeProcessMessages(server, in, p1Out, p1);

    String output = p1Buffer.toString();
    assertTrue(output.contains("PLAYERS"));
    assertTrue(output.contains("p1"));
    assertTrue(output.contains("Alice"));
    assertTrue(output.contains("p2"));
    assertTrue(output.contains("Bob"));
  }

  @Test
  void processMessages_scoreboardReturnsFormattedScores() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter p1Buffer = new StringWriter();
    PrintWriter p1Out = new PrintWriter(p1Buffer, true);
    PlayerSession p1 = registry.registerPlayer("p1", "Alice", p1Out);
    PlayerSession p2 = registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

    p1.recordWin();
    p2.recordLoss();

    BufferedReader in = new BufferedReader(new StringReader("SCOREBOARD\n"));
    invokeProcessMessages(server, in, p1Out, p1);

    String output = p1Buffer.toString();
    assertTrue(output.contains("SCOREBOARD"));
    assertTrue(output.contains("p1"));
    assertTrue(output.contains("Alice"));
    assertTrue(output.contains("W:1"));
    assertTrue(output.contains("p2"));
    assertTrue(output.contains("Bob"));
  }

  @Test
  void processMessages_quitRemovesPlayerAndClientWriter() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);
    GameRegistry registry = getRegistry(server);

    StringWriter outBuffer = new StringWriter();
    PrintWriter out = new PrintWriter(outBuffer, true);
    PlayerSession player = registry.registerPlayer("p1", "Alice", out);

    Set<PrintWriter> connectedClients = getConnectedClients(server);
    connectedClients.add(out);

    BufferedReader in = new BufferedReader(new StringReader("QUIT\n"));
    invokeProcessMessages(server, in, out, player);

    assertTrue(outBuffer.toString().contains("BYE"));
    assertNull(registry.getPlayer("p1"));
    assertTrue(!connectedClients.contains(out));
  }

  @Test
  void handleClient_withInvalidHandshake_returnsProtocolError() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    ByteArrayInputStream in = new ByteArrayInputStream("HELLO\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("ERROR: First message must be REGISTER <id> <name>"));
    assertEquals(0, getRegistry(server).getPlayerCount());
  }

  @Test
  void handleClient_registerThenQuit_welcomesAndRemovesPlayer() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    String inputLines = String.join("\n", "REGISTER p1 Alice", "QUIT") + "\n";
    ByteArrayInputStream in = new ByteArrayInputStream(inputLines.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("WELCOME p1"));
    assertTrue(output.contains("WAITING Waiting for another player to join..."));
    assertTrue(output.contains("BYE"));
    assertNull(getRegistry(server).getPlayer("p1"));
  }

  @Test
  void handleClient_duplicatePlayerId_returnsError() throws Exception {
    GameControllerFactory factory = defaultFactory();
    GameServer server = new GameServer("TestServer", 23456, factory);

    ByteArrayInputStream firstIn = new ByteArrayInputStream("REGISTER p1 Alice\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream firstOut = new ByteArrayOutputStream();
    Socket firstClient = new StubSocket(firstIn, firstOut);
    invokeHandleClient(server, firstClient);

    ByteArrayInputStream secondIn = new ByteArrayInputStream("REGISTER p1 Bob\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream secondOut = new ByteArrayOutputStream();
    Socket secondClient = new StubSocket(secondIn, secondOut);
    invokeHandleClient(server, secondClient);

    String secondOutput = secondOut.toString(StandardCharsets.UTF_8);
    assertTrue(secondOutput.contains("ERROR: Player ID 'p1' is already taken."));
    assertEquals(1, getRegistry(server).getPlayerCount());
  }

  private static GameRegistry getRegistry(GameServer server) throws Exception {
    Field field = GameServer.class.getDeclaredField("registry");
    field.setAccessible(true);
    return (GameRegistry) field.get(server);
  }

  private static void invokePrivate(GameServer server, String methodName) throws Exception {
    Method method = GameServer.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    method.invoke(server);
  }

  private static void invokeHandleNewGame(
      GameServer server, PrintWriter out, PlayerSession requester, String[] participantIds)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod(
        "handleNewGame", PrintWriter.class, PlayerSession.class, String[].class);
    method.setAccessible(true);
    method.invoke(server, out, requester, participantIds);
  }

  private static void invokeProcessMessages(
      GameServer server, BufferedReader in, PrintWriter out, PlayerSession player)
      throws Exception {
    Method method = GameServer.class.getDeclaredMethod(
        "processMessages", BufferedReader.class, PrintWriter.class, PlayerSession.class);
    method.setAccessible(true);
    method.invoke(server, in, out, player);
  }

  private static void invokeHandleClient(GameServer server, Socket client) throws Exception {
    Method method = GameServer.class.getDeclaredMethod("handleClient", Socket.class);
    method.setAccessible(true);
    method.invoke(server, client);
  }

  @SuppressWarnings("unchecked")
  private static Set<PrintWriter> getConnectedClients(GameServer server) throws Exception {
    Field field = GameServer.class.getDeclaredField("connectedClients");
    field.setAccessible(true);
    return (Set<PrintWriter>) field.get(server);
  }

  private static void setField(GameServer server, String fieldName, Object value) throws Exception {
    Field field = GameServer.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(server, value);
  }

  private static void waitForCondition(java.util.function.BooleanSupplier condition, long timeoutMs)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static GameControllerFactory defaultFactory() {
    return () -> new GameController(new HeadlessView());
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
