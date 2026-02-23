package fr.ubordeaux.pdp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a move in a checkers game.
 *
 * <p>A move can be a simple diagonal step, a single or multi-capture sequence, and/or a promotion.
 * The move stores the starting index, the full path of squares visited, the indices of any captured
 * pieces, and whether the move results in a promotion.
 */
public class Move {

  private final int from;
  private final List<Integer> path;
  private final List<Integer> captured;
  private boolean promotion;

  /**
   * Creates a simple (non-capture) move between two adjacent diagonal squares.
   *
   * @param from the index of the starting square
   * @param to the index of the destination square
   */
  public Move(int from, int to) {
    this.from = from;
    this.path = new ArrayList<>();
    this.captured = new ArrayList<>();
    this.path.add(from);
    this.path.add(to);
    this.promotion = false;
  }

  /**
   * Creates a capture move with a full movement path and the list of captured piece indices.
   *
   * @param path the full sequence of squares visited, including the starting square
   * @param captured the indices of all pieces captured along the path
   */
  public Move(List<Integer> path, List<Integer> captured) {
    this.from = path.get(0);
    this.path = new ArrayList<>(path);
    this.captured = new ArrayList<>(captured);
    this.promotion = false;
  }

  /**
   * Returns the index of the starting square.
   *
   * @return the starting square index
   */
  public int getFrom() {
    return from;
  }

  /**
   * Returns the index of the destination square (last square in the path).
   *
   * @return the destination square index
   */
  public int getTo() {
    return path.get(path.size() - 1);
  }

  /**
   * Returns the full movement path, including the starting square.
   *
   * @return an unmodifiable view of the path
   */
  public List<Integer> getPath() {
    return path;
  }

  /**
   * Returns the indices of all pieces captured during this move.
   *
   * @return the list of captured piece indices, empty if no captures occurred
   */
  public List<Integer> getCaptured() {
    return captured;
  }

  /**
   * Returns whether this move captures at least one opponent piece.
   *
   * @return {@code true} if at least one piece is captured
   */
  public boolean isCapture() {
    return !captured.isEmpty();
  }

  /**
   * Returns whether this move results in a promotion.
   *
   * @return {@code true} if the moving piece is promoted at the end of this move
   */
  public boolean isPromotion() {
    return promotion;
  }

  /**
   * Sets whether this move results in a promotion.
   *
   * @param promotion {@code true} if the piece should be promoted after this move
   */
  public void setPromotion(boolean promotion) {
    this.promotion = promotion;
  }

  /**
   * Returns whether this is a plain simple move (no captures, single step).
   *
   * @return {@code true} if the path has exactly two squares and no captures occurred
   */
  public boolean isSimpleMove() {
    return captured.isEmpty() && path.size() == 2;
  }

  /**
   * Returns a human-readable representation of the move.
   *
   * <p>Squares are joined by {@code "-"} for simple moves or {@code "x"} for captures. A
   * {@code "(promotion)"} suffix is appended when applicable. Example: {@code "21x14x7
   * (promotion)"}
   *
   * @return the string representation of this move
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < path.size(); i++) {
      if (i > 0) {
        sb.append(isCapture() ? "x" : "-");
      }
      sb.append(path.get(i));
    }

    if (promotion) {
      sb.append(" (promotion)");
    }

    return sb.toString();
  }
}