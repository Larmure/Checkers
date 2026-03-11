package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.model.core.*;

public class ContinueCommand implements Command {
  private final GameCheckers game;

  public ContinueCommand(GameCheckers game) {
    this.game = game;
  }

  @Override
  public void execute() {
    game.setState(State.IN_GAME);
    game.notifyObservers();
  }
}