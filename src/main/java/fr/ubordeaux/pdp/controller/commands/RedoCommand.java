package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.tools.Internationalization;

/**
 * Concrete implementation of {@link Command} and {@link Helpable} used to redo a previously undone 
 * move.
 * This command allows the user to redo moves that were undone, providing a way to navigate through
 */
public class RedoCommand implements Command, Helpable {
  /** The game controller to operate on. */
  private final GameController controller;
  /** The arguments for the redo command. */
  private final String[] args;

  /**
   * Constructs a RedoCommand with the given game controller and optional arguments.
   *
   * @param controller the GameController instance to operate on
   * @param args the arguments for the redo command
   */
  public RedoCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /**
   * Executes the redo command by invoking the redoGame method on the controller with the
   * appropriate number of moves to redo based on the provided arguments. 
   * If no arguments are provided, it defaults to redoing one move.
   */
  @Override
  public void execute() {
    if (args != null && args.length > 0) {
      try {
        int n = Integer.parseInt(args[0]);
        controller.redoGame(n);
      } catch (NumberFormatException e) {
        System.out.println("Invalid argument for redo command.");
      }
    } else {
      controller.redoGame(1);
    }
  }

  /**
   * Returns the help string for the redo command.
   *
   * @return A brief description of the redo functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("redo.help");
  }

}
