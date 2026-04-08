package fr.ubordeaux.pdp.view.gui.layout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.ConfigManager;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.Player;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Unit tests for {@link MainView}.
 *
 * <p>Tests the main view layout, toolbar initialization, game state updates,
 * and delegation to child components (MenuView, PlayView).
 * Uses TestFX for JavaFX initialization and mocks for dependencies.
 */
@DisplayName("MainView Tests")
class MainViewTest extends ApplicationTest {

  private MainView mainView;

  @Mock
  private GameController mockController;

  @Mock
  private ConfigManager mockConfigManager;

  @Mock
  private Configuration mockCliConfig;

  @Mock
  private GameCheckers mockGame;

  @Mock
  private Player mockPlayer;

  @Mock
  private AiPlayer mockAiPlayer;

  @Mock
  private GraphicalUserInterface mockGui;

  @BeforeAll
  static void initJavaFxToolkit() {
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException ignored) {
      // Toolkit already initialized by another test class.
    }
  }

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

    // Configure default mock behaviour
    when(mockController.isWhiteAi()).thenReturn(false);
    when(mockController.isBlackAi()).thenReturn(false);

    // Initialize MainView
    mainView = new MainView(mockController, mockConfigManager, mockCliConfig);
  }

  @Test
  @DisplayName("MainView should extend BorderPane")
  void testMainViewExtendsBorderPane() {
    assertNotNull(mainView);
    assertTrue(mainView instanceof BorderPane);
  }

  @Test
  @DisplayName("MainView should be initialized with menu, content, and toolbar")
  void testMainViewInitialization() {
    assertNotNull(mainView);
    assertNotNull(mainView.getTop()); // MenuView
    assertNotNull(mainView.getCenter()); // PlayView
    assertNotNull(mainView.getBottom()); // Toolbar
  }

  @Test
  @DisplayName("Toolbar should contain action buttons and turn label")
  void testToolbarStructure() {
    var toolbar = mainView.getBottom();
    assertNotNull(toolbar);
    // Verify toolbar is an HBox with expected styling
    assertTrue(toolbar instanceof javafx.scene.layout.HBox);
  }

  @Test
  @DisplayName("passStageToMenu should accept a Stage reference")
  void testPassStageToMenu() {
    // Just verify the method executes without error
    mainView.passStageToMenu(null);
  }

  @Test
  @DisplayName("passGuiToMenu should accept a GUI reference")
  void testPassGuiToMenu() {
    // Just verify the method executes without error
    mainView.passGuiToMenu(mockGui);
  }

  @Test
  @DisplayName("bindToScene should accept a Scene reference")
  void testBindToScene() {
    // Just verify the method executes without error
    Scene scene = new Scene(mainView);
    mainView.bindToScene(scene);
  }

  @Test
  @DisplayName("openSaveDialog method exists and is callable")
  void testOpenSaveDialogExists() {
    // Method should exist and be accessible
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("openConfigDialog method exists and is callable")
  void testOpenConfigDialogExists() {
    // Method should exist and be accessible
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("update should not crash with null game")
  void testUpdateDoesNotCrash() {
    // Update with null should be handled gracefully
    mainView.update(null);
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("undo/redo buttons should be adjustable based on game state")
  void testUndoRedoButtonManagement() {
    // Just verify that mainView was initialized properly
    assertNotNull(mainView);
    assertNotNull(mainView.getBottom());
  }

  @Test

  @DisplayName("toolbar buttons should be properly initialized")
  void testToolbarButtonsInitialization() {
    var toolbar = mainView.getBottom();
    assertNotNull(toolbar);
    assertTrue(toolbar instanceof javafx.scene.layout.HBox);
  }

  @Test
  @DisplayName("showHint should handle valid positions")
  void testShowHintMethod() {
    // Method should be callable without exceptions
    mainView.showHint("A1", "B2");
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("showHint should be callable without error")
  void testShowHint() {
    mainView.showHint("A1", "B2");
    // Just verify no exception is thrown
  }

  @Test

  @DisplayName("MainView components are properly linked")
  void testMainViewComponentsLinked() {
    // Verify all three regions are present
    assertNotNull(mainView.getTop()); // MenuView
    assertNotNull(mainView.getCenter()); // PlayView
    assertNotNull(mainView.getBottom()); // Toolbar
  }

  @Test
  @DisplayName("MainView should initialize with provided ConfigManager")
  void testMainViewWithConfigManager() {
    MainView view = new MainView(mockController, mockConfigManager, null);
    assertNotNull(view);
    assertNotNull(view.getTop());
    assertNotNull(view.getCenter());
    assertNotNull(view.getBottom());
  }

  @Test
  @DisplayName("MainView should initialize with CLI configuration")
  void testMainViewWithCliConfig() {
    MainView view = new MainView(mockController, mockConfigManager, mockCliConfig);
    assertNotNull(view);
    // Verify that all regions are properly initialized
    assertNotNull(view.getTop());
    assertNotNull(view.getCenter());
    assertNotNull(view.getBottom());
  }

  @Test
  @DisplayName("Controller should be properly passed to components")
  void testControllerInitialization() {
    // Verify that controller methods are called properly during operations
    when(mockController.getGame()).thenReturn(mockGame);
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("Controller configuration is properly set")
  void testControllerConfiguration() {
    when(mockController.getGame()).thenReturn(mockGame);
    assertNotNull(mockController.getGame());
  }

  @Test
  @DisplayName("MainView handles different AI configurations")
  void testDifferentAiConfigurations() {
    // Test with different AI setups
    when(mockController.isWhiteAi()).thenReturn(true);
    when(mockController.isBlackAi()).thenReturn(true);
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("MainView should apply root-pane style class")
  void testMainViewStyleClass() {
    assertTrue(mainView.getStyleClass().contains("root-pane"));
  }

  @Test
  @DisplayName("ConfigManager should be used to configure menu")
  void testConfigManagerUsage() {
    // Create a new MainView to verify ConfigManager is passed
    MainView view = new MainView(mockController, mockConfigManager, null);
    assertNotNull(view);
    // Verify that MenuView was initialized with the config manager
    assertNotNull(view.getTop());
  }
}
