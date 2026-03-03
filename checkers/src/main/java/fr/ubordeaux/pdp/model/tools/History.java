package fr.ubordeaux.pdp.model.tools;

import java.util.ArrayDeque;
import java.util.Deque;

import fr.ubordeaux.pdp.model.player.*;
import fr.ubordeaux.pdp.model.core.Move;

public class History {

  private static String HEADER = "[history]";

  private final Deque<ColorMove> history;
  private final Deque<ColorMove> redoHistory;

  public History() {
    history = new ArrayDeque<>();
    redoHistory = new ArrayDeque<>();
  }

  public History(String historyToString) {
    history = new ArrayDeque<>();
    redoHistory = new ArrayDeque<>();
    loadHistory(historyToString);
  }

  public void addMove(PlayerColor color, Move move) {
    history.add(new ColorMove(color, move));
  }

  public void reMove() {
    if (history.size() != 0) {
      history.removeLast();
    } else {
      throw new IllegalArgumentException("History is Empty");
    }
  }

  public Move getLastMove() {
    if (history.size() != 0) {
      return history.peekLast().getMove();
    } else {
      throw new IllegalArgumentException("History is Empty");
    }
  }

  private void loadHistory(String historyToString) {
    // TODO.
  }

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
        line += " {Prise simple}";
      } else if (cm.getMove().getCaptured().size() >= 1) {
        line += " {Prise multiple}";
      }
      if (cm.getMove().isPromotion()) {
        line += " {Promotion}";
      }
      line += "\n";
      h = line + h;
    }
    return HEADER + "\n" + h;
  }

  // FONCTION REDO
  public void addMoveRedo(PlayerColor color, Move move) {
    redoHistory.add(new ColorMove(color, move));
  }

  public Move getLastMoveRedo() {
    if (redoHistory.size() != 0) {
      return redoHistory.peekLast().getMove();
    } else {
      throw new IllegalArgumentException("Redo History is Empty");
    }
  }

  // Retire le dernier coup du redoHistory
  public void removeLastMoveRedo() {
    if (!redoHistory.isEmpty()) {
      redoHistory.removeLast();
    } else {
      throw new IllegalArgumentException("Redo History is Empty");
    }
  }

  // Vide l'historique Redo (à appeler quand un nouveau coup est joué)
  public void clearRedo() {
    redoHistory.clear();
  }

  // Vérifie si un redo est possible
  public boolean hasRedo() {
    return !redoHistory.isEmpty();
  }

  private class ColorMove {

    private PlayerColor color;
    private Move move;

    public ColorMove(PlayerColor c, Move m) {
      color = c;
      move = m;
    }

    public PlayerColor getColor() {
      return color;
    }

    public Move getMove() {
      return move;
    }
  }

}
