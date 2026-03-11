package fr.ubordeaux.pdp.controller.commands;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.LoadBoard;

public class LoadCommand implements Command, Helpable {

    private final GameController controller;
    private final String[] args;
    private final String saveDirectory =
        System.getProperty("user.dir") + File.separator + "Sauvegarde";

    public LoadCommand(GameController controller, String[] args) {
      this.controller = controller;
      this.args = args;
    }

    @Override
    public void execute() {
      if (args == null || args.length == 0) {
        System.out.println(getHelp());
        return;
      }

      String fileName = args[0].trim();
      if (fileName.isEmpty()) {
        System.out.println(getHelp());
        return;
      }

      Path path = Paths.get(saveDirectory, fileName);
      File file = path.toFile();

      if (!file.exists()) {
        System.out.println("Loading Error: File not found: " + path);
        return;
      }

      Configuration defaultConfig = Configuration.getDefaultConfiguration();
      GameCheckers tempGame = new GameCheckers(defaultConfig);
      LoadBoard tempLoader = new LoadBoard(tempGame);

      tempLoader.loadGameData(fileName);

      Configuration loadedConfig = tempLoader.getLoadedConfiguration();
      if (loadedConfig == null) {
        System.out.println("Loading Error: unable to load configuration.");
        return;
      }

      GameCheckers loadedGame = new GameCheckers(loadedConfig);
      LoadBoard finalLoader = new LoadBoard(loadedGame);
      finalLoader.loadGameData(fileName);

      if (finalLoader.getLoadedConfiguration() == null) {
        System.out.println("Loading Error: unable to restore game.");
        return;
      }

      controller.setGame(loadedGame, loadedConfig);
      System.out.println("Game loaded: " + fileName);
    }

    @Override
    public String getHelp() {
      return "load FILE : Load a game from a file.\nExample: load mygame.txt";
    }
}