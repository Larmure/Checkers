package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.Internationalization;

public class RedoCommand implements Command, Helpable {

  @Override
  public void execute() {
      System.out.println(Internationalization.get("redo.execute"));
  }

  /**
   * Returns the help string for the redo command.
   *
   * @return A brief description of the redo functionality.
   */
  @Override
  public String getHelp() {
      return Internationalization.get("redo.help");
  }

}
