package fr.ubordeaux.pdp.controller;

public class ShowCommand implements Command, Helpable {

  @Override
  public void execute() {
      throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Returns the help string for the show command.
   *
   * @return A brief description of the show functionality.
   */
  @Override
  public String getHelp() {
      return "show board|history|time|configuration : Display the board, moves history, the remaining time of each player or the configuration.";
  }

}
