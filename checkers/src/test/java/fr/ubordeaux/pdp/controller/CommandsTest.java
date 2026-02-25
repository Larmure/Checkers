package fr.ubordeaux.pdp.controller;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.Configuration;
import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.controller.commands.*;

class CommandsTest {

    private GameController controller;
    private MockView view;

    @BeforeEach
    void setUp() {
        view = new MockView();
        controller = new GameController(view);
        // Initialization to avoid NullPointerException inside commands
        controller.startNewGame(Configuration.getDefaultConfiguration());
    }

    // =========================================================================
    // NEW COMMAND
    // =========================================================================
    @Test
    void testNewCommandExecute() {
        // Test without arguments (else branch)
        new NewCommand(controller, new String[]{}).execute();
        
        // Test with valid arguments
        String[] args = {"-b", "-s", "10", "-t", "60"};
        new NewCommand(controller, args).execute();
        
        // Test with invalid syntax (triggers catch)
        new NewCommand(controller, new String[]{"-z"}).execute(); 
        
        assertNotNull(new NewCommand(controller, null).getHelp());
    }

    // =========================================================================
    // HELP COMMAND
    // =========================================================================
    @Test
    void testHelpCommandExecute() {
        // Global list
        new HelpCommand(null).execute();
        new HelpCommand(new String[]{}).execute();
        
        // Specific help for existing command
        new HelpCommand(new String[]{"new"}).execute();
        
        // Non-existent command
        new HelpCommand(new String[]{"ghost"}).execute();
    }

    // =========================================================================
    // SHOW COMMAND
    // =========================================================================
    @Test
    void testShowCommandExecute() {
        // Without arguments
        new ShowCommand(controller, null).execute();
        
        // Switch branches
        new ShowCommand(controller, new String[]{"board"}).execute();
        new ShowCommand(controller, new String[]{"configuration"}).execute();
        
        // Default case
        new ShowCommand(controller, new String[]{"unknown"}).execute();
        
        // Exceptions for not yet implemented features
        assertThrows(IllegalArgumentException.class, 
            () -> new ShowCommand(controller, new String[]{"history"}).execute());
    }

    // =========================================================================
    // SET COMMAND
    // =========================================================================
    @Test
    void testSetCommandExecute() {
        // Invalid formats
        new SetCommand(controller, null).execute();
        new SetCommand(controller, new String[]{"debug"}).execute(); // missing =VALUE
        
        // Invalid boolean values
        new SetCommand(controller, new String[]{"debug=maybe"}).execute();
        
        // Success cases
        new SetCommand(controller, new String[]{"debug=true"}).execute();
        assertTrue(controller.isDebug());
        
        new SetCommand(controller, new String[]{"verbose=false"}).execute();
        assertFalse(controller.isVerbose());
        
        // Unknown parameter
        new SetCommand(controller, new String[]{"unknwon=true"}).execute();
    }

    // Minimal mock for the controller
    private static class MockView extends GameView {
        @Override public void start() {}
        @Override public void display(fr.ubordeaux.pdp.model.GameCheckers g) {}
        @Override public void update(fr.ubordeaux.pdp.model.GameCheckers g) {}
    }

    @Test
    void testQuitCommandHelp() {
        QuitCommand quit = new QuitCommand();
        String help = quit.getHelp();
        assertNotNull(help);
        assertTrue(help.contains("exit"), "Help message must mention program exit.");
    }
}