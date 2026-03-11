package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;
import java.util.Timer;

public class PauseCommand implements Command, Helpable {
  private final Timer blitzTimer;
  private final GameCheckers game;

  public PauseCommand(Timer blitzTimer, GameCheckers game) {
    this.blitzTimer = blitzTimer;
    this.game = game;
  }

  @Override
  public void execute() {
    blitzTimer.cancel();
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
