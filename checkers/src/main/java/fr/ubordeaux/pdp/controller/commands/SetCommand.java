package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;

public class SetCommand implements Command, Helpable {


  /** The raw arguments provided by the user in the shell. */
  private final String[] args;
  
  /** The controller to which the game initialization is delegated. */
  private final GameController controller;

  public SetCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  @Override
public void execute() {
    if (args == null || args.length == 0) {
        System.out.println(getHelp());
        return;
    }
    
    String[] parts = args[0].split("=", 2);
    if (parts.length < 2) {
        System.out.println("Invalid format. Expected PARAM=VALUE.\n" + getHelp());
        return;
    }
    
    String param = parts[0].trim().toLowerCase();
    String value = parts[1].trim().toLowerCase();
    
    if (!value.equals("true") && !value.equals("false")) {
        System.out.println("Invalid value '" + value + "'. Only true or false are accepted.");
        return;
    }
    
    boolean boolValue = Boolean.parseBoolean(value);
    
    switch (param) {
      case "debug"   -> { controller.setDebug(boolValue);   System.out.println("debug set to " + boolValue);   }
      case "verbose" -> { controller.setVerbose(boolValue); System.out.println("verbose set to " + boolValue); }
      default -> System.out.println("'" + param + "' is not a valid parameter. Only debug or verbose are accepted.");
    }
}

  /**
   * Returns the help string for the set command.
   *
   * @return A brief description of the set functionality.
   */
  @Override
  public String getHelp() {
    return "set PARAM=VALUE : Change the current configuration.\nExemple: 'set debug=true'";
  }

}
