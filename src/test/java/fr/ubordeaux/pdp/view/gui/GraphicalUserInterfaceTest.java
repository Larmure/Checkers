package fr.ubordeaux.pdp.view.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.player.Player;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.view.gui.layout.MainView;
import java.lang.reflect.Field;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class GraphicalUserInterfaceTest {

  @BeforeAll
  static void initJavaFxToolkit() {
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException ignored) {
      // JavaFX toolkit already initialized by another test class.
    }
  }

  @Test
  void testConstructorWithCliConfigStoresConfig() throws Exception {
    Configuration cfg = Configuration.getDefaultConfiguration();
    GraphicalUserInterface gui = new GraphicalUserInterface(cfg);

    Field cliConfigField = GraphicalUserInterface.class.getDeclaredField("cliConfig");
    cliConfigField.setAccessible(true);

    assertSame(cfg, cliConfigField.get(gui));
  }

  @Test
  void testShowHintWithNullMainViewDoesNotThrow() {
    GraphicalUserInterface gui = new GraphicalUserInterface();

    assertDoesNotThrow(() -> gui.showHint("A1", "B2"));
  }

  @Test
  void testShowGameOverAlertReturnsImmediatelyWhenAlreadyShown() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    setGameOverAlert(gui, true);

    assertDoesNotThrow(() -> gui.showGameOverAlert(null, false));
    assertTrue(isGameOverAlert(gui));
  }

  @Test
  void testShowGameOverAlertUsesTimeExpiredHeader() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Player whitePlayer = Mockito.mock(Player.class);
    Player blackPlayer = Mockito.mock(Player.class);
    Stage stage = Mockito.mock(Stage.class);

    Mockito.when(game.isWhiteTurn()).thenReturn(true);
    Mockito.when(game.getWhitePlayer()).thenReturn(whitePlayer);
    Mockito.when(game.getBlackPlayer()).thenReturn(blackPlayer);
    Mockito.when(blackPlayer.getName()).thenReturn("Noir");
    Mockito.when(game.getWhiteScore()).thenReturn(12);
    Mockito.when(game.getBlackScore()).thenReturn(8);

    setStage(gui, stage);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> Mockito.when(alert.showAndWait())
                .thenReturn(Optional.empty()))) {
      mockedPlatform.when(() -> Platform.runLater(Mockito.any(Runnable.class)))
          .thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
          });

      gui.showGameOverAlert(game, true);

      assertTrue(isGameOverAlert(gui));
      assertEquals(1, mockedAlerts.constructed().size());
      Alert alert = mockedAlerts.constructed().get(0);

      Mockito.verify(alert).initOwner(stage);
      Mockito.verify(alert).setTitle(Internationalization.get("gui.gameover.title"));
      Mockito.verify(alert).setHeaderText(
          String.format(Internationalization.get("gui.gameover.time_expired"), "Noir"));
      Mockito.verify(alert).setContentText(String.format(
          Internationalization.get("game.white_score") + " : %d\n"
              + Internationalization.get("game.black_score") + " : %d",
          12,
          8));
      Mockito.verify(alert).showAndWait();
    }
  }

  @Test
  void testShowGameOverAlertUsesDrawHeader() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Stage stage = Mockito.mock(Stage.class);

    Mockito.when(game.isDraw()).thenReturn(true);
    Mockito.when(game.isWhiteTurn()).thenReturn(false);
    Mockito.when(game.getWhiteScore()).thenReturn(3);
    Mockito.when(game.getBlackScore()).thenReturn(3);

    setStage(gui, stage);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> Mockito.when(alert.showAndWait())
                .thenReturn(Optional.empty()))) {
      mockedPlatform.when(() -> Platform.runLater(Mockito.any(Runnable.class)))
          .thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
          });

      gui.showGameOverAlert(game, false);

      assertTrue(isGameOverAlert(gui));
      Alert alert = mockedAlerts.constructed().get(0);

      Mockito.verify(alert).setHeaderText(Internationalization.get("gui.gameover.draw"));
      Mockito.verify(alert).setContentText(String.format(
          Internationalization.get("game.white_score") + " : %d\n"
              + Internationalization.get("game.black_score") + " : %d",
          3,
          3));
    }
  }

  @Test
  void testShowGameOverAlertUsesWinnerHeader() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Player whitePlayer = Mockito.mock(Player.class);
    Player blackPlayer = Mockito.mock(Player.class);
    Stage stage = Mockito.mock(Stage.class);

    Mockito.when(game.isWhiteTurn()).thenReturn(false);
    Mockito.when(game.isDraw()).thenReturn(false);
    Mockito.when(game.getWhitePlayer()).thenReturn(whitePlayer);
    Mockito.when(game.getBlackPlayer()).thenReturn(blackPlayer);
    Mockito.when(whitePlayer.getName()).thenReturn("Blanc");
    Mockito.when(game.getWhiteScore()).thenReturn(5);
    Mockito.when(game.getBlackScore()).thenReturn(7);

    setStage(gui, stage);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> Mockito.when(alert.showAndWait())
                .thenReturn(Optional.empty()))) {
      mockedPlatform.when(() -> Platform.runLater(Mockito.any(Runnable.class)))
          .thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
          });

      gui.showGameOverAlert(game, false);

      Alert alert = mockedAlerts.constructed().get(0);
      Mockito.verify(alert).setHeaderText(
          String.format(Internationalization.get("gui.gameover.winner"), "Blanc"));
    }
  }

  @Test
  void testUpdateResetsGameOverAlertWhenStateIsInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);

    setGameOverAlert(gui, true);
    gui.update(game);

    assertFalse(isGameOverAlert(gui));
  }

  @Test
  void testUpdateKeepsGameOverAlertWhenStateIsNotInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.FINISHED);

    setGameOverAlert(gui, true);
    gui.update(game);

    assertTrue(isGameOverAlert(gui));
  }

  @Test
  void testDisplayDelegatesToUpdateAndResetsFlagInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);

    setGameOverAlert(gui, true);
    gui.display(game);

    assertFalse(isGameOverAlert(gui));
  }

  @Test
  void testStartWhenToolkitAlreadyInitializedThrowsIllegalStateException() {
    GraphicalUserInterface gui = new GraphicalUserInterface();

    assertThrows(IllegalStateException.class, gui::start);
  }

  @Test
  void testRequestQuitWithoutGameExitsDirectly() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameController controller = Mockito.mock(GameController.class);

    gui.setController(controller);
    Mockito.when(controller.getGame()).thenReturn(null);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class)) {
      gui.requestQuit();

      Mockito.verify(controller).getGame();
      Mockito.verify(controller, Mockito.never()).hasUnsavedChanges();
      Mockito.verify(controller, Mockito.never())
          .executeCommand(Mockito.eq("pause"), Mockito.any(String[].class));
      mockedPlatform.verify(Platform::exit);
    }
  }

  @Test
  void testRequestQuitWhenGameInProgressAndUserConfirmsSave() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameController controller = Mockito.mock(GameController.class);
    GameCheckers game = Mockito.mock(GameCheckers.class);
    MainView mainView = Mockito.mock(MainView.class);

    Mockito.when(controller.getGame()).thenReturn(game);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);
    Mockito.when(controller.hasUnsavedChanges()).thenReturn(true);
    gui.setController(controller);
    setMainView(gui, mainView);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> {
              Mockito.when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
              Mockito.when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.YES));
            })) {
      gui.requestQuit();

      Mockito.verify(controller).executeCommand("pause", new String[0]);
      Mockito.verify(controller).hasUnsavedChanges();
      Mockito.verify(mainView).openSaveDialog();
      mockedPlatform.verify(Platform::exit);
      assertEquals(1, mockedAlerts.constructed().size());
    }
  }

  @Test
  void testRequestQuitWhenGameInProgressAndUserDeclinesSave() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameController controller = Mockito.mock(GameController.class);
    GameCheckers game = Mockito.mock(GameCheckers.class);

    Mockito.when(controller.getGame()).thenReturn(game);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);
    Mockito.when(controller.hasUnsavedChanges()).thenReturn(true);
    gui.setController(controller);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> {
              Mockito.when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
              Mockito.when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.NO));
            })) {
      gui.requestQuit();

      Mockito.verify(controller).executeCommand("pause", new String[0]);
      Mockito.verify(controller).hasUnsavedChanges();
      Mockito.verify(controller, Mockito.never())
          .executeCommand(Mockito.eq("continue"), Mockito.any(String[].class));
      mockedPlatform.verify(Platform::exit);
      assertEquals(1, mockedAlerts.constructed().size());
    }
  }

  @Test
  void testRequestQuitWhenGameInProgressAndUserCancels() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface();
    GameController controller = Mockito.mock(GameController.class);
    GameCheckers game = Mockito.mock(GameCheckers.class);

    Mockito.when(controller.getGame()).thenReturn(game);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);
    Mockito.when(controller.hasUnsavedChanges()).thenReturn(true);
    gui.setController(controller);

    try (MockedStatic<Platform> mockedPlatform = Mockito.mockStatic(Platform.class);
        MockedConstruction<Alert> mockedAlerts = Mockito.mockConstruction(Alert.class,
            (alert, context) -> {
              Mockito.when(alert.getButtonTypes()).thenReturn(FXCollections.observableArrayList());
              Mockito.when(alert.showAndWait()).thenReturn(Optional.of(ButtonType.CANCEL));
            })) {
      gui.requestQuit();

      Mockito.verify(controller).executeCommand("pause", new String[0]);
      Mockito.verify(controller).hasUnsavedChanges();
      Mockito.verify(controller).executeCommand("continue", new String[0]);
      mockedPlatform.verify(Platform::exit, Mockito.never());
      assertEquals(1, mockedAlerts.constructed().size());
    }
  }

  private void setStage(GraphicalUserInterface gui, Stage stage) throws Exception {
    Field field = GraphicalUserInterface.class.getDeclaredField("stage");
    field.setAccessible(true);
    field.set(gui, stage);
  }

  private void setMainView(GraphicalUserInterface gui, MainView mainView) throws Exception {
    Field field = GraphicalUserInterface.class.getDeclaredField("mainView");
    field.setAccessible(true);
    field.set(gui, mainView);
  }

  private void setGameOverAlert(GraphicalUserInterface gui, boolean value) throws Exception {
    Field f = GraphicalUserInterface.class.getDeclaredField("gameOverAlert");
    f.setAccessible(true);
    f.setBoolean(gui, value);
  }

  private boolean isGameOverAlert(GraphicalUserInterface gui) throws Exception {
    Field f = GraphicalUserInterface.class.getDeclaredField("gameOverAlert");
    f.setAccessible(true);
    return f.getBoolean(gui);
  }
}
