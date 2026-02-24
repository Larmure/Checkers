package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

public class SaveCommand implements Command, Helpable {

  @Override
  public void execute() {
      throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Returns the help string for the save command.
   *
   * @return A brief description of the save functionality.
   */
  @Override
  public String getHelp() {
      return "save FILE : Save the current game.";
  }

}
