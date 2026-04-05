package fr.ubordeaux.pdp.view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import fr.ubordeaux.pdp.controller.ShellCommandRouter;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandLineInterfaceTest {

  private CommandLineInterface cli;
  private SpyController spyController;

  @BeforeEach
  void setUp() {
    cli = new CommandLineInterface(true, true);
    spyController = new SpyController(null);
    cli.setController(spyController);

    ClientSession session = mock(ClientSession.class);
    when(session.getMode()).thenReturn(ClientMode.LOCAL);

    ShellCommandRouter router = new ShellCommandRouter(spyController, session);
    cli.setRouter(router);
  }

  @Test
  void testDisplayCalls() {
    GameCheckers game = new GameCheckers(Configuration.getDefaultConfiguration());
    /* assertDoesNotThrow(() -> cli.display(game)); */
    /* assertDoesNotThrow(() -> cli.update(game)); */
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
    assertDoesNotThrow(() -> cli.handleInput(""));
    assertDoesNotThrow(() -> cli.handleInput(null));
    assertFalse(spyController.executeCommandCalled);
    assertFalse(spyController.executeMoveCalled);
  }

  @Test
  void testReadInputWithCommands() {
    LineReader mockReader = mock(LineReader.class);
    when(mockReader.readLine(">> "))
        .thenReturn("show")
        .thenReturn("B2 C3")
        .thenThrow(new EndOfFileException());

    cli.setLineReader(mockReader);
    cli.start();

    cli.handleInput(cli.readInput());
    cli.handleInput(cli.readInput());

    assertTrue(spyController.executeCommandCalled, "The 'show' command should have been executed.");
    assertTrue(spyController.executeMoveCalled, "The move 'B2 C3' should have been executed.");
    assertNull(cli.readInput(), "End of input must return null.");
  }

  /**
   * Spy controller used to observe local command execution.
   */
  private static class SpyController extends fr.ubordeaux.pdp.controller.GameController {
    boolean executeCommandCalled = false;
    boolean executeMoveCalled = false;

    SpyController(GameView view) {
      super(view);
    }

    @Override
    public void executeCommand(String commandName, String[] args) {
      this.executeCommandCalled = true;
    }

    @Override
    public void executeMove(String from, String to, boolean isManoury) {
      this.executeMoveCalled = true;
    }
  }
}