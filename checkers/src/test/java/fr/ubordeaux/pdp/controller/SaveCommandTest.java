package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

import fr.ubordeaux.pdp.controller.commands.SaveCommand;

class SaveCommandTest {

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
  }

  private ByteArrayOutputStream captureOut() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));
    return out;
  }

  private ByteArrayOutputStream captureErr() {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err));
    return err;
  }

  /**
   * Sets the game field to null via reflection to simulate no game in progress.
   */
  private void setGameNull() throws Exception {
    Field f = GameController.class.getDeclaredField("game");
    f.setAccessible(true);
    f.set(controller, null);
  }

  // =========================================================
  // Branch: game == null -> prints error and returns
  // =========================================================

  @Test
  void testExecute_noGame_printsError() throws Exception {
    setGameNull();

    ByteArrayOutputStream out = captureOut();
    new SaveCommand(controller, new String[] { "somefile.dat" }).execute();
    restoreStreams();

    assertTrue(out.toString().contains("No game in progress"),
        "Must print 'No game in progress' when game is null.");
  }

  // =========================================================
  // Branch: args == null -> prints help and returns
  // =========================================================

  @Test
  void testExecute_nullArgs_printsHelp() {
    ByteArrayOutputStream out = captureOut();
    new SaveCommand(controller, null).execute();
    restoreStreams();

    assertTrue(out.toString().contains("save"),
        "Must print help when args is null.");
  }

  // =========================================================
  // Branch: args empty -> prints help and returns
  // =========================================================

  @Test
  void testExecute_emptyArgs_printsHelp() {
    ByteArrayOutputStream out = captureOut();
    new SaveCommand(controller, new String[] {}).execute();
    restoreStreams();

    assertTrue(out.toString().contains("save"),
        "Must print help when args is empty.");
  }

  // =========================================================
  // Branch: fileName blank -> prints help and returns
  // =========================================================

  @Test
  void testExecute_blankFileName_printsHelp() {
    ByteArrayOutputStream out = captureOut();
    new SaveCommand(controller, new String[] { "   " }).execute();
    restoreStreams();

    assertTrue(out.toString().contains("save"),
        "Must print help when filename is blank.");
  }

  // =========================================================
  // Branch: invalid path -> catch block, prints error on stderr
  // =========================================================

  @Test
  void testExecute_validPath_noErrorOnStderr() throws Exception {
    java.io.File tempFile = java.io.File.createTempFile("save_test", ".dat");
    tempFile.deleteOnExit();

    ByteArrayOutputStream err = captureErr();
    new SaveCommand(controller, new String[] { tempFile.getAbsolutePath() }).execute();
    restoreStreams();

    // If stderr has content, SaveBoard threw an exception
    assertEquals("", err.toString(),
        "No error expected on stderr for a valid save path. Error was: " + err);
  }

  // =========================================================
  // getHelp
  // =========================================================

  @Test
  void testGetHelp_returnsNonNullString() {
    SaveCommand cmd = new SaveCommand(controller, null);
    assertNotNull(cmd.getHelp(), "getHelp() must return a non-null string.");
    assertFalse(cmd.getHelp().isBlank(), "getHelp() must return a non-empty string.");
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