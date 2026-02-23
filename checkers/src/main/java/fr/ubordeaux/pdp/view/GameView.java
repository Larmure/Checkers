package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.GameCheckers;

/**
 * Abstract representation of the game's user interface.
 * This class serves as the base for all types of views (CLI, GUI) and 
 * implements the {@link Observer} interface to react to model updates.
 *
 * @version 1.0
 */
public abstract class GameView implements Observer {

  /**
   * The controller used to relay user actions from the view to the game logic.
   */
  protected GameController controller;

  /**
   * Links the view to its controller.
   *
   * @param controller The controller instance to be associated with this view.
   */
  public void setController(GameController controller) {
    this.controller = controller;   
  }

  /**
   * Initializes and starts the view's main lifecycle.
   * For a CLI, this might launch the input loop. For a GUI, it would display the frame.
   */
  public abstract void start();
  
  /**
   * Renders the current state of the game to the user.
   */
  public abstract void display(GameCheckers game);
}
