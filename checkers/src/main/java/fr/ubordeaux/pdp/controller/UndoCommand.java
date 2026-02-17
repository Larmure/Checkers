package fr.ubordeaux.pdp.controller;

public class UndoCommand implements Command, Helpable {

  @Override
  public void execute() {
      throw new UnsupportedOperationException("Not supported yet.");
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
