package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.LoadBoard;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads a previously saved game from a file in the save directory.
 *
 * <p>The load process reads the save file in a single pass.
 * Settings, board rows, and history are collected from the file, then the
 * corresponding {@link GameCheckers} instance is reconstructed and injected
 * into the controller.
 *
 * <p>On success, the controller is updated via
 * {@link GameController#setGame(GameCheckers, Configuration)}.
 */
public class LoadCommand implements Command, Helpable {

  /** The directory where save files are stored. */
  private static final String SAVE_DIRECTORY = System.getProperty("user.dir") + File.separator
      + "Sauvegarde";

  /** The game controller. */
  private final GameController controller;
  /** The command arguments, expected to contain the file name. */
  private final String[] args;

  /**
   * Constructs a new LoadCommand with the specified controller and arguments.
   *
   * @param controller the game controller to update upon successful loading
   * @param args the command arguments, expected to contain the file name
   */
  public LoadCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /**
   * Executes the load command.
   *
   * <p>Expected argument: the file name (e.g. {@code mygame.txt}).
   * The file must exist in the save directory.
   */
  @Override
  public void execute() {
    if (args == null || args.length == 0 || args[0].trim().isEmpty()) {
      System.out.println(getHelp());
      return;
    }

    String fileName = Paths.get(args[0].trim()).getFileName().toString();
    if (fileName == null || fileName.isBlank()) {
      System.out.println(getHelp());
      return;
    }

    Path path = Paths.get(SAVE_DIRECTORY, fileName);

    if (!path.toFile().exists()) {
      System.out.println("Loading error: file not found: " + path);
      return;
    }

    LoadBoard loader = new LoadBoard();
    loader.loadGameData(fileName);

    GameCheckers loadedGame = loader.getLoadedGame();
    Configuration loadedConfig = loader.getLoadedConfiguration();

    if (loadedGame == null || loadedConfig == null) {
      System.out.println("Loading error: could not restore game state.");
      return;
    }

    controller.setGame(loadedGame, loadedConfig);
    System.out.println("Game loaded: " + fileName);
  }

  /**
   * Returns the help message for the load command.
   *
   * @return the help message
   */
  @Override
  public String getHelp() {
    return "load <file> — Loads a saved game from the save directory.\n"
        + "Example: load mygame.txt";
  }
}