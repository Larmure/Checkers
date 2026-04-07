package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.view.HeadlessView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NewCommand}.
 */
class NewCommandTest {

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  @Test
  void execute_withExplicitOptions_buildsExpectedConfiguration() {
    GameController controller = new GameController(new HeadlessView());
    String[] args = {
        "-b", "-c", "-t", "60", "-s", "10", "-a", "w",
        "-at", "6", "-ad", "4", "-as", "UCT",
        "-wam", "mcts", "-bam", "alphabeta", "--ai-minimax-scoring", "simple"
    };

    new NewCommand(controller, args).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertNotNull(config);
    assertTrue(config.isBlitz());
    assertTrue(config.isContest());
    assertEquals(60, config.getTime());
    assertEquals(10, config.getSize());
    assertTrue(config.iswhiteAi());
    assertFalse(config.isblackAi());
    assertEquals(6000L, config.getAiTime());
    assertEquals(4, config.getAiDepth());
    assertEquals(SelectionMode.UCT, config.getSelectionMode());
    assertEquals("mcts", config.getWhiteAiMode());
    assertEquals("alphabeta", config.getBlackAiMode());
    assertEquals("simple", config.getMinimaxScoring());
  }

  @Test
  void execute_withInvalidAiTimeAndScoring_usesFallbackValues() {
    GameController controller = new GameController(new HeadlessView());
    String[] args = { "-a", "b", "-at", "0", "--ai-minimax-scoring", "invalid" };

    new NewCommand(controller, args).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertNotNull(config);
    assertFalse(config.iswhiteAi());
    assertTrue(config.isblackAi());
    assertEquals(Ai.DEFAULT_MAX_TIME_MS, config.getAiTime());
    assertEquals(Utils.DEFAULT_MINIMAX_SCORING, config.getMinimaxScoring());
  }

  @Test
  void execute_withInvalidSelection_keepsDefaultAndPrintsWarning() {
    GameController controller = new GameController(new HeadlessView());
    String[] args = { "-as", "bad" };
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new NewCommand(controller, args).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertEquals(Mcts.DEFAULT_SELECTION_MODE, config.getSelectionMode());
    assertTrue(output().contains("bad".toUpperCase()));
  }

  @Test
  void execute_withMlSelection_setsMlMode() {
    GameController controller = new GameController(new HeadlessView());

    new NewCommand(controller, new String[] { "-as", "ml" }).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertEquals(SelectionMode.ML, config.getSelectionMode());
  }

  @Test
  void execute_withLongDepthOption_setsExplicitDepth() {
    GameController controller = new GameController(new HeadlessView());

    new NewCommand(controller, new String[] { "--ai-minimax-depth", "6" }).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertEquals(6, config.getAiDepth());
  }

  @Test
  void execute_withAiAll_enablesBothAiPlayers() {
    GameController controller = new GameController(new HeadlessView());

    new NewCommand(controller, new String[] { "-a", "a" }).execute();

    Configuration config = controller.getGame().getConfiguration();
    assertTrue(config.iswhiteAi());
    assertTrue(config.isblackAi());
  }

  @Test
  void execute_withInvalidArguments_printsInvalidMessageAndDoesNotStartGame() {
    GameController controller = new GameController(new HeadlessView());
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new NewCommand(controller, new String[] { "-z" }).execute();

    assertTrue(output().contains(Internationalization.get("new.invalid")));
    assertNull(controller.getGame());
  }

  @Test
  void execute_withInvalidNumberFormat_printsInvalidMessageAndDoesNotStartGame() {
    GameController controller = new GameController(new HeadlessView());
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

    new NewCommand(controller, new String[] { "-t", "not_a_number" }).execute();

    assertTrue(output().contains(Internationalization.get("new.invalid")));
    assertNull(controller.getGame());
  }

  @Test
  void getHelp_returnsNonEmptyMessage() {
    String help = new NewCommand(new GameController(new HeadlessView()), null).getHelp();

    assertNotNull(help);
    assertFalse(help.isBlank());
  }

  private String output() {
    return outContent.toString(StandardCharsets.UTF_8);
  }
}