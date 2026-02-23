package fr.ubordeaux.pdp.model;

/**
 * Represents the terminal state of the game.
 * <p>In this state, no further moves are allowed, and the game loop is instructed to stop.
 */
public class FinishedState implements State {

  @Override
  public boolean isGameOver() {
    // Signals the controller to break the main game loop.
    return true;
  }

  @Override
  public void handle() {
    System.out.println("Game Over. Thank you for playing!");
  }
}