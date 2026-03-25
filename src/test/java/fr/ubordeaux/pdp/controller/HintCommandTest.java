package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import fr.ubordeaux.pdp.controller.commands.HintCommand;

public class HintCommandTest {

    private SpyController spyController;
    private HintCommand hintCommand;

    // ========================================================================
    // SPY CLASS CREATION (To replace Mockito)
    // ========================================================================

    private static class DummyView extends GameView {
        @Override 
        public void start() {}
        
        @Override 
        public void display(GameCheckers game) {}
        
          // Method from the Observer interface
        public void update(GameCheckers game) {} 
        
          // The method added to display a hint
        public void showHint(String from, String to) {}
    }

    /**
      * A "Spy" controller: it extends your real GameController.
      * We override ONLY the displayHint method to capture
      * the AI result instead of displaying it on screen.
     */
    private static class SpyController extends GameController {
        boolean hintWasCalled = false;
        String capturedFrom = null;
        String capturedTo = null;

        public SpyController(GameView view) {
            super(view);
        }

        @Override
        public void displayHint(String from, String to) {
            this.hintWasCalled = true;
            this.capturedFrom = from;
            this.capturedTo = to;
        }
    }

    // ========================================================================
    // TEST SETUP
    // ========================================================================

    @BeforeEach
    void setUp() {
        spyController = new SpyController(new DummyView());

        GameCheckers realGame = new GameCheckers();
        
        spyController.setGame(realGame, realGame.getConfiguration());

        hintCommand = new HintCommand(spyController);
    }

    // ========================================================================
    // TEST EXECUTION
    // ========================================================================

    @Test
    void testExecute_ShouldRunAI_AndTriggerDisplayHint() {
        // --- ACT (Execution) ---
        hintCommand.execute();

        // --- ASSERT (Checks) ---
        assertTrue(spyController.hintWasCalled, "La commande devrait appeler displayHint() sur le contrôleur.");
        
        assertNotNull(spyController.capturedFrom, "La case de départ de l'indice ne doit pas être nulle.");
        assertNotNull(spyController.capturedTo, "La case d'arrivée de l'indice ne doit pas être nulle.");
        
        assertTrue(spyController.capturedFrom.matches("[A-Z][0-9]+"), "Le format de départ doit être algébrique (ex: A3)");
        assertTrue(spyController.capturedTo.matches("[A-Z][0-9]+"), "Le format d'arrivée doit être algébrique (ex: B4)");
    }

    @Test
    void testGetHelp_ShouldReturnValidString() {
        // --- ACT ---
        String helpText = hintCommand.getHelp();

        // --- ASSERT ---
        assertNotNull(helpText, "Le texte d'aide ne doit pas être nul.");
        assertFalse(helpText.trim().isEmpty(), "Le texte d'aide ne doit pas être vide.");
    }
}