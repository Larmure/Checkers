package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.Internationalization;

public class UndoCommand implements Command, Helpable {

  @Override
  public void execute() {
      System.out.println(Internationalization.get("undo.execute"));
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
