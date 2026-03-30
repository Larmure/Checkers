package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GameServerTest {

  private static final String LOCALHOST = "127.0.0.1";
  private static final long START_TIMEOUT_MS = 2_000;
  private static final long JOIN_TIMEOUT_MS = 1_500;
  private static final long GAME_START_TIMEOUT_MS = 2_000;
  private static final int CLIENT_READ_TIMEOUT_MS = 150;

  private GameServer server;
  private Thread serverThread;

  @AfterEach
  void tearDown() throws Exception {
    if (server != null && server.isRunning()) {
      server.stop();
    }
    if (serverThread != null) {
      serverThread.join(1_000);
    }
  }


  @Test
  void startAndStopShouldUpdateRunningState() throws Exception {
    int port = getFreePort();

    startServer(port);

    assertTrue(server.isRunning());
    assertEquals(port, server.getPort());

    server.stop();
    waitUntilStopped(server, 1_000);

    assertTrue(!server.isRunning());
  }

  @Test
  void startShouldThrowWhenPortAlreadyInUse() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0)) {
      int port = occupied.getLocalPort();
      GameControllerFactory factory = this::newController;
      GameServer blockedServer = new GameServer(
          "BlockedServer",
          port,
          factory,
          true
      );

      IOException exception = assertThrows(IOException.class, blockedServer::start);
      assertTrue(exception.getMessage().contains("already in use"));
    }
  }

  @Test
  void invalidHandshakeShouldReturnError() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (Socket socket = new Socket(LOCALHOST, port)) {
      socket.setSoTimeout(1_000);

      PrintWriter out = new PrintWriter(
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
          true
      );
      BufferedReader in = new BufferedReader(
          new InputStreamReader(socket.getInputStream())
      );

      out.println("HELLO");

      String response = in.readLine();
      assertNotNull(response);
      assertTrue(response.contains("First message must be REGISTER"));
    }
  }

  @Test
  void duplicatePlayerIdShouldBeRejected() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (TestClient firstClient = connectClient(port, "alice", "Alice");
         TestClient secondClient = connectClientRaw(port)) {

      secondClient.send("REGISTER alice OtherAlice");

      String response = secondClient.awaitContains("already taken", JOIN_TIMEOUT_MS);
      assertNotNull(response);
      assertTrue(response.contains("already taken"));
    }
  }

  @Test
  void statusAndPlayersShouldReturnServerInformation() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (TestClient alice = connectClient(port, "alice", "Alice");
         TestClient bob = connectClient(port, "bob", "Bob")) {

      alice.send("STATUS");
      String status = alice.awaitContains("STATUS port=", JOIN_TIMEOUT_MS);

      assertNotNull(status);
      assertTrue(status.contains("port=" + port));
      assertTrue(status.contains("players=2"));

      alice.send("PLAYERS");
      String header = alice.awaitContains("PLAYERS", JOIN_TIMEOUT_MS);
      String line1 = alice.awaitLine(JOIN_TIMEOUT_MS);
      String line2 = alice.awaitLine(JOIN_TIMEOUT_MS);

      assertNotNull(header);
      assertNotNull(line1);
      assertNotNull(line2);

      String combined = line1 + "\n" + line2;
      assertTrue(combined.contains("alice"));
      assertTrue(combined.contains("bob"));
    }
  }

  @Test
  void awayBackAndMoveWithoutGameShouldWork() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (TestClient alice = connectClient(port, "alice", "Alice")) {
      alice.send("AWAY");
      String away = alice.awaitContains("STATUS_CHANGED away", JOIN_TIMEOUT_MS);
      assertNotNull(away);

      alice.send("BACK");
      String back = alice.awaitContains("STATUS_CHANGED idle", JOIN_TIMEOUT_MS);
      assertNotNull(back);

      alice.send("MOVE b6-a5");
      String error = alice.awaitContains("Not in a game", JOIN_TIMEOUT_MS);
      assertNotNull(error);
    }
  }

  @Test
  void invitationAcceptShouldStartGameForBothPlayers() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (TestClient alice = connectClient(port, "alice", "Alice");
         TestClient bob = connectClient(port, "bob", "Bob")) {

      alice.send("NEW bob");

      String sent = alice.awaitContains("INVITATION_SENT", JOIN_TIMEOUT_MS);
      String received = bob.awaitContains("INVITATION_RECEIVED", JOIN_TIMEOUT_MS);

      assertNotNull(sent);
      assertNotNull(received);

      bob.send("ACCEPT");

      String accepted = alice.awaitContains("INVITATION_ACCEPTED", JOIN_TIMEOUT_MS);
      String gameStartAlice = alice.awaitContains(
          "GAME_START",
          GAME_START_TIMEOUT_MS
      );
      String gameStartBob = bob.awaitContains(
          "GAME_START",
          GAME_START_TIMEOUT_MS
      );

      assertNotNull(accepted);
      assertNotNull(gameStartAlice);
      assertNotNull(gameStartBob);
      assertTrue(gameStartAlice.contains("players=alice bob"));
      assertTrue(gameStartBob.contains("players=alice bob"));
    }
  }

  @Test
  void quitShouldReturnBye() throws Exception {
    int port = getFreePort();
    startServer(port);

    try (TestClient alice = connectClient(port, "alice", "Alice")) {
      alice.send("QUIT");

      String bye = alice.awaitContains("BYE", JOIN_TIMEOUT_MS);
      assertNotNull(bye);
    }
  }

  private GameController newController() {
    return new GameController(new HeadlessView());
  }

  private void startServer(int port) throws Exception {
    GameControllerFactory factory = this::newController;
    server = new GameServer("TestServer", port, factory, true);

    AtomicReference<Throwable> startError = new AtomicReference<>();

    serverThread = new Thread(
        () -> {
          try {
            server.start();
          } catch (Throwable throwable) {
            startError.set(throwable);
          }
        },
        "game-server-test-thread"
    );

    serverThread.setDaemon(true);
    serverThread.start();

    long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
    while (!server.isRunning() && System.currentTimeMillis() < deadline) {
      if (startError.get() != null) {
        fail("Server failed to start: " + startError.get().getMessage());
      }
      Thread.sleep(20);
    }

    assertTrue(server.isRunning(), "Server did not start in time.");
  }

  private void waitUntilStopped(GameServer gameServer, long timeoutMs)
      throws Exception {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (gameServer.isRunning() && System.currentTimeMillis() < deadline) {
      Thread.sleep(20);
    }
  }

  private int getFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private TestClient connectClient(int port, String id, String name)
      throws Exception {
    TestClient client = connectClientRaw(port);
    client.send("REGISTER " + id + " " + name);

    String welcome = client.awaitContains("WELCOME " + id, JOIN_TIMEOUT_MS);
    assertNotNull(welcome, "Expected WELCOME for " + id);

    return client;
  }

  private TestClient connectClientRaw(int port) throws Exception {
    Socket socket = new Socket(LOCALHOST, port);
    socket.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
    return new TestClient(socket);
  }

  private static final class TestClient implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private TestClient(Socket socket) throws IOException {
      this.socket = socket;
      this.in = new BufferedReader(
          new InputStreamReader(socket.getInputStream())
      );
      this.out = new PrintWriter(
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
          true
      );
    }

    private void send(String line) {
      out.println(line);
    }

    private String awaitLine(long timeoutMs) throws Exception {
      long deadline = System.currentTimeMillis() + timeoutMs;

      while (System.currentTimeMillis() < deadline) {
        try {
          return in.readLine();
        } catch (SocketTimeoutException ignored) {
          // Continue polling until timeout.
        }
      }
      return null;
    }

    private String awaitContains(String expected, long timeoutMs)
        throws Exception {
      long deadline = System.currentTimeMillis() + timeoutMs;

      while (System.currentTimeMillis() < deadline) {
        try {
          String line = in.readLine();
          if (line == null) {
            return null;
          }
          if (line.contains(expected)) {
            return line;
          }
        } catch (SocketTimeoutException ignored) {
          // Continue polling until timeout.
        }
      }
      return null;
    }

    @Override
    public void close() throws Exception {
      socket.close();
    }
  }
}