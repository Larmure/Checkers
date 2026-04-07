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
 * Tests for {@link QuitClientCommand}.
 */
class QuitClientCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_whenConnected_sendsQuitAndDisconnectsWithoutExitFlag() {
    FakeClientSession session = new FakeClientSession(true);
    QuitClientCommand command = new QuitClientCommand(session);

    command.execute();

    assertEquals("QUIT", session.lastSent);
    assertTrue(session.disconnectCalled);
    assertFalse(command.shouldExit());
  }

  @Test
  void execute_whenLocal_setsExitFlagAndPrintsGoodbye() {
    FakeClientSession session = new FakeClientSession(false);
    QuitClientCommand command = new QuitClientCommand(session);
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertFalse(session.disconnectCalled);
    assertNull(session.lastSent);
    assertTrue(command.shouldExit());
    assertTrue(output().contains("Exiting client. Goodbye!"));
  }

  @Test
  void getHelp_returnsNonEmptyDescription() {
    QuitClientCommand command = new QuitClientCommand(new FakeClientSession(false));

    String help = command.getHelp();

    assertNotNull(help);
    assertTrue(help.contains("quit"));
    assertTrue(help.contains("Disconnects from the server"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }

  private static final class FakeClientSession extends ClientSession {

    private final boolean connected;
    private String lastSent;
    private boolean disconnectCalled;

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

    @Override
    public void disconnect() {
      this.disconnectCalled = true;
    }
  }
}
