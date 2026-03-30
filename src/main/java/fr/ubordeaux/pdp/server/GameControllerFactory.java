package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;

/**
 * Factory that produces a fresh {@link GameController} for each new game session.
 *
 * <p>Using a per-session factory (rather than sharing a single controller instance)
 * ensures that concurrent sessions each have their own isolated game state.
 *
 * <p>This is a functional interface and can be supplied as a lambda:
 *
 * <pre>{@code
 * GameControllerFactory factory = () -> new GameController(new ConsoleView());
 * }</pre>
 */
@FunctionalInterface
public interface GameControllerFactory {

  /**
   * Creates and returns a new {@link GameController} instance.
   *
   * @return a fully initialised controller ready to manage a game.
   */
  GameController create();
}