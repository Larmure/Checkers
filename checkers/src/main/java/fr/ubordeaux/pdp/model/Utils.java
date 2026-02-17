package fr.ubordeaux.pdp.model;

import java.util.Map;

import fr.ubordeaux.pdp.controller.*;

public class Utils {

	public static final int DEFAULT_BOARD_SIZE = 8;
	public static final int DEFAULT_TIME = 30;

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
    "show board|history|time",
    "show configuration",
    "set PARAM=VALUE"
  };

}
