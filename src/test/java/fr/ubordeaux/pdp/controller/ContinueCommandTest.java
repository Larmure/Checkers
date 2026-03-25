package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ContinueCommandTest {

  private GameController mockController;
  private GameCheckers mockGame;
  private ContinueCommand continueCommand;

  @BeforeEach
  public void setUp() {
    mockController = mock(GameController.class);
    mockGame = mock(GameCheckers.class);

    // Link the mocked game to the mocked controller
    when(mockController.getGame()).thenReturn(mockGame);

    continueCommand = new ContinueCommand(mockController);
  }

  @Test
  public void testExecute_InBlitzMode() {
    when(mockController.isBlitz()).thenReturn(true);

    continueCommand.execute();

    // Timer must be started and game state resumed
    verify(mockController, times(1)).startBlitzTimer();
    verify(mockGame, times(1)).setState(State.IN_GAME);
    verify(mockGame, times(1)).notifyObservers();
  }

  @Test
  public void testExecute_NotInBlitzMode() {
    when(mockController.isBlitz()).thenReturn(false);

    continueCommand.execute();

    // Timer must NOT be started, but game state still resumes
    verify(mockController, never()).startBlitzTimer();
    verify(mockGame, times(1)).setState(State.IN_GAME);
    verify(mockGame, times(1)).notifyObservers();
  }
}