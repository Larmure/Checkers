package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

public class UndoCommand implements Command, Helpable {
  private final GameController controller;
  private final String[] args;

  public UndoCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  @Override
  public void execute() {
    if (args != null && args.length > 0) {
      try {
        int n = Integer.parseInt(args[0]);
        controller.undoGame(n);
      } catch (NumberFormatException e) {
        System.out.println("Invalid argument for undo command.");
      }
    } else {
      controller.undoGame(1);
    }
  }

  /**
   * Returns the help string for the undo command.
   *
   * @return A brief description of the undo functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("undo.help");
  }

}
