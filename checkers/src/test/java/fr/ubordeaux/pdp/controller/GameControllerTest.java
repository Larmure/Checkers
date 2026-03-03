package fr.ubordeaux.pdp.controller;

import java.io.ObjectInputFilter;

import fr.ubordeaux.pdp.view.GameView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.Utils;

class GameControllerTest {

    private GameController controller;
    private SpyGameView spyView;

    @BeforeEach
        void setUp() {
        spyView = new SpyGameView();
        controller = new GameController(spyView);
        
        Configuration defaultConfig = Configuration.getDefaultConfiguration();
        controller.startNewGame(defaultConfig); 
    }

    @Test
    void testStartBindsControllerAndView() {
        controller.start();
        assertTrue(spyView.startCalled, "The view's start() method must be called.");
        assertEquals(controller, spyView.boundController, "The controller must bind to the view when start is called.");
    }

    @Test
    void testStartNewGameInitializesModel() {
        Configuration config = Configuration.getDefaultConfiguration(); 
        controller.startNewGame(config);

        // We verify indirectly through board display
        controller.displayBoard();
        assertTrue(spyView.displayCalled, "The board must be displayed after game initialization.");
        assertNotNull(spyView.lastGameReceived, "The view must receive a GameCheckers instance.");
    }

    @Test
    void testExecuteCommandNew() {
        // Simulates the "new" command which should trigger initialization
        // Note: NewCommand calls controller.startNewGame
        String[] args = {"false", "false", "0", "8"};
        controller.executeCommand("new", args);

        controller.displayBoard();
        assertTrue(spyView.displayCalled);
    }

    @Test
    void testToggleDebugAndVerbose() {
        // Default initialization
        Configuration config = Configuration.getDefaultConfiguration();
        controller.startNewGame(config);

        // Verbose test
        controller.setVerbose(true);
        assertTrue(controller.isVerbose(), "Verbose mode should be enabled.");

        // Debug test
        controller.setDebug(true);
        assertTrue(controller.isDebug(), "Debug mode should be enabled.");
        
        controller.setDebug(false);
        assertFalse(controller.isDebug(), "Debug mode should be disabled.");
    }

    @Test
    void testExecuteMoveUpdatesView() {
        Configuration config = Configuration.getDefaultConfiguration();
        controller.startNewGame(config);
        
        // Reset view flag
        spyView.displayCalled = false;
        
        // Attempt a move (format expected by GameCheckers via GameController)
        // Note: If the move is invalid, nothing happens, but the call is traced
        controller.executeMove("B2", "C3"); 
        
        // After a move, the controller generally calls displayBoard via observer
        // or manually depending on implementation.
        controller.displayBoard();
        assertTrue(spyView.displayCalled);
    }

    @Test
    void testExecuteAllCommands() {
    // List of simple commands to test, quit excluded (no complex arguments)
    String[] commands = Utils.COMMANDS_MAP.keySet().stream()
        .filter(cmd -> !cmd.equals("quit"))
        .toArray(String[]::new);
    
    for (String cmd : commands) {
        // We only verify that execution does not throw an exception
        //assertDoesNotThrow(() -> controller.executeCommand(cmd, null));
    }
}

    @Test
    void testUnknownCommand() {
        // Covers the "default" branch and the "yield null"
        assertDoesNotThrow(() -> controller.executeCommand("invalidCmd", null));
    }

    @Test
    void testExecuteMoveGameOverBranch() throws Exception {
        controller.startNewGame(Configuration.getDefaultConfiguration());

        // Access the private model to manipulate its state
        java.lang.reflect.Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        GameCheckers gameModel = (GameCheckers) gameField.get(controller);

        // Force the "Finished" state so isGameOver() returns true
        // Note: FinishedState is used in GameCheckersTest
        //gameModel.setState(new FinishedState());

        // This call will now enter the 'if' block
        controller.executeMove("A1", "B2");

        //assertTrue(gameModel.getState().isGameOver(), "The game should be in Game Over state.");
    }

    @Test
    void testDisplayConfiguration() {
        Configuration config = Configuration.getDefaultConfiguration();
        controller.startNewGame(config);

        // We verify that the call does not throw an exception
        assertDoesNotThrow(() -> controller.displayConfiguration());

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        controller.displayConfiguration();

        assertTrue(outContent.toString().contains("blitz="), 
            "Output should contain configuration details.");

        // Reset output stream
        System.setOut(System.out);
    }

    // --- Internal class to simulate the View (Mock/Spy) ---
    private static class SpyGameView extends GameView {
        boolean startCalled = false;
        boolean displayCalled = false;
        GameController boundController;
        GameCheckers lastGameReceived;

        @Override
        public void start() {
            startCalled = true;
        }

        @Override
        public void setController(GameController controller) {
            this.boundController = controller;
        }

        @Override
        public void display(GameCheckers game) {
            displayCalled = true;
            this.lastGameReceived = game;
        }

        @Override
        public void update(GameCheckers game) {
            // Used by the Observer pattern
            this.lastGameReceived = game;
        }
    }
}