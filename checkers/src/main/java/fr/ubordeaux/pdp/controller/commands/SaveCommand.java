package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.BoardSauvegarde;

public class SaveCommand implements Command, Helpable {

  private final GameController controller;
  private final String[] args;

  public SaveCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

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

    new BoardSauvegarde(game.getBoard(), game).saveToFile(fileName);
    controller.setUnsavedChanges(false);
    System.out.println("[DEBUG] unsavedChanges est maintenant : " + controller.hasUnsavedChanges());
  }

  @Override
  public String getHelp() {
    return "save FILE : Save the current game.\nExample: save mygame.txt";
  }
}