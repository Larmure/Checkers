package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.tools.Internationalization;

/**
 * Command to show various aspects of the game such as the board, move history,
 * time, and configuration.
 *
 * @version 1.0
 */
public class ShowCommand implements Command, Helpable {

  /** The raw arguments provided by the user in the shell. */
  private final String[] args;

  /** The controller to which the game initialization is delegated. */
  private final GameController controller;

  /**
   * Constructs a ShowCommand with the specified game controller and arguments.
   *
   * @param gc the GameController instance to operate on
   * @param args the arguments for the show command, where args[0] should specify what to show
   *     (e.g., "board", "history", "time", "configuration")
   */
  public ShowCommand(GameController gc, String[] args) {
    this.controller = gc;
    this.args = args;
  }

  /**
   * Executes the show command by interpreting the provided arguments and
   * displaying
   * the corresponding information. It supports showing the board, move history,
   * time, and configuration. If the argument is unrecognized, it provides a
   * default message to the user.
   *
   */
  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println(getHelp());
      return;
    }

    switch (args[0]) {
      case "board" -> controller.displayBoard();
      case "history" -> controller.displayHistory();
      case "time" -> controller.displayTime();
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
