package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

public class SaveCommand implements Command, Helpable {

  @Override
  public void execute() {
    System.out.println(Internationalization.get("save.execute"));
  }

  /**
   * Returns the help string for the save command.
   *
   * @return A brief description of the save functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("save.help");
  }

}
