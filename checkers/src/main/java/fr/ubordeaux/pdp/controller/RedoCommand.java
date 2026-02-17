package fr.ubordeaux.pdp.controller;

public class RedoCommand implements Command, Helpable {

  @Override
  public void execute() {
      throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Returns the help string for the redo command.
   *
   * @return A brief description of the redo functionality.
   */
  @Override
  public String getHelp() {
      return "redo [N] : Replay the last canceled move. (or the N-last)";
  }

}
