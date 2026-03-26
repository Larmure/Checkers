package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameViewTest {

    // We create a minimal implementation to test the abstract class
    private static class MockGameView extends GameView {
        @Override public void start() {}
        @Override public void display(GameCheckers game) {}
        @Override public void update(GameCheckers game) {}
        @Override public void showHint(String from, String to) {}
        
        // Getter to verify the protected field
        public GameController getController() {
            return this.controller;
        }
    }

    @Test
    void testSetController() {
        MockGameView view = new MockGameView();
        // We create a controller (even if the view is null for the controller here)
        GameController controller = new GameController(view);
        
        view.setController(controller);
        
        assertEquals(controller, view.getController(), 
            "The controller must be correctly assigned to the protected field of GameView.");
    }
}