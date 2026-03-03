package fr.ubordeaux.pdp.model.core;

/**
 * Interface representing a specific state in the game lifecycle (State
 * Pattern).
 * 
 * <p>
 * This allows the game to switch behavior dynamically between being in-progress
 * and finished, without the controller needing complex conditional logic.
 */
public enum State {

  IN_GAME, FINISHED, PAUSE;
}