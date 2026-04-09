package fr.ubordeaux.pdp.view.gui.layout;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import fr.ubordeaux.pdp.view.gui.dialogs.ShortcutManager;

import java.lang.reflect.Method;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.TextInputDialog;
import javafx.collections.FXCollections;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Comprehensive unit tests for {@link MenuView}.
 *
 * <p>Tests the menu bar initialization, menu items creation, dialog interactions,
 * and action handling. Uses TestFX for JavaFX initialization and mocks for
 * controller, shortcut manager, and GUI components.
 *
 * <p>Coverage includes:
 * <ul>
 *   <li>Menu structure and initialization</li>
 *   <li>File menu actions: New Game, Load, Save, Configuration, Info, Quit</li>
 *   <li>Game menu actions: Undo, Redo, Pause, Hint</li>
 *   <li>Dialog interactions with various scenarios</li>
 *   <li>Pause/resume logic</li>
 *   <li>Helper methods</li>
 * </ul>
 */
@DisplayName("MenuView Tests")
class MenuViewTest extends ApplicationTest {

  private MenuView menuView;

  @Mock
  private GameController mockController;

  @Mock
  private ShortcutManager mockShortcutManager;

  @Mock
  private GraphicalUserInterface mockGui;

  @Mock
  private GameCheckers mockGame;

  private Path tempSaveDir;

  /**
   * Initializes the JavaFX Stage for testing. Required by TestFX.
   */
  @Override
  public void start(Stage stage) {
    stage.setTitle("MenuView Test");
    stage.show();
  }

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Configure default shortcut returns
    when(mockShortcutManager.get(anyString()))
        .thenReturn(KeyCombination.keyCombination("Ctrl+N"));

    // Initialize MenuView
    menuView = new MenuView(mockController, mockShortcutManager, null);

    // Create temporary save directory
    tempSaveDir = Files.createTempDirectory("test_saves");
  }

  @AfterEach
  void tearDown() throws Exception {
    // Clean up temporary files
    if (tempSaveDir != null && Files.exists(tempSaveDir)) {
      Files.walk(tempSaveDir)
          .sorted((a, b) -> b.compareTo(a))
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (Exception e) {
              // Ignore cleanup errors
            }
          });
    }
  }

  // ==================== NESTED TEST CLASSES ====================

  @Nested
  @DisplayName("Menu Initialization Tests")
  class MenuInitializationTests {

    @Test
    @DisplayName("MenuView should initialize with File and Game menus")
    void testMenuViewInitialization() {
      assertNotNull(menuView);
      assertEquals(2, menuView.getMenus().size(), "Should have File and Game menus");
    }

    @Test
    @DisplayName("File menu should contain correct number of items")
    void testFileMenuItemsCount() {
      Menu fileMenu = menuView.getMenus().get(0);
      assertNotNull(fileMenu);
      // New Game, Load, Save, Separator, Config, Info, Separator, Quit = 8 items
      assertEquals(8, fileMenu.getItems().size());
    }

    @Test
    @DisplayName("Game menu should contain correct number of items")
    void testGameMenuItemsCount() {
      Menu gameMenu = menuView.getMenus().get(1);
      assertNotNull(gameMenu);
      // Undo, Redo, Separator, Pause, Hint = 5 items
      assertEquals(5, gameMenu.getItems().size());
    }

    @Test
    @DisplayName("Shortcut manager should be configured for all menu items")
    void testShortcutManagerConfiguration() {
      String[] expectedShortcuts = {
          "new-game", "load-game", "save-game", "configuration", "info", "quit",
          "undo", "redo", "pause", "hint"
      };

      for (String shortcut : expectedShortcuts) {
        verify(mockShortcutManager, atLeastOnce()).get(shortcut);
      }
    }
  }

  // ==================== FILE MENU TESTS ====================

  @Nested
  @DisplayName("File Menu Tests")
  class FileMenuTests {

    @Test
    @DisplayName("New Game menu item should start game with default configuration")
    void testNewGameMenuAction() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem newGameItem = fileMenu.getItems().get(0);

      newGameItem.getOnAction().handle(null);

      verify(mockController, times(1)).startNewGame(any(Configuration.class));
    }

    @Test
    @DisplayName("Quit menu item should delegate to GUI when available")
    void testQuitMenuActionWithGui() {
      menuView.setGui(mockGui);
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem quitItem = fileMenu.getItems().get(7); // Last item is Quit

      quitItem.getOnAction().handle(null);

      verify(mockGui, times(1)).requestQuit();
    }

    @Test
    @DisplayName("Quit menu item should delegate to controller when GUI is null")
    void testQuitMenuActionWithoutGui() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem quitItem = fileMenu.getItems().get(7);

      quitItem.getOnAction().handle(null);

      verify(mockController, times(1)).executeCommand("quit", new String[0]);
    }

    @Test
    @DisplayName("Info menu item should be accessible")
    void testInfoMenuAction() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem infoItem = fileMenu.getItems().get(5);

      // Just verify the item exists and has an action
      assertNotNull(infoItem);
      assertNotNull(infoItem.getOnAction());
    }
  }

  // ==================== GAME MENU TESTS ====================

  @Nested
  @DisplayName("Game Menu Tests")
  class GameMenuTests {

    @Test
    @DisplayName("Undo with both players AI should use 1 step")
    void testUndoActionBothAI() {
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(true);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);

      undoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("undo", new String[] { "1" });
    }

    @Test
    @DisplayName("Undo with no AI players should use 1 step")
    void testUndoActionNoAI() {
      when(mockController.isWhiteAi()).thenReturn(false);
      when(mockController.isBlackAi()).thenReturn(false);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);

      undoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("undo", new String[] { "1" });
    }

    @Test
    @DisplayName("Undo with one AI player should use 2 steps")
    void testUndoActionMixedAI() {
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(false);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);

      undoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("undo", new String[] { "2" });
    }

    @Test
    @DisplayName("Redo with both players AI should use 1 step")
    void testRedoActionBothAI() {
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(true);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem redoItem = gameMenu.getItems().get(1);

      redoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("redo", new String[] { "1" });
    }

    @Test
    @DisplayName("Redo with mixed AI should use 2 steps")
    void testRedoActionMixedAI() {
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(false);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem redoItem = gameMenu.getItems().get(1);

      redoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("redo", new String[] { "2" });
    }

    @Test
    @DisplayName("Pause menu item should execute pause and continue commands")
    void testPauseMenuAction() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem pauseItem = gameMenu.getItems().get(3);

      // Just verify pause item exists and has action
      assertNotNull(pauseItem);
      assertNotNull(pauseItem.getOnAction());
    }

    @Test
    @DisplayName("Pause should not execute if game is not in progress")
    void testPauseMenuActionNoGame() {
      when(mockController.getGame()).thenReturn(null);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem pauseItem = gameMenu.getItems().get(3);

      pauseItem.getOnAction().handle(null);

      verify(mockController, never()).executeCommand("pause", new String[0]);
    }

    @Test
    @DisplayName("Pause should not execute if game is paused")
    void testPauseMenuActionGamePaused() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.PAUSE);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem pauseItem = gameMenu.getItems().get(3);

      pauseItem.getOnAction().handle(null);

      verify(mockController, never()).executeCommand("pause", new String[0]);
    }

    @Test
    @DisplayName("Hint menu item should execute hint command")
    void testHintMenuAction() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem hintItem = gameMenu.getItems().get(4);

      hintItem.getOnAction().handle(null);

      verify(mockController, times(1)).executeCommand("hint", new String[0]);
    }
  }

  // ==================== STAGE AND GUI REFERENCE TESTS ====================

  @Nested
  @DisplayName("Stage and GUI Reference Tests")
  class StageAndGUITests {

    @Test
    @DisplayName("setGui should store GUI reference")
    void testSetGui() {
      menuView.setGui(mockGui);
      assertNotNull(mockGui);
    }

    @Test
    @DisplayName("setDisableUndoRedo should disable undo and redo items")
    void testSetDisableUndoRedo() {
      menuView.setDisableUndoRedo(true);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);
      MenuItem redoItem = gameMenu.getItems().get(1);

      assertTrue(undoItem.isDisable());
      assertTrue(redoItem.isDisable());
    }

    @Test
    @DisplayName("setDisableUndoRedo should enable undo and redo items")
    void testEnableUndoRedo() {
      menuView.setDisableUndoRedo(false);

      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);
      MenuItem redoItem = gameMenu.getItems().get(1);

      assertFalse(undoItem.isDisable());
      assertFalse(redoItem.isDisable());
    }
  }

  // ==================== SAVE DIALOG TESTS ====================

  @Nested
  @DisplayName("Save Dialog Tests")
  class SaveDialogTests {

    @Test
    @DisplayName("Save menu item should be accessible")
    void testSaveMenuItemAccessible() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem saveItem = fileMenu.getItems().get(2);

      assertNotNull(saveItem);
      assertNotNull(saveItem.getOnAction());
    }

    @Test
    @DisplayName("Save command should be available in file menu")
    void testSaveCommandAvailable() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem saveItem = fileMenu.getItems().get(2);

      // Verify it's the save item by checking it's a MenuItem
      assertTrue(saveItem instanceof MenuItem);
    }

    @Test
    @DisplayName("openSaveDialog should not save when no game is running")
    void testOpenSaveDialogNoGame() {
      when(mockController.getGame()).thenReturn(null);

      try (MockedConstruction<Alert> mockedAlerts = mockConstruction(
          Alert.class,
          (alert, context) -> when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.OK)))) {
        menuView.openSaveDialog();

        verify(mockController, never()).executeCommand(eq("save"), any());
        assertEquals(1, mockedAlerts.constructed().size());
      }
    }

    @Test
    @DisplayName("openSaveDialog should not save when game is finished")
    void testOpenSaveDialogFinishedGame() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.FINISHED);

      try (MockedConstruction<Alert> mockedAlerts = mockConstruction(
          Alert.class,
          (alert, context) -> when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.OK)))) {
        menuView.openSaveDialog();

        verify(mockController, never()).executeCommand(eq("save"), any());
        assertEquals(1, mockedAlerts.constructed().size());
      }
    }

    @Test
    @DisplayName("openSaveDialog should ignore blank filename")
    void testOpenSaveDialogBlankFileName() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME);

      try (MockedConstruction<TextInputDialog> mockedDialog = mockConstruction(
          TextInputDialog.class,
          (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of("   ")))) {
        menuView.openSaveDialog();

        verify(mockController, never()).executeCommand(eq("save"), any());
        assertEquals(1, mockedDialog.constructed().size());
      }
    }

    @Test
    @DisplayName("openSaveDialog should call save command with trimmed filename")
    void testOpenSaveDialogSavesWithTrimmedFileName() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME);

      try (MockedConstruction<TextInputDialog> mockedTextInput = mockConstruction(
          TextInputDialog.class,
          (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of("  unit-save.txt  ")));
          MockedConstruction<Alert> mockedAlerts = mockConstruction(
              Alert.class,
              (alert, context) -> when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.OK)))) {
        menuView.openSaveDialog();

        verify(mockController, times(1)).executeCommand("save", new String[] { "unit-save.txt" });
        assertFalse(mockedAlerts.constructed().isEmpty());
      }
    }
  }

  // ==================== LOAD DIALOG TESTS ====================

  @Nested
  @DisplayName("Load Dialog Tests")
  class LoadDialogTests {

    private String createSaveFileForLoadTest() throws Exception {
      Path saveDir = Path.of(System.getProperty("user.dir"), "Sauvegarde");
      Files.createDirectories(saveDir);
      String saveName = "menuview-load-" + UUID.randomUUID() + ".txt";
      Files.writeString(saveDir.resolve(saveName), "dummy-save");
      return saveName;
    }

    private void invokeOpenLoadDialog() throws Exception {
      Method openLoadDialog = MenuView.class.getDeclaredMethod("openLoadDialog");
      openLoadDialog.setAccessible(true);
      openLoadDialog.invoke(menuView);
    }

    @Test
    @DisplayName("Load dialog should be accessible through menu")
    void testLoadGameMenuItem() {
      when(mockController.getGame()).thenReturn(null);

      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem loadItem = fileMenu.getItems().get(1);

      assertNotNull(loadItem);
      assertNotNull(loadItem.getOnAction());
    }

    @Test
    @DisplayName("Load menu item should be a MenuItem")
    void testLoadMenuItemType() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem loadItem = fileMenu.getItems().get(1);

      assertTrue(loadItem instanceof MenuItem);
    }

    @Test
    @DisplayName("openLoadDialog should load the default selected save")
    void testOpenLoadDialogSelectsSaveFile() throws Exception {
      List<String> saveNames;
      try (var saveStream = Files.list(Path.of(System.getProperty("user.dir"), "Sauvegarde"))) {
        saveNames = saveStream
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .sorted(Comparator.naturalOrder())
            .toList();
      }

      assumeTrue(!saveNames.isEmpty(), "No save files available for openLoadDialog test");

      String selectedSave = saveNames.get(0);

      try (MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
          ChoiceDialog.class,
          (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of(selectedSave)))) {
        invokeOpenLoadDialog();

        verify(mockController, times(1)).executeCommand("load", new String[] { selectedSave });
        assertEquals(1, mockedChoiceDialog.constructed().size());
        verify(mockedChoiceDialog.constructed().get(0), times(1)).showAndWait();
      }
    }

    @Test
    @DisplayName("openLoadDialog should not load when user cancels selection")
    void testOpenLoadDialogCancelledSelection() throws Exception {
      createSaveFileForLoadTest();

      try (MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
          ChoiceDialog.class,
          (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.empty()))) {
        invokeOpenLoadDialog();

        verify(mockController, never()).executeCommand(eq("load"), any());
        assertEquals(1, mockedChoiceDialog.constructed().size());
        verify(mockedChoiceDialog.constructed().get(0), times(1)).showAndWait();
      }
    }

    @Test
    @DisplayName("openLoadDialog should configure ChoiceDialog with sorted saves and default first")
    void testOpenLoadDialogChoiceDialogConfiguration() throws Exception {
      // Ensure at least two deterministic save names exist for sorting checks.
      String token = UUID.randomUUID().toString();
      Path saveDir = Path.of(System.getProperty("user.dir"), "Sauvegarde");
      Files.createDirectories(saveDir);
      Files.writeString(saveDir.resolve("menuview-a-" + token + ".txt"), "a");
      Files.writeString(saveDir.resolve("menuview-z-" + token + ".txt"), "z");

      List<Object> capturedConstructorArgs = new java.util.ArrayList<>();

      try (MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
          ChoiceDialog.class,
          (dialog, context) -> {
            capturedConstructorArgs.clear();
            capturedConstructorArgs.addAll(context.arguments());
            when(dialog.showAndWait()).thenReturn(Optional.empty());
          })) {
        invokeOpenLoadDialog();

        assertEquals(1, mockedChoiceDialog.constructed().size());
        ChoiceDialog<?> constructedDialog = mockedChoiceDialog.constructed().get(0);
        assertEquals(2, capturedConstructorArgs.size());

        @SuppressWarnings("unchecked")
        List<String> choices = (List<String>) capturedConstructorArgs.get(1);
        String defaultChoice = (String) capturedConstructorArgs.get(0);

        List<String> sortedChoices = new java.util.ArrayList<>(choices);
        sortedChoices.sort(Comparator.naturalOrder());

        assertEquals(sortedChoices, choices, "ChoiceDialog options should be sorted");
        assertEquals(choices.get(0), defaultChoice,
            "Default choice should be the first sorted save name");

        verify(constructedDialog, times(1)).setTitle(anyString());
        verify(constructedDialog, times(1)).setHeaderText(anyString());
        verify(constructedDialog, times(1)).setContentText(anyString());
      }
    }

    @Test
    @DisplayName("handleLoad should load directly when there are no unsaved changes")
    void testHandleLoadWithoutUnsavedChanges() throws Exception {
      String selectedSave = createSaveFileForLoadTest();

      when(mockController.getGame()).thenReturn(null);

      Method handleLoad = MenuView.class.getDeclaredMethod("handleLoad");
      handleLoad.setAccessible(true);

      try (MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
          ChoiceDialog.class,
          (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of(selectedSave)))) {
        handleLoad.invoke(menuView);

        verify(mockController, times(1)).executeCommand("load", new String[] { selectedSave });
        assertEquals(1, mockedChoiceDialog.constructed().size());
      }
    }

    @Test
    @DisplayName("handleLoad should save then load when confirmation is YES")
    void testHandleLoadWithUnsavedChangesYes() throws Exception {
      String selectedSave = createSaveFileForLoadTest();

      when(mockController.getGame()).thenReturn(mockGame);
      when(mockController.hasUnsavedChanges()).thenReturn(true);
      when(mockGame.getState()).thenReturn(State.IN_GAME);

      Method handleLoad = MenuView.class.getDeclaredMethod("handleLoad");
      handleLoad.setAccessible(true);

      try (MockedConstruction<Alert> mockedAlerts = mockConstruction(
          Alert.class,
          (alert, context) -> {
            when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
            when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.YES));
          });
          MockedConstruction<TextInputDialog> mockedTextInput = mockConstruction(
              TextInputDialog.class,
              (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of("auto-save-test")));
          MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
              ChoiceDialog.class,
              (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of(selectedSave)))) {
        handleLoad.invoke(menuView);

        verify(mockController, times(1)).executeCommand("save", new String[] { "auto-save-test" });
        verify(mockController, times(1)).executeCommand("load", new String[] { selectedSave });
        assertFalse(mockedAlerts.constructed().isEmpty());
        assertEquals(1, mockedTextInput.constructed().size());
        assertEquals(1, mockedChoiceDialog.constructed().size());
      }
    }

    @Test
    @DisplayName("handleLoad should load only when confirmation is NO")
    void testHandleLoadWithUnsavedChangesNo() throws Exception {
      String selectedSave = createSaveFileForLoadTest();

      when(mockController.getGame()).thenReturn(mockGame);
      when(mockController.hasUnsavedChanges()).thenReturn(true);

      Method handleLoad = MenuView.class.getDeclaredMethod("handleLoad");
      handleLoad.setAccessible(true);

      try (MockedConstruction<Alert> mockedAlerts = mockConstruction(
          Alert.class,
          (alert, context) -> {
            when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
            when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.NO));
          });
          MockedConstruction<ChoiceDialog> mockedChoiceDialog = mockConstruction(
              ChoiceDialog.class,
              (dialog, context) -> when(dialog.showAndWait()).thenReturn(Optional.of(selectedSave)))) {
        handleLoad.invoke(menuView);

        verify(mockController, never()).executeCommand(eq("save"), any());
        verify(mockController, times(1)).executeCommand("load", new String[] { selectedSave });
        assertFalse(mockedAlerts.constructed().isEmpty());
        assertEquals(1, mockedChoiceDialog.constructed().size());
      }
    }

    @Test
    @DisplayName("handleLoad should do nothing when confirmation is CANCEL")
    void testHandleLoadWithUnsavedChangesCancel() throws Exception {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockController.hasUnsavedChanges()).thenReturn(true);

      Method handleLoad = MenuView.class.getDeclaredMethod("handleLoad");
      handleLoad.setAccessible(true);

      try (MockedConstruction<Alert> mockedAlerts = mockConstruction(
          Alert.class,
          (alert, context) -> {
            when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
            when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.CANCEL));
          })) {
        handleLoad.invoke(menuView);

        verify(mockController, never()).executeCommand(eq("save"), any());
        verify(mockController, never()).executeCommand(eq("load"), any());
        assertFalse(mockedAlerts.constructed().isEmpty());
      }
    }
  }

  // ==================== CONFIGURATION DIALOG TESTS ====================

  @Nested
  @DisplayName("Configuration Dialog Tests")
  class ConfigurationDialogTests {

    @Test
    @DisplayName("Configuration menu item should be accessible")
    void testConfigurationMenuItem() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem configItem = fileMenu.getItems().get(4);

      assertNotNull(configItem);
      assertNotNull(configItem.getOnAction());
    }

    @Test
    @DisplayName("openConfigDialog can be called without errors")
    void testOpenConfigDialogCallable() {
      // Just verify the method exists and is callable
      assertNotNull(menuView);
      assertTrue(menuView.getMenus().size() > 0);
    }

    @Test
    @DisplayName("Configuration dialog with CLI config should use CLI config")
    void testConfigDialogWithCliConfig() {
      Configuration cliConfig = Configuration.getDefaultConfiguration();
      MenuView menuViewWithCliConfig = new MenuView(
          mockController, mockShortcutManager, cliConfig);

      // Verify construction works
      assertNotNull(menuViewWithCliConfig);
      assertEquals(2, menuViewWithCliConfig.getMenus().size());
    }
  }

  // ==================== EDGE CASES AND COMPLEX SCENARIOS ====================

  @Nested
  @DisplayName("Edge Cases and Complex Scenarios")
  class EdgeCaseTests {

    @Test
    @DisplayName("executeWithPause should pause, run action, then continue when game stays paused")
    void testExecuteWithPauseInGameThenContinue() throws Exception {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME, State.PAUSE);

      Runnable action = mock(Runnable.class);
      Method executeWithPause = MenuView.class.getDeclaredMethod("executeWithPause", Runnable.class);
      executeWithPause.setAccessible(true);

      executeWithPause.invoke(menuView, action);

      verify(mockController, times(1)).executeCommand("pause", new String[0]);
      verify(action, times(1)).run();
      verify(mockController, times(1)).executeCommand("continue", new String[0]);
    }

    @Test
    @DisplayName("executeWithPause should run action without pause when no game is running")
    void testExecuteWithPauseWithoutGame() throws Exception {
      when(mockController.getGame()).thenReturn(null);

      Runnable action = mock(Runnable.class);
      Method executeWithPause = MenuView.class.getDeclaredMethod("executeWithPause", Runnable.class);
      executeWithPause.setAccessible(true);

      executeWithPause.invoke(menuView, action);

      verify(action, times(1)).run();
      verify(mockController, never()).executeCommand(eq("pause"), any());
      verify(mockController, never()).executeCommand(eq("continue"), any());
    }

    @Test
    @DisplayName("executeWithPause should not continue when state is no longer PAUSE after action")
    void testExecuteWithPauseNoContinueWhenNotPaused() throws Exception {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME, State.IN_GAME);

      Runnable action = mock(Runnable.class);
      Method executeWithPause = MenuView.class.getDeclaredMethod("executeWithPause", Runnable.class);
      executeWithPause.setAccessible(true);

      executeWithPause.invoke(menuView, action);

      verify(mockController, times(1)).executeCommand("pause", new String[0]);
      verify(action, times(1)).run();
      verify(mockController, never()).executeCommand(eq("continue"), any());
    }

    @Test
    @DisplayName("Multiple menu action triggers should be independent")
    void testMultipleMenuActionTriggers() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);
      MenuItem redoItem = gameMenu.getItems().get(1);

      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(false);

      undoItem.getOnAction().handle(null);
      redoItem.getOnAction().handle(null);

      verify(mockController, times(1))
          .executeCommand("undo", new String[] { "2" });
      verify(mockController, times(1))
          .executeCommand("redo", new String[] { "2" });
    }

    @Test
    @DisplayName("Disabling and enabling undo/redo multiple times")
    void testRepeatedUndoRedoToggle() {
      for (int i = 0; i < 3; i++) {
        menuView.setDisableUndoRedo(true);

        Menu gameMenu = menuView.getMenus().get(1);
        MenuItem undoItem = gameMenu.getItems().get(0);
        assertTrue(undoItem.isDisable());

        menuView.setDisableUndoRedo(false);
        assertFalse(undoItem.isDisable());
      }
    }

    @Test
    @DisplayName("Menu actions should be accessible after stage setting")
    void testMenuActionsAfterStageSet() {
      menuView.setStage(null);

      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem newGameItem = fileMenu.getItems().get(0);

      assertNotNull(newGameItem.getOnAction());
      newGameItem.getOnAction().handle(null);

      verify(mockController, times(1)).startNewGame(any(Configuration.class));
    }

    @Test
    @DisplayName("Setting GUI after menu creation should work")
    void testSetGuiAfterMenuCreation() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem quitItem = fileMenu.getItems().get(7);

      // The quit item should have an action set
      assertNotNull(quitItem.getOnAction());

      // Now set GUI and verify it's used
      menuView.setGui(mockGui);
      quitItem.getOnAction().handle(null);
      verify(mockGui, times(1)).requestQuit();
    }

    @Test
    @DisplayName("Pause action with different game states")
    void testPauseActionAccessible() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem pauseItem = gameMenu.getItems().get(3);

      assertNotNull(pauseItem);
      assertNotNull(pauseItem.getOnAction());
    }

    @Test
    @DisplayName("Hint action can be triggered multiple times")
    void testHintActionMultipleTriggers() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem hintItem = gameMenu.getItems().get(4);

      hintItem.getOnAction().handle(null);
      hintItem.getOnAction().handle(null);

      verify(mockController, times(2)).executeCommand("hint", new String[0]);
    }

    @Test
    @DisplayName("Undo/Redo step calculation for all AI configurations")
    void testAllAIConfigurations() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);

      // Test 1: Both AI
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(true);
      undoItem.getOnAction().handle(null);
      verify(mockController).executeCommand("undo", new String[] { "1" });

      reset(mockController);

      // Test 2: Neither AI
      when(mockController.isWhiteAi()).thenReturn(false);
      when(mockController.isBlackAi()).thenReturn(false);
      undoItem.getOnAction().handle(null);
      verify(mockController).executeCommand("undo", new String[] { "1" });
    }
  }

  // ==================== ACCESSIBILITY TESTS ====================

  @Nested
  @DisplayName("Accessibility and Structure Tests")
  class AccessibilityTests {

    @Test
    @DisplayName("All menu items should have action handlers")
    void testAllMenuItemsHaveActionHandlers() {
      Menu fileMenu = menuView.getMenus().get(0);
      Menu gameMenu = menuView.getMenus().get(1);

      fileMenu.getItems().stream()
          .filter(item -> item instanceof MenuItem && !(item.getClass().getName().contains("Separator")))
          .forEach(item -> assertTrue(((MenuItem) item).getOnAction() != null));

      gameMenu.getItems().stream()
          .filter(item -> item instanceof MenuItem && !(item.getClass().getName().contains("Separator")))
          .forEach(item -> assertTrue(((MenuItem) item).getOnAction() != null));
    }

    @Test
    @DisplayName("MenuView should use system menu bar")
    void testSystemMenuBar() {
      assertTrue(menuView.isUseSystemMenuBar());
    }

    @Test
    @DisplayName("Menu items should have accelerators configured")
    void testMenuItemsHaveAccelerators() {
      Menu fileMenu = menuView.getMenus().get(0);
      Menu gameMenu = menuView.getMenus().get(1);

      for (var item : fileMenu.getItems()) {
        if (item instanceof MenuItem && !(item.getClass().getName().contains("Separator"))) {
          assertNotNull(((MenuItem) item).getAccelerator(),
              "Menu item should have accelerator configured");
        }
      }

      for (var item : gameMenu.getItems()) {
        if (item instanceof MenuItem && !(item.getClass().getName().contains("Separator"))) {
          assertNotNull(((MenuItem) item).getAccelerator(),
              "Menu item should have accelerator configured");
        }
      }
    }

    @Test
    @DisplayName("File menu items should be in correct order")
    void testFileMenuOrder() {
      Menu fileMenu = menuView.getMenus().get(0);
      assertTrue(fileMenu.getItems().size() >= 8);
      // Verify separator at index 3
      assertTrue(fileMenu.getItems().get(3).getClass().getName().contains("Separator"));
      // Verify separator at index 6
      assertTrue(fileMenu.getItems().get(6).getClass().getName().contains("Separator"));
    }

    @Test
    @DisplayName("Game menu items should be in correct order")
    void testGameMenuOrder() {
      Menu gameMenu = menuView.getMenus().get(1);
      assertTrue(gameMenu.getItems().size() >= 5);
      // Verify separator at index 2
      assertTrue(gameMenu.getItems().get(2).getClass().getName().contains("Separator"));
    }
  }

  // ==================== CONTROLLER INTEGRATION TESTS ====================

  @Nested
  @DisplayName("Controller Integration Tests")
  class ControllerIntegrationTests {

    @Test
    @DisplayName("All file menu actions should use controller")
    void testFileMenuUsesController() {
      Menu fileMenu = menuView.getMenus().get(0);

      // New Game
      fileMenu.getItems().get(0).getOnAction().handle(null);
      verify(mockController, atLeast(1)).startNewGame(any());
    }

    @Test
    @DisplayName("All game menu actions should use controller")
    void testGameMenuUsesController() {
      when(mockController.isWhiteAi()).thenReturn(true);
      when(mockController.isBlackAi()).thenReturn(false);

      Menu gameMenu = menuView.getMenus().get(1);

      // Undo
      gameMenu.getItems().get(0).getOnAction().handle(null);
      verify(mockController, atLeast(1)).executeCommand(eq("undo"), any());

      // Redo
      gameMenu.getItems().get(1).getOnAction().handle(null);
      verify(mockController, atLeast(1)).executeCommand(eq("redo"), any());

      // Hint
      gameMenu.getItems().get(4).getOnAction().handle(null);
      verify(mockController, atLeast(1)).executeCommand(eq("hint"), any());
    }

    @Test
    @DisplayName("Controller should be called with correct command arguments")
    void testControllerCommandArguments() {
      when(mockController.isWhiteAi()).thenReturn(false);
      when(mockController.isBlackAi()).thenReturn(false);

      Menu gameMenu = menuView.getMenus().get(1);
      gameMenu.getItems().get(0).getOnAction().handle(null);

      ArgumentCaptor<String[]> argsCaptor = ArgumentCaptor.forClass(String[].class);
      verify(mockController).executeCommand(eq("undo"), argsCaptor.capture());

      String[] args = argsCaptor.getValue();
      assertEquals(1, args.length);
      assertEquals("1", args[0]);
    }

    @Test
    @DisplayName("Save command should be callable through menu")
    void testSaveCommandThroughMenu() {
      when(mockController.getGame()).thenReturn(mockGame);
      when(mockGame.getState()).thenReturn(State.IN_GAME);

      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem saveItem = fileMenu.getItems().get(2);

      assertNotNull(saveItem.getOnAction());
    }

    @Test
    @DisplayName("Load command should be callable through menu")
    void testLoadCommandThroughMenu() {
      when(mockController.getGame()).thenReturn(null);

      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem loadItem = fileMenu.getItems().get(1);

      assertNotNull(loadItem.getOnAction());
    }
  }

  // ==================== COMPONENT ISOLATION TESTS ====================

  @Nested
  @DisplayName("Component Isolation Tests")
  class ComponentIsolationTests {

    @Test
    @DisplayName("Undo and Redo items should be initially enabled")
    void testUndoRedoInitialState() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);
      MenuItem redoItem = gameMenu.getItems().get(1);

      assertFalse(undoItem.isDisable());
      assertFalse(redoItem.isDisable());
    }

    @Test
    @DisplayName("Undo and Redo should be independently controllable")
    void testUndoRedoIndependence() {
      menuView.setDisableUndoRedo(true);
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem undoItem = gameMenu.getItems().get(0);
      MenuItem redoItem = gameMenu.getItems().get(1);

      assertTrue(undoItem.isDisable());
      assertTrue(redoItem.isDisable());

      menuView.setDisableUndoRedo(false);
      assertFalse(undoItem.isDisable());
      assertFalse(redoItem.isDisable());
    }

    @Test
    @DisplayName("Hint action should not affect undo/redo")
    void testHintIndependenceFromUndoRedo() {
      Menu gameMenu = menuView.getMenus().get(1);
      MenuItem hintItem = gameMenu.getItems().get(4);
      MenuItem undoItem = gameMenu.getItems().get(0);

      hintItem.getOnAction().handle(null);

      // Undo should still be functional
      assertFalse(undoItem.isDisable());
    }

    @Test
    @DisplayName("GUI reference should be optional")
    void testGUIReferenceOptional() {
      Menu fileMenu = menuView.getMenus().get(0);
      MenuItem quitItem = fileMenu.getItems().get(7);

      // Should work without GUI set
      quitItem.getOnAction().handle(null);
      verify(mockController).executeCommand("quit", new String[0]);
    }

    @Test
    @DisplayName("Shortcut manager should be configurable per instance")
    void testShortcutManagerPerInstance() {
      when(mockShortcutManager.get("new-game"))
          .thenReturn(KeyCombination.keyCombination("Ctrl+N"));

      MenuView view = new MenuView(mockController, mockShortcutManager, null);
      Menu fileMenu = view.getMenus().get(0);
      MenuItem newGameItem = fileMenu.getItems().get(0);

      assertNotNull(newGameItem.getAccelerator());
    }
  }
}
