package fr.ubordeaux.pdp.model;

/**
 * Interface representing a specific state in the game lifecycle (State Pattern).
 * <p>This allows the game to switch behavior dynamically between being in-progress
 * and finished, without the controller needing complex conditional logic.
 */
public interface State {

  /**
   * Determines if the game loop should terminate.
   *
   * @return {@code true} if the game is over and input should stop; {@code false} otherwise.
   */
  boolean isGameOver();

  /**
   * Executes the logic corresponding to the current state.
   * <p>For an active game, this might print status or prompt for moves.
   * For a finished game, this handles the end-of-match summary.
   */
  void handle();
}