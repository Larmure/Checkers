package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.server.GameServer;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ShellCommandRouter}.
 */
public class ShellCommandRouterTest {

  private FakeGameController controller;
  private FakeClientSession session;
  private ShellCommandRouter router;

  private PrintStream originalOut;
  private ByteArrayOutputStream outContent;

  /**
   * Sets up mocks and captures standard output.
   */
  @BeforeEach
  public void setUp() {
    controller = new FakeGameController();
    session = new FakeClientSession();
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
    ServerStartCommand.activeServer = null;
  }

  /**
   * Ensures null input is ignored.
   */
  @Test
  public void testRouteNullInput() {
    router.route(null);

    assertEquals(0, controller.executeCommandCalls);
    assertEquals(0, controller.executeMoveCalls);
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures blank input is ignored.
   */
  @Test
  public void testRouteBlankInput() {
    router.route("   ");

    assertEquals(0, controller.executeCommandCalls);
    assertEquals(0, controller.executeMoveCalls);
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures a move is executed locally in LOCAL mode.
   */
  @Test
  public void testLocalMoveExecutedLocally() {
    session.mode = ClientMode.LOCAL;

    router.route("A1 B2");

    assertEquals(1, controller.executeMoveCalls);
    assertEquals("A1", controller.lastMoveFrom);
    assertEquals("B2", controller.lastMoveTo);
    assertFalse(controller.lastMoveManoury);
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures a Manoury move is executed locally in LOCAL mode.
   */
  @Test
  public void testLocalManouryMoveExecutedLocally() {
    session.mode = ClientMode.LOCAL;

    router.route("12-16");

    assertEquals(1, controller.executeMoveCalls);
    assertEquals("12", controller.lastMoveFrom);
    assertEquals("16", controller.lastMoveTo);
    assertTrue(controller.lastMoveManoury);
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures a move is forwarded in CONNECTED mode.
   */
  @Test
  public void testConnectedMoveForwarded() {
    session.mode = ClientMode.CONNECTED;

    router.route("A1 B2");

    assertEquals(List.of("MOVE A1-B2"), session.sentMessages);
    assertEquals(0, controller.executeMoveCalls);
  }

  /**
   * Ensures a regular command is forwarded in CONNECTED mode.
   */
  @Test
  public void testConnectedCommandForwarded() {
    session.mode = ClientMode.CONNECTED;

    router.route("undo 2");

    assertEquals(List.of("undo 2"), session.sentMessages);
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures blocked commands in SERVER mode print a message.
   */
  @Test
  public void testServerModeBlocksGameCommands() {
    session.mode = ClientMode.SERVER;

    router.route("new -b");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
    assertEquals(0, controller.executeCommandCalls);
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures allowed management commands are forwarded in SERVER mode.
   */
  @Test
  public void testServerManagementCommandForwarded() {
    session.mode = ClientMode.SERVER;

    router.route("status");

    assertEquals(List.of("status"), session.sentMessages);
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server list` is blocked when mode is not LOCAL.
   */
  @Test
  public void testServerListBlockedOutsideLocalMode() {
    session.mode = ClientMode.SERVER;

    router.route("server list");

    String output = outContent.toString();
    assertTrue(output.contains("[blocked]"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server start` is blocked when mode is not LOCAL.
   */
  @Test
  public void testServerStartBlockedOutsideLocalMode() {
    session.mode = ClientMode.SERVER;

    router.route("server start 23456");

    String output = outContent.toString();
    assertTrue(output.contains("[blocked]"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server stop` is blocked when mode is not SERVER.
   */
  @Test
  public void testServerStopBlockedOutsideServerMode() {
    session.mode = ClientMode.LOCAL;

    router.route("server stop");

    String output = outContent.toString();
    assertTrue(output.contains("[blocked]"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures unknown server subcommands print a dedicated help message.
   */
  @Test
  public void testUnknownServerSubcommandPrintsError() {
    session.mode = ClientMode.LOCAL;

    router.route("server banana");

    String output = outContent.toString();
    assertTrue(output.contains("Unknown server command"));
    assertTrue(output.contains("server list | server start [PORT] | server stop"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server` with no subcommand reaches the unknown-subcommand branch.
   */
  @Test
  public void testServerCommandWithoutSubcommandPrintsError() {
    session.mode = ClientMode.LOCAL;

    router.route("server");

    String output = outContent.toString();
    assertTrue(output.contains("Unknown server command"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server list` in LOCAL mode goes through the allowed execution path.
   * The discovery UDP port is occupied to force immediate exit from discoverServers.
   */
  @Test
  public void testServerListInLocalMode_executesCommandPath() throws Exception {
    session.mode = ClientMode.LOCAL;

    try (DatagramSocket ignored = new DatagramSocket(12346)) {
      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> router.route("server list"));
    }

    String output = outContent.toString();
    assertFalse(output.contains("[blocked]"));
    assertTrue(session.sentMessages.isEmpty());
    assertEquals(0, controller.executeCommandCalls);
  }

  /**
   * Ensures `server start` in LOCAL mode reaches start command execution path.
   * We inject a fake already-running server to avoid starting real network services.
   */
  @Test
  public void testServerStartLocalMode_withExplicitPort_usesStartPath() {
    session.mode = ClientMode.LOCAL;
    ServerStartCommand.activeServer = new FakeRunningGameServer(24680);

    assertDoesNotThrow(() -> router.route("server start 23456"));

    String output = outContent.toString();
    assertTrue(output.contains("A server is already running on port 24680"));
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures `server start` in LOCAL mode also handles the no-arg variant.
   */
  @Test
  public void testServerStartLocalMode_withoutPort_usesStartPath() {
    session.mode = ClientMode.LOCAL;
    ServerStartCommand.activeServer = new FakeRunningGameServer(12345);

    assertDoesNotThrow(() -> router.route("server start"));

    String output = outContent.toString();
    assertTrue(output.contains("A server is already running on port 12345"));
    assertTrue(session.sentMessages.isEmpty());
  }

  /**
   * Ensures `server stop` in SERVER mode reaches stop command execution path.
   */
  @Test
  public void testServerStopInServerMode_callsStopCommand() {
    session.mode = ClientMode.SERVER;
    ServerStartCommand.activeServer = null;

    router.route("server stop");

    String output = outContent.toString();
    assertTrue(output.contains("No server is currently running."));
    assertFalse(output.contains("[blocked]"));
    assertTrue(session.sentMessages.isEmpty());
  }
  private static final class FakeRunningGameServer extends GameServer {
    private final int port;

    private FakeRunningGameServer(int port) {
      super(
          "FakeServer",
          port,
          () -> new GameController(new NoopGameView()),
          false,
          false);
      this.port = port;
    }

    @Override
    public boolean isRunning() {
      return true;
    }

    @Override
    public int getPort() {
      return port;
    }
  }

  /**
   * Ensures join is blocked in SERVER mode.
   */
  @Test
  public void testJoinBlockedInServerMode() {
    session.mode = ClientMode.SERVER;

    router.route("join 127.0.0.1:1234");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
  }

  /**
   * Ensures ping is blocked in LOCAL mode.
   */
  @Test
  public void testPingBlockedInLocalMode() {
    session.mode = ClientMode.LOCAL;

    router.route("ping");

    String output = outContent.toString();

    assertTrue(output.contains("[blocked]"));
  }

  private static final class FakeClientSession extends ClientSession {
    private ClientMode mode = ClientMode.LOCAL;
    private final List<String> sentMessages = new ArrayList<>();

    @Override
    public ClientMode getMode() {
      return mode;
    }

    @Override
    public void send(String message) {
      sentMessages.add(message);
    }
  }

  private static final class FakeGameController extends GameController {
    private int executeMoveCalls = 0;
    private String lastMoveFrom;
    private String lastMoveTo;
    private boolean lastMoveManoury;

    private int executeCommandCalls = 0;
    private String lastCommand;
    private String[] lastArgs;

    private FakeGameController() {
      super(new NoopGameView());
    }

    @Override
    public void executeMove(String from, String to, boolean isManoury) {
      executeMoveCalls++;
      lastMoveFrom = from;
      lastMoveTo = to;
      lastMoveManoury = isManoury;
    }

    @Override
    public void executeCommand(String commandName, String[] args) {
      executeCommandCalls++;
      lastCommand = commandName;
      lastArgs = args;
    }
  }

  private static final class NoopGameView extends GameView {
    @Override
    public void start() {
      // No-op for tests.
    }

    @Override
    public void setController(GameController controller) {
      this.controller = controller;
    }

    @Override
    public void display(GameCheckers game) {
      // No-op for tests.
    }

    @Override
    public void update(GameCheckers game) {
      // No-op for tests.
    }

    @Override
    public void showHint(String from, String to) {
      // No-op for tests.
    }
  }
}