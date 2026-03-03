package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandLineInterfaceTest {

  private CommandLineInterface cli;
  private SpyController spyController;

  @BeforeEach
  void setUp() {
    cli = new CommandLineInterface(true, true);
    spyController = new SpyController(null);
    cli.setController(spyController);
  }

  @Test
  void testDisplayCalls() {
    GameCheckers game = new GameCheckers(Configuration.getDefaultConfiguration());
    /*assertDoesNotThrow(() -> cli.display(game));*/
    /*assertDoesNotThrow(() -> cli.update(game));*/
  }

  @Test
  void testHandleInputMove() {
    cli.handleInput("B2 C3");
    assertTrue(spyController.executeMoveCalled, "Move 'B2 C3' should have been executed.");
  }

  @Test
  void testHandleInputCommand() {
    cli.handleInput("show");
    assertTrue(spyController.executeCommandCalled, "Command 'show' should have been executed.");
  }

  @Test
  void testHandleInputEmpty() {
    // Should not crash or call anything
    assertDoesNotThrow(() -> cli.handleInput(""));
    assertDoesNotThrow(() -> cli.handleInput(null));
    assertFalse(spyController.executeCommandCalled);
    assertFalse(spyController.executeMoveCalled);
  }

  @Test
  void testInputLoopWithCommands() throws InterruptedException {
    // Mock LineReader to return inputs then throw EndOfFileException
    LineReader mockReader = mock(LineReader.class);
    when(mockReader.readLine(anyString()))
        .thenReturn("show")
        .thenReturn("B2 C3")
        .thenReturn("")
        .thenThrow(new EndOfFileException());

    cli.setLineReader(mockReader);
    cli.start();
    cli.join();

    assertTrue(spyController.executeCommandCalled, "The 'show' command should have been executed.");
    assertTrue(spyController.executeMoveCalled, "The move 'B2 C3' should have been executed.");
  }

  // --- Spy class ---
  private static class SpyController extends GameController {
    boolean executeCommandCalled = false;
    boolean executeMoveCalled = false;

    public SpyController(GameView view) {
      super(view);
    }

    @Override
    public void executeCommand(String commandName, String[] args) {
      this.executeCommandCalled = true;
    }

    @Override
    public void executeMove(String from, String to) {
      this.executeMoveCalled = true;
    }
  }
}