package fr.ubordeaux.pdp.controller;

/**
 * Interface for commands that can provide their own usage documentation. Implementing this
 * interface allows a command to describe its syntax and options, which is particularly useful for
 * generating a dynamic help system.
 *
 * @version 1.0
 */
public interface Helpable {

  /**
   * Returns a string describing how to use the command.
   *
   * @return The help message including syntax, options, and description.
   */
  String getHelp();
}
