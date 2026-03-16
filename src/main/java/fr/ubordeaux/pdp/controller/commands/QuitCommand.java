package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.SaveBoard;
import java.util.Scanner;

/**
 * Concrete implementation of {@link Command} used to terminate the application.
 * This command provides a clean way for the user to exit the game environment
 * from the command line interface.
 *
 * @version 1.0
 */
public class QuitCommand implements Command, Helpable {
  /** The game controller to access game state and save functionality. */
  private final GameController controller;
  /** The action to perform when quitting the application. */
  private final Runnable exitAction;

  /**
   * Constructs a QuitCommand with the given game controller.
   *
   * @param controller the game controller to access game state and save
   *                   functionality
   */
  public QuitCommand(GameController controller) {
    this(controller, () -> System.exit(0));
  }

  /**
   * Constructs a QuitCommand with the given game controller and exit action.
   *
   * @param controller the game controller to access game state and save functionality
   * @param exitAction the action to perform when quitting the application,
   *     allowing for custom behavior in tests or alternative exit strategies
   */
  public QuitCommand(GameController controller, Runnable exitAction) {
    this.controller = controller;
    this.exitAction = exitAction;
  }

  /**
   * Executes the quit sequence.
   * Prompts the user to save if there are unsaved changes. Displays a termination
   * message and exit with a status code of 0.
   */
  @Override
  public void execute() {
    if (controller.hasUnsavedChanges() && controller.getGame() != null) {
      @SuppressWarnings("resource")
      Scanner scanner = new Scanner(System.in);
      boolean handled = false;

      while (!handled) {
        System.out.print(Internationalization.get("quit.save"));
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("y")) {
          System.out.print(Internationalization.get("quit.path"));
          String path = scanner.nextLine().trim();

          try {
            GameCheckers game = controller.getGame();
            new SaveBoard(game.getBoard(), game).saveToFile(path);
            System.out.println(Internationalization.get("quit.save.ok"));
            handled = true;
          } catch (Exception e) {
            // If an error occurs, the loop continues to ask the user again
            System.err.println(Internationalization.get("quit.save.error", e.getMessage()));
          }
        } else {
          // Any input other than 'y' or 'Y' is treated as default 'N'
          handled = true;
        }
      }
    }

    System.out.println(Internationalization.get("quit.execute"));
    exitAction.run();
  }

  /**
   * Returns the help string for the quit command.
   *
   * @return A brief description of the quit functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("quit.help");
  }

}