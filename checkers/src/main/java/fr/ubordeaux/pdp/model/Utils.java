package fr.ubordeaux.pdp.model;

import java.util.Map;
import java.util.Set;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.commands.HelpCommand;
import fr.ubordeaux.pdp.controller.commands.HintCommand;
import fr.ubordeaux.pdp.controller.commands.LoadCommand;
import fr.ubordeaux.pdp.controller.commands.NewCommand;
import fr.ubordeaux.pdp.controller.commands.PauseCommand;
import fr.ubordeaux.pdp.controller.commands.QuitCommand;
import fr.ubordeaux.pdp.controller.commands.RedoCommand;
import fr.ubordeaux.pdp.controller.commands.SaveCommand;
import fr.ubordeaux.pdp.controller.commands.SetCommand;
import fr.ubordeaux.pdp.controller.commands.ShowCommand;
import fr.ubordeaux.pdp.controller.commands.UndoCommand;

/**
 * Global utility class containing game constants and the command registry.
 * This class serves as a central point for default configurations and 
 * provides a mapping between command names and their respective implementations.
 *
 * @version 1.0
 */
public class Utils {

  public static final int DEFAULT_BOARD_SIZE = 8;
  public static final boolean DEFAULT_DEBUG = false;
  public static final boolean DEFAULT_VERBOSE = false;
  public static final boolean DEFAULT_BLITZ = true;
  public static final int DEFAULT_TIME = 30;
  public static final boolean DEFAULT_CONTEST = false;

  public static final String MOVE_REGEX = "^[a-zA-Z]\\d{1,2}\\s[a-zA-Z]\\d{1,2}$";

  /** Set of board sizes accepted by the constructor. */
  public static final Set<Integer> VALID_SIZES = Set.of(8, 10, 12);

  /**
   * An immutable map linking command keywords to their representative instances.
   * <p>Note: Some instances are initialized with {@code null} parameters as they 
   * are primarily used for metadata retrieval (like help strings) rather than execution.
   */
  public static final Map<String, Command> COMMANDS_MAP = Map.ofEntries(
          Map.entry("new", new NewCommand(null, null)),
          Map.entry("help", new HelpCommand(null)),
          Map.entry("quit", new QuitCommand()),
          Map.entry("load", new LoadCommand(null,null)),
          Map.entry("save", new SaveCommand(null, null)),
          Map.entry("pause", new PauseCommand()),
          Map.entry("hint", new HintCommand()),
          Map.entry("undo", new UndoCommand()),
          Map.entry("redo", new RedoCommand()),
          Map.entry("show", new ShowCommand(null, null)),
          Map.entry("set", new SetCommand(null, null))
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
