package fr.ubordeaux.pdp.model;

import java.util.Map;

import fr.ubordeaux.pdp.controller.*;

public class Utils {

  public static final int DEFAULT_BOARD_SIZE = 8;
  public static final boolean DEFAULT_VERBOSE = false;
  public static final boolean DEFAULT_BLITZ = false;
  public static final int DEFAULT_TIME = 30;

  public static final Map<String, Command> COMMANDS_MAP = createCommandsMap();

  private static Map<String, Command> createCommandsMap() {
    return Map.of(
        "new", new NewCommand(null, null),
        "help", new HelpCommand(null),
        "quit", new QuitCommand());
  }

}
