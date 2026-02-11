package fr.ubordeaux.pdp.view;

import java.util.Arrays;
import java.util.Scanner;

import fr.ubordeaux.pdp.controller.GameController;

public class CLI extends GameView {

  private boolean verbose = false;
  private boolean debug = false;

  public CLI(boolean verbose, boolean debug) {
    this.verbose = verbose;
    this.debug = debug;
  }

  @Override
  public void display() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void update() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void start() {
    if (verbose) 
      System.out.println("[Info] CLI mode started with verbose output.");
    if (debug) 
      System.out.println("[Debug] CLI mode started with debug output.");
    
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.print("> ");

      if (!scanner.hasNextLine())
        break;
    
      String input = scanner.nextLine().trim();
    
      if (input.isEmpty())
        continue;
    
      if (input.equalsIgnoreCase("exit"))
        break;
    
      try {
        String[] tokens = input.split("\\s+");
        String commandName = tokens[0];     
        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);
    
        controller.executeCommand(commandName, args);

      } catch (Exception e) {
        System.out.println("Invalid command: " + e.getMessage());
      }
    }

  }

  @Override
  public void setController(GameController controller) {
    this.controller = controller;
  }

}