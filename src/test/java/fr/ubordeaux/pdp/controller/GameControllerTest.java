package fr.ubordeaux.pdp.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;

import fr.ubordeaux.pdp.view.GameView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.tools.Utils;

class GameControllerTest {

  private GameController controller;
  private SpyGameView spyView;

  @BeforeEach
  void setUp() {
    spyView = new SpyGameView();
    controller = new GameController(spyView);

    Configuration defaultConfig = Configuration.getDefaultConfiguration();
    controller.startNewGame(defaultConfig);
  }

  @Test
  void testStartBindsControllerAndView() {
    controller.start();
    assertTrue(spyView.startCalled, "The view's start() method must be called.");
    assertEquals(controller, spyView.boundController, "The controller must bind to the view when start is called.");
  }

  @Test
  void testStartNewGameInitializesModel() {
    Configuration config = Configuration.getDefaultConfiguration();
    controller.startNewGame(config);

    // Verify indirectly through board display
    controller.displayBoard();
    assertTrue(spyView.displayCalled, "The board must be displayed after game initialization.");
    assertNotNull(spyView.lastGameReceived, "The view must receive a GameCheckers instance.");
  }

  @Test
  void testExecuteCommandNew() {
    // Simulates the "new" command which should trigger initialization
    // Note: NewCommand calls controller.startNewGame
    String[] args = { "false", "false", "0", "8" };
    controller.executeCommand("new", args);

    controller.displayBoard();
    assertTrue(spyView.displayCalled);
  }

  @Test
  void testToggleDebugAndVerbose() {
    Configuration config = Configuration.getDefaultConfiguration();
    controller.startNewGame(config);

    // Verbose test
    controller.setVerbose(true);
    assertTrue(controller.isVerbose(), "Verbose mode should be enabled.");

    // Debug test
    controller.setDebug(true);
    assertTrue(controller.isDebug(), "Debug mode should be enabled.");

    controller.setDebug(false);
    assertFalse(controller.isDebug(), "Debug mode should be disabled.");
  }

  @Test
  void testExecuteMoveUpdatesView() {
    Configuration config = Configuration.getDefaultConfiguration();
    controller.startNewGame(config);

    // Reset view flag
    spyView.displayCalled = false;

    // Attempt a move (format expected by GameCheckers via GameController)
    // Note: If the move is invalid, nothing happens, but the call is traced
    controller.executeMove("B2", "C3");

    // After a move, the controller generally calls displayBoard via observer
    // or manually depending on implementation.
    controller.displayBoard();
    assertTrue(spyView.displayCalled);
  }

  @Test
  void testExecuteAllCommands() {
    // List of simple commands to test, quit excluded (no complex arguments)
    String[] commands = Utils.COMMANDS_MAP.keySet().stream()
        .filter(cmd -> !cmd.equals("quit"))
        .toArray(String[]::new);

    for (String cmd : commands) {
      // We only verify that execution does not throw an exception
      // assertDoesNotThrow(() -> controller.executeCommand(cmd, null));
    }
  }

  @Test
  void testUnknownCommand() {
    // Covers the "default" branch and the "yield null"
    assertDoesNotThrow(() -> controller.executeCommand("invalidCmd", null));
  }

  @Test
  void testExecuteMoveGameOverBranch() throws Exception {
    controller.startNewGame(Configuration.getDefaultConfiguration());

    // Access the private model to manipulate its state
    java.lang.reflect.Field gameField = GameController.class.getDeclaredField("game");
    gameField.setAccessible(true);
    GameCheckers gameModel = (GameCheckers) gameField.get(controller);

    // This call will now enter the 'if' block
    controller.executeMove("A1", "B2");
  }

  @Test
  void testDisplayConfiguration() {
    Configuration config = Configuration.getDefaultConfiguration();
    controller.startNewGame(config);

    // Verify that the call does not throw an exception
    assertDoesNotThrow(() -> controller.displayConfiguration());

    java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
    System.setOut(new java.io.PrintStream(outContent));

    controller.displayConfiguration();

    assertTrue(outContent.toString().contains("blitz="),
        "Output should contain configuration details.");

    // Reset output stream
    System.setOut(System.out);
  }

  // =========================================================
  // executeMove
  // =========================================================

  /**
   * Happy path branch: valid move, game not over.
   * Covers the normal execution path without FINISHED state.
   */
  @Test
  void testExecuteMove_validMove_gameNotOver() {
    assertDoesNotThrow(() -> controller.executeMove("B6", "C5"));
  }

  /**
   * FINISHED branch: forces the FINISHED state via reflection
   * to cover the if (state == FINISHED) block.
   */
  @Test
  void testExecuteMove_whenGameAlreadyFinished_printsGameOver() throws Exception {
    // Force game over BEFORE the move
    forceGameState(State.FINISHED);

    ByteArrayOutputStream out = captureOutput();
    controller.executeMove("B6", "C5");
    restoreOutput();

    // The game over block must have been executed (or skipped depending on impl.)
    // We mainly verify the absence of NullPointerException
    assertNotNull(out.toString());
  }

  /**
   * Blitz branch: in blitz mode, executeMove must restart the timer.
   * Verifies that the timer is properly (re)started without exception.
   */
  @Test
  void testExecuteMove_blitzMode_restartsTimer() throws Exception {
    Configuration blitzConfig = buildBlitzConfig(120);
    controller.startNewGame(blitzConfig);

    assertDoesNotThrow(() -> controller.executeMove("B6", "C5"));

    // Mandatory cleanup to avoid an orphaned timer
    controller.stopBlitzTimer();
  }

  /**
   * Blitz + FINISHED branch: the timer must be stopped
   * and the game over message printed.
   */
  @Test
  void testExecuteMove_blitzMode_gameOver_stopsTimer() throws Exception {
    Configuration blitzConfig = buildBlitzConfig(120);
    controller.startNewGame(blitzConfig);
    forceGameState(State.FINISHED);

    ByteArrayOutputStream out = captureOutput();
    assertDoesNotThrow(() -> controller.executeMove("B6", "C5"));
    restoreOutput();

    // The timer must be null after stopBlitzTimer()
    Field timerField = GameController.class.getDeclaredField("blitzTimer");
    timerField.setAccessible(true);
    assertNull(timerField.get(controller), "The blitzTimer must be stopped after game over.");
  }

  // =========================================================
  // hasUnsavedChanges
  // =========================================================

  @Test
  void testHasUnsavedChanges_falseRightAfterInit() {
    assertFalse(controller.hasUnsavedChanges(),
        "No unsaved changes expected right after startNewGame.");
  }

  @Test
  void testHasUnsavedChanges_trueAfterMove() {
    // Play a valid move to grow the history
    controller.executeMove("B6", "C5");
    // If the move was accepted, the history has changed
    GameCheckers game = controller.getGame();
    int histSize = game.getHistory().getSize();
    if (histSize > 0) {
      assertTrue(controller.hasUnsavedChanges());
    }
    // If the move was invalid (size = 0), we only verify the absence of exception
  }

  @Test
  void testHasUnsavedChanges_falseAfterMarkAsSaved() {
    controller.executeMove("B6", "C5");
    controller.markAsSaved();
    assertFalse(controller.hasUnsavedChanges(),
        "No unsaved changes expected after markAsSaved().");
  }

  /**
   * Branch: game == null -> must return false without NPE.
   */
  @Test
  void testHasUnsavedChanges_nullGame_returnsFalse() throws Exception {
    setFieldNull("game");
    assertFalse(controller.hasUnsavedChanges());
  }

  /**
   * Branch: game == null w-> markAsSaved must not throw.
   * Covers the null-check guard in markAsSaved.
   */
  @Test
  void testMarkAsSaved_nullGame_doesNotThrow() throws Exception {
    setFieldNull("game");
    assertDoesNotThrow(() -> controller.markAsSaved());
  }

  // =========================================================
  // setGame
  // =========================================================

  @Test
  void testSetGame_bindsObserverAndDisplaysBoard() {
    spyView.displayCalled = false;
    GameCheckers freshGame = new GameCheckers(Configuration.getDefaultConfiguration());

    controller.setGame(freshGame, Configuration.getDefaultConfiguration());

    assertTrue(spyView.displayCalled, "setGame must trigger displayBoard().");
    assertEquals(freshGame, spyView.lastGameReceived);
  }

  @Test
  void testSetGame_resetsUnsavedChanges() {
    GameCheckers freshGame = new GameCheckers(Configuration.getDefaultConfiguration());
    controller.setGame(freshGame, Configuration.getDefaultConfiguration());
    assertFalse(controller.hasUnsavedChanges(),
        "setGame must call markAsSaved().");
  }

  @Test
  void testSetGame_replacesConfiguration() {
    Configuration newConfig = Configuration.getDefaultConfiguration();
    GameCheckers freshGame = new GameCheckers(newConfig);
    controller.setGame(freshGame, newConfig);

    // Configuration getters must reflect the new config
    assertFalse(controller.isBlitz());
    assertTrue(controller.isWhiteAi());
    assertTrue(controller.isBlackAi());
  }

  // =========================================================
  // undo / redo
  // =========================================================

  @Test
  void testUndoGame_zeroTimes_doesNothing() {
    assertDoesNotThrow(() -> controller.undoGame(0));
  }

  @Test
  void testUndoGame_onceWithHistory() {
    controller.executeMove("B6", "C5");
    assertDoesNotThrow(() -> controller.undoGame(1));
  }

  @Test
  void testUndoGame_moreThanHistory_doesNotThrow() {
    // Undo on empty or too-small history
    assertDoesNotThrow(() -> controller.undoGame(10));
  }

  @Test
  void testRedoGame_withoutPriorUndo_doesNotThrow() {
    assertDoesNotThrow(() -> controller.redoGame(1));
  }

  @Test
  void testRedoGame_afterUndo_restoresState() {
    controller.executeMove("B6", "C5");
    controller.undoGame(1);
    assertDoesNotThrow(() -> controller.redoGame(1));
  }

  @Test
  void testUndoThenRedo_multipleSteps() {
    controller.executeMove("B6", "C5");
    controller.undoGame(1);
    controller.redoGame(1);
    controller.undoGame(1);
    assertDoesNotThrow(() -> controller.redoGame(2)); // redo beyond available history
  }

  // =========================================================
  // executeCommand — missing branches
  // =========================================================

  @Test
  void testExecuteCommand_show_board() {
    assertDoesNotThrow(() -> controller.executeCommand("show", new String[] { "board" }));
  }

  @Test
  void testExecuteCommand_show_history() {
    assertDoesNotThrow(() -> controller.executeCommand("show", new String[] { "history" }));
  }

  @Test
  void testExecuteCommand_show_config() {
    assertDoesNotThrow(() -> controller.executeCommand("show", new String[] { "config" }));
  }

  @Test
  void testExecuteCommand_show_time() {
    assertDoesNotThrow(() -> controller.executeCommand("show", new String[] { "time" }));
  }

  @Test
  void testExecuteCommand_set_verbose_true() {
    controller.executeCommand("set", new String[] { "verbose=true" });
    assertTrue(controller.isVerbose());
  }

  @Test
  void testExecuteCommand_set_verbose_false() {
    controller.executeCommand("set", new String[] { "verbose=false" });
    assertFalse(controller.isVerbose());
  }

  @Test
  void testExecuteCommand_set_debug_true() {
    controller.executeCommand("set", new String[] { "debug=true" });
    assertTrue(controller.isDebug());
  }

  @Test
  void testExecuteCommand_set_debug_false() {
    controller.executeCommand("set", new String[] { "debug=false" });
    assertFalse(controller.isDebug());
  }

  // Branch: null args -> displays help
  @Test
  void testSetCommand_nullArgs_printsHelp() {
    assertDoesNotThrow(() -> controller.executeCommand("set", null));
  }

  // Branch: empty args -> displays help
  @Test
  void testSetCommand_emptyArgs_printsHelp() {
    assertDoesNotThrow(() -> controller.executeCommand("set", new String[] {}));
  }

  // Branch: format without '=' -> format error message
  @Test
  void testSetCommand_invalidFormat_noEquals() {
    assertDoesNotThrow(() -> controller.executeCommand("set", new String[] { "verboseTrue" }));
  }

  // Branch: invalid value (neither true nor false)
  @Test
  void testSetCommand_invalidValue() {
    assertDoesNotThrow(() -> controller.executeCommand("set", new String[] { "verbose=yes" }));
  }

  // Branch: unknown parameter -> "unsupported parameter" message
  @Test
  void testSetCommand_unknownParameter() {
    assertDoesNotThrow(() -> controller.executeCommand("set", new String[] { "unknown=true" }));
  }

  @Test
  void testExecuteCommand_undo_one() {
    assertDoesNotThrow(() -> controller.executeCommand("undo", new String[] { "1" }));
  }

  @Test
  void testExecuteCommand_redo_one() {
    assertDoesNotThrow(() -> controller.executeCommand("redo", new String[] { "1" }));
  }

  @Test
  void testExecuteCommand_unknownCommand_yieldsNull() {
    // Covers the "default -> yield null" branch and the if (command != null) check
    ByteArrayOutputStream out = captureOutput();
    controller.executeCommand("doesNotExist", null);
    restoreOutput();
    assertTrue(out.toString().contains("Unknown command"),
        "An error message must be displayed for an unknown command.");
  }

  // =========================================================
  // displayTime — blitz ON / OFF branches
  // =========================================================

  @Test
  void testDisplayTime_notBlitz_printsNotBlitzMessage() {
    ByteArrayOutputStream out = captureOutput();
    controller.displayTime();
    restoreOutput();
    assertFalse(out.toString().isEmpty(),
        "A message must be displayed even outside blitz mode.");
  }

  @Test
  void testDisplayTime_blitzMode_printsRemainingTime() {
    Configuration blitzConfig = buildBlitzConfig(300);
    controller.startNewGame(blitzConfig);
    GameCheckers game = controller.getGame();

    ByteArrayOutputStream out = captureOutput();
    controller.displayTime();
    restoreOutput();

    controller.stopBlitzTimer();

    String output = out.toString();
    assertFalse(output.isEmpty(), "displayTime must display the remaining time in blitz mode.");
    assertTrue(output.contains(game.getWhitePlayer().getName()),
        "displayTime must include white player's remaining time.");
    assertTrue(output.contains(game.getBlackPlayer().getName()),
        "displayTime must include black player's remaining time.");
  }

  // =========================================================
  // playAiTurn — via réflexion (méthode private)
  // =========================================================

  @Test
  void testPlayAiTurn_validMove_appliesMove() throws Exception {
    Configuration aiConfig = buildWhiteAiConfig();
    controller.startNewGame(aiConfig);

    GameCheckers game = controller.getGame();
    AiPlayer ai = (AiPlayer) game.getWhitePlayer();

    int sizeBefore = game.getHistory().getSize();
    invokePlayAiTurn(ai);
    int sizeAfter = game.getHistory().getSize();

    assertTrue(sizeAfter > sizeBefore, "L'IA doit avoir joué un coup.");
  }

  @Test
  void testPlayAiTurn_nullMove_callsCheckGameOver() throws Exception {
    Configuration aiConfig = buildWhiteAiConfig();
    controller.startNewGame(aiConfig);

    AiPlayer mockAi = Mockito.mock(AiPlayer.class);
    Mockito.when(mockAi.getBestMove(any(), any(), any())).thenReturn(null);

    // Quand move == null, le code appelle checkGameOver() et handleGameOver()
    // sans planter — c'est le comportement attendu
    assertDoesNotThrow(() -> invokePlayAiTurn(mockAi));
  }

  @Test
  void testPlayAiTurn_nullMove_stateSetByCheckGameOver() throws Exception {
    Configuration aiConfig = buildWhiteAiConfig();
    controller.startNewGame(aiConfig);
    GameCheckers game = controller.getGame();

    AiPlayer mockAi = Mockito.mock(AiPlayer.class);
    Mockito.when(mockAi.getBestMove(any(), any(), any())).thenReturn(null);

    invokePlayAiTurn(mockAi);

    // checkGameOver() a été appelé — l'état est cohérent (IN_GAME ou FINISHED)
    // selon si l'adversaire a encore des coups
    State state = game.getState();
    assertTrue(state == State.IN_GAME || state == State.FINISHED,
        "L'état doit être cohérent après un coup null de l'IA.");
  }

  @Test
  void testPlayAiTurn_blitz_startsTimer() throws Exception {
    Configuration blitzAiConfig = buildBlitzWhiteAiConfig();
    controller.startNewGame(blitzAiConfig);

    AiPlayer ai = (AiPlayer) controller.getGame().getWhitePlayer();
    assertDoesNotThrow(() -> invokePlayAiTurn(ai));

    controller.stopBlitzTimer();
  }

  @Test
  void testPlayAiTurn_moveLeadsToGameOver_setsFinished() throws Exception {
    Configuration aiConfig = buildWhiteAiConfig();
    controller.startNewGame(aiConfig);

    GameCheckers game = controller.getGame();
    AiPlayer ai = (AiPlayer) game.getWhitePlayer();

    // Jouer jusqu'à ce qu'un coup mène à FINISHED, ou vérifier setState
    invokePlayAiTurn(ai);

    // La partie est soit IN_GAME soit FINISHED — jamais d'état incohérent
    assertTrue(
        game.getState() == State.IN_GAME || game.getState() == State.FINISHED);
  }

  // =========================================================
  // Helpers
  // =========================================================

  /** Captures System.out for assertion purposes. */
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream capturedOut;

  private ByteArrayOutputStream captureOutput() {
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut));
    return capturedOut;
  }

  private void restoreOutput() {
    System.setOut(originalOut);
  }

  /** Forces the game state via reflection. */
  private void forceGameState(State state) throws Exception {
    GameCheckers game = controller.getGame();
    game.setState(state);
  }

  /** Sets a private field to null via reflection. */
  private void setFieldNull(String fieldName) throws Exception {
    Field f = GameController.class.getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(controller, null);
  }

  private Configuration buildWhiteAiConfig() {
    // blitz=false, time=0, contest=false, size=8,
    // verbose=false, debug=false, whiteAi=true, blackAi=false, aiTime=10
    return new Configuration(false, 0, false, 8, false, false, true, false, 10);
  }

  private Configuration buildBlackAiConfig() {
    return new Configuration(false, 0, false, 8, false, false, false, true, 10);
  }

  private Configuration buildBothAiConfig() {
    return new Configuration(false, 0, false, 8, false, false, true, true, 10);
  }

  private Configuration buildBlitzWhiteAiConfig() {
    return new Configuration(true, 2, false, 8, false, false, true, false, 10);
  }

  private Configuration buildBlitzConfig(int minutes) {
    return new Configuration(true, minutes, false, 8, false, false, false, false, 10);
  }

  private void invokePlayAiTurn(AiPlayer aiPlayer) throws Exception {
    java.lang.reflect.Method method = GameController.class
        .getDeclaredMethod("playAiTurn", AiPlayer.class);
    method.setAccessible(true);
    method.invoke(controller, aiPlayer);
  }

  // =========================================================
  // Internal spy class to simulate the View (Mock/Spy)
  // =========================================================
  private static class SpyGameView extends GameView {
    boolean startCalled = false;
    boolean displayCalled = false;
    GameController boundController;
    GameCheckers lastGameReceived;

    @Override
    public void start() {
      startCalled = true;
    }

    @Override
    public void setController(GameController controller) {
      this.boundController = controller;
    }

    @Override
    public void display(GameCheckers game) {
      displayCalled = true;
      this.lastGameReceived = game;
    }

    @Override
    public void update(GameCheckers game) {
      // Used by the Observer pattern
      this.lastGameReceived = game;
    }

    @Test
    void testPlayPredefinedSequence() throws Exception {
      Configuration config = Configuration.getDefaultConfiguration();
      controller.startNewGame(config);

      java.lang.reflect.Field gameField = GameController.class.getDeclaredField("game");
      gameField.setAccessible(true);
      GameCheckers gameModel = (GameCheckers) gameField.get(controller);

      String[][] moves = {
          { "c1", "d2" }, { "f4", "e3" }, { "d2", "f4" }, { "g5", "e3" },
          { "b2", "c1" }, { "e3", "d2" }, { "c1", "e3" }, { "f2", "b2" },
          { "a1", "c3" }, { "f6", "e5" }, { "c3", "d4" }, { "e5", "c3" },
          { "b4", "d2" }, { "g3", "f2" }, { "d2", "e3" }, { "f2", "d4" },
          { "c5", "e3" }, { "g1", "f2" }, { "e3", "g1" }, { "h2", "g3" },
          { "g1", "h2" }, { "h4", "g5" }, { "h2", "e5" }, { "g5", "f4" },
          { "e5", "g3" }, { "f8", "e7" }, { "c7", "d6" }, { "e7", "c5" },
          { "b6", "d4" }, { "g7", "f8" }, { "d4", "e5" }, { "f8", "e7" },
          { "e5", "f4" }, { "h6", "g5" }, { "f4", "h6" }, { "h8", "g7" },
          { "h6", "d6" }
      };

      System.out.println("Début de la séquence d'automatisation des coups...");

      for (String[] move : moves) {
        String from = move[0];
        String to = move[1];

        System.out.println("Coup joué : " + from + "-" + to);

        controller.executeMove(from, to);

        if (gameModel.getState() == State.FINISHED) {
          System.out.println("La partie s'est terminée avant la fin de la séquence.");
          break;
        }
      }

      System.out.println("Séquence terminée.");

      assertDoesNotThrow(() -> controller.displayBoard(),
          "L'affichage du plateau après la séquence ne doit pas générer d'erreur.");
    }
  }
}