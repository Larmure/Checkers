package fr.ubordeaux.pdp.model;

public class PauseState implements State {
    private final GameCheckers game;

    public PauseState(GameCheckers game) {
        this.game = game;
    }

    @Override
    public boolean isGameOver() {
        // The game is not over, but input should be paused.
        return false;
    }

    @Override
    public void handle() {
        System.out.println("Game is paused. Press 'resume' to continue.");
    }
}