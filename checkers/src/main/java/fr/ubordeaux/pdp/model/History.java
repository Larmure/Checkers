package fr.ubordeaux.pdp.model;

import java.util.ArrayDeque;
import java.util.Deque;

public class History {

  private static String HEADER = "[history]";

	private final Deque<ColorMove> history;
    
  public History() {
    history = new ArrayDeque<>();
  }

  public History(String historyToString) {
    history = new ArrayDeque<>();
    loadHistory(historyToString);
  }

  public void addMove(PlayerColor color, Move move) {
		history.add(new ColorMove(color, move));
  }

  public void reMove() {
    if (history.size() != 0) {
      history.removeLast();
    }
    else {
      throw new IllegalArgumentException("History is Empty");
    }
  }

  public Move getLastMove() {
    if (history.size() != 0) {
      return history.peekLast().getMove();
    }
    else {
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
      }
      else if (cm.getColor() == PlayerColor.BLACK) {
        line += "B " + cm.getMove();
      }
      if (cm.getMove().getCaptured().size() == 1) {
        line += " {Prise simple}";
      } 
      else if (cm.getMove().getCaptured().size() >= 1) {
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
