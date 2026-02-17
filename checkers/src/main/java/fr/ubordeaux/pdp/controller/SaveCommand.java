package fr.ubordeaux.pdp.controller;

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
