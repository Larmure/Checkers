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
 * Tests for {@link PlayersCommand}.
 */
class PlayersCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_whenNoPlayersConnected_printsPlaceholder() {
    GameRegistry registry = new GameRegistry();
    PlayersCommand command = new PlayersCommand(registry);

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertTrue(output().contains("No players currently connected."));
  }

  @Test
  void execute_whenPlayersConnected_printsHeaderAndRows() {
    GameRegistry registry = new GameRegistry();
    PlayerSession alice = registry.registerPlayer("p1", "Alice", new PrintWriter(new ByteArrayOutputStream(), true));
    PlayerSession bob = registry.registerPlayer("p2", "Bob", new PrintWriter(new ByteArrayOutputStream(), true));

    bob.setStatus(PlayerSession.Status.INGAME);

    PlayersCommand command = new PlayersCommand(registry);

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    String out = output();
    assertTrue(out.contains("ID"));
    assertTrue(out.contains("NAME"));
    assertTrue(out.contains("STATUS"));
    assertTrue(out.contains("p1"));
    assertTrue(out.contains("Alice"));
    assertTrue(out.contains("idle"));
    assertTrue(out.contains("p2"));
    assertTrue(out.contains("Bob"));
    assertTrue(out.contains("ingame"));
    assertTrue(alice.getStatus() == PlayerSession.Status.IDLE);
  }

  @Test
  void getHelp_describesCommandUsage() {
    PlayersCommand command = new PlayersCommand(new GameRegistry());

    String help = command.getHelp();

    assertTrue(help.contains("players"));
    assertTrue(help.contains("idle"));
    assertTrue(help.contains("ingame"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}
