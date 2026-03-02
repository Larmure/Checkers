package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.Internationalization;

public class HintCommand implements Command, Helpable {

  @Override
  public void execute() {
    System.out.println(Internationalization.get("hint.execute"));
  }

  /**
   * Returns the help string for the hint command.
   *
   * @return A brief description of the hint functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("hint.help");
  }

}
