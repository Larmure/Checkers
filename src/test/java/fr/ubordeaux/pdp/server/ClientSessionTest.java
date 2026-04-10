package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * Tests for {@link ClientSession}.
 */
class ClientSessionTest {

  @Test
  void defaults_areCorrect() {
    ClientSession session = new ClientSession();

    assertEquals(ClientMode.LOCAL, session.getMode());
    assertFalse(session.isConnected());
    assertEquals("localhost", session.getDefaultHost());
    assertEquals(12345, session.getDefaultPort());
    assertNull(session.getCurrentServer());
    assertFalse(session.isGuiMode());
    assertFalse(session.isServerRequiresGui());
  }

  @Test
  void enterServerMode_switchesModeToServer() {
    ClientSession session = new ClientSession();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      session.enterServerMode();
    } finally {
      System.setOut(originalOut);
    }

    assertEquals(ClientMode.SERVER, session.getMode());
    assertTrue(out.toString().contains("[mode] Now in SERVER mode."));
  }

  @Test
  void setGuiMode_andIsGuiMode_trackFlag() {
    ClientSession session = new ClientSession();

    session.setGuiMode(true);

    assertTrue(session.isGuiMode());
  }

  @Test
  void connect_successfullyOpensSocketAndStartsListener() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      ClientSession session = new ClientSession();
      GameController controller = Mockito.mock(GameController.class);
      session.setController(controller);

      CountDownLatch releaseConnection = new CountDownLatch(1);
      Thread acceptThread = new Thread(
          () -> acceptRegisterAndReply(serverSocket, "WELCOME p1 mode=ANY", releaseConnection));
      acceptThread.start();

      ByteArrayOutputStream printed = new ByteArrayOutputStream();
      PrintStream originalOut = System.out;
      System.setOut(new PrintStream(printed));

      try {
        session.connect("127.0.0.1", serverSocket.getLocalPort());
        waitForConnection(session);
        session.send("REGISTER p1 Alice");
        waitForOutput(printed, "Connected to 127.0.0.1:" + serverSocket.getLocalPort());

        assertTrue(session.isConnected());
        assertEquals("127.0.0.1:" + serverSocket.getLocalPort(), session.getCurrentServer());
        assertEquals(ClientMode.CONNECTED, session.getMode());
      } finally {
        releaseConnection.countDown();
        acceptThread.join(1_000);
        System.setOut(originalOut);
        session.disconnect();
      }
    }
  }

  @Test
  void connect_whenAlreadyConnected_printsWarningAndKeepsState() throws Exception {
    ClientSession session = new ClientSession();
    setField(session, "connected", true);
    setField(session, "currentServer", "localhost:12345");

    ByteArrayOutputStream printed = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(printed));

    try {
      session.connect("127.0.0.1", 1);
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(printed.toString().contains("Already connected to localhost:12345"));
    assertTrue(session.isConnected());
    assertEquals("localhost:12345", session.getCurrentServer());
  }

  @Test
  void connect_whenConnectionFails_printsFailureAndLeavesDisconnected() throws Exception {
    ClientSession session = new ClientSession();

    ByteArrayOutputStream printed = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(printed));

    try {
      session.connect("127.0.0.1", 65_000);
    } finally {
      System.setOut(originalOut);
    }

    assertFalse(session.isConnected());
    assertNull(session.getCurrentServer());
    assertTrue(printed.toString().contains("Connection failed:"));
  }

  @Test
  void exitServerMode_switchesModeBackToLocal() {
    ClientSession session = new ClientSession();
    session.enterServerMode();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      session.exitServerMode();
    } finally {
      System.setOut(originalOut);
    }

    assertEquals(ClientMode.LOCAL, session.getMode());
    assertTrue(out.toString().contains("[mode] Server stopped. Back to LOCAL mode."));
  }

  @Test
  void disconnect_resetsStateAndClosesResources() throws Exception {
    ClientSession session = new ClientSession();

    BufferedReader in = Mockito.mock(BufferedReader.class);
    PrintWriter out = Mockito.mock(PrintWriter.class);
    Socket socket = Mockito.mock(Socket.class);

    setField(session, "in", in);
    setField(session, "out", out);
    setField(session, "socket", socket);
    setField(session, "connected", true);
    setField(session, "currentServer", "localhost:12345");
    setField(session, "mode", ClientMode.CONNECTED);

    ByteArrayOutputStream printed = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(printed));

    try {
      session.disconnect();
    } finally {
      System.setOut(originalOut);
    }

    verify(in).close();
    verify(out).close();
    verify(socket).isClosed();
    verify(socket).close();

    assertFalse(session.isConnected());
    assertEquals(null, session.getCurrentServer());
    assertEquals(ClientMode.LOCAL, session.getMode());
    assertTrue(printed.toString().contains("Disconnected from server."));
  }

  @Test
  void send_writesMessageWhenWriterExists() throws Exception {
    ClientSession session = new ClientSession();

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter, true);
    setField(session, "out", writer);
    setField(session, "connected", true);

    session.send("PING");

    assertTrue(stringWriter.toString().contains("PING"));
  }

  @Test
  void send_doesNothingWhenDisconnected() throws Exception {
    ClientSession session = new ClientSession();

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter, true);
    setField(session, "out", writer);
    setField(session, "connected", false);

    session.send("PING");

    assertTrue(stringWriter.toString().isEmpty());
  }

  @Test
  void send_doesNothingWhenWriterIsNull() {
    ClientSession session = new ClientSession();

    session.send("PING");

    assertTrue(true);
  }

  @Test
  void handleServerMessage_gameStart_stopsTimerAndStartsNewGame() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "GAME_START You are white");
    } finally {
      System.setOut(originalOut);
    }

    verify(controller).stopBlitzTimer();
    verify(controller).startNewGame(any());
    assertTrue(out.toString().contains("Game started! GAME_START You are white"));
  }

  @Test
  void handleServerMessage_gameStart_inGuiModeOpensGuiWindow() throws Exception {
    ClientSession session = new ClientSession();
    session.setGuiMode(true);
    GameController controller = Mockito.mock(GameController.class);
    session.setController(controller);

    try (
        MockedConstruction<GraphicalUserInterface> mockedGui = Mockito.mockConstruction(GraphicalUserInterface.class)) {
      invokeHandleServerMessage(session, "GAME_START session=1 mode=GUI");

      assertEquals(1, mockedGui.constructed().size());
      GraphicalUserInterface gui = mockedGui.constructed().get(0);
      Mockito.verify(gui).setController(controller);
      Mockito.verify(gui).start();
      verify(controller).stopBlitzTimer();
      verify(controller).startNewGame(any());
    }
  }

  @Test
  void handleServerMessage_gameStart_guiRequiredOpensWarningWhenCliOnly() throws Exception {
    ClientSession session = new ClientSession();
    session.setGuiMode(false);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "WELCOME p1 mode=GUI");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(session.isServerRequiresGui());
    assertTrue(out.toString().contains("GUI-only mode"));
  }

  @Test
  void handleServerMessage_byeDisconnectsClient() throws Exception {
    ClientSession session = new ClientSession();
    setField(session, "connected", true);
    setField(session, "currentServer", "localhost:12345");
    setField(session, "mode", ClientMode.CONNECTED);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "BYE");
    } finally {
      System.setOut(originalOut);
    }

    assertFalse(session.isConnected());
    assertNull(session.getCurrentServer());
    assertEquals(ClientMode.LOCAL, session.getMode());
    assertTrue(out.toString().contains("Server: BYE"));
  }

  @Test
  void printRemotePromptOnlyWhenConnected() throws Exception {
    ClientSession connected = new ClientSession();
    setField(connected, "connected", true);
    setField(connected, "currentServer", "localhost:12345");

    ClientSession disconnected = new ClientSession();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokePrintRemotePromptIfConnected(connected);
      invokePrintRemotePromptIfConnected(disconnected);
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString().contains("[localhost:12345] > "));
  }

  @Test
  void handleServerMessage_moveOk_appliesLocalMove() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "MOVE_OK A3-B4");
    } finally {
      System.setOut(originalOut);
    }

    verify(controller).executeMove("A3", "B4", false);
    assertTrue(out.toString().contains("You played: A3-B4"));
  }

  @Test
  void handleServerMessage_opponentMove_appliesOpponentMove() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "OPPONENT_MOVE C5-D4");
    } finally {
      System.setOut(originalOut);
    }

    verify(controller).executeMove("C5", "D4", false);
    assertTrue(out.toString().contains("Opponent played: C5-D4"));
  }

  @Test
  void handleServerMessage_genericMessage_printsServerPrefix() throws Exception {
    ClientSession session = new ClientSession();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "WELCOME");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString().contains("Server: WELCOME"));
  }

  @Test
  void handleServerMessage_moveOk_invalidFormat_printsRawServerMessage() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "MOVE_OK invalidmove");
    } finally {
      System.setOut(originalOut);
    }

    verifyNoInteractions(controller);
    assertTrue(out.toString().contains("Server: MOVE_OK invalidmove"));
  }

  @Test
  void handleServerMessage_moveOk_whenControllerThrows_printsWarning() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    doThrow(new RuntimeException("boom")).when(controller).executeMove(eq("A3"), eq("B4"), eq(false));
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "MOVE_OK A3-B4");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString().contains("[warning] Could not apply local move: boom"));
  }

  @Test
  void handleServerMessage_plainMessagePrintsAsServerLine() throws Exception {
    ClientSession session = new ClientSession();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "STATUS_CHANGED away");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString().contains("[status] Your status is now: away"));
  }

  @Test
  void handleServerMessage_opponentMove_whenControllerThrows_printsWarning() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    doThrow(new RuntimeException("boom")).when(controller).executeMove(Mockito.anyString(), Mockito.anyString(),
        eq(false));
    session.setController(controller);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleServerMessage(session, "OPPONENT_MOVE C5-D4");
    } finally {
      System.setOut(originalOut);
    }

    assertTrue(out.toString().contains("[warning] Could not apply local move: boom"));
  }

  @Test
  void disconnect_ignoresIOExceptionWhileClosing() throws Exception {
    ClientSession session = new ClientSession();

    BufferedReader in = Mockito.mock(BufferedReader.class);
    doThrow(new java.io.IOException("close failed")).when(in).close();

    setField(session, "in", in);
    setField(session, "out", Mockito.mock(PrintWriter.class));
    setField(session, "socket", Mockito.mock(Socket.class));
    setField(session, "connected", true);
    setField(session, "mode", ClientMode.CONNECTED);
    setField(session, "currentServer", "localhost:12345");

    session.disconnect();

    assertFalse(session.isConnected());
    assertEquals(ClientMode.LOCAL, session.getMode());
    assertNull(session.getCurrentServer());
  }

  @Test
  void handleUnexpectedServerStop_whenAlreadyDisconnectedDoesNothing() throws Exception {
    ClientSession session = new ClientSession();

    invokeHandleUnexpectedServerStop(session);

    assertFalse(session.isConnected());
    assertEquals(ClientMode.LOCAL, session.getMode());
  }

  @Test
  void handleUnexpectedServerStop_whenConnectedResetsState() throws Exception {
    ClientSession session = new ClientSession();
    setField(session, "connected", true);
    setField(session, "currentServer", "localhost:12345");
    setField(session, "serverRequiresGui", true);
    setField(session, "guiWindowOpened", true);
    setField(session, "mode", ClientMode.CONNECTED);
    Socket socket = Mockito.mock(Socket.class);
    Mockito.when(socket.isClosed()).thenReturn(false);
    setField(session, "socket", socket);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    System.setOut(new PrintStream(out));

    try {
      invokeHandleUnexpectedServerStop(session);
    } finally {
      System.setOut(originalOut);
    }

    assertFalse(session.isConnected());
    assertEquals(ClientMode.LOCAL, session.getMode());
    assertNull(session.getCurrentServer());
    assertFalse(session.isServerRequiresGui());
  }

  @Test
  void closeQuietly_closesPartialResourcesAndIgnoresFailures() throws Exception {
    ClientSession session = new ClientSession();
    BufferedReader reader = Mockito.mock(BufferedReader.class);
    Mockito.doThrow(new IOException("reader close failed")).when(reader).close();
    PrintWriter writer = Mockito.mock(PrintWriter.class);
    Socket socket = Mockito.mock(Socket.class);
    Mockito.when(socket.isClosed()).thenReturn(false);
    Mockito.doThrow(new IOException("socket close failed")).when(socket).close();

    invokeCloseQuietly(session, reader, writer, socket);

    verify(reader).close();
    verify(writer).close();
    verify(socket).close();
  }

  private static void invokeHandleServerMessage(ClientSession session, String message)
      throws Exception {
    Method method = ClientSession.class.getDeclaredMethod("handleServerMessage", String.class);
    method.setAccessible(true);
    method.invoke(session, message);
  }

  private static void invokeHandleUnexpectedServerStop(ClientSession session)
      throws Exception {
    Method method = ClientSession.class.getDeclaredMethod("handleUnexpectedServerStop");
    method.setAccessible(true);
    method.invoke(session);
  }

  private static void invokePrintRemotePromptIfConnected(ClientSession session)
      throws Exception {
    Method method = ClientSession.class.getDeclaredMethod("printRemotePromptIfConnected");
    method.setAccessible(true);
    method.invoke(session);
  }

  private static void invokeCloseQuietly(
      ClientSession session,
      BufferedReader reader,
      PrintWriter writer,
      Socket socket)
      throws Exception {
    Method method = ClientSession.class.getDeclaredMethod(
        "closeQuietly",
        BufferedReader.class,
        PrintWriter.class,
        Socket.class);
    method.setAccessible(true);
    method.invoke(session, reader, writer, socket);
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void waitForConnection(ClientSession session) throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      if (session.isConnected()) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static void waitForOutput(ByteArrayOutputStream out, String expected)
      throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      if (out.toString(StandardCharsets.UTF_8).contains(expected)) {
        return;
      }
      Thread.sleep(20);
    }
  }

  private static void acceptRegisterAndReply(
      ServerSocket serverSocket,
      String reply,
      CountDownLatch releaseConnection) {
    try (Socket socket = serverSocket.accept();
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
             true)) {
      reader.readLine();
      writer.println(reply);
      releaseConnection.await();
    } catch (IOException | InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}

