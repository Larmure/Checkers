package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.GameServer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ServerStartCommand}.
 */
class ServerStartCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    stopActiveServer();
  }

  @Test
  void execute_startsServerOnProvidedPort_andEntersServerMode() throws Exception {
    int port = findFreePort();
    ClientSession session = new ClientSession();
    ServerStartCommand command = new ServerStartCommand(null, new String[] { String.valueOf(port) }, session);

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    waitForServerToStart();

    GameServer server = ServerStartCommand.activeServer;
    assertNotNull(server);
    assertEquals(port, server.getPort());
    assertTrue(server.isRunning());
    assertEquals(ClientMode.SERVER, session.getMode());
    assertTrue(output().contains("Server started on port " + port + "."));
  }

  @Test
  void execute_withInvalidPort_usesDefaultPortAndPrintsWarning() throws Exception {
    ClientSession session = new ClientSession();
    ServerStartCommand command = new ServerStartCommand(null, new String[] { "abc" }, session);

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    waitForServerToStart();

    GameServer server = ServerStartCommand.activeServer;
    assertNotNull(server);
    assertEquals(12345, server.getPort());
    assertEquals(ClientMode.SERVER, session.getMode());
    assertTrue(output().contains("Invalid port 'abc'. Using default: 12345"));

    stopActiveServer();
  }

  @Test
  void execute_whenServerAlreadyRunning_printsMessageAndDoesNothing() throws Exception {
    int port = findFreePort();
    GameServer runningServer = new GameServer("GameServer", port,
        () -> new fr.ubordeaux.pdp.controller.GameController(new fr.ubordeaux.pdp.view.HeadlessView()));
    setRunningFlag(runningServer, true);
    ServerStartCommand.activeServer = runningServer;

    ClientSession session = new ClientSession();
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new ServerStartCommand(null, new String[] { "23456" }, session).execute();

    assertTrue(output().contains("A server is already running on port " + port + "."));
    assertEquals(ClientMode.LOCAL, session.getMode());
    assertTrue(ServerStartCommand.activeServer == runningServer);
  }

  @Test
  void getHelp_describesUsage() {
    ServerStartCommand command = new ServerStartCommand(null, null, new ClientSession());

    String help = command.getHelp();

    assertTrue(help.contains("server start [PORT]"));
    assertTrue(help.contains("default: 12345"));
  }

  private static int findFreePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static void waitForServerToStart() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 2000;
    while (System.currentTimeMillis() < deadline) {
      if (ServerStartCommand.activeServer != null && ServerStartCommand.activeServer.isRunning()) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static void stopActiveServer() {
    if (ServerStartCommand.activeServer != null) {
      ServerStartCommand.activeServer.stop();
      ServerStartCommand.activeServer = null;
    }
  }

  private static void setRunningFlag(GameServer server, boolean running) throws Exception {
    var field = GameServer.class.getDeclaredField("running");
    field.setAccessible(true);
    field.set(server, running);
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}
