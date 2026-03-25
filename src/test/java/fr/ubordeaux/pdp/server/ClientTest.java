package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.ubordeaux.pdp.controller.GameController;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link Client}.
 *
 * <p>These tests focus on the unified shell routing logic:
 *
 * <ul>
 *   <li>LOCAL mode → game commands go to GameController
 *   <li>CONNECTED mode → commands are forwarded to the remote server
 *   <li>SERVER mode → game commands are blocked
 *   <li>mode guard helpers ({@code blockIf}, {@code blockUnless})
 *   <li>prompt text according to current mode
 * </ul>
 *
 * <p>Important: these tests assume {@link Client} was slightly refactored for testability:
 *
 * <ul>
 *   <li>a package-private constructor exists:
 *       {@code Client(ClientSession session, GameController controller)}
 *   <li>the following methods are package-private instead of private:
 *       {@code dispatchDefault}, {@code blockIf}, {@code blockUnless}, {@code prompt}
 * </ul>
 */
class ClientTest {

  @Test
  void dispatchDefault_localMode_callsControllerWithRemainingArgs() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    Client client = new Client(session, controller);

    client.dispatchDefault("move", new String[] {"move", "a3", "b4"});

    verify(controller).executeCommand("move", new String[] {"a3", "b4"});
  }

  @Test
  void dispatchDefault_localMode_withNoExtraArgs_callsControllerWithEmptyArray() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    Client client = new Client(session, controller);

    client.dispatchDefault("help", new String[] {"help"});

    verify(controller).executeCommand("help", new String[0]);
  }

  @Test
  void dispatchDefault_connectedMode_forwardsWholeCommandLineToServer() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.CONNECTED);

    Client client = new Client(session, controller);

    client.dispatchDefault("move", new String[] {"move", "a3", "b4"});

    verify(session).send("move a3 b4");
  }

  @Test
  void dispatchDefault_serverMode_printsBlockedMessage() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.SERVER);

    Client client = new Client(session, controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      client.dispatchDefault("move", new String[] {"move", "a3", "b4"});
    } finally {
      System.setOut(originalOut);
    }

    String printed = out.toString();
    assertTrue(printed.contains("[blocked] Game commands are unavailable in SERVER mode."));
    assertTrue(printed.contains("Use 'server stop' to return to local mode."));
  }

  @Test
  void blockIf_returnsTrue_andPrintsMessage_whenModeMatchesBlocked() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.SERVER);

    Client client = new Client(session, controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    boolean result;
    try {
      result = client.blockIf(ClientMode.SERVER, "Cannot do this in server mode.");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(result);
    assertTrue(out.toString().contains("[blocked] Cannot do this in server mode."));
  }

  @Test
  void blockIf_returnsFalse_andPrintsNothing_whenModeDoesNotMatchBlocked() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    Client client = new Client(session, controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    boolean result;
    try {
      result = client.blockIf(ClientMode.SERVER, "Cannot do this in server mode.");
    } finally {
      System.setOut(originalOut);
    }

    assertFalse(result);
    assertEquals("", out.toString());
  }

  @Test
  void blockUnless_returnsFalse_andPrintsNothing_whenModeMatchesRequired() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    Client client = new Client(session, controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    boolean result;
    try {
      result = client.blockUnless(ClientMode.LOCAL, "Only local mode is allowed.");
    } finally {
      System.setOut(originalOut);
    }

    assertFalse(result);
    assertEquals("", out.toString());
  }

  @Test
  void blockUnless_returnsTrue_andPrintsMessage_whenModeDoesNotMatchRequired() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.CONNECTED);

    Client client = new Client(session, controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    boolean result;
    try {
      result = client.blockUnless(ClientMode.LOCAL, "Only local mode is allowed.");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(result);
    assertTrue(out.toString().contains("[blocked] Only local mode is allowed."));
  }

  @Test
  void prompt_returnsLocalPrompt_inLocalMode() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    Client client = new Client(session, controller);

    assertEquals("[local] > ", client.prompt());
  }

  @Test
  void prompt_returnsConnectedPrompt_inConnectedMode() {
    ClientSession session = Mockito.mock(ClientSession.class);
    GameController controller = Mockito.mock(GameController.class);

    when(session.getMode()).thenReturn(ClientMode.CONNECTED);
    when(session.getCurrentServer()).thenReturn("192.168.1.10:12345");

    Client client = new Client(session, controller);

    assertEquals("[192.168.1.10:12345] > ", client.prompt());
  }
}