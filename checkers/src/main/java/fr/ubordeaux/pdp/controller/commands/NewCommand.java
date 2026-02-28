package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.Configuration;
import fr.ubordeaux.pdp.model.Internationalization;
import fr.ubordeaux.pdp.model.Utils;

/**
 * Concrete implementation of {@link Command} that handles the initialization of a new game.
 * This class uses Apache Commons CLI to parse specific game options such as 
 * blitz mode, contest mode, time limits, and board size.
 *
 * @version 1.0
 */
public class NewCommand implements Command, Helpable {

  /** The raw arguments provided by the user in the shell. */
  private final String[] args;

  /** The controller to which the game initialization is delegated. */
  private final GameController controller;

  /**
   * Constructs a NewCommand with the required context and arguments.
   *
   * @param controller The {@link GameController} that will start the game.
   * @param args       The string arguments to be parsed (e.g., "-b -s 10").
   */
  public NewCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /**
   * Parses the arguments and triggers the creation of a new game.
   * If the arguments are invalid (wrong format or unknown options), 
   * an error message is displayed to the user.
   */
  @Override
  public void execute() {
    CommandLineParser parser = new DefaultParser();
    
    try {
      CommandLine cmd = parser.parse(newOptions(), args);
  
    
      boolean hasBlitz = cmd.hasOption("b");
      boolean hasContest = cmd.hasOption("c");
      int blitzTime = Integer.parseInt(cmd.getOptionValue("t", String.valueOf(Utils.DEFAULT_TIME)));
      int size = Integer.parseInt(cmd.getOptionValue("s", String.valueOf(Utils.DEFAULT_BOARD_SIZE)));
          
      controller.startNewGame(new Configuration(
          hasBlitz, blitzTime, hasContest, size,
          controller.isVerbose(), controller.isDebug(),
          controller.isWhiteIsAi(), controller.isBlackIsAi()
      ));

      
    } catch (ParseException | NumberFormatException e) {
      System.out.println(Internationalization.get("new.invalid") + e.getMessage());
    }
  }

  /**
   * Defines the available CLI options for the "new" command.
   * <ul>
   * <li>-b, --blitz : Enable blitz mode</li>
   * <li>-c, --contest : Enable contest mode</li>
   * <li>-t, --time : Set time limit in seconds</li>
   * <li>-s, --size : Set board size </li>
   * </ul>
   *
   * @return An {@link Options} object containing the CLI schema.
   */
  private Options newOptions() {
    Options opts = new Options();
    opts.addOption("b", "blitz", false, "Blitz mode");
    opts.addOption("c", "contest", false, "Contest mode");
    opts.addOption("t", "time", true, "Time limit");
    opts.addOption("s", "size", true, "Board size");
    return opts;
  }

  /**
   * Returns the help string for the new command.
   *
   * @return A brief description of the new game functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("new.help");
  }
    
}
