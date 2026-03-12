package fr.ubordeaux.pdp.model.player;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.ai.MinMax;
import fr.ubordeaux.pdp.model.player.ai.MinMaxAlphaBeta;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

class AIPlayerTest {

  private MinMaxAlphaBeta testAlgorithm;
  private SimpleEvaluator testEvaluator;
  private Board testBoard;
  private ManagerUndoRedo testUndo;

  @BeforeEach
  void setUp() {
    testAlgorithm = new MinMaxAlphaBeta(2);
    testEvaluator = new SimpleEvaluator();
    testBoard = new Board(8);
    testUndo = new ManagerUndoRedo(testBoard);
  }

  @Test
  @DisplayName("Constructor with valid parameters should create AIPlayer")
  void testConstructorWithValidParameters() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    assertEquals("Test AI", aiPlayer.getName());
    assertEquals(testAlgorithm, aiPlayer.getAlgorithm());
    assertEquals(testEvaluator, aiPlayer.getEvaluator());
  }

  @Test
  @DisplayName("Constructor with null algorithm should throw IllegalArgumentException")
  void testConstructorWithNullAlgorithm() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new AIPlayer("Test AI", null, testEvaluator));
    assertEquals("AI algorithm cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Constructor with null evaluator should throw IllegalArgumentException")
  void testConstructorWithNullEvaluator() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new AIPlayer("Test AI", testAlgorithm, null));
    assertEquals("Evaluator cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Constructor with default parameters should create AIPlayer with defaults")
  void testConstructorWithDefaults() {
    AIPlayer aiPlayer = new AIPlayer("Default AI");

    assertEquals("Default AI", aiPlayer.getName());
    assertNotNull(aiPlayer.getAlgorithm());
    assertTrue(aiPlayer.getAlgorithm() instanceof MinMaxAlphaBeta);
    assertNotNull(aiPlayer.getEvaluator());
    assertTrue(aiPlayer.getEvaluator() instanceof SimpleEvaluator);
  }

  @Test
  @DisplayName("getBestMove with valid parameters should return a move")
  void testGetBestMoveWithValidParameters() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    Move result = aiPlayer.getBestMove(testUndo, testBoard, PlayerColor.WHITE);

    assertNotNull(result);
    // Vérifications basiques du mouvement
    assertTrue(result.getPath().size() >= 2);
    assertTrue(result.getPath().get(0) >= 0);
    assertTrue(result.getPath().get(1) < testBoard.getIndexMax());
  }

  @Test
  @DisplayName("getBestMove with null undo should throw IllegalArgumentException")
  void testGetBestMoveWithNullUndo() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> aiPlayer.getBestMove(null, testBoard, PlayerColor.WHITE));
    assertEquals("Undo manager cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("getBestMove with null board should throw IllegalArgumentException")
  void testGetBestMoveWithNullBoard() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> aiPlayer.getBestMove(testUndo, null, PlayerColor.WHITE));
    assertEquals("Board cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("getBestMove with null player should throw IllegalArgumentException")
  void testGetBestMoveWithNullPlayer() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> aiPlayer.getBestMove(testUndo, testBoard, null));
    assertEquals("Player cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("setAlgorithm with valid algorithm should update algorithm")
  void testSetAlgorithmWithValidAlgorithm() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);
    MinMax newAlgorithm = new MinMax(3);

    aiPlayer.setAlgorithm(newAlgorithm);

    assertEquals(newAlgorithm, aiPlayer.getAlgorithm());
  }

  @Test
  @DisplayName("setAlgorithm with null algorithm should throw IllegalArgumentException")
  void testSetAlgorithmWithNullAlgorithm() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> aiPlayer.setAlgorithm(null));
    assertEquals("AI algorithm cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("setEvaluator with valid evaluator should update evaluator")
  void testSetEvaluatorWithValidEvaluator() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);
    SimpleEvaluator newEvaluator = new SimpleEvaluator();

    aiPlayer.setEvaluator(newEvaluator);

    assertEquals(newEvaluator, aiPlayer.getEvaluator());
  }

  @Test
  @DisplayName("setEvaluator with null evaluator should throw IllegalArgumentException")
  void testSetEvaluatorWithNullEvaluator() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> aiPlayer.setEvaluator(null));
    assertEquals("Evaluator cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Integration test with real components")
  void testIntegrationWithRealComponents() {
    Board realBoard = new Board(8);
    ManagerUndoRedo realUndo = new ManagerUndoRedo(realBoard);
    AIPlayer aiPlayer = new AIPlayer("Integration AI",
        new MinMaxAlphaBeta(2), new SimpleEvaluator());

    Move move = aiPlayer.getBestMove(realUndo, realBoard, PlayerColor.WHITE);

    assertNotNull(move);
    // Vérifications basiques
    assertTrue(move.getPath().size() >= 2);
    assertTrue(move.getPath().get(0) >= 0);
    assertTrue(move.getPath().get(1) < realBoard.getIndexMax());
  }

  @Test
  @DisplayName("AIPlayer should inherit from Player")
  void testAIPlayerInheritance() {
    AIPlayer aiPlayer = new AIPlayer("Test AI", testAlgorithm, testEvaluator);

    assertTrue(aiPlayer instanceof Player);
    assertEquals("Test AI", aiPlayer.getName());
  }
}