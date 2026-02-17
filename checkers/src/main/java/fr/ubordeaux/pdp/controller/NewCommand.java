package fr.ubordeaux.pdp.controller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Concrete implementation of {@link Command} that handles the initialization of a new game.
 * This class uses Apache Commons CLI to parse specific game options such as 
 * blitz mode, contest mode, time limits, and board size.
 *
 * @version 1.0
 */
public class NewCommand implements Command {

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
  
      // If we use time option without blitz, we ignore it.
      int blitzTime = 0;
      boolean hasTimeOption = cmd.hasOption("t");
      if (hasBlitz) {
        String tValue = cmd.getOptionValue("t", "30");
        blitzTime = Integer.parseInt(tValue);
      } else if (hasTimeOption) {
        System.out.println("Warning: time option used without blitz option.");
      }
  
      int size = cmd.hasOption("s")
          ? Integer.parseInt(cmd.getOptionValue("s"))
          : 8;
  
      controller.startNewGame(hasBlitz, hasContest, blitzTime, size);
  
    } catch (ParseException | NumberFormatException e) {
      System.out.println("Invalid command syntax: " + e.getMessage());
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
    
}
