package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PlayerSession}.
 */
class PlayerSessionTest {

  @Test
  void constructor_initializesFieldsCorrectly() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);

    assertEquals("p1", session.getId());
    assertEquals("Daniel", session.getName());
    assertEquals(PlayerSession.Status.IDLE, session.getStatus());
    assertTrue(session.isIdle());
    assertEquals(0, session.getWins());
    assertEquals(0, session.getLosses());
    assertEquals(0, session.getDraws());
    assertEquals(0, session.getGamesPlayed());
  }

  @Test
  void send_writesMessageToOutput() {
    StringWriter buffer = new StringWriter();
    PrintWriter out = new PrintWriter(buffer, true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.send("HELLO");

    assertTrue(buffer.toString().contains("HELLO"));
  }

  @Test
  void setStatus_changesStatusAndIdleFlag() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.setStatus(PlayerSession.Status.INGAME);

    assertEquals(PlayerSession.Status.INGAME, session.getStatus());
    assertTrue(!session.isIdle());
  }

  @Test
  void recordWin_incrementsWinsAndGamesPlayed() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.recordWin();

    assertEquals(1, session.getWins());
    assertEquals(0, session.getLosses());
    assertEquals(0, session.getDraws());
    assertEquals(1, session.getGamesPlayed());
  }

  @Test
  void recordLoss_incrementsLossesAndGamesPlayed() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.recordLoss();

    assertEquals(0, session.getWins());
    assertEquals(1, session.getLosses());
    assertEquals(0, session.getDraws());
    assertEquals(1, session.getGamesPlayed());
  }

  @Test
  void recordDraw_incrementsDrawsAndGamesPlayed() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.recordDraw();

    assertEquals(0, session.getWins());
    assertEquals(0, session.getLosses());
    assertEquals(1, session.getDraws());
    assertEquals(1, session.getGamesPlayed());
  }

  @Test
  void multipleResults_accumulateCorrectly() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.recordWin();
    session.recordLoss();
    session.recordDraw();
    session.recordWin();

    assertEquals(2, session.getWins());
    assertEquals(1, session.getLosses());
    assertEquals(1, session.getDraws());
    assertEquals(4, session.getGamesPlayed());
  }

  @Test
  void toString_containsMainSessionInformationWhenIdle() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    String text = session.toString();

    assertTrue(text.contains("p1"));
    assertTrue(text.contains("Daniel"));
    assertTrue(text.contains("idle"));
    assertTrue(text.contains("W:0"));
    assertTrue(text.contains("L:0"));
    assertTrue(text.contains("D:0"));
  }

  @Test
  void toString_containsUpdatedStatisticsAndStatus() {
    PrintWriter out = new PrintWriter(new StringWriter(), true);

    PlayerSession session = new PlayerSession("p1", "Daniel", out);
    session.setStatus(PlayerSession.Status.INGAME);
    session.recordWin();
    session.recordDraw();

    String text = session.toString();

    assertTrue(text.contains("p1"));
    assertTrue(text.contains("Daniel"));
    assertTrue(text.contains("ingame"));
    assertTrue(text.contains("W:1"));
    assertTrue(text.contains("L:0"));
    assertTrue(text.contains("D:1"));
  }
}