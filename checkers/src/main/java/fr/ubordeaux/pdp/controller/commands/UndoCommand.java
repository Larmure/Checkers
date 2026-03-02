package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

public class UndoCommand implements Command, Helpable {

  @Override
  public void execute() {
      System.out.println("Undo: This feature is not implemented yet.");
  }

  /**
   * Returns the help string for the undo command.
   *
   * @return A brief description of the undo functionality.
   */
  @Override
  public String getHelp() {
      return "undo [N] : Cancel the last move played. (or the N-last)";
  }

}
