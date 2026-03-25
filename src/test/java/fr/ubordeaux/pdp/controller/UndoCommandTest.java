package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.controller.GameController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.commands.UndoCommand;

public class UndoCommandTest {

  private GameController mockController;

  @BeforeEach
  public void setUp() {
    mockController = mock(GameController.class);
  }

  // Covers the 'else' branch (args == null)
  @Test
  public void testExecute_WithNullArgs() {
    UndoCommand command = new UndoCommand(mockController, null);
    command.execute();

    verify(mockController, times(1)).undoGame(1);
  }

  // Covers the 'else' branch (args.length == 0)
  @Test
  public void testExecute_WithEmptyArgs() {
    UndoCommand command = new UndoCommand(mockController, new String[] {});
    command.execute();

    verify(mockController, times(1)).undoGame(1);
  }

  // Covers the 'if' branch with valid number parsing
  @Test
  public void testExecute_WithValidNumberArg() {
    UndoCommand command = new UndoCommand(mockController, new String[] { "3" });
    command.execute();

    verify(mockController, times(1)).undoGame(3);
  }

  // Covers the 'catch (NumberFormatException)' branch
  @Test
  public void testExecute_WithInvalidArg() {
    UndoCommand command = new UndoCommand(mockController, new String[] { "abc" });
    command.execute();

    // Ensure undoGame is never called if parsing fails
    verify(mockController, never()).undoGame(anyInt());
  }

  // Covers the getHelp() method
  @Test
  public void testGetHelp() {
    UndoCommand command = new UndoCommand(mockController, null);
    String help = command.getHelp();

    assertNotNull(help);
  }
}