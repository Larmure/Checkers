package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.server.ClientSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PingCommand}.
 */
class PingCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_whenNotConnected_printsGuidanceAndDoesNotSendPing() {
    FakeClientSession session = new FakeClientSession(false);
    PingCommand command = new PingCommand(session);
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertNull(session.lastSent);
    assertTrue(output().contains("Not connected to any server. Use 'join' first."));
  }

  @Test
  void execute_whenConnected_sendsPingAndPrintsRtt() {
    FakeClientSession session = new FakeClientSession(true);
    PingCommand command = new PingCommand(session);
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertEquals("PING", session.lastSent);
    String output = output();
    assertTrue(output.contains("PING sent. RTT (client-side):"));
    assertTrue(output.contains("ms"));
  }

  @Test
  void getHelp_returnsNonEmptyDescription() {
    PingCommand command = new PingCommand(new FakeClientSession(false));

    String help = command.getHelp();

    assertNotNull(help);
    assertFalse(help.isBlank());
    assertTrue(help.contains("ping"));
    assertTrue(help.contains("RTT"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }

  private static final class FakeClientSession extends ClientSession {

    private final boolean connected;
    private String lastSent;

    private FakeClientSession(boolean connected) {
      this.connected = connected;
    }

    @Override
    public boolean isConnected() {
      return connected;
    }

    @Override
    public void send(String message) {
      this.lastSent = message;
    }
  }
}
