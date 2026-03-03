package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

/**
 * Defines a receiver for update notifications from an observable object.
 * This interface is a core component of the Observer Design Pattern,
 * allowing the View to stay synchronized with the Model without being
 * tightly coupled to its implementation.
 *
 * @version 1.0
 */
public interface Observer {

  /**
   * Invoked when the observed object's state has changed.
   * Implementing classes (like {@link GameView}) should use this method
   * to refresh their display or process the new state.
   */
  void update(GameCheckers game);

}
