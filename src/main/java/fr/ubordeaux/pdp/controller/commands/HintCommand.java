package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.tools.Internationalization;

/**
 * Executes the hint command by providing a hint to the player.
 * The actual hint generation logic is not implemented in this version and
 * simply prints a placeholder message to the console.
 *
 * @version 1.0
 */
public class HintCommand implements Command, Helpable {

  /**
   * Executes the hint command by providing a hint to the player.
   * The actual hint generation logic is not implemented in this version and
   * simply prints a placeholder message to the console.
   */
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
