package fr.ubordeaux.pdp.model;

/**
 * Interface for the Observer pattern.
 * 
 * <p>Classes implementing this interface allow {@link fr.ubordeaux.pdp.view.Observer} instances
 * to register and receive notifications when the internal state changes.
 */
public interface Subject {

  /**
   * Notifies all registered observers that the subject's state has changed.
   * 
   * <p>This typically triggers a UI refresh or a console output update.
   */
  void notifyObservers();
}