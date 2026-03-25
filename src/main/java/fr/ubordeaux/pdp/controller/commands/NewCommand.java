package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Concrete implementation of {@link Command} that handles the initialization of
 * a new game.
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

      final boolean hasBlitz = cmd.hasOption("b");
      final boolean hasContest = cmd.hasOption("c");
      final int blitzTime = Integer.parseInt(cmd.getOptionValue("t",
          String.valueOf(Utils.DEFAULT_TIME)));
      final int size = Integer.parseInt(cmd.getOptionValue("s",
          String.valueOf(Utils.DEFAULT_BOARD_SIZE)));
      final String aiPlayers = cmd.getOptionValue("ai", "none");
      long aiTime = Long.parseLong(cmd.getOptionValue("at",
          String.valueOf(Ai.DEFAULT_MAX_TIME_MS)));
      aiTime *= 1000; // Convert seconds to milliseconds
      if (aiTime < Ai.MIN_TIME_MS || aiTime > Ai.MAX_TIME_MS) {
        aiTime = Ai.DEFAULT_MAX_TIME_MS;
      }
      String aiMode = cmd.getOptionValue("am", Utils.DEFAULT_AI_MODE);
      int aiDepth = Integer.parseInt(cmd.getOptionValue("ad",
          String.valueOf(Ai.DEFAULT_DEPTH)));

      SelectionMode selectionMode = Mcts.DEFAULT_SELECTION_MODE;
      if (cmd.hasOption("as")) {
        String selectionValue = cmd.getOptionValue("as").toUpperCase();
        if ("UCT".equals(selectionValue)) {
          selectionMode = SelectionMode.UCT;
        } else if ("ML".equals(selectionValue)) {
          selectionMode = SelectionMode.ML;
        } else {
          System.out.println(Internationalization.get("app.warn.invalid_ai_selection")
              + selectionValue);
        }
      }

      controller.startNewGame(new Configuration(
          hasBlitz, blitzTime, hasContest, size,
          controller.isVerbose(), controller.isDebug(),
          "a".equals(aiPlayers) || "w".equals(aiPlayers), "a".equals(aiPlayers)
              || "b".equals(aiPlayers),
          aiTime, aiMode, aiDepth, selectionMode));

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
   * <li>-s, --size : Set board size</li>
   * <li>-a, --ai : Set AI players (a=all, b=black, w=white)</li>
   * <li>-at, --ai-time : Set AI time limit in seconds</li>
   * <li>-am, --ai-mode : Set AI mode</li>
   * <li>-ad, --ai-depth : Set AI search depth</li>
   * <li>-as, --ai-selection : Set MCTS selection mode (uct|ml)</li>
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
    opts.addOption("a", "ai", true, "AI players (a=all, b=black, w=white)");
    opts.addOption("at", "ai-time", true, "AI time limit in seconds");
    opts.addOption("am", "ai-mode", true, "AI mode");
    opts.addOption("ad", "ai-depth", true, "AI search depth");
    opts.addOption("as", "ai-mcts-selection", true, "MCTS selection mode (uct|ml)");
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
