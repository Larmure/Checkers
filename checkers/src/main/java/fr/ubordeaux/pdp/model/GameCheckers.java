package fr.ubordeaux.pdp.model;

import java.util.List;

/**
 * Main logic class for the Checkers game, managing rules and state transitions.
 * Implements requirement.
 */
public class GameCheckers {

  private State state;
  private Board board;
  private Player whitePlayer;
  private Player blackPlayer;
  private boolean isWhiteTurn;

  /**
   * Constructs a new game with a board of the specified size.
   *
   * @param size The size of the board (8, 10, or 12).
   */
  public GameCheckers() {
    this.board = new Board(8);
    this.isWhiteTurn = true;
    // this.state = new InGameState(this);
  }

  /**
   * Returns a list of all legal moves for a given player.
   * Mandatory captures are handled by the Board logic.
   */
  public List<Move> getPossibleMoves(Player player) {
    boolean isWhite = (player == whitePlayer);
    return isWhite ? board.getWhiteValidMoves() : board.getBlackValidMoves();
  }

  /**
   * Validates if a move is legal according to current turn and rules.
   */
  public boolean isValidMove(Move move, Player player) {
    // Check if it's the player's turn 
    if ((isWhiteTurn && player != whitePlayer) || (!isWhiteTurn && player != blackPlayer)) {
      return false;
    }
    return getPossibleMoves(player).contains(move);
  }

  /**
   * Applies a move to the board, handles captures/promotions, and swaps turns.
   */
  public void applyMove(Move move) {
    board.applyMove(move);
    this.isWhiteTurn = !this.isWhiteTurn;

    // TODO: notifyObservers(); (Requirement for Observer Pattern in diagram)
  }

  /**
   * Checks for game over conditions: no moves left or draw.
   * Implements part of F10.
   */
  public /* State */ void checkGameOver() {
    Player currentPlayer = isWhiteTurn ? whitePlayer : blackPlayer;

    // If no moves are possible, the player has lost
    if (getPossibleMoves(currentPlayer).isEmpty()) {
      // return new FinishedState();
    }

    // Default state: game continues
    // return new InGameState(this);
  }
}