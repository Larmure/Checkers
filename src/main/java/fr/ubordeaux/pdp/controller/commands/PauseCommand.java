package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import java.util.Timer;

/**
 * Concrete implementation of {@link Command} used to pause the game.
 * This command allows the user to temporarily halt the game, changing the game
 * state to PAUSE and notifying observers to update the game state accordingly.
 *
 * @version 1.0
 */
public class PauseCommand implements Command, Helpable {
  /** The timer used for blitz mode. */
  private final Timer blitzTimer;
  /** The game instance to operate on. */
  private final GameCheckers game;

  /**
  * Constructs a PauseCommand with the given blitz timer and game instance.
  *
  * @param blitzTimer the Timer instance used for blitz mode,
  *     which will be canceled when the game is paused
  * @param game the GameCheckers instance to operate on when the command
  *     is executed, allowing it to change the game state to PAUSE
  */
  public PauseCommand(Timer blitzTimer, GameCheckers game) {
    this.blitzTimer = blitzTimer;
    this.game = game;
  }

  /**
  * Executes the pause command by canceling the blitz timer if the game is in blitz mode
  * and setting the game state to PAUSE.
  *
  * <p>It also prints appropriate messages based on the current state of the game.
  * If the game is not in blitz mode, it informs the user that pausing is not
  * applicable. If the game is already paused, it informs the user that the game
  * is already paused.
  *
  * <p>Otherwise, it cancels the blitz timer, sets the game state to PAUSE,
  * and prints a message indicating that the game has been paused.
  */
  @Override
  public void execute() {
    if (game.getState() == State.PAUSE) {
      System.out.println(Internationalization.get("game.already_paused"));
      return;
    }

    if (game.getConfiguration().isBlitz() && blitzTimer != null) {
      blitzTimer.cancel();
    }

    game.setState(State.PAUSE);
    System.out.println(Internationalization.get("game.pause"));
  }

  /**
   * Returns the help string for the pause command.
   *
   * @return A brief description of the pause functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("pause.help");
  }

}
