package fr.ubordeaux.pdp.model.tools;

import java.util.List;

import fr.ubordeaux.pdp.model.player.*;
import fr.ubordeaux.pdp.model.core.*;

/**
 * Manages the undo and redo operations for the Checkers game.
 *
 * <p>
 * This class acts as a coordinator between the {@link Board} and the
 * {@link History},
 * ensuring that moves are accurately recorded, reverted, and reapplied while
 * maintaining
 * the correct state of the board (including captured pieces and promotions).
 */
public class ManagerUndoRedo {

  private final History history;
  private final Board board;

  /**
   * Constructs a new {@code ManagerUndoRedo} instance.
   *
   * @param board the game board on which the moves will be applied and reverted
   */
  public ManagerUndoRedo(Board board) {
    this.board = board;
    this.history = new History();
  }

  /**
   * Registers a newly played move in the history and clears the redo stack.
   *
   * <p>
   * This method should be called every time a player makes a valid new move,
   * as making a new move invalidates any previously undone moves available for
   * redo.
   *
   * @param color the color of the player who made the move
   * @param move  the move that was played
   */
  public void registerMove(PlayerColor color, Move move) {
    history.addMove(color, move);
    history.clearRedo();
  }

  /**
   * Reverts the last played move on the board and adds it to the redo stack.
   *
   * <p>
   * This process includes reversing the piece's trajectory, demoting a piece if
   * a promotion occurred during the move, and restoring any captured pieces to
   * their
   * exact previous state.
   *
   * @param isWhiteTurn a boolean indicating if it is currently white's turn
   *                    before the undo
   * @return {@code true} if the undo operation was successful, {@code false}
   *         otherwise
   */
  public boolean undo(boolean isWhiteTurn) {
    try {
      Move lastMove = history.getLastMove();

      if (lastMove != null) {
        PlayerColor colorOfMove = !isWhiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
        history.addMoveRedo(colorOfMove, lastMove);

        // Reverse the main movement trajectory
        Move undoMove = new Move(lastMove.getTo(), lastMove.getFrom());
        board.applyMove(undoMove);

        // Revert promotion if the piece was promoted during the last move
        if (lastMove.isPromotion()) {
          board.demoteBit(lastMove.getFrom());
        }

        // Restore all captured pieces with their exact original types
        List<Integer> captures = lastMove.getCaptured();
        List<String> types = lastMove.getCapturedColors();

        for (int i = 0; i < captures.size(); i++) {
          board.restorePiece(captures.get(i), types.get(i));
        }

        // Remove the move from the main history stack
        history.reMove();

        return true;
      }
    } catch (IllegalArgumentException e) {
      System.out.println(Internationalization.get("game.no_moves_to_undo"));
    }

    return false;
  }

  /**
   * Reapplies the last undone move on the board and restores it to the main
   * history.
   *
   * <p>
   * The move's captured colors are cleared before reapplication to prevent
   * duplicating captured piece records during the board's standard applyMove
   * phase.
   *
   * @param isWhiteTurn a boolean indicating if it is currently white's turn
   *                    before the redo
   * @return {@code true} if the redo operation was successful, {@code false}
   *         otherwise
   */
  public boolean redo(boolean isWhiteTurn) {
    if (!history.hasRedo()) {
      System.out.println(Internationalization.get("game.no_moves_to_redo"));
      return false;
    }

    try {
      Move redoMove = history.getLastMoveRedo();
      PlayerColor currentColor = isWhiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;

      if (redoMove != null) {
        // Clear captured colors to avoid duplication when applyMove processes the
        // captures again
        redoMove.getCapturedColors().clear();

        // Reapply the move onto the board
        board.applyMove(redoMove);

        // Update the history stacks
        history.addMove(currentColor, redoMove);
        history.removeLastMoveRedo();

        return true;
      }
    } catch (IllegalArgumentException e) {
      System.out.println(Internationalization.get("game.redo_failed") + " " + e.getMessage());
    }

    return false;
  }
}