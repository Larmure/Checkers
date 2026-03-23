package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.State;

/**
 * Concrete implementation of {@link Command} used to continue a paused game.
 * This command allows the user to resume a game that was previously paused,
 * changing the game state back to IN_GAME and notifying observers to update
 * the game state accordingly.
 *
 * @version 1.0
 */
public class ContinueCommand implements Command {
  /** The game instance on which this command operates. */
  private final GameController gc;

  /**
   * Constructs a ContinueCommand with the given game instance.
   *
   * @param gc the GameController instance to operate on when the command is executed
   */
  public ContinueCommand(GameController gc) {
    this.gc = gc;
  }

  /**
   * Executes the continue command by setting the game state to IN_GAME and
   * notifying observers to update the game state accordingly.
   */
  @Override
  public void execute() {
    if (gc.isBlitz()) {
      gc.startBlitzTimer();
    }
    gc.getGame().setState(State.IN_GAME);
    gc.getGame().notifyObservers();
  }
}