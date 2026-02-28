package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.Internationalization;

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
      case "history" -> System.out.println(Internationalization.get("show.history"));
      case "time" -> System.out.println(Internationalization.get("show.time"));
      case "configuration" -> controller.displayConfiguration();
      default -> System.out.println(args[0] + Internationalization.get("show.default"));
    }
  }

  /**
   * Returns the help string for the show command.
   *
   * @return A brief description of the show functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("show.help");
  }

}
