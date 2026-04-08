package fr.ubordeaux.pdp.server;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fr.ubordeaux.pdp.controller.GameController;

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
    assertEquals(null, session.getCurrentServer());
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

    verify(controller).executeMove("A3", "B4",false);
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

    verify(controller).executeMove("C5", "D4",false);
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
  void handleServerMessage_opponentMove_whenControllerThrows_printsWarning() throws Exception {
    ClientSession session = new ClientSession();
    GameController controller = Mockito.mock(GameController.class);
    doThrow(new RuntimeException("boom")).when(controller).executeMove(Mockito.anyString(), Mockito.anyString(), eq(false));
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
    assertEquals(null, session.getCurrentServer());
  }

  private static void invokeHandleServerMessage(ClientSession session, String message)
      throws Exception {
    Method method = ClientSession.class.getDeclaredMethod("handleServerMessage", String.class);
    method.setAccessible(true);
    method.invoke(session, message);
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}