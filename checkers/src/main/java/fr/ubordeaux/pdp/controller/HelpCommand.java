package fr.ubordeaux.pdp.controller;

import fr.ubordeaux.pdp.model.Utils;

public class HelpCommand implements Command, Helpable {

  private final String[] args;

  public HelpCommand(String[] args) {
    this.args = args;
  }

  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println("List of all availaible commands:");
      for (String cmd : Utils.COMMANDS_LIST) {
        System.out.println("- " + cmd);
      }
      return;
    }

    String targetName = args[0];
    Command targetCommand = Utils.COMMANDS_MAP.get(targetName);
    if (targetCommand == null) {
      System.out.println("Error : The command '" + targetName + "' doesn't exist.");
    } else if (targetCommand instanceof Helpable helpable) { 
      System.out.println(helpable.getHelp());
    } else {
      System.out.println("There is no help for '" + targetName + "'.");
    }
  }
  
  @Override
  public String getHelp() {
    return "help [CMD] : Display the help of the shell of 'CMD'.";
  }

}
