package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Timer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.commands.PauseCommand;

public class PauseCommandTest {

  private GameCheckers mockGame;
  private Configuration mockConfig;
  private Timer mockTimer;
  private PauseCommand pauseCommand;

  @BeforeEach
  public void setUp() {
    mockGame = mock(GameCheckers.class);
    mockConfig = mock(Configuration.class);
    mockTimer = mock(Timer.class);

    // Link the mocked configuration to the mocked game
    when(mockGame.getConfiguration()).thenReturn(mockConfig);

    pauseCommand = new PauseCommand(mockTimer, mockGame);
  }

  @Test
  public void testExecute_NotInBlitzMode() {
    when(mockConfig.isBlitz()).thenReturn(false);
    when(mockGame.getState()).thenReturn(State.IN_GAME);

    pauseCommand.execute();

    // Timer should not be canceled but state should be set to PAUSE
    verify(mockTimer, never()).cancel();
    verify(mockGame, times(1)).setState(State.PAUSE);
  }

  @Test
  public void testExecute_InBlitzMode_NotPaused() {
    when(mockConfig.isBlitz()).thenReturn(true);
    when(mockGame.getState()).thenReturn(State.IN_GAME);

    pauseCommand.execute();

    // Timer must be canceled and game state set to PAUSE
    verify(mockTimer, times(1)).cancel();
    verify(mockGame, times(1)).setState(State.PAUSE);
  }

  @Test
  public void testExecute_InBlitzMode_AlreadyPaused() {
    when(mockConfig.isBlitz()).thenReturn(true);
    when(mockGame.getState()).thenReturn(State.PAUSE);

    pauseCommand.execute();

    // Should not cancel the timer again or change state
    verify(mockTimer, never()).cancel();
    verify(mockGame, never()).setState(any());
  }

  @Test
  public void testGetHelp() {
    // Just verify it doesn't return null or crash
    String helpText = pauseCommand.getHelp();
    assertNotNull(helpText);
  }
}