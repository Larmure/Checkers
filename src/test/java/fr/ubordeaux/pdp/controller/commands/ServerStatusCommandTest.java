package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.server.GameRegistry;
import fr.ubordeaux.pdp.server.PlayerSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ServerStatusCommand}.
 */
class ServerStatusCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_withNullRegistry_printsUnavailableMessage() {
    ServerStatusCommand command = new ServerStatusCommand(12345, null);
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertTrue(output().contains("Server status unavailable."));
  }

  @Test
  void execute_withRegistry_printsPortClientsAndGames() {
    GameRegistry registry = new GameRegistry();
    PlayerSession p1 = registry.registerPlayer("p1", "Alice",
        new PrintWriter(new ByteArrayOutputStream(), true));
    PlayerSession p2 = registry.registerPlayer("p2", "Bob",
        new PrintWriter(new ByteArrayOutputStream(), true));

    registry.createSession(List.of(p1, p2), null);

    ServerStatusCommand command = new ServerStatusCommand(23456, registry);
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    String output = output();
    assertTrue(output.contains("Server status:"));
    assertTrue(output.contains("port=23456"));
    assertTrue(output.contains("clients=2"));
    assertTrue(output.contains("games=1"));
  }

  @Test
  void getHelp_describesCommandUsage() {
    ServerStatusCommand command = new ServerStatusCommand();

    String help = command.getHelp();

    assertTrue(help.contains("server status"));
    assertTrue(help.contains("port"));
    assertTrue(help.contains("connected clients"));
    assertTrue(help.contains("active game sessions"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}
