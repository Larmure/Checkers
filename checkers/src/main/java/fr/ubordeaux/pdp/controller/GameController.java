package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;

public class GameController {
  private final GameView view;
  private final GameCheckers game;

  public GameController(GameCheckers game, GameView view) {
    this.view = view;
    this.game = game;
  }

  public void start() {
    view.setController(this);
    view.start();
  }

  public void executeCommand(String commandName, String[] args) {
    Command command = switch (commandName.toLowerCase()) {
      case "new" -> new NewCommand(this, args);
      case "quit" -> new QuitCommand();
      default -> {
        System.out.println("Unknown command: " 
            + commandName);
        yield null; 
      }
    };

    if (command != null) {
      command.execute();
    }
  }

  public void startNewGame(boolean blitz, boolean contest, int time, int size) {
    System.out.println("Initializing new game with options: " 
        + (blitz ? "Blitz " : "") 
        + (contest ? "Contest " : "") 
        + (time > 0 ? "Time=" + time + "s " : "") 
        + (size != 8 ? "Size=" + size : ""));
  }
}
