package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoadCommand}.
 */
class LoadCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_withMissingFile_printsErrorMessage() {
    GameController controller = new GameController(new HeadlessView());
    LoadCommand command = new LoadCommand(controller, new String[] { "does_not_exist.txt" });

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    String output = output();
    assertTrue(output.contains("Loading error: file not found:"));
  }

  @Test
  void execute_withBlankArguments_printsHelp() {
    GameController controller = new GameController(new HeadlessView());
    LoadCommand command = new LoadCommand(controller, new String[] { "   " });

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    String output = output();
    assertTrue(output.contains("load <file>"));
    assertTrue(output.contains("Loads a saved game from the save directory."));
  }

  @Test
  void execute_withValidSaveFile_loadsGameIntoController() {
    GameController controller = new GameController(new HeadlessView());
    LoadCommand command = new LoadCommand(controller, new String[] { "load_ok.txt" });

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    command.execute();

    assertNotNull(controller.getGame());
    assertTrue(output().contains("Game loaded: load_ok.txt"));
  }

  @Test
  void getHelp_describesUsage() {
    LoadCommand command = new LoadCommand(new GameController(new HeadlessView()), null);

    String help = command.getHelp();

    assertTrue(help.contains("load <file>"));
    assertTrue(help.contains("Example: load mygame.txt"));
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}
