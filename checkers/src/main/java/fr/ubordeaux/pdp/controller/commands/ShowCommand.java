package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;

public class ShowCommand implements Command, Helpable {

  /** The raw arguments provided by the user in the shell. */
  private final String[] args;

  /** The controller to which the game initialization is delegated. */
  private final GameController controller;

  public ShowCommand(GameController gc, String[] args) {
    this.controller = gc;
    this.args = args;
  }

  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println(getHelp());
      return;
    }
    
    switch (args[0]) {
      case "board" -> controller.displayBoard();
      case "history" -> throw new IllegalArgumentException("show history not implemented yet");
      case "time" -> throw new IllegalArgumentException("show time not implemented yet");
      case "configuration" -> controller.displayGameOptions();
      default -> throw new AssertionError();
    }
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
