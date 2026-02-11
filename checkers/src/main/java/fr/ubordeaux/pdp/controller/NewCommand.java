package fr.ubordeaux.pdp.controller;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class NewCommand implements Command {
  private final String[] args;
  private final GameController controller;

  public NewCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  @Override
  public void execute() {
    CommandLineParser parser = new DefaultParser();
    
    try {
      CommandLine cmd = parser.parse(newOptions(), args);
  
      boolean blitz = cmd.hasOption("b");
      boolean contest = cmd.hasOption("c");
  
      int time = cmd.hasOption("t")
          ? Integer.parseInt(cmd.getOptionValue("t"))
          : 0;
  
      int size = cmd.hasOption("s")
          ? Integer.parseInt(cmd.getOptionValue("s"))
          : 8;
  
      controller.startNewGame(blitz, contest, time, size);
  
    } catch (ParseException | NumberFormatException e) {
      // On ne capture que ce qui est lié à une mauvaise saisie utilisateur
      System.out.println("Invalid command syntax: " + e.getMessage());
    }
  }

  private Options newOptions() {
    Options opts = new Options();
    opts.addOption("b", "blitz", false, "Blitz mode");
    opts.addOption("c", "contest", false, "Contest mode");
    opts.addOption("t", "time", true, "Time limit");
    opts.addOption("s", "size", true, "Board size");
    return opts;
  }
    
}
