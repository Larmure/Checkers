package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;

public class RedoCommand implements Command, Helpable {
  private GameController controller;

  public RedoCommand(GameController controller) {
      this.controller = controller;
  }

  @Override
  public void execute() {
    controller.redoGame();
  }

  /**
   * Returns the help string for the redo command.
   *
   * @return A brief description of the redo functionality.
   */
  @Override
  public String getHelp() {
      return "redo [N] : Replay the last canceled move. (or the N-last)";
  }

}
