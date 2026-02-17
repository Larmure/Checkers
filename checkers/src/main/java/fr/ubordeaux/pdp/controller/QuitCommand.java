package fr.ubordeaux.pdp.controller;

/**
 * Concrete implementation of {@link Command} used to terminate the application.
 * This command provides a clean way for the user to exit the game environment 
 * from the command line interface.
 *
 * @version 1.0
 */
public class QuitCommand implements Command, Helpable {

  /**
   * Executes the quit sequence.
   * Displays a termination message and shuts down the Java Virtual Machine (JVM) 
   * with a status code of 0 (successful termination).
   *
   * <p>TODO: We should ask if the user want to save before leaving. [F16]
   */
  @Override
  public void execute() {
    System.out.println("Exiting the game.");
    System.exit(0);
  }

  /**
   * Returns the help string for the quit command.
   *
   * @return A brief description of the quit functionality.
   */
  @Override
  public String getHelp() {
    return "quit : exit the program.";
  }

}