package fr.ubordeaux.pdp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a move in a checkers game.
 *
 * A move can be:
 * - A simple move (one step diagonal)
 * - A capture move (possibly multiple captures)
 * - A promotion move
 *
 * The move stores:
 * - The starting index
 * - The full path of movement
 * - The list of captured piece indices
 * - Whether the move results in a promotion
 */
public class Move {

    private final int from;
    private final List<Integer> path;
    private final List<Integer> captured;
    private boolean promotion;

    /**
     * Creates a simple move.
     *
     * @param from starting index
     * @param to destination index
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
     * Creates a capture move with full path and captured pieces.
     *
     * @param path movement path (must include starting square)
     * @param captured list of captured indices
     */
    public Move(List<Integer> path, List<Integer> captured) {
        this.from = path.get(0);
        this.path = new ArrayList<>(path);
        this.captured = new ArrayList<>(captured);
        this.promotion = false;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return path.get(path.size() - 1);
    }

    public List<Integer> getPath() {
        return path;
    }

    public List<Integer> getCaptured() {
        return captured;
    }

    public boolean isCapture() {
        return !captured.isEmpty();
    }

    public boolean isPromotion() {
        return promotion;
    }

    public void setPromotion(boolean promotion) {
        this.promotion = promotion;
    }

    public boolean isSimpleMove() {
        return captured.isEmpty() && path.size() == 2;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        
        // On compare le départ et l'arrivée (dernière case du chemin)
        // Cela suffit généralement pour identifier un coup unique
        return from == move.from && 
               this.getTo() == move.getTo();
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(from, getTo());
    }
}
