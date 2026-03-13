package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.controller.commands.*;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuitCommandTest {

  private GameController controller;
  private SpyGameView spyView;

  @BeforeEach
  void setUp() {
    spyView = new SpyGameView();
    controller = new GameController(spyView);
    controller.startNewGame(Configuration.getDefaultConfiguration());
  }

  // =========================================================
  // Helpers
  // =========================================================

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  private void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
    System.setIn(System.in);
  }

  /** Simulates stdin input for Scanner inside QuitCommand. */
  private void provideInput(String input) {
    System.setIn(new ByteArrayInputStream(input.getBytes()));
  }

  /** Creates a QuitCommand that records whether exit was called. */
  private QuitCommand makeCmd(boolean[] exitCalled) {
    return new QuitCommand(controller, () -> exitCalled[0] = true);
  }

  // =========================================================
  // Branch: no unsaved changes -> exits directly without prompting
  // =========================================================

  @Test
  void testExecute_noUnsavedChanges_exitsWithZero() {
    // markAsSaved is already called in startNewGame -> no unsaved changes
    assertFalse(controller.hasUnsavedChanges());

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    assertTrue(exitCalled[0], "Exit must be called.");
  }

  // =========================================================
  // Branch: unsaved changes + 'y' + valid path -> save succeeds
  // =========================================================

  /**
   * When the user answers 'y' and provides a valid file path,
   * SaveBoard succeeds, handled = true and exit is called.
   */
  @Test
  void testExecute_userAnswersYes_validPath_savesAndExits() throws Exception {
    forceUnsavedChanges();

    java.io.File tempFile = java.io.File.createTempFile("checkers_save", ".dat");
    tempFile.deleteOnExit();

    // "y\n" -> réponse à quit.save
    // chemin\n -> réponse à quit.path
    // \n -> ligne de sécurité au cas où le scanner redemande
    provideInput("y\n" + tempFile.getAbsolutePath() + "\n\n");

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    assertTrue(exitCalled[0], "Exit must be called after successful save.");
    restoreStreams();
  }

  /**
   * 'Y' uppercase must behave the same as 'y' (equalsIgnoreCase).
   */
  @Test
  void testExecute_userAnswersYesUppercase_validPath_savesAndExits() throws Exception {
    forceUnsavedChanges();

    java.io.File tempFile = java.io.File.createTempFile("checkers_save_upper", ".dat");
    tempFile.deleteOnExit();
    provideInput("Y\n" + tempFile.getAbsolutePath() + "\n\n");

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    assertTrue(exitCalled[0], "Exit must be called after successful save with uppercase Y.");
    restoreStreams();
  }

  // =========================================================
  // Branch: unsaved changes + 'y' + invalid path -> exception caught,
  // loop re-prompts -> 'n' exits without saving
  // =========================================================

  /**
   * When the user answers 'y' but the path is invalid (save throws),
   * the catch block logs an error and the loop continues.
   * The user then answers 'n' to skip saving and exit.
   */
  @Test
  void testExecute_userAnswersYes_invalidPath_loopRetries_thenNo() throws Exception {
    forceUnsavedChanges();

    provideInput("y\n/invalid/path/does/not/exist.dat\nn\n");

    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err));

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    restoreStreams();

    assertTrue(exitCalled[0], "Exit must be called after giving up save.");
    assertTrue(err.toString().length() > 0, "An error message must be printed on stderr.");
  }

  // =========================================================
  // Branch: unsaved changes + 'n' -> else branch -> handled = true immediately
  // =========================================================

  /**
   * When the user answers 'n', the else branch sets handled = true
   * and the loop exits without saving.
   */
  @Test
  void testExecute_userAnswersNo_skipsSave() throws Exception {
    forceUnsavedChanges();
    provideInput("n\n");

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    assertTrue(exitCalled[0]);
    restoreStreams();
  }

  /**
   * Any input other than 'y'/'Y' (e.g. empty Enter) must be treated as 'N'.
   * Covers the else branch with a non-standard input.
   */
  @Test
  void testExecute_userPressesEnter_treatedAsNo() throws Exception {
    forceUnsavedChanges();
    provideInput("\n");

    boolean[] exitCalled = { false };
    makeCmd(exitCalled).execute();

    assertTrue(exitCalled[0]);
    restoreStreams();
  }

  // =========================================================
  // getHelp
  // =========================================================

  @Test
  void testGetHelp_returnsNonNullString() {
    QuitCommand cmd = new QuitCommand(controller);
    assertNotNull(cmd.getHelp(), "getHelp() must return a non-null string.");
    assertFalse(cmd.getHelp().isBlank(), "getHelp() must return a non-empty string.");
  }

  /**
   * Forces lastSavedMoveCount to 0 while history has moves, making
   * hasUnsavedChanges() return true.
   */
  private void forceUnsavedChanges() throws Exception {
    // Directly manipulate lastSavedMoveCount to simulate unsaved state
    java.lang.reflect.Field field = GameController.class.getDeclaredField("lastSavedMoveCount");
    field.setAccessible(true);
    field.set(controller, -1); // differs from current history size -> hasUnsavedChanges() = true
  }

  // =========================================================
  // Internal spy view
  // =========================================================

  private static class SpyGameView extends GameView {
    @Override
    public void start() {
    }

    @Override
    public void setController(GameController c) {
    }

    @Override
    public void display(GameCheckers g) {
    }

    @Override
    public void update(GameCheckers g) {
    }
  }
}