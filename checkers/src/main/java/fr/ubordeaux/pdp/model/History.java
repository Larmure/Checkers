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
    if (historyToString == null) {
      return;
    }

    String[] lines = historyToString.split("\\R");

    for (String rawLine : lines) {
      String line = rawLine.trim();

      if (line.isEmpty()) {
        continue;
      }

      if (line.equalsIgnoreCase(HEADER)) {
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
            default -> throw new IllegalArgumentException( "History line must start with W or B: " + line);
        }

      String moveText = line.substring(1).trim();

      if (moveText.isEmpty()) {
        throw new IllegalArgumentException(
            "Missing move after color: " + line);
      }

      Move move = Move.fromSaveString(moveText);
      history.add(new ColorMove(color, move));
    }
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
    return h;
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
