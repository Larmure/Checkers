package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineInterfaceTest {

    private CommandLineInterface cli;
    private SpyController spyController;
    private final InputStream originalIn = System.in;

    @BeforeEach
    void setUp() {
        // Initialize the view in verbose/debug mode to cover those branches
        cli = new CommandLineInterface(true, true);
        spyController = new SpyController(null);
        cli.setController(spyController);
    }

    @AfterEach
    void tearDown() {
        // Always restore the original input stream
        System.setIn(originalIn);
    }

    @Test
    void testDisplayCalls() {
        // Verify that display does not crash with a model
        GameCheckers game = new GameCheckers(Configuration.getDefaultConfiguration());
        assertDoesNotThrow(() -> cli.display(game));
        assertDoesNotThrow(() -> cli.update(game));
    }

    @Test
    void testInputLoopWithCommands() throws InterruptedException {
        // Simulate a sequence of user inputs:
        // 1. A "show" command
        // 2. A move "B2 C3" (format validated by Utils.MOVE_REGEX)
        // 3. An empty line (to cover the "continue" branch)
        String input = "show\nB2 C3\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        cli.start();
        
        // Wait briefly or interrupt if needed
        // Note: Since the thread runs an infinite loop on scanner.nextLine(),
        // it will stop when the ByteArrayInputStream is exhausted (hasNextLine() -> false).
        cli.join(); 

        assertTrue(spyController.executeCommandCalled, "The 'show' command should have been executed.");
        assertTrue(spyController.executeMoveCalled, "The move 'B2 C3' should have been executed.");
    }

    // --- Internal Spy class to verify controller calls ---
    private static class SpyController extends GameController {
        boolean executeCommandCalled = false;
        boolean executeMoveCalled = false;

        public SpyController(GameView view) { super(view); }

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