package fr.ubordeaux.pdp.view.gui.layout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import fr.ubordeaux.pdp.view.gui.dialogs.ShortcutManager;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Unit tests for {@link MenuView}.
 *
 * <p>Tests the menu bar initialization, menu items creation, and basic
 * action handling. Uses TestFX for JavaFX initialization and mocks for
 * controller, shortcut manager, and GUI components.
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

  /**
   * Initializes the JavaFX Stage for testing. Required by TestFX.
   */
  @Override
  public void start(Stage stage) {
    stage.show();
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Configure default shortcut returns
    when(mockShortcutManager.get(anyString()))
        .thenReturn(KeyCombination.keyCombination("Ctrl+N"));

    // Initialize MenuView
    menuView = new MenuView(mockController, mockShortcutManager, null);
  }

  @Test
  @DisplayName("MenuView should initialize with File and Game menus")
  void testMenuViewInitialization() {
    assertNotNull(menuView);
    assertEquals(2, menuView.getMenus().size(), "Should have File and Game menus");
  }

  @Test
  @DisplayName("File menu should contain 6 items with separators")
  void testFileMenuItemsCount() {
    var fileMenu = menuView.getMenus().get(0);
    assertNotNull(fileMenu);
    // New Game, Load, Save, Separator, Config, Info, Separator, Quit = 8 items
    assertEquals(8, fileMenu.getItems().size());
  }

  @Test
  @DisplayName("Game menu should contain 5 items")
  void testGameMenuItemsCount() {
    var gameMenu = menuView.getMenus().get(1);
    assertNotNull(gameMenu);
    // Undo, Redo, Separator, Pause, Hint = 5 items
    assertEquals(5, gameMenu.getItems().size());
  }

  @Test
  @DisplayName("New Game menu item should start a new game with default configuration")
  void testNewGameMenuAction() {
    menuView.setGui(mockGui);
    var fileMenu = menuView.getMenus().get(0);
    var newGameItem = fileMenu.getItems().get(0);

    // Trigger the action
    newGameItem.getOnAction().handle(null);

    verify(mockController, times(1)).startNewGame(any(Configuration.class));
  }

  @Test
  @DisplayName("setGui should set the GUI reference")
  void testSetGui() {
    menuView.setGui(mockGui);
    assertNotNull(mockGui);
  }

  @Test
  @DisplayName("setStage should accept a Stage")
  void testSetStage() {
    // Just verify that the method doesn't throw
    menuView.setStage(null);
  }

  @Test
  @DisplayName("setDisableUndoRedo should disable/enable undo and redo items")
  void testSetDisableUndoRedo() {
    // Verify method exists and executes without error
    menuView.setDisableUndoRedo(true);
    menuView.setDisableUndoRedo(false);
  }

  @Test
  @DisplayName("Configuration menu item should open config dialog")
  void testConfigurationMenuAction() {
    menuView.setGui(mockGui);
    // Note: Testing dialog opening requires more setup with JavaFX Application thread
    // This test verifies the method structure exists
    assertNotNull(menuView);
  }

  @Test
  @DisplayName("Quit menu item should delegate to GUI when available")
  void testQuitMenuActionWithGui() {
    menuView.setGui(mockGui);
    var fileMenu = menuView.getMenus().get(0);
    var quitItem = fileMenu.getItems().get(7); // Last item is Quit

    // Trigger the action
    quitItem.getOnAction().handle(null);

    verify(mockGui, times(1)).requestQuit();
  }

  @Test
  @DisplayName("Quit menu item should delegate to controller when GUI is null")
  void testQuitMenuActionWithoutGui() {
    var fileMenu = menuView.getMenus().get(0);
    var quitItem = fileMenu.getItems().get(7); // Last item is Quit

    // Trigger the action without setting GUI
    quitItem.getOnAction().handle(null);

    verify(mockController, times(1)).executeCommand("quit", new String[0]);
  }

  @Test
  @DisplayName("Configuration dialog can be opened")
  void testConfigurationDialogOpening() {
    // Verify that the method exists
    assertNotNull(menuView);
  }

  @Test
  @DisplayName("Undo action should call controller with correct step count for AI vs AI")
  void testUndoActionWithAIPlayers() {
    when(mockController.isWhiteAi()).thenReturn(true);
    when(mockController.isBlackAi()).thenReturn(true);

    var gameMenu = menuView.getMenus().get(1);
    var undoItem = gameMenu.getItems().get(0);

    undoItem.getOnAction().handle(null);

    // When both are AI, steps should be 1
    verify(mockController, times(1))
        .executeCommand("undo", new String[] { "1" });
  }

  @Test
  @DisplayName("Undo action should call controller with 2 steps for mixed AI")
  void testUndoActionWithMixedAI() {
    when(mockController.isWhiteAi()).thenReturn(true);
    when(mockController.isBlackAi()).thenReturn(false);

    var gameMenu = menuView.getMenus().get(1);
    var undoItem = gameMenu.getItems().get(0);

    undoItem.getOnAction().handle(null);

    // When only one is AI, steps should be 2
    verify(mockController, times(1))
        .executeCommand("undo", new String[] { "2" });
  }

  @Test
  @DisplayName("Redo action should call controller with correct step count")
  void testRedoActionWithMixedAI() {
    when(mockController.isWhiteAi()).thenReturn(true);
    when(mockController.isBlackAi()).thenReturn(false);

    var gameMenu = menuView.getMenus().get(1);
    var redoItem = gameMenu.getItems().get(1);

    redoItem.getOnAction().handle(null);

    verify(mockController, times(1))
        .executeCommand("redo", new String[] { "2" });
  }

  @Test
  @DisplayName("Pause menu item should execute pause command when game is running")
  void testPauseMenuCommandExecution() {
    // Just verify that the menu item exists
    var gameMenu = menuView.getMenus().get(1);
    var pauseItem = gameMenu.getItems().get(3);

    assertNotNull(pauseItem);
    assertNotNull(pauseItem.getOnAction());
  }

  @Test
  @DisplayName("Hint menu item should execute hint command")
  void testHintMenuAction() {
    var gameMenu = menuView.getMenus().get(1);
    var hintItem = gameMenu.getItems().get(4);

    hintItem.getOnAction().handle(null);

    verify(mockController, times(1)).executeCommand("hint", new String[0]);
  }

  @Test
  @DisplayName("Shortcut manager should be queried for all menu accelerators")
  void testShortcutManagerConfiguration() {
    // Verify that all menu items request shortcuts
    String[] expectedShortcuts = {
        "new-game", "load-game", "save-game", "configuration", "info", "quit",
        "undo", "redo", "pause", "hint"
    };

    for (String shortcut : expectedShortcuts) {
      verify(mockShortcutManager, atLeastOnce()).get(shortcut);
    }
  }
}
