package fr.ubordeaux.pdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConfigManagerTest {

  @TempDir
  Path tempDir;

  private String originalUserHome;
  private Path configFile;

  @BeforeEach
  public void setUp() {
    originalUserHome = System.getProperty("user.home");
    System.setProperty("user.home", tempDir.toString());
    Internationalization.init();
    configFile = tempDir.resolve(".checkersrc");
  }

  @AfterEach
  public void tearDown() {
    if (originalUserHome != null) {
      System.setProperty("user.home", originalUserHome);
    }
  }

  @Test
  public void testLoadCreatesDefaultConfigWhenFileDoesNotExist() throws IOException {
    ConfigManager config = new ConfigManager();

    config.load();

    assertTrue(Files.exists(configFile), ".checkersrc should be created");
    assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
    assertEquals(Utils.DEFAULT_TIME, config.getTime());
    assertEquals(Utils.DEFAULT_CONTEST, config.isContest());
    assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    assertEquals(Utils.DEFAULT_DEBUG, config.isDebug());

    List<String> lines = Files.readAllLines(configFile);
    assertTrue(lines.contains("[defaults]"));
    assertTrue(lines.contains("[shortcuts]"));
  }

  @Test
  public void testLoadReadsValidDefaults() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "blitz = true",
        "timeout = 12",
        "contest = true",
        "size = 10",
        "debug = true",
        "",
        "[shortcuts]",
        "new-game = CTRL+N",
        "load-game = CTRL+L",
        "save-game = CTRL+S",
        "configuration = CTRL+C",
        "info = CTRL+I",
        "quit = CTRL+Q",
        "undo = CTRL+Z",
        "redo = CTRL+Y",
        "pause = P",
        "hint = H"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertTrue(config.isBlitz());
    assertEquals(12, config.getTime());
    assertTrue(config.isContest());
    assertEquals(10, config.getSize());
    assertTrue(config.isDebug());

    assertEquals("CTRL+N", config.getShortcut("new-game"));
    assertEquals("CTRL+L", config.getShortcut("load-game"));
    assertEquals("CTRL+S", config.getShortcut("save-game"));
    assertEquals("CTRL+C", config.getShortcut("configuration"));
    assertEquals("CTRL+I", config.getShortcut("info"));
    assertEquals("CTRL+Q", config.getShortcut("quit"));
    assertEquals("CTRL+Z", config.getShortcut("undo"));
    assertEquals("CTRL+Y", config.getShortcut("redo"));
    assertEquals("P", config.getShortcut("pause"));
    assertEquals("H", config.getShortcut("hint"));
  }

  @Test
  public void testLoadResetsWhenHeaderIsMissing() throws IOException {
    Files.write(configFile, List.of(
        "verbose = true",
        "blitz = true",
        "timeout = 20",
        "contest = true",
        "size = 10",
        "debug = true"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
    assertEquals(Utils.DEFAULT_TIME, config.getTime());
    assertEquals(Utils.DEFAULT_CONTEST, config.isContest());
    assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    assertEquals(Utils.DEFAULT_DEBUG, config.isDebug());

    List<String> lines = Files.readAllLines(configFile);
    assertTrue(lines.contains("[defaults]"));
    assertTrue(lines.contains("[shortcuts]"));
  }

  @Test
  public void testLoadFallsBackForInvalidBooleanAndTimeoutValues() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = maybe",
        "blitz = nope",
        "timeout = abc",
        "contest = invalid",
        "size = 10",
        "debug = ???"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
    assertEquals(Utils.DEFAULT_TIME, config.getTime());
    assertEquals(Utils.DEFAULT_CONTEST, config.isContest());
    assertEquals(10, config.getSize());
    assertEquals(Utils.DEFAULT_DEBUG, config.isDebug());
  }

  @Test
  public void testLoadFallsBackForInvalidAllowedSize() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 9",
        "debug = true"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertFalse(config.isBlitz());
    assertEquals(30, config.getTime());
    assertFalse(config.isContest());
    assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    assertTrue(config.isDebug());
  }

  @Test
  public void testLoadWithNonNumericSizeTriggersGlobalReset() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "blitz = true",
        "timeout = 15",
        "contest = true",
        "size = abc",
        "debug = true"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
    assertEquals(Utils.DEFAULT_TIME, config.getTime());
    assertEquals(Utils.DEFAULT_CONTEST, config.isContest());
    assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    assertEquals(Utils.DEFAULT_DEBUG, config.isDebug());
  }

  @Test
  public void testLoadUsesDefaultsWhenKeysAreMissing() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "size = 12"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
    assertEquals(Utils.DEFAULT_TIME, config.getTime());
    assertEquals(Utils.DEFAULT_CONTEST, config.isContest());
    assertEquals(12, config.getSize());
    assertEquals(Utils.DEFAULT_DEBUG, config.isDebug());
  }

  @Test
  public void testGetShortcutReturnsNullForUnknownAction() {
    ConfigManager config = new ConfigManager();

    assertNull(config.getShortcut("unknown-action"));
  }

  @Test
  public void testSetShortcutUpdatesValueInMemory() {
    ConfigManager config = new ConfigManager();

    config.setShortcut("new-game", "ALT+N");
    config.setShortcut("hint", "CTRL+H");

    assertEquals("ALT+N", config.getShortcut("new-game"));
    assertEquals("CTRL+H", config.getShortcut("hint"));
  }

  @Test
  public void testSaveShortcutsCreatesFileIfMissing() throws IOException {
    ConfigManager config = new ConfigManager();
    config.setShortcut("new-game", "ALT+N");
    config.setShortcut("quit", "ESC");
    config.saveShortcuts();

    assertTrue(Files.exists(configFile));

    List<String> lines = Files.readAllLines(configFile);
    assertTrue(lines.contains("[shortcuts]"));
    assertTrue(lines.contains("new-game = ALT+N"));
    assertTrue(lines.contains("quit = ESC"));
  }

  @Test
  public void testSaveShortcutsReplacesExistingShortcutsSection() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "blitz = false",
        "timeout = 20",
        "contest = false",
        "size = 8",
        "debug = false",
        "",
        "[shortcuts]",
        "new-game = OLD",
        "quit = OLDQ",
        "",
        "[other]",
        "value = test"
    ));
    ConfigManager config = new ConfigManager();
    config.setShortcut("new-game", "CTRL+N");
    config.setShortcut("load-game", "CTRL+L");
    config.setShortcut("save-game", "CTRL+S");
    config.setShortcut("configuration", "CTRL+C");
    config.setShortcut("info", "CTRL+I");
    config.setShortcut("quit", "CTRL+Q");
    config.setShortcut("undo", "CTRL+Z");
    config.setShortcut("redo", "CTRL+Y");
    config.setShortcut("pause", "P");
    config.setShortcut("hint", "H");

    config.saveShortcuts();

    List<String> lines = Files.readAllLines(configFile);

    assertTrue(lines.contains("[defaults]"));
    assertTrue(lines.contains("[other]"));
    assertTrue(lines.contains("value = test"));

    assertTrue(lines.contains("[shortcuts]"));
    assertTrue(lines.contains("new-game = CTRL+N"));
    assertTrue(lines.contains("load-game = CTRL+L"));
    assertTrue(lines.contains("save-game = CTRL+S"));
    assertTrue(lines.contains("configuration = CTRL+C"));
    assertTrue(lines.contains("info = CTRL+I"));
    assertTrue(lines.contains("quit = CTRL+Q"));
    assertTrue(lines.contains("undo = CTRL+Z"));
    assertTrue(lines.contains("redo = CTRL+Y"));
    assertTrue(lines.contains("pause = P"));
    assertTrue(lines.contains("hint = H"));

    assertFalse(lines.contains("new-game = OLD"));
    assertFalse(lines.contains("quit = OLDQ"));
  }

  @Test
  public void testLoadReadsShortcutsSectionOnlyWhenPresent() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 10",
        "contest = false",
        "size = 8",
        "debug = false"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(Utils.DEFAULT_SHORTCUT_NEW_GAME, config.getShortcut("new-game"));
    assertEquals(Utils.DEFAULT_SHORTCUT_LOAD_GAME, config.getShortcut("load-game"));
    assertEquals(Utils.DEFAULT_SHORTCUT_SAVE_GAME, config.getShortcut("save-game"));
    assertEquals(Utils.DEFAULT_SHORTCUT_CONFIGURATION, config.getShortcut("configuration"));
    assertEquals(Utils.DEFAULT_SHORTCUT_INFO, config.getShortcut("info"));
    assertEquals(Utils.DEFAULT_SHORTCUT_QUIT, config.getShortcut("quit"));
    assertEquals(Utils.DEFAULT_SHORTCUT_UNDO, config.getShortcut("undo"));
    assertEquals(Utils.DEFAULT_SHORTCUT_REDO, config.getShortcut("redo"));
    assertEquals(Utils.DEFAULT_SHORTCUT_PAUSE, config.getShortcut("pause"));
    assertEquals(Utils.DEFAULT_SHORTCUT_HINT, config.getShortcut("hint"));
  }

  @Test
  public void testLoadIgnoresUnknownSection() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "",
        "[other]",
        "foo = bar",
        "",
        "[shortcuts]",
        "new-game = CTRL+N"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertEquals("CTRL+N", config.getShortcut("new-game"));
  }

  @Test
  public void testLoadIgnoresMalformedLineInDefaults() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "this line is invalid",
        "timeout = 15",
        "contest = false",
        "debug = true",
        "blitz = false",
        "size = 10"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertEquals(15, config.getTime());
    assertFalse(config.isContest());
    assertTrue(config.isDebug());
    assertFalse(config.isBlitz());
    assertEquals(10, config.getSize());
  }

  @Test
  public void testLoadIgnoresEmptyLinesAndComments() throws IOException {
    Files.write(configFile, List.of(
        "",
        "# comment",
        "[defaults]",
        "",
        "verbose = true",
        "# another comment",
        "blitz = false",
        "timeout = 20",
        "contest = true",
        "size = 10",
        "debug = false"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertFalse(config.isBlitz());
    assertEquals(20, config.getTime());
    assertTrue(config.isContest());
    assertEquals(10, config.getSize());
    assertFalse(config.isDebug());
  }

  @Test
  public void testLoadIgnoresUnknownKeyInDefaults() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = true",
        "mystery = value",
        "blitz = false",
        "timeout = 25",
        "contest = false",
        "size = 8",
        "debug = true"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isVerbose());
    assertFalse(config.isBlitz());
    assertEquals(25, config.getTime());
    assertFalse(config.isContest());
    assertEquals(8, config.getSize());
    assertTrue(config.isDebug());
  }

  @Test
  public void testLoadIgnoresMalformedLineInShortcuts() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 10",
        "contest = false",
        "size = 8",
        "debug = false",
        "",
        "[shortcuts]",
        "new-game = CTRL+N",
        "this line is invalid",
        "quit = CTRL+Q"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals("CTRL+N", config.getShortcut("new-game"));
    assertEquals("CTRL+Q", config.getShortcut("quit"));
  }

  @Test
  public void testSetShortcutWithUnknownActionDoesNotChangeKnownShortcuts() {
    ConfigManager config = new ConfigManager();

    String initialNewGame = config.getShortcut("new-game");
    String initialQuit = config.getShortcut("quit");

    config.setShortcut("unknown-action", "CTRL+X");

    assertEquals(initialNewGame, config.getShortcut("new-game"));
    assertEquals(initialQuit, config.getShortcut("quit"));
    assertNull(config.getShortcut("unknown-action"));
  }

  @Test
  public void testLoadIgnoresUnknownKeyInShortcuts() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 10",
        "contest = false",
        "size = 8",
        "debug = false",
        "",
        "[shortcuts]",
        "new-game = CTRL+N",
        "weird-key = XXX",
        "quit = CTRL+Q"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals("CTRL+N", config.getShortcut("new-game"));
    assertEquals("CTRL+Q", config.getShortcut("quit"));
    assertNull(config.getShortcut("weird-key"));
  }

  @Test
  public void testLoadReadsAiOptionsCorrectly() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false",
        "white-ai = true",
        "black-ai = true",
        "ai-time = 10",
        "white-ai-mode = alphabeta",
        "black-ai-mode = mcts",
        "ai-depth = 7",
        "ai-selection-mode = ML",
        "minimax-scoring = advanced"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(config.isWhiteAi());
    assertTrue(config.isBlackAi());
    assertEquals(10000L, config.getAiTime()); // 10 seconds -> 10000ms
    assertEquals("alphabeta", config.getWhiteAiMode());
    assertEquals("mcts", config.getBlackAiMode());
    assertEquals(7, config.getAiDepth());
    assertEquals("ML", config.getSelectionMode().name());
    assertEquals("advanced", config.getMinimaxScoring());
  }

  @Test
  public void testLoadUsesDefaultsForMissingAiOptions() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertFalse(config.isWhiteAi());
    assertFalse(config.isBlackAi());
    assertEquals(5000L, config.getAiTime()); // DEFAULT_MAX_TIME_MS = 5000ms
    assertEquals("minimax", config.getWhiteAiMode());
    assertEquals("minimax", config.getBlackAiMode());
    assertEquals(5, config.getAiDepth()); // DEFAULT_DEPTH = 5
    assertEquals("UCT", config.getSelectionMode().name()); // DEFAULT_SELECTION_MODE
    assertEquals("max", config.getMinimaxScoring());
  }

  @Test
  public void testLoadFallsBackForInvalidAiBooleans() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false",
        "white-ai = maybe",
        "black-ai = nope"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertFalse(config.isWhiteAi());
    assertFalse(config.isBlackAi());
  }

  @Test
  public void testLoadFallsBackForInvalidAiTime() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false",
        "ai-time = abc"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(5000L, config.getAiTime()); // Falls back to DEFAULT
  }

  @Test
  public void testLoadFallsBackForInvalidAiDepth() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false",
        "ai-depth = xyz"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals(5, config.getAiDepth()); // Falls back to DEFAULT
  }

  @Test
  public void testLoadFallsBackForInvalidSelectionMode() throws IOException {
    Files.write(configFile, List.of(
        "[defaults]",
        "verbose = false",
        "blitz = false",
        "timeout = 30",
        "contest = false",
        "size = 8",
        "debug = false",
        "ai-selection-mode = INVALID"
    ));
    ConfigManager config = new ConfigManager();
    config.load();

    assertEquals("UCT", config.getSelectionMode().name()); // Falls back to DEFAULT
  }

  @Test
  public void testCreateDefaultConfigIncludesAiOptions() throws IOException {
    ConfigManager config = new ConfigManager();
    config.load();

    assertTrue(Files.exists(configFile));
    List<String> lines = Files.readAllLines(configFile);

    assertTrue(lines.contains("white-ai = " + Utils.DEFAULT_WHITE_AI));
    assertTrue(lines.contains("black-ai = " + Utils.DEFAULT_BLACK_AI));
    assertTrue(lines.contains("ai-time = 5")); // DEFAULT_MAX_TIME_MS / 1000
    assertTrue(lines.contains("white-ai-mode = " + Utils.DEFAULT_AI_MODE));
    assertTrue(lines.contains("black-ai-mode = " + Utils.DEFAULT_AI_MODE));
    assertTrue(lines.contains("ai-depth = 5")); // DEFAULT_DEPTH
    assertTrue(lines.contains("minimax-scoring = " + Utils.DEFAULT_MINIMAX_SCORING));
  }
}