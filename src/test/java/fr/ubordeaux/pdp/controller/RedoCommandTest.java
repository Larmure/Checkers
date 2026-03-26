package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.controller.GameController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.commands.RedoCommand;

public class RedoCommandTest {

  private GameController mockController;

  @BeforeEach
  public void setUp() {
    mockController = mock(GameController.class);
  }

  // Covers the 'else' branch (args == null)
  @Test
  public void testExecute_WithNullArgs() {
    RedoCommand command = new RedoCommand(mockController, null);
    command.execute();

    verify(mockController, times(1)).redoGame(1);
  }

  // Covers the 'else' branch (args.length == 0)
  @Test
  public void testExecute_WithEmptyArgs() {
    RedoCommand command = new RedoCommand(mockController, new String[] {});
    command.execute();

    verify(mockController, times(1)).redoGame(1);
  }

  // Covers the 'if' branch with valid number parsing
  @Test
  public void testExecute_WithValidNumberArg() {
    RedoCommand command = new RedoCommand(mockController, new String[] { "4" });
    command.execute();

    verify(mockController, times(1)).redoGame(4);
  }

  // Covers the 'catch (NumberFormatException)' branch
  @Test
  public void testExecute_WithInvalidArg() {
    RedoCommand command = new RedoCommand(mockController, new String[] { "invalid" });
    command.execute();

    // Ensure redoGame is never called if parsing fails
    verify(mockController, never()).redoGame(anyInt());
  }

  // Covers the getHelp() method
  @Test
  public void testGetHelp() {
    RedoCommand command = new RedoCommand(mockController, null);
    String help = command.getHelp();

    assertNotNull(help);
  }
}