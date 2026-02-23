package fr.ubordeaux.pdp.model;

/**
 * Represents the active state where the game is currently being played.
 * <p>Holds a reference to the game context to access player information during the loop.
 */
public class InGameState implements State {

  private final GameCheckers game;

  /**
   * Initializes the state with a reference to the game logic.
   *
   * @param game The main game controller context.
   */
  public InGameState(GameCheckers game) {
    this.game = game;
  }

  @Override
  public boolean isGameOver() {
    // The game continues; the controller loop should keep running.
    return false;
  }

  @Override 
  public void handle() {
    System.out.println("The game continues. Current turn: " + game.getCurrentPlayer());
  }
}