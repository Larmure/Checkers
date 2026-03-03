package fr.ubordeaux.pdp.model.player;

/**
 * Represents a player controlled by user input.
 * 
 * <p>
 * This class distinguishes human participants from AI or remote network
 * players.
 */
public class HumanPlayer extends Player {

  /**
   * Creates a new human player.
   *
   * @param name The name to display in the UI.
   */
  public HumanPlayer(String name) {
    super(name);
  }
}