package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
    PlayerSession p2 =
        registry.registerPlayer("p2", "Bob", new PrintWriter(new StringWriter(), true));

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

    ByteArrayInputStream in =
        new ByteArrayInputStream("HELLO\n".getBytes(StandardCharsets.UTF_8));
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

    ByteArrayInputStream in =
        new ByteArrayInputStream("REGISTER p1 Alice\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Socket client = new StubSocket(in, out);

    invokeHandleClient(server, client);

    String output = out.toString(StandardCharsets.UTF_8);
    assertTrue(output.contains("WELCOME p1"));
    assertTrue(output.contains("WAITING"));
    assertNull(getRegistry(server).getPlayer("p1"));
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
    Method method =
        GameServer.class.getDeclaredMethod(
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