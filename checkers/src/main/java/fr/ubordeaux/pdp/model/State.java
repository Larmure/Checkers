package fr.ubordeaux.pdp.model;

/**
 * Interface representing a specific state in the checkers game lifecycle. 
 * Allows for state-specific behavior and transitions.
 */
public interface State {

  /**
   * Handles the current game logic or transitions based on the active state.
   *
   * @param game The GameCheckers instance to act upon.
   */
  void handle(GameCheckers game);
}