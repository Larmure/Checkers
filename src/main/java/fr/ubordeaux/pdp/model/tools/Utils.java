package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.commands.ContinueCommand;
import fr.ubordeaux.pdp.controller.commands.HelpCommand;
import fr.ubordeaux.pdp.controller.commands.HintCommand;
import fr.ubordeaux.pdp.controller.commands.LoadCommand;
import fr.ubordeaux.pdp.controller.commands.NewCommand;
import fr.ubordeaux.pdp.controller.commands.PauseCommand;
import fr.ubordeaux.pdp.controller.commands.QuitCommand;
import fr.ubordeaux.pdp.controller.commands.RedoCommand;
import fr.ubordeaux.pdp.controller.commands.SaveCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;
import fr.ubordeaux.pdp.controller.commands.SetCommand;
import fr.ubordeaux.pdp.controller.commands.ShowCommand;
import fr.ubordeaux.pdp.controller.commands.UndoCommand;
import java.util.Map;
import java.util.Set;

/**
 * Global utility class containing game constants and the command registry.
 * This class serves as a central point for default configurations and
 * provides a mapping between command names and their respective
 * implementations.
 *
 * @version 1.0
 */
public class Utils {

  /**
   * Private constructor to prevent instantiation of this utility class.
   * This class is not meant to be instantiated, as it only contains static members.
   */
  private Utils() {
  }

  /* Default game settings */

  /** The default size of the game board. */
  public static final int DEFAULT_BOARD_SIZE = 8;
  /** The default debug mode setting. */
  public static final boolean DEFAULT_DEBUG = false;
  /** The default verbose mode setting. */
  public static final boolean DEFAULT_VERBOSE = false;
  /** The default blitz mode setting. */
  public static final boolean DEFAULT_BLITZ = false;
  /** The default time limit for each player's turn. */
  public static final int DEFAULT_TIME = 30;
  /** The default contest mode setting. */
  public static final boolean DEFAULT_CONTEST = false;
  /** The default setting for whether the white player is controlled by AI. */
  public static final boolean DEFAULT_WHITE_AI = false;
  /** The default setting for whether the black player is controlled by AI. */
  public static final boolean DEFAULT_BLACK_AI = false;
  /** The default time limit for AI moves in seconds. */
  public static final int DEFAULT_AI_TIME = 5;
  /** The default AI mode. */
  public static final String DEFAULT_AI_MODE = "minimax";

  /** Regular expression for validating move syntax. */
  public static final String MOVE_REGEX = "^[a-zA-Z]\\d{1,2}\\s[a-zA-Z]\\d{1,2}$";

  /** Set of board sizes accepted by the constructor. */
  public static final Set<Integer> VALID_SIZES = Set.of(8, 10, 12);

  /**
   * An immutable map linking command keywords to their representative instances.
   * 
   * <p>Note: Some instances are initialized with {@code null} parameters as they
   * are primarily used for metadata retrieval (like help strings) rather than
   * execution.
   */
  public static final Map<String, Command> COMMANDS_MAP = Map.ofEntries(
      Map.entry("new", new NewCommand(null, null)),
      Map.entry("help", new HelpCommand(null)),
      Map.entry("quit", new QuitCommand(null)),
      Map.entry("load", new LoadCommand(null, null)),
      Map.entry("save", new SaveCommand(null, null)),
      Map.entry("pause", new PauseCommand(null, null)),
      Map.entry("hint", new HintCommand()),
      Map.entry("undo", new UndoCommand(null, null)),
      Map.entry("redo", new RedoCommand(null, null)),
      Map.entry("show", new ShowCommand(null, null)),
      Map.entry("set", new SetCommand(null, null)),
      Map.entry("server list", new ServerListCommand()),
      Map.entry("server start", new ServerStartCommand(null, null)),
      Map.entry("server stop", new ServerStopCommand()),
      Map.entry("continue", new ContinueCommand(null)));

  /**
   * A formatted list of all available commands and their expected syntax.
   * This array is used to display the global help menu to the user.
   */
  public static final String[] COMMANDS_LIST = {
      "new [ARGS]",
      "help [CMD]",
      "quit",
      "load FILE",
      "save FILE",
      "pause",
      "hint",
      "undo [N]",
      "redo [N]",
      "show board|history|time|configuration",
      "set PARAM=VALUE",
      "server list",
      "server start [PORT]",
      "server stop"
  };

  /** Set of valid AI modes for the game. */
  public static final Set<String> VALID_AI_MODES = Set.of(
      "minimax", "alphabeta", "mcts", "iterative");
}
