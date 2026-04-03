package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains a record of moves played during a game to support undo, redo, and persistence.
 *
 * <p>It uses two double-ended queues (Deques) to manage the move stacks and provides methods to
 * serialize and deserialize the game history.
 */
public class History {

  /** Stack of moves that have been played. */
  private final Deque<ColorMove> history;

  /** Stack of moves that have been undone and are available for redo. */
  private final Deque<ColorMove> redoHistory;

  /** Constructs an empty game history. */
  public History() {
    history = new ArrayDeque<>();
    redoHistory = new ArrayDeque<>();
  }

  /**
   * Constructs a game history from a serialized string representation.
   *
   * @param historyToString the string representation of the move history.
   */
  public History(String historyToString) {
    history = new ArrayDeque<>();
    redoHistory = new ArrayDeque<>();
    loadHistory(historyToString);
  }

  /**
   * Adds a move to the primary history stack.
   *
   * @param color the color of the player who performed the move.
   * @param move the move performed.
   */
  public void addMove(PlayerColor color, Move move) {
    history.add(new ColorMove(color, move));
  }

  /**
   * Removes the most recent move from the history stack.
   *
   * @throws IllegalArgumentException if the history is empty.
   */
  public void reMove() {
    if (history.size() != 0) {
      history.removeLast();
    } else {
      throw new IllegalArgumentException("History is Empty");
    }
  }

  /**
   * Retrieves the most recent move from the history stack without removing it.
   *
   * @return the last {@link Move} played.
   * @throws IllegalArgumentException if the history is empty.
   */
  public Move getLastMove() {
    if (history.size() != 0) {
      return history.peekLast().getMove();
    } else {
      throw new IllegalArgumentException("History is Empty");
    }
  }

  /**
   * Parses a serialized history string and populates the history stack.
   *
   * @param historyToString the serialized move data.
   * @throws IllegalArgumentException if the string format is invalid.
   */
  private void loadHistory(String historyToString) {
    if (historyToString == null) {
      return;
    }

    String[] lines = historyToString.split("\\R");

    for (String rawLine : lines) {
      String line = rawLine.trim();

      if (line.isEmpty()) {
        continue;
      }

      line = line.replaceAll("\\{.*?\\}", "").trim();

      if (line.isEmpty()) {
        continue;
      }

      char colorChar = line.charAt(0);
      PlayerColor color;

      switch (colorChar) {
        case 'W' -> color = PlayerColor.WHITE;
        case 'B' -> color = PlayerColor.BLACK;
        default ->
          throw new IllegalArgumentException("History line must start with W or B: " + line);
      }

      String moveText = line.substring(1).trim();

      if (moveText.isEmpty()) {
        throw new IllegalArgumentException("Missing move after color: " + line);
      }

      Move move = Move.fromSaveString(moveText);
      history.add(new ColorMove(color, move));
    }
  }

  /**
   * Generates a string representation of the current move history.
   *
   * @return a formatted string containing all moves and metadata (captures, promotions).
   */
  public String historyString() {
    String h = "";
    for (ColorMove cm : history) {
      String line = "";

      if (cm.getColor() == PlayerColor.WHITE) {
        line += "W " + cm.getMove();
      } else if (cm.getColor() == PlayerColor.BLACK) {
        line += "B " + cm.getMove();
      }

      if (cm.getMove().getCaptured().size() == 1) {
        line += " {Prise simple} " + capturesString(cm.getMove());
      } else if (cm.getMove().getCaptured().size() >= 1) {
        line += " {Prise multiple} " + capturesString(cm.getMove());
      }

      if (cm.getMove().isPromotion()) {
        line += " {Promotion}";
      }

      line += "\n";
      h += line;
    }
    return h;
  }

  /**
   * Generates a string representation of the captured pieces in a move.
   *
   * @param move the move containing captured pieces.
   * @return a formatted string of the captured pieces.
   */
  private String capturesString(Move move) {
    StringBuilder sb = new StringBuilder();
    for (Integer cp : move.getCaptured()) {
      sb.append(cp).append(";");
    }
    return sb.toString();
  }

  /**
   * Adds a move to the redo stack.
   *
   * @param color the color of the player who performed the move.
   * @param move the move to be stored for redo.
   */
  public void addMoveRedo(PlayerColor color, Move move) {
    redoHistory.add(new ColorMove(color, move));
  }

  /**
   * Retrieves the most recent move from the redo stack.
   *
   * @return the last {@link Move} undone.
   * @throws IllegalArgumentException if the redo history is empty.
   */
  public Move getLastMoveRedo() {
    if (redoHistory.size() != 0) {
      return redoHistory.peekLast().getMove();
    } else {
      throw new IllegalArgumentException("Redo History is Empty");
    }
  }

  /**
   * Removes the most recent move from the redo stack.
   *
   * @throws IllegalArgumentException if the redo history is empty.
   */
  public void removeLastMoveRedo() {
    if (!redoHistory.isEmpty()) {
      redoHistory.removeLast();
    } else {
      throw new IllegalArgumentException("Redo History is Empty");
    }
  }

  /** Clears all moves currently stored in the redo stack. */
  public void clearRedo() {
    redoHistory.clear();
  }

  /**
   * Checks if there are moves available to redo.
   *
   * @return {@code true} if the redo stack is not empty, {@code false} otherwise.
   */
  public boolean hasRedo() {
    return !redoHistory.isEmpty();
  }

  /**
   * Returns the number of moves currently in the primary history.
   *
   * @return the size of the history stack.
   */
  public int getSize() {
    return history.size();
  }

  /** Internal wrapper class to associate a player's color with a specific move. */
  private static class ColorMove {

    /** The color of the player. */
    private final PlayerColor color;

    /** The move performed by the player. */
    private final Move move;

    /**
     * Constructs a {@code ColorMove} entry.
     *
     * @param c the player color.
     * @param m the move data.
     */
    public ColorMove(PlayerColor c, Move m) {
      this.color = c;
      this.move = m;
    }

    /**
     * Gets the player color.
     *
     * @return the color.
     */
    public PlayerColor getColor() {
      return color;
    }

    /**
     * Gets the move.
     *
     * @return the move.
     */
    public Move getMove() {
      return move;
    }
  }
}