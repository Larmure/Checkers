package fr.ubordeaux.pdp.model;

import java.util.Map;

import fr.ubordeaux.pdp.controller.*;

/**
 * Global utility class containing game constants and the command registry.
 * This class serves as a central point for default configurations and 
 * provides a mapping between command names and their respective implementations.
 *
 * @version 1.0
 */
public class Utils {

  /** The standard dimension of the checkers board (8x8). */
	public static final int DEFAULT_BOARD_SIZE = 8;

  /** The default time limit per player in seconds. */
	public static final int DEFAULT_TIME = 30;

  /**
   * An immutable map linking command keywords to their representative instances.
   * <p>Note: Some instances are initialized with {@code null} parameters as they 
   * are primarily used for metadata retrieval (like help strings) rather than execution.
   */
  public static final Map<String, Command> COMMANDS_MAP = Map.ofEntries(
      Map.entry("new", new NewCommand(null, null)),
      Map.entry("help", new HelpCommand(null)),
      Map.entry("quit", new QuitCommand()),
      Map.entry("load", new LoadCommand()),
      Map.entry("save", new SaveCommand()),
      Map.entry("pause", new PauseCommand()),
      Map.entry("hint", new HintCommand()),
      Map.entry("undo", new UndoCommand()),
      Map.entry("redo", new RedoCommand()),
      Map.entry("show", new ShowCommand()),
      Map.entry("set", new SetCommand())
    );

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
    "set PARAM=VALUE"
  };

}
