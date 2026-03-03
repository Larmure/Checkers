package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

/**
 * Concrete implementation of {@link Command} and {@link Helpable} that provides
 * assistance to the user.
 * It can either list all available commands or provide specific documentation
 * for a given command by leveraging the {@link Helpable} interface.
 *
 * @version 1.0
 */
public class HelpCommand implements Command, Helpable {

  /**
   * The arguments passed to the help command (usually the name of another
   * command).
   */
  private final String[] args;

  /**
   * Constructs a HelpCommand with optional arguments.
   *
   * @param args Arguments where the first element is the target command name for
   *             help.
   */
  public HelpCommand(String[] args) {
    this.args = args;
  }

  /**
   * Executes the help logic.
   * If no arguments are provided, it lists all registered commands.
   * If a command name is provided, it attempts to retrieve its help string
   * if the command implements {@link Helpable}.
   */
  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println(Internationalization.get("help") + "\n");
      for (String cmd : Utils.COMMANDS_LIST) {
        System.out.println("- " + cmd);
      }
      return;
    }

    String targetName = args[0];
    Command targetCommand = Utils.COMMANDS_MAP.get(targetName);
    if (targetCommand == null) {
      System.out.println(Internationalization.get("command.not_found") + targetName);
    } else if (targetCommand instanceof Helpable helpable) {
      System.out.println(helpable.getHelp());
    } else {
      System.out.println(Internationalization.get("command.no_help") + targetName);
    }
  }

  /**
   * Returns the help string for the help command itself.
   *
   * @return A string explaining the syntax for using the help command.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("help.help");
  }

}
