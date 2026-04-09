package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.server.ClientSession;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JoinCommand}.
 */
class JoinCommandTest {

  private final PrintStream originalOut = System.out;
  private final InputStream originalIn = System.in;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setIn(originalIn);
  }

  @Test
  void execute_withNullAddress_usesDefaultHostAndPort() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = false;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new JoinCommand(session, null).execute();

    assertEquals("localhost", session.lastHost);
    assertEquals(12345, session.lastPort);
    assertTrue(session.connectCalled);
  }

  @Test
  void execute_withAddressAndPort_parsesAndConnectsToTarget() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = false;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new JoinCommand(session, "192.168.1.50:4567").execute();

    assertEquals("192.168.1.50", session.lastHost);
    assertEquals(4567, session.lastPort);
    assertTrue(session.connectCalled);
  }

  @Test
  void execute_withInvalidPort_usesDefaultPortAndPrintsWarning() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = false;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new JoinCommand(session, "example.com:notanumber").execute();

    assertEquals("example.com", session.lastHost);
    assertEquals(12345, session.lastPort);
    assertTrue(out().contains("Invalid port in 'example.com:notanumber'. Using default: 12345"));
  }

  @Test
  void execute_whenConnectionFails_doesNotSendRegister() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = false;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new JoinCommand(session, "localhost:12345").execute();

    assertTrue(session.sentMessages.isEmpty());
    assertTrue(!session.disconnectCalled);
  }

  @Test
  void execute_withBlankPlayerId_disconnects() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = true;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    System.setIn(new ByteArrayInputStream("   \n".getBytes(StandardCharsets.UTF_8)));

    new JoinCommand(session, "localhost:12345").execute();

    assertTrue(session.disconnectCalled);
    assertTrue(session.sentMessages.isEmpty());
    assertTrue(out().contains("Player ID cannot be empty. Disconnecting."));
  }

  @Test
  void execute_withValidPlayerId_sendsRegisterHandshake() {
    FakeClientSession session = new FakeClientSession();
    session.connectShouldSucceed = true;

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    System.setIn(new ByteArrayInputStream("alice\n".getBytes(StandardCharsets.UTF_8)));

    new JoinCommand(session, "localhost:12345").execute();

    assertEquals(1, session.sentMessages.size());
    assertEquals("REGISTER alice alice", session.sentMessages.get(0));
    assertTrue(out().contains("Waiting for server response..."));
  }

  @Test
  void getHelp_describesUsage() {
    String help = new JoinCommand(new FakeClientSession(), null).getHelp();

    assertTrue(help.contains("join [IP[:PORT]]"));
    assertTrue(help.contains("localhost:12345"));
  }

  private String out() {
    return outContent.toString(StandardCharsets.UTF_8);
  }

  private static class FakeClientSession extends ClientSession {

    boolean connectShouldSucceed = false;
    boolean connected = false;
    boolean connectCalled = false;
    boolean disconnectCalled = false;
    String lastHost;
    int lastPort;
    java.util.List<String> sentMessages = new java.util.ArrayList<>();

    @Override
    public void connect(String host, int port) {
      this.connectCalled = true;
      this.lastHost = host;
      this.lastPort = port;
      this.connected = connectShouldSucceed;
    }

    @Override
    public boolean isConnected() {
      return connected;
    }

    @Override
    public void send(String message) {
      sentMessages.add(message);
    }

    @Override
    public void disconnect() {
      disconnectCalled = true;
      connected = false;
    }
  }
}
