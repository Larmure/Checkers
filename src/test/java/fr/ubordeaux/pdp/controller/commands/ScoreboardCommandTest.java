package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.server.GameRegistry;
import fr.ubordeaux.pdp.server.PlayerSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ScoreboardCommand}.
 */
class ScoreboardCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_withNoPlayedGames_printsPlaceholderMessage() {
    GameRegistry registry = new GameRegistry();
    registry.registerPlayer("p1", "Alice", new PrintWriter(new ByteArrayOutputStream(), true));
    registry.registerPlayer("p2", "Bob", new PrintWriter(new ByteArrayOutputStream(), true));

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new ScoreboardCommand(registry).execute();

    assertTrue(output().contains("No games have been played yet."));
  }

  @Test
  void execute_sortsPlayersByWinsDescending_andSkipsInactivePlayers() {
    GameRegistry registry = new GameRegistry();
    PlayerSession alice = registry.registerPlayer("p1", "Alice", new PrintWriter(new ByteArrayOutputStream(), true));
    PlayerSession bob = registry.registerPlayer("p2", "Bob", new PrintWriter(new ByteArrayOutputStream(), true));
    PlayerSession carol = registry.registerPlayer("p3", "Carol", new PrintWriter(new ByteArrayOutputStream(), true));

    alice.recordWin();
    alice.recordWin();
    bob.recordWin();
    bob.recordLoss();
    carol.recordDraw();

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new ScoreboardCommand(registry).execute();

    String output = output();
    assertTrue(output.contains("RANK"));
    assertTrue(output.contains("p1"));
    assertTrue(output.contains("p2"));
    assertTrue(output.contains("p3"));
    assertTrue(output.indexOf("p1") < output.indexOf("p2"));
    assertTrue(output.indexOf("p2") < output.indexOf("p3"));
  }

  @Test
  void execute_ignoresPlayersWithoutPlayedGames() {
    GameRegistry registry = new GameRegistry();
    PlayerSession alice = registry.registerPlayer("p1", "Alice", new PrintWriter(new ByteArrayOutputStream(), true));
    registry.registerPlayer("p2", "Bob", new PrintWriter(new ByteArrayOutputStream(), true));

    alice.recordWin();
    alice.recordLoss();
    // Bob has never played a game and should not appear in the output.

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new ScoreboardCommand(registry).execute();

    String output = output();
    assertTrue(output.contains("p1"));
    assertTrue(!output.contains("p2"));
  }

  @Test
  void getHelp_describesTheCommand() {
    ScoreboardCommand command = new ScoreboardCommand(new GameRegistry());

    String help = command.getHelp();

    assertTrue(help.contains("scoreboard"));
    assertTrue(help.contains("victories"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}
