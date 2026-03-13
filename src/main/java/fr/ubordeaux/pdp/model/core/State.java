package fr.ubordeaux.pdp.model.core;

/**
 * Interface representing a specific state in the game lifecycle (State
 * Pattern).
 * 
 * <p>This allows the game to switch behavior dynamically between being in-progress
 * and finished, without the controller needing complex conditional logic.
 */
public enum State {
  /** The game is currently in progress. */
  IN_GAME,
  /** The game has been finished. */
  FINISHED,
  /** The game is paused. */
  PAUSE;
}