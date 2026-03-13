package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.tools.Internationalization;

/**
 * Command to undo previous moves in the game.
 *
 * @version 1.0
 */
public class UndoCommand implements Command, Helpable {

  /** 
   * The game controller instance. 
   */
  private final GameController controller;

  /** 
   * Optional arguments for the undo command (e.g., number of moves to undo). 
   */
  private final String[] args;

  /** 
   * Creates an undo command with the specified game controller and arguments. 
   */
  public UndoCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /** 
   * Executes the undo command. 
   * If an argument is provided, it attempts to parse it as the number of moves to undo. 
   * If no argument is provided, it defaults to undoing one move. 
   * If the argument is invalid (not a number), it prints an error message. 
   */
  @Override
  public void execute() {
    if (args != null && args.length > 0) {
      try {
        int n = Integer.parseInt(args[0]);
        controller.undoGame(n);
      } catch (NumberFormatException e) {
        System.out.println("Invalid argument for undo command.");
      }
    } else {
      controller.undoGame(1);
    }
  }

  /**
   * Returns the help string for the undo command.
   *
   * @return A brief description of the undo functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("undo.help");
  }

}
