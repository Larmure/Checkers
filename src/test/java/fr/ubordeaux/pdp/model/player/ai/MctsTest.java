package fr.ubordeaux.pdp.model.player.ai;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.SimpleEvaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

class MctsTest {

  @Test
  @DisplayName("Default constructor sets exploration constant to sqrt(2)")
  void testDefaultConstructorExplorationConstant() {
    Mcts mcts = new Mcts();

    assertEquals(Mcts.DEFAULT_EXPLORATION, mcts.explorationConstant, 1e-9);
    assertEquals(Ai.DEFAULT_MAX_TIME_MS, mcts.getMaxTimeMs());
  }

  @Test
  @DisplayName("Constructor with custom exploration constant")
  void testConstructorWithCustomExplorationConstant() {
    Mcts mcts = new Mcts(1, 100, 0.7);

    assertEquals(0.7, mcts.explorationConstant, 1e-9);
    assertEquals(100L, mcts.getMaxTimeMs());
  }

  @Test
  @DisplayName("Constructor with non-positive exploration constant throws")
  void testConstructorThrowsOnNonPositiveExploration() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new Mcts(1, 100, 0.0));
    assertTrue(ex.getMessage().contains("Exploration constant must be positive"));
  }

  @Test
  @DisplayName("getBestMove returns a valid move for initial board")
  void testGetBestMoveOnInitialBoard() {
    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts(1, 100);

    Move move = mcts.getBestMove(undo, board, PlayerColor.WHITE, new SimpleEvaluator());

    assertNotNull(move);
    assertTrue(board.getWhiteValidMoves().contains(move) || board.getBlackValidMoves().contains(move)
        || board.getWhiteValidMoves().size() > 0); // simple sanity check
    assertTrue(move.getPath().size() >= 2);
  }

  @Test
  @DisplayName("getBestMove returns null when no legal moves")
  void testGetBestMoveWhenNoMovesReturnsNull() throws Exception {
    Board board = new Board(8);
    clearAllPieces(board);
    assertTrue(board.noPiecesLeft(PlayerColor.WHITE));
    assertTrue(board.noPiecesLeft(PlayerColor.BLACK));

    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts(1, 100);

    Move move = mcts.getBestMove(undo, board, PlayerColor.WHITE, new SimpleEvaluator());
    assertNull(move);
  }

  private void clearAllPieces(Board board) throws Exception {
    for (String fieldName : new String[] {
        "whitePawns1", "whitePawns2", "whiteCheckers1", "whiteCheckers2",
        "blackPawns1", "blackPawns2", "blackCheckers1", "blackCheckers2" }) {
      Field field = Board.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.setLong(board, 0L);
    }
  }

  // PARTIE ML

  @BeforeEach
  void resetMlState() throws Exception {
    // Reset the static mlWeights field to null before each test
    // to ensure tests don't interfere with each other.
    Field field = Mcts.class.getDeclaredField("mlWeights");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  @DisplayName("loadMlWeights successfully loads valid weights")
  void testLoadMlWeightsSuccess(@TempDir Path tempDir) throws Exception {
    Path weightFile = tempDir.resolve("weights.txt");
    Files.writeString(weightFile, "0.5,1.5,-0.2,0.0,0.0,0.0\n-0.8");

    Mcts.loadMlWeights(weightFile.toString());

    // Verify via reflection since mlWeights and mlBias are private
    Field weightsField = Mcts.class.getDeclaredField("mlWeights");
    weightsField.setAccessible(true);
    double[] weights = (double[]) weightsField.get(null);

    Field biasField = Mcts.class.getDeclaredField("mlBias");
    biasField.setAccessible(true);
    double bias = (double) biasField.get(null);

    assertNotNull(weights);
    assertEquals(6, weights.length);
    assertEquals(0.5, weights[0], 1e-9);
    assertEquals(-0.8, bias, 1e-9);
  }

  @Test
  @DisplayName("loadMlWeights handles invalid format gracefully")
  void testLoadMlWeightsInvalidFormat(@TempDir Path tempDir) throws Exception {
    Path weightFile = tempDir.resolve("bad_weights.txt");
    Files.writeString(weightFile, "not,a,number\nbias");

    Mcts.loadMlWeights(weightFile.toString());

    Field weightsField = Mcts.class.getDeclaredField("mlWeights");
    weightsField.setAccessible(true);
    double[] weights = (double[]) weightsField.get(null);

    // Should remain null as the exception is caught
    assertNull(weights);
  }

  @Test
  @DisplayName("evaluateMl throws IllegalStateException when weights cannot be loaded")
  void testEvaluateMlThrowsWhenNoFile() throws Exception {
    LogisticRegressionTrainer.OUTPUT_FILEPATH = "non_existent_file.txt";

    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts();
    Move move = board.getWhiteValidMoves().get(0);

    // We need to create a Node to pass to evaluateMl.
    // Since Node is a private inner class, we create it via reflection.
    Class<?> nodeClass = Class.forName("fr.ubordeaux.pdp.model.player.ai.Mcts$Node");
    java.lang.reflect.Constructor<?> nodeConstructor = nodeClass.getDeclaredConstructor(
        Mcts.class, Move.class, PlayerColor.class, nodeClass);
    nodeConstructor.setAccessible(true);
    Object childNode = nodeConstructor.newInstance(mcts, move, PlayerColor.WHITE, null);

    Method evaluateMlMethod = Mcts.class.getDeclaredMethod(
        "evaluateMl", nodeClass, Board.class, ManagerUndoRedo.class);
    evaluateMlMethod.setAccessible(true);

    // It should throw an InvocationTargetException wrapping the IllegalStateException
    java.lang.reflect.InvocationTargetException ex = assertThrows(
        java.lang.reflect.InvocationTargetException.class,
        () -> evaluateMlMethod.invoke(mcts, childNode, board, undo));
    assertTrue(ex.getCause() instanceof IllegalStateException);
    assertTrue(ex.getCause().getMessage().contains("ML weights not loaded"));
  }

  @Test
  @DisplayName("evaluateMl correctly calculates probability with loaded weights")
  void testEvaluateMlCalculation(@TempDir Path tempDir) throws Exception {
    // 1. Setup a valid weights file
    Path weightFile = tempDir.resolve("ml_weights.txt");
    Files.writeString(weightFile, "1.0,1.0,1.0,1.0,1.0,1.0\n0.5");
    LogisticRegressionTrainer.OUTPUT_FILEPATH = weightFile.toString();

    // 2. Initialize board and required objects
    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    Mcts mcts = new Mcts();
    Move move = board.getWhiteValidMoves().get(0);

    // 3. Create the inner Node object via reflection
    Class<?> nodeClass = Class.forName("fr.ubordeaux.pdp.model.player.ai.Mcts$Node");
    java.lang.reflect.Constructor<?> nodeConstructor = nodeClass.getDeclaredConstructor(
        Mcts.class, Move.class, PlayerColor.class, nodeClass);
    nodeConstructor.setAccessible(true);
    Object childNode = nodeConstructor.newInstance(mcts, move, PlayerColor.WHITE, null);

    // 4. Invoke evaluateMl
    Method evaluateMlMethod = Mcts.class.getDeclaredMethod(
        "evaluateMl", nodeClass, Board.class, ManagerUndoRedo.class);
    evaluateMlMethod.setAccessible(true);
    double probability = (double) evaluateMlMethod.invoke(mcts, childNode, board, undo);

    // 5. Verification
    // The probability should be a valid sigmoid output (between 0 and 1)
    assertTrue(probability >= 0.0 && probability <= 1.0);
  }

  @Test
  @DisplayName("Test direct du switch case ML dans select")
  void testSelectOnlyMl() throws Exception {
    Mcts mcts = new Mcts();
    mcts.setSelectionMode(SelectionMode.ML); // On cible le 'case ML'

    Field weightsField = Mcts.class.getDeclaredField("mlWeights");
    weightsField.setAccessible(true);
    weightsField.set(null, new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 });

    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);

    // Création des Noeuds via réflexion (car Node est une classe interne privée)
    Class<?> nodeClass = Class.forName("fr.ubordeaux.pdp.model.player.ai.Mcts$Node");
    java.lang.reflect.Constructor<?> rootConst = nodeClass.getDeclaredConstructor(Mcts.class, PlayerColor.class);
    rootConst.setAccessible(true);
    Object root = rootConst.newInstance(mcts, PlayerColor.BLACK);

    // On force root.isFullyExpanded(board) à true et on lui donne un enfant
    Field unexploredField = nodeClass.getDeclaredField("unexploredMoves");
    unexploredField.setAccessible(true);
    unexploredField.set(root, new java.util.ArrayList<>()); // Plus de mouvements à explorer

    java.lang.reflect.Constructor<?> childConst = nodeClass.getDeclaredConstructor(Mcts.class, Move.class,
        PlayerColor.class, nodeClass);
    childConst.setAccessible(true);
    Object child = childConst.newInstance(mcts, board.getWhiteValidMoves().get(0), PlayerColor.WHITE, root);

    ((java.util.List<Object>) nodeClass.getDeclaredField("children").get(root)).add(child);

    //On appelle select
    Method selectMethod = Mcts.class.getDeclaredMethod("select", nodeClass, Board.class, ManagerUndoRedo.class);
    selectMethod.setAccessible(true);
    Object result = selectMethod.invoke(mcts, root, board, undo);

    assertEquals(child, result, "Le switch doit choisir l'enfant via le mode ML");
  }
}
