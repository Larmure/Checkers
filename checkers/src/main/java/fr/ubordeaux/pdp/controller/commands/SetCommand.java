package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

/**
 * Command to set controller parameters like debug and verbose.
 * It expects arguments in the format "parameter=value", where parameter can be
 * "debug" or "verbose" and value can be "true" or "false".
 *
 * @version 1.0
 */
public class SetCommand implements Command, Helpable {

  /** The raw arguments provided by the user in the shell. */
  private final String[] args;

  /** The controller to which the game initialization is delegated. */
  private final GameController controller;

  /**
   * Constructs a SetCommand with the given controller and arguments.
   *
   * @param controller The GameController instance to update based on the command
   *                   arguments.
   * @param args       The raw arguments provided by the user, expected to be in
   *                   the format "parameter=value".
   */
  public SetCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  /**
   * Executes the set command by parsing the arguments and updating the
   * controller's parameters accordingly.
   * It handles various error cases such as missing arguments, invalid formats,
   * and unsupported parameters, providing user feedback through the console.
   */
  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println(getHelp());
      return;
    }

    String[] parts = args[0].split("=", 2);
    if (parts.length < 2) {
      System.out.println(Internationalization.get("set.execute.format") + getHelp());
      return;
    }

    String param = parts[0].trim().toLowerCase();
    String value = parts[1].trim().toLowerCase();

    if (!value.equals("true") && !value.equals("false")) {
      System.out.println(Internationalization.get("set.execute.value1") + value
          + Internationalization.get("set.execute.value2"));
      return;
    }

    boolean boolValue = Boolean.parseBoolean(value);

    switch (param) {
      case "debug" -> {
        controller.setDebug(boolValue);
        System.out.println(Internationalization.get("set.debug") + boolValue);
      }
      case "verbose" -> {
        controller.setVerbose(boolValue);
        System.out.println(Internationalization.get("set.verbose") + boolValue);
      }
      default -> System.out.println("'" + param + Internationalization.get("set.parameter"));
    }
  }

  /**
   * Returns the help string for the set command.
   *
   * @return A brief description of the set functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("set.help");
  }

}
