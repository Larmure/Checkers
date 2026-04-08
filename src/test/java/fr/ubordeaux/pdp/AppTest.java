package fr.ubordeaux.pdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import fr.ubordeaux.pdp.model.tools.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;

/**
 * Unit tests for App command line arguments (F1).
 * Uses App.run() to test logic without triggering System.exit().
 */
public class AppTest {

  @BeforeEach
  public void setUp() {
    App.reset();
  }

  @Test
  public void testHelpOption() {
    String[] args = { "-h" };
    // We expect status 1 (clean stop)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Help option should return status 1");
  }

  @Test
  public void testVersionOption() {
    String[] args = { "-V" };
    // We expect status 1 (clean stop)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Version option should return status 1");
  }

  @Test
  public void testVerboseOption() {
    String[] args = { "-v" };
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status, "Verbose option should return status 0");
    assertTrue(App.isVerbose(), "Verbose flag should be true");
  }

  @Test
  public void testDebugOption() {
    String[] args = { "-d" };
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isDebug(), "Debug flag should be true");
  }

  @Test
  public void testInvalidOption() {
    String[] args = { "-z" }; // Non-existent option
    // We expect status 2 (error)
    int status = App.run(args);
    assertEquals(App.EXIT_ERROR, status, "Invalid option should return status 2");
  }

  @Test
  public void testNoOptions() {
    String[] args = {};
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertFalse(App.isVerbose());
    assertFalse(App.isDebug());
  }

  @Test
  public void testGuiOption() {
    String[] args = { "-g" };
    int status = App.run(args);

    assertEquals(App.EXIT_GUI, status);
  }

  @Test
  public void testBlitzAndTimeOptions() {
    String[] args = { "-b", "-t", "5" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    // Assuming you have static getters in App for these fields
    assertTrue(App.isBlitz(), "Blitz flag should be true");
    assertEquals(5, App.getTime(), "Time should be set to 5");
  }

  @Test
  public void testInvalidTime() {
    String[] args = { "-t", "abc" };
    App.run(args);

    assertEquals(Utils.DEFAULT_TIME, App.getTime());
  }

  @Test
  public void testMissingArgument() {
    int status = App.run(new String[] { "-t" });

    assertEquals(App.EXIT_ERROR, status);
  }

  @Test
  public void testInvalidSize() {
    App.run(new String[] { "-s", "abc" });

    assertEquals(Utils.DEFAULT_BOARD_SIZE, App.getSize());
  }

  @Test
  public void testSizeAndContestOptions() {
    String[] args = { "-s", "10", "-c", "true" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(10, App.getSize(), "Size should be 10");
    assertTrue(App.isContest(), "Contest flag should be true");
  }

  @Test
  public void testUnrecognizedArguments() {
    String[] args = { "-v", "hello", "extra" };
    int status = App.run(args);

    assertEquals(App.EXIT_ERROR, status, "Extra arguments should trigger an error status");
  }

  @Test
  public void testPositionalSaveFileArgumentInCliMode() {
    String[] args = { "Sauvegarde/fandu.txt" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals("Sauvegarde/fandu.txt", App.getStartupSaveFile());
  }

  @Test
  public void testPositionalSaveFileArgumentInGuiMode() {
    String[] args = { "-g", "Sauvegarde/fandu.txt" };
    int status = App.run(args);

    assertEquals(App.EXIT_GUI, status);
    assertEquals("Sauvegarde/fandu.txt", App.getStartupSaveFile());
  }

  @Test
  public void testContestOptionWithSaveFileArgument() {
    String[] args = { "-c", "Sauvegarde/fandu.txt" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isContest());
    assertEquals("Sauvegarde/fandu.txt", App.getStartupSaveFile());
  }

  @Test
  public void testLegacyContestAliasWithSaveFileArgument() {
    String[] args = { "-contest", "Sauvegarde/fandu.txt" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isContest());
    assertEquals("Sauvegarde/fandu.txt", App.getStartupSaveFile());
  }

  @Test
  public void testRunExecutionFlow() {
    String[] args = { "-v" };
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isVerbose(), "Verbose flag should remain true after run().");
  }

  @Test
  public void testAiTimeOption() {
    String[] args = { "-at", "2" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(2000, App.getAiTime(), "AI time should be 2000 ms");
  }

  @Test
  public void testInvalidAiTime() {
    App.run(new String[] { "-at", "abc" });

    assertEquals(Ai.DEFAULT_MAX_TIME_MS, App.getAiTime());
  }

  @Test
  public void testAiDepthOption() {
    String[] args = { "-ad", "4" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(4, App.getAiDepth(), "AI depth should be 4");
  }

  @Test
  public void testInvalidAiDepth() {
    App.run(new String[] { "-ad", "abc" });

    assertEquals(Ai.DEFAULT_DEPTH, App.getAiDepth());
  }

  @Test
  public void testAiDepthAutoFromTimeWhenNotProvided() {
    App.run(new String[] { "-at", "2" });

    assertEquals(Ai.suggestDepthFromTime(2000), App.getAiDepth());
  }

  @Test
  public void testAiWhiteFlag() {
    String[] args = { "-a", "W" };
    App.run(args);

    assertTrue(App.isWhiteAi());
    assertFalse(App.isBlackAi());
  }

  @Test
  public void testAiBothFlag() {
    String[] args = { "-a", "A" };
    App.run(args);

    assertTrue(App.isWhiteAi());
    assertTrue(App.isBlackAi());
  }

  @Test
  public void testAiBlack() {
    App.run(new String[] { "-a", "B" });

    assertFalse(App.isWhiteAi());
    assertTrue(App.isBlackAi());
  }

  @Test
  public void testAiInvalidColor() {
    App.run(new String[] { "-a", "X" });

    assertTrue(App.isWhiteAi()); // fallback
  }

  @Test
  public void testAiEmptyOption() {
    App.run(new String[] { "-a", "" });

    assertTrue(App.isWhiteAi());
    assertFalse(App.isBlackAi());
  }

  @Test
  public void testAiMode() {
    App.run(new String[] { "-am", "mcts" });

    assertEquals("mcts", App.aiMode);
  }

  @Test
  public void testInvalidSelectionMode() {
    App.run(new String[] { "-as", "invalid" });

    // fallback valeur par défaut
    assertEquals(Mcts.DEFAULT_SELECTION_MODE, App.getSelectionMode());
  }

  @Test
  public void testValidSelectionMode() {
    int status = App.run(new String[] { "-as", "uct" });
    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(SelectionMode.UCT, App.getSelectionMode());
  }

  @Test
  public void testValidTrainingNumber() {
    int status = App.run(new String[] { "-tr", "10" });
    assertEquals(App.EXIT_TRAINING, status);
  }

  @Test
  public void testMinimaxScoringOption() {
    int status = App.run(new String[] { "--ai-minimax-scoring", "advanced" });
    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals("advanced", App.getMinimaxScoring());
  }

  @Test
  public void testInvalidMinimaxScoringOptionFallsBackToDefault() {
    int status = App.run(new String[] { "--ai-minimax-scoring", "invalid" });
    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(Utils.DEFAULT_MINIMAX_SCORING, App.getMinimaxScoring());
  }

  @Test
  public void testInvalidTrainingNumber() {
    int status = App.run(new String[] { "-tr", "abc" });
    assertEquals(App.EXIT_TRAINING, status);
  }
}