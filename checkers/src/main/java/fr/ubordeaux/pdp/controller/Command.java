package fr.ubordeaux.pdp.controller;

/**
 * Functional interface representing an executable action.
 * Following the Command Design Pattern, this interface encapsulates a request 
 * as an object, allowing for decoupling between the invoker and the 
 * actual logic execution.
 *
 * @version 1.0
 */
public interface Command {

  /**
   * Performs the logic associated with this command.
   * This method is called by the executor (e.g., the Controller or a CommandBag) 
   * to trigger the action defined by the concrete implementation.
   */
  void execute();

}