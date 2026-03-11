package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.controller.commands.LoadCommand;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LoadCommandTest {

  private GameController controller;
  private SpyGameView spyView;

  @TempDir
  Path tempDir;

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
  private ByteArrayOutputStream capturedOut;

  private void captureOutput() {
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut));
  }

  private void restoreOutput() {
    System.setOut(originalOut);
  }

  /**
   * Injects a custom saveDirectory into LoadCommand via reflection,
   * pointing to the @TempDir so file resolution works in tests.
   */
  private LoadCommand makeCmd(String[] args, String saveDir) throws Exception {
    LoadCommand cmd = new LoadCommand(controller, args);
    Field f = LoadCommand.class.getDeclaredField("saveDirectory");
    f.setAccessible(true);
    f.set(cmd, saveDir);
    return cmd;
  }

  /**
   * Creates a valid save file in tempDir with [settings] section.
   */
  private File createValidSaveFile(String fileName) throws Exception {
    File file = tempDir.resolve(fileName).toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("[settings]\n");
      fw.write("board-size=8\n");
      fw.write("time-mode=normal\n");
      fw.write("debug=false\n");
      fw.write("[game]\n");
      fw.write("current-player=white\n");
    }
    return file;
  }

  /**
   * Creates a save file missing board-size in [settings].
   */
  private File createSaveFileMissingSize(String fileName) throws Exception {
    File file = tempDir.resolve(fileName).toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("[settings]\n");
      fw.write("time-mode=normal\n");
    }
    return file;
  }

  /**
   * Creates a save file with blitz mode enabled.
   */
  private File createBlitzSaveFile(String fileName) throws Exception {
    File file = tempDir.resolve(fileName).toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("[settings]\n");
      fw.write("board-size=8\n");
      fw.write("time-mode=blitz\n");
      fw.write("debug=true\n");
    }
    return file;
  }

  /**
   * Creates a save file with comments and blank lines to test stripComments.
   */
  private File createSaveFileWithComments(String fileName) throws Exception {
    File file = tempDir.resolve(fileName).toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("# This is a comment\n");
      fw.write("[settings]\n");
      fw.write("board-size=8 # inline comment\n");
      fw.write("time-mode={ignored}normal\n");
      fw.write("\n"); // blank line
      fw.write("debug=false\n");
    }
    return file;
  }

  // =========================================================
  // Branch: args null -> prints help
  // =========================================================

  @Test
  void testExecute_nullArgs_printsHelp() throws Exception {
    LoadCommand cmd = makeCmd(null, tempDir.toString());

    captureOutput();
    cmd.execute();
    restoreOutput();

    assertTrue(capturedOut.toString().contains("load"),
        "Help message must be printed when args is null.");
  }

  // =========================================================
  // Branch: args empty -> prints help
  // =========================================================

  @Test
  void testExecute_emptyArgs_printsHelp() throws Exception {
    LoadCommand cmd = makeCmd(new String[] {}, tempDir.toString());

    captureOutput();
    cmd.execute();
    restoreOutput();

    assertTrue(capturedOut.toString().contains("load"),
        "Help message must be printed when args is empty.");
  }

  // =========================================================
  // Branch: args[0] is blank -> prints help
  // =========================================================

  @Test
  void testExecute_blankFileName_printsHelp() throws Exception {
    LoadCommand cmd = makeCmd(new String[] { "   " }, tempDir.toString());

    captureOutput();
    cmd.execute();
    restoreOutput();

    assertTrue(capturedOut.toString().contains("load"),
        "Help message must be printed when filename is blank.");
  }

  // =========================================================
  // Branch: file does not exist -> prints error
  // =========================================================

  @Test
  void testExecute_fileNotFound_printsError() throws Exception {
    LoadCommand cmd = makeCmd(new String[] { "nonexistent.txt" }, tempDir.toString());

    captureOutput();
    cmd.execute();
    restoreOutput();

    assertTrue(capturedOut.toString().contains("not found"),
        "Error message must be printed when file does not exist.");
  }

  // =========================================================
  // Branch: file exists but missing board-size -> format error
  // =========================================================

  @Test
  void testExecute_missingSizeInSettings_printsFormatError() throws Exception {
    createSaveFileMissingSize("missing_size.txt");
    LoadCommand cmd = makeCmd(new String[] { "missing_size.txt" }, tempDir.toString());

    captureOutput();
    cmd.execute();
    restoreOutput();

    assertTrue(capturedOut.toString().contains("Missing board-size"),
        "Format error must be printed when board-size is absent.");
  }

  // =========================================================
  // Branch: valid file with comments and blank lines -> stripComments covered
  // =========================================================

  @Test
  void testExecute_fileWithComments_parsedCorrectly() throws Exception {
    createSaveFileWithComments("commented.txt");
    LoadCommand cmd = makeCmd(new String[] { "commented.txt" }, tempDir.toString());

    // Should not throw and should reach setGame
    captureOutput();
    assertDoesNotThrow(() -> cmd.execute());
    restoreOutput();
  }

  // =========================================================
  // Branch: valid file, time-mode=blitz -> blitz=true in Configuration
  // =========================================================

  @Test
  void testExecute_blitzMode_configuredCorrectly() throws Exception {
    createBlitzSaveFile("blitz_save.txt");
    LoadCommand cmd = makeCmd(new String[] { "blitz_save.txt" }, tempDir.toString());

    captureOutput();
    assertDoesNotThrow(() -> cmd.execute());
    restoreOutput();

    assertTrue(controller.isBlitz(), "Controller must be in blitz mode after loading blitz save.");
  }

  // =========================================================
  // Branch: unknown key in settings -> default branch (ignored)
  // =========================================================

  @Test
  void testExecute_unknownSettingsKey_ignored() throws Exception {
    File file = tempDir.resolve("unknown_key.txt").toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("[settings]\n");
      fw.write("board-size=8\n");
      fw.write("unknown-key=somevalue\n"); // triggers default branch
      fw.write("starting-player=white\n"); // also ignored
    }
    LoadCommand cmd = makeCmd(new String[] { "unknown_key.txt" }, tempDir.toString());

    captureOutput();
    assertDoesNotThrow(() -> cmd.execute());
    restoreOutput();
  }

  // =========================================================
  // Branch: section stops reading after [settings] when size is known
  // =========================================================

  @Test
  void testExecute_stopsAfterSettingsSection() throws Exception {
    File file = tempDir.resolve("multi_section.txt").toFile();
    try (FileWriter fw = new FileWriter(file)) {
      fw.write("[settings]\n");
      fw.write("board-size=8\n");
      fw.write("[game]\n"); // triggers section break since size != null
      fw.write("board-size=99\n"); // must be ignored
    }
    LoadCommand cmd = makeCmd(new String[] { "multi_section.txt" }, tempDir.toString());

    captureOutput();
    assertDoesNotThrow(() -> cmd.execute());
    restoreOutput();
  }

  // =========================================================
  // getHelp
  // =========================================================

  @Test
  void testGetHelp_returnsNonNullString() {
    LoadCommand cmd = new LoadCommand(controller, null);
    assertNotNull(cmd.getHelp());
    assertFalse(cmd.getHelp().isBlank());
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