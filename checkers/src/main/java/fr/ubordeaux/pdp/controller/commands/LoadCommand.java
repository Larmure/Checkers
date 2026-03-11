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
 * <p>The load process runs in two passes:
 *
 * <ol>
 *   <li>A first pass reads the configuration from {@code [settings]} to determine
 *       the board size and game options.</li>
 *   <li>A second pass reconstructs the full game state (board + history) using
 *       a {@link GameCheckers} built from the loaded configuration.</li>
 * </ol>
 *
 * <p>On success, the controller is updated via
 * {@link GameController#setGame(GameCheckers, Configuration)}.
 */
public class LoadCommand implements Command, Helpable {

  private static final String SAVE_DIRECTORY =
        System.getProperty("user.dir") + File.separator + "Sauvegarde";

  private final GameController controller;
  private final String[] args;

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

    String fileName = args[0].trim();
    Path path = Paths.get(SAVE_DIRECTORY, fileName);

    if (!path.toFile().exists()) {
      System.out.println("Loading error: file not found: " + path);
      return;
    }

    // Pass 1: read configuration from [settings]
    Configuration defaultConfig = Configuration.getDefaultConfiguration();
    LoadBoard firstPass = new LoadBoard(new GameCheckers(defaultConfig));
    firstPass.loadGameData(fileName);

    Configuration loadedConfig = firstPass.getLoadedConfiguration();
    if (loadedConfig == null) {
      System.out.println("Loading error: could not read configuration.");
      return;
    }

    // Pass 2: reconstruct full game state with the correct configuration
    GameCheckers loadedGame = new GameCheckers(loadedConfig);
    LoadBoard secondPass = new LoadBoard(loadedGame);
    secondPass.loadGameData(fileName);

    if (secondPass.getLoadedConfiguration() == null) {
      System.out.println("Loading error: could not restore game state.");
      return;
    }

    controller.setGame(loadedGame, loadedConfig);
    System.out.println("Game loaded: " + fileName);
  }

  @Override
  public String getHelp() {
    return "load <file> — Loads a saved game from the save directory.\n"
      + "Example: load mygame.txt";
  }
}