package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

public class SetCommand implements Command, Helpable {

  @Override
  public void execute() {
      throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Returns the help string for the set command.
   *
   * @return A brief description of the set functionality.
   */
  @Override
  public String getHelp() {
      return "set PARAM=VALUE : Change the current configuration.\nExemple: 'set debug=true'";
  }

}
