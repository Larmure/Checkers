package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;

public class RedoCommand implements Command, Helpable {
  private GameController controller;
  private final String[] args;

  public RedoCommand(GameController controller, String[] args) {
      this.controller = controller;
      this.args = args;
  }

  @Override
  public void execute() {
    if (args != null && args.length > 0) {
      try {
        int n = Integer.parseInt(args[0]);
        controller.redoGame(n);
      } catch (NumberFormatException e) {
        System.out.println("Invalid argument for redo command.");
      }
    } else {
      controller.redoGame(1);
    }
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
