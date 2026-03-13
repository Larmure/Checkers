package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.SaveBoard;

/**
 * Command that saves the current game to a file.
 */
public class SaveCommand implements Command, Helpable {
  /** The game controller to access game state and save functionality. */
  private final GameController controller;
  /** The arguments for the save command. */
  private final String[] args;

  /**
   * Constructs a SaveCommand with the given game controller and arguments.
   *
   * @param controller the game controller to access game state and save functionality
   * @param args the arguments for the save command, where args[0] should be the file name to save 
   *     the game to
   */
  public SaveCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /**
   * Executes the save command by saving the current game state to a file specified in 
   * the arguments. 
   */
  @Override
  public void execute() {
    GameCheckers game = controller.getGame();
    if (game == null) {
      System.out.println("No game in progress. Use 'new' to start a game first.");
      return;
    }

    if (args == null || args.length == 0) {
      System.out.println(getHelp());
      return;
    }

    String fileName = args[0].trim();
    if (fileName.isEmpty()) {
      System.out.println(getHelp());
      return;
    }

    try {
      new SaveBoard(game.getBoard(), game).saveToFile(fileName);
      controller.markAsSaved();
    } catch (Exception e) {
      System.err.println("Save Error: " + e.getMessage());
    }
  }

  /**
   * Returns the help message for this command.
   *
   * @return a string describing how to use the save command
   */
  @Override
  public String getHelp() {
    return "save FILE : Save the current game.\nExample: save mygame.txt";
  }
}