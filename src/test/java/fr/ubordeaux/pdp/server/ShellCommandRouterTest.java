package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.ubordeaux.pdp.controller.GameController;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link ShellCommandRouter}.
 */
public class ShellCommandRouterTest {

  private GameController controller;
  private ClientSession session;
  private ShellCommandRouter router;

  private PrintStream originalOut;
  private ByteArrayOutputStream outContent;

  /**
   * Sets up mocks and captures standard output.
   */
  @BeforeEach
  public void setUp() {
    controller = Mockito.mock(GameController.class);
    session = Mockito.mock(ClientSession.class);
    router = new ShellCommandRouter(controller, session);

    originalOut = System.out;
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
  }

  /**
   * Restores standard output.
   */
  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
  }

  /**
   * Ensures null input is ignored.
   */
  @Test
  public void testRouteNullInput() {
    router.route(null);

    verify(controller, never()).executeCommand(anyString(), any(String[].class));
    verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
    verify(session, never()).send(anyString());
  }

  /**
   * Ensures blank input is ignored.
   */
  @Test
  public void testRouteBlankInput() {
    router.route("   ");

    verify(controller, never()).executeCommand(anyString(), any(String[].class));
    verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
    verify(session, never()).send(anyString());
  }

  /**
   * Ensures a move is executed locally in LOCAL mode.
   */
  @Test
  public void testLocalMoveExecutedLocally() {
    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    router.route("A1 B2");

    verify(controller).executeMove("A1", "B2", false);
    verify(session, never()).send(anyString());
  }

  /**
   * Ensures a Manoury move is executed locally in LOCAL mode.
   */
  @Test
  public void testLocalManouryMoveExecutedLocally() {
    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    router.route("12-16");

    verify(controller).executeMove("12", "16", true);
    verify(session, never()).send(anyString());
  }

  /**
   * Ensures a move is forwarded in CONNECTED mode.
   */
  @Test
  public void testConnectedMoveForwarded() {
    when(session.getMode()).thenReturn(ClientMode.CONNECTED);

    router.route("A1 B2");

    verify(session).send("MOVE A1-B2");
    verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
  }


  /**
   * Ensures a regular command is forwarded in CONNECTED mode.
   */
  @Test
  public void testConnectedCommandForwarded() {
    when(session.getMode()).thenReturn(ClientMode.CONNECTED);

    router.route("undo 2");

    verify(session).send("undo 2");
    verify(controller, never()).executeCommand(anyString(), any(String[].class));
  }

  /**
   * Ensures blocked commands in SERVER mode print a message.
   */
  @Test
  public void testServerModeBlocksGameCommands() {
    when(session.getMode()).thenReturn(ClientMode.SERVER);

    router.route("new -b");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
    verify(controller, never()).executeCommand(anyString(), any(String[].class));
    verify(session, never()).send("new -b");
  }

  /**
   * Ensures allowed management commands are forwarded in SERVER mode.
   */
  @Test
  public void testServerManagementCommandForwarded() {
    when(session.getMode()).thenReturn(ClientMode.SERVER);

    router.route("status");

    verify(session).send("status");
    verify(controller, never()).executeCommand(anyString(), any(String[].class));
  }

  /**
   * Ensures join is blocked in SERVER mode.
   */
  @Test
  public void testJoinBlockedInServerMode() {
    when(session.getMode()).thenReturn(ClientMode.SERVER);

    router.route("join 127.0.0.1:1234");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
  }

  /**
   * Ensures ping is blocked in LOCAL mode.
   */
  @Test
  public void testPingBlockedInLocalMode() {
    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    router.route("ping");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
  }
}