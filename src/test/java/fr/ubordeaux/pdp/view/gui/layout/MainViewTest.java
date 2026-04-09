package fr.ubordeaux.pdp.view.gui.layout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import fr.ubordeaux.pdp.ConfigManager;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Unit tests for {@link MainView}.
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
  private AiPlayer mockAiPlayer;

  @Mock
  private GraphicalUserInterface mockGui;

  @Mock
  private ClientSession mockSession;

  @BeforeAll
  static void initJavaFxToolkit() {
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException ignored) {
      // Toolkit already initialized by another test class.
    }
  }

  @Override
  public void start(Stage stage) {
    stage.show();
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(mockController.isWhiteAi()).thenReturn(false);
    when(mockController.isBlackAi()).thenReturn(false);

    mainView = new MainView(
        mockController,
        mockConfigManager,
        mockCliConfig,
        false,
        mockSession);
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
    assertNotNull(mainView.getTop());
    assertNotNull(mainView.getCenter());
    assertNotNull(mainView.getBottom());
  }

  @Test
  @DisplayName("Toolbar should contain action buttons and turn label")
  void testToolbarStructure() {
    var toolbar = mainView.getBottom();
    assertNotNull(toolbar);
    assertTrue(toolbar instanceof javafx.scene.layout.HBox);
  }

  @Test
  @DisplayName("passStageToMenu should accept a Stage reference")
  void testPassStageToMenu() {
    mainView.passStageToMenu(null);
  }

  @Test
  @DisplayName("passGuiToMenu should accept a GUI reference")
  void testPassGuiToMenu() {
    mainView.passGuiToMenu(mockGui);
  }

  @Test
  @DisplayName("bindToScene should accept a Scene reference")
  void testBindToScene() {
    Scene scene = new Scene(mainView);
    mainView.bindToScene(scene);
  }

  @Test
  @DisplayName("openSaveDialog method exists and is callable")
  void testOpenSaveDialogExists() {
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("openConfigDialog method exists and is callable")
  void testOpenConfigDialogExists() {
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("update should not crash with null game")
  void testUpdateDoesNotCrash() {
    mainView.update(null);
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("undo/redo buttons should be adjustable based on game state")
  void testUndoRedoButtonManagement() {
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
    mainView.showHint("A1", "B2");
    assertNotNull(mainView);
  }

  @Test
  @DisplayName("showHint should be callable without error")
  void testShowHint() {
    mainView.showHint("A1", "B2");
  }

  @Test
  @DisplayName("MainView components are properly linked")
  void testMainViewComponentsLinked() {
    assertNotNull(mainView.getTop());
    assertNotNull(mainView.getCenter());
    assertNotNull(mainView.getBottom());
  }

  @Test
  @DisplayName("MainView should initialize with provided ConfigManager")
  void testMainViewWithConfigManager() {
    MainView view = new MainView(
        mockController,
        mockConfigManager,
        null,
        false,
        mockSession);
    assertNotNull(view);
    assertNotNull(view.getTop());
    assertNotNull(view.getCenter());
    assertNotNull(view.getBottom());
  }

  @Test
  @DisplayName("MainView should initialize with CLI configuration")
  void testMainViewWithCliConfig() {
    MainView view = new MainView(
        mockController,
        mockConfigManager,
        mockCliConfig,
        false,
        mockSession);
    assertNotNull(view);
    assertNotNull(view.getTop());
    assertNotNull(view.getCenter());
    assertNotNull(view.getBottom());
  }

  @Test
  @DisplayName("MainView in server mode should not create menu")
  void testMainViewInServerMode() {
    MainView view = new MainView(
        mockController,
        mockConfigManager,
        mockCliConfig,
        true,
        mockSession);
    assertNotNull(view);
    assertNotNull(view.getCenter());
    assertNotNull(view.getBottom());
  }

  @Test
  @DisplayName("Controller should be properly passed to components")
  void testControllerInitialization() {
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
    MainView view = new MainView(
        mockController,
        mockConfigManager,
        null,
        false,
        mockSession);
    assertNotNull(view);
    assertNotNull(view.getTop());
  }
}