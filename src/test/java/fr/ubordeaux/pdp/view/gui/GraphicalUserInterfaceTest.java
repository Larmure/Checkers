package fr.ubordeaux.pdp.view.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import java.lang.reflect.Field;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
    GraphicalUserInterface gui = new GraphicalUserInterface(cfg, false, null);

    Field cliConfigField = GraphicalUserInterface.class.getDeclaredField("cliConfig");
    cliConfigField.setAccessible(true);

    assertSame(cfg, cliConfigField.get(gui));
  }

  @Test
  void testShowHintWithNullMainViewDoesNotThrow() {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);

    assertDoesNotThrow(() -> gui.showHint("A1", "B2"));
  }

  @Test
  void testShowGameOverAlertReturnsImmediatelyWhenAlreadyShown() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);
    setGameOverAlert(gui, true);

    assertDoesNotThrow(() -> gui.showGameOverAlert(null, false));
    assertTrue(isGameOverAlert(gui));
  }

  @Test
  void testUpdateResetsGameOverAlertWhenStateIsInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);

    setGameOverAlert(gui, true);
    gui.update(game);

    assertFalse(isGameOverAlert(gui));
  }

  @Test
  void testUpdateKeepsGameOverAlertWhenStateIsNotInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.FINISHED);

    setGameOverAlert(gui, true);
    gui.update(game);

    assertTrue(isGameOverAlert(gui));
  }

  @Test
  void testDisplayDelegatesToUpdateAndResetsFlagInGame() throws Exception {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);
    GameCheckers game = Mockito.mock(GameCheckers.class);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);

    setGameOverAlert(gui, true);
    gui.display(game);

    assertFalse(isGameOverAlert(gui));
  }

  @Test
  void testStartWhenToolkitAlreadyInitializedThrowsIllegalStateException() {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);

    assertThrows(IllegalStateException.class, gui::start);
  }

  @Test
  void testRequestQuitWhenGameInProgressPausesBeforeUnsavedCheck() {
    GraphicalUserInterface gui = new GraphicalUserInterface(false, null);
    GameController controller = Mockito.mock(GameController.class);
    GameCheckers game = Mockito.mock(GameCheckers.class);

    Mockito.when(controller.getGame()).thenReturn(game);
    Mockito.when(game.getState()).thenReturn(State.IN_GAME);
    RuntimeException sentinel = new RuntimeException("stop-before-exit");
    Mockito.when(controller.hasUnsavedChanges()).thenThrow(sentinel);
    gui.setController(controller);

    RuntimeException thrown = assertThrows(RuntimeException.class, gui::requestQuit);
    assertSame(sentinel, thrown);
    Mockito.verify(controller).executeCommand("pause", new String[0]);
    Mockito.verify(controller).hasUnsavedChanges();
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