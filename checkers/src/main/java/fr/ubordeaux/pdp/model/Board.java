package fr.ubordeaux.pdp.model;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class Board {

    private static final Set<Integer> VALID_SIZES = Set.of(8, 10,12);

    // Masques pour le plateau 8x8 (on peut généraliser ensuite)
    private long LEFT_EDGE_MASK;
    private long RIGHT_EDGE_MASK;

    private Map<String, Integer> diagsPair = new HashMap<>();
    private Map<String, Integer> diagsUnpair = new HashMap<>();

    private int sizeBoard;
    private int indexMax;

    private long whitePawns1;
    private long whiteCheckers1;
    private long blackPawns1;
    private long blackCheckers1;

    private long whitePawns2;
    private long whiteCheckers2;
    private long blackPawns2;
    private long blackCheckers2;

    public Board(int size) {
        if (!VALID_SIZES.contains(size)){
            throw new IllegalArgumentException("Invalid board size: " + size);
        }
        this.sizeBoard = size;
        this.indexMax = (this.sizeBoard * this.sizeBoard)/2;
        initPosition();
        initEdgeMasks();
        initDiag();
    }

    /*
          INITIALISATION
    */
    private void initPosition() {

        this.whitePawns1 = 0L;
        this.blackPawns1 = 0L;
        this.whiteCheckers1 = 0L;
        this.blackCheckers1 = 0L;

        this.whitePawns2 = 0L;
        this.blackPawns2 = 0L;
        this.whiteCheckers2 = 0L;
        this.blackCheckers2 = 0L;

        switch (this.sizeBoard) {
            case 8:
                // on décale au 12 ème bit le 1 et on soustrait 1
                // ce qui fait que les 12 premiers indices sont des 1
                this.whitePawns1 = (1L << 12) - 1;
                long v1 = ((1L << 12) - 1);
                // on décale à gauche de 20 les 12 premiers 1
                this.blackPawns1 = v1 << 20;
                break;
            case 10:
                // on décale au 20 ème bit le 1 et on soustrait 1
                // ce qui fait que les 20 premiers indices sont des 1
                this.whitePawns1 = (1L << 20) - 1;
                long v2 = ((1L << 20) - 1);
                // on décale à gauche de 30 les 20 premiers 1
                this.blackPawns1 = v2 << 30;
                break;
            case 12:
                // on décale au 30 ème bit le 1 et on soustrait 1
                // ce qui fait que les 30 premiers indices sont des 1
                this.whitePawns1 = (1L << 30) - 1;
                long v3 = ((1L << 30) - 1);
                // on décale à gauche de 42 les 30 premiers 1
                this.blackPawns1 = v3 << 42;
                // on aura un debordement de 8 à gauche on le met donc 
                // dans le deuxième bitboard
                this.blackPawns2 = ((1L << 8) - 1);
                break;
            default:
                throw new IllegalArgumentException(
                    "Taille de plateau invalide : " + this.sizeBoard +
                    " (valeurs autorisées : 8, 10, 12)"
                );
        }
    }

    private void initDiag() {
        this.diagsPair.put("NW", (this.sizeBoard/2)-1);
        this.diagsPair.put("NE", (this.sizeBoard/2));
        this.diagsPair.put("SW", -((this.sizeBoard/2)+1));
        this.diagsPair.put("SE", -(this.sizeBoard/2));

        this.diagsUnpair.put("NW", (this.sizeBoard/2));
        this.diagsUnpair.put("NE", (this.sizeBoard/2)+1);
        this.diagsUnpair.put("SW", -((this.sizeBoard/2)));
        this.diagsUnpair.put("SE", -((this.sizeBoard/2)-1));
    }

    private void initEdgeMasks() {
        LEFT_EDGE_MASK = 0L;
        RIGHT_EDGE_MASK = 0L;

        for (int row = 0; row < sizeBoard; row++) {
            int leftIndex = boardToBitIndex(row, 0);
            int rightIndex = boardToBitIndex(row, sizeBoard - 1);
            if (leftIndex >= 0) LEFT_EDGE_MASK |= (leftIndex < 64 ? 1L << leftIndex : 0L);
            if (rightIndex >= 0) RIGHT_EDGE_MASK |= (rightIndex < 64 ? 1L << rightIndex : 0L);
        }
    }

    
    // Vérifie si un pion est sur la colonne gauche ou droite
    private boolean onLeftEdge(int index) {
        return (index < 64) ? ((LEFT_EDGE_MASK & (1L << index)) != 0)
                            : ((LEFT_EDGE_MASK & (1L << (index - 64))) != 0);
    }

    private boolean onRightEdge(int index) {
        return (index < 64) ? ((RIGHT_EDGE_MASK & (1L << index)) != 0)
                            : ((RIGHT_EDGE_MASK & (1L << (index - 64))) != 0);
    }

    /*
          ALL Types Of Presence
    */
    private boolean hasPawn(int index, long bitboard1, long bitboard2) {
        if (index < 0 || index >= this.indexMax) {
            throw new IllegalArgumentException("Index hors limites (0-127)");
        }

        if (index < 64) {
            return ((bitboard1 >>> index) & 1L) == 1L;
        } else {
            return ((bitboard2 >>> (index - 64)) & 1L) == 1L;
        }
    }

    public boolean isWhitePawn(String square) {
        int index = squareToIndex(square);
        return isBitWhitePawn(index);
    }

    public boolean isBlackPawn(String square) {
        int index = squareToIndex(square);
        return isBitBlackPawn(index);
    }

    public boolean isWhiteChecker(String square) {
        int index = squareToIndex(square);
        return isBitWhiteChecker(index);
    }

    public boolean isBlackChecker(String square) {
        int index = squareToIndex(square);
        return isBitBlackChecker(index);
    }

    private boolean isBitWhitePawn(int index) {
        return hasPawn(index, whitePawns1, whitePawns2);
    }

    private boolean isBitBlackPawn(int index) {
        return hasPawn(index, blackPawns1, blackPawns2);
    }


    private boolean isBitWhiteChecker(int index) {
        return hasPawn(index, whiteCheckers1, whiteCheckers2);
    }

    private boolean isBitBlackChecker(int index) {
        return hasPawn(index, blackCheckers1, blackCheckers2);
    }

    public boolean occupied(String square) {
        int index = squareToIndex(square);
        return isOccupied(index);
    }

    private boolean isOccupied(int index) {
        return isBitWhitePawn(index) || isBitBlackPawn(index)
                || isBitWhiteChecker(index) || isBitBlackChecker(index);
    }


    /*
          Operation
    */
    private void addWhitePawn(int index) {
        if (index < 64) {
            whitePawns1 |= (1L << index);
        } else {
            whitePawns2 |= (1L << (index - 64));
        }
    }

    private void removeWhitePawn(int index) {
        if (index < 64) {
            whitePawns1 &= ~(1L << index);
        } else {
            whitePawns2 &= ~(1L << (index - 64));
        }
    }

    private void addBlackPawn(int index) {
        if (index < 64) {
            blackPawns1 |= (1L << index);
        } else {
            blackPawns2 |= (1L << (index - 64));
        }
    }

    private void removeBlackPawn(int index) {
        if (index < 64) {
            blackPawns1 &= ~(1L << index);
        } else {
            blackPawns2 &= ~(1L << (index - 64));
        }
    }

    private void addWhiteChecker(int index) {
        if (index < 64) {
            whiteCheckers1 |= (1L << index);
        } else {
            whiteCheckers2 |= (1L << (index - 64));
        }
    }

    private void removeWhiteChecker(int index) {
        if (index < 64) {
            whiteCheckers1 &= ~(1L << index);
        } else {
            whiteCheckers2 &= ~(1L << (index - 64));
        }
    }

    private void addBlackChecker(int index) {
        if (index < 64) {
            blackCheckers1 |= (1L << index);
        } else {
            blackCheckers2 |= (1L << (index - 64));
        }
    }

    private void removeBlackChecker(int index) {
        if (index < 64) {
            blackCheckers1 &= ~(1L << index);
        } else {
            blackCheckers2 &= ~(1L << (index - 64));
        }
    }

    public void promote(String square) {
        int index = squareToIndex(square);
        promoteBit(index);
    }

    private void promoteBit(int index) {
        if (index < 0 || index >= indexMax) {
            throw new IllegalArgumentException("Move out of board bounds");
        }

        if (isBitWhitePawn(index)) {
            removeWhitePawn(index);
            addWhiteChecker(index);
            return;
        }

        if (isBitBlackPawn(index)) {
            removeBlackPawn(index);
            addBlackChecker(index);
            return;
        }

        throw new IllegalArgumentException("No piece at source index: " + index);
    } 



    //        Application Move with Move        //

    public void applyMove(Move move) {

        int from = move.getFrom();
        int to = move.getTo();

        // Move piece
        if (isBitWhitePawn(from)) {
            removeWhitePawn(from);
            addWhitePawn(to);
        } else if (isBitBlackPawn(from)) {
            removeBlackPawn(from);
            addBlackPawn(to);
        } else if (isBitWhiteChecker(from)) {
            removeWhiteChecker(from);
            addWhiteChecker(to);
        } else if (isBitBlackChecker(from)) {
            removeBlackChecker(from);
            addBlackChecker(to);
        }

        // Remove captured pieces
        for (int captured : move.getCaptured()) {

            if (isBitWhitePawn(captured)) removeWhitePawn(captured);
            if (isBitBlackPawn(captured)) removeBlackPawn(captured);
            if (isBitWhiteChecker(captured)) removeWhiteChecker(captured);
            if (isBitBlackChecker(captured)) removeBlackChecker(captured);
        }

        // Vérifier promotion automatique pour les pions
        boolean promoted = false;
        if (isBitWhitePawn(to)) {
            int row = to / (sizeBoard / 2);
            if (row == sizeBoard - 1) { // dernière ligne pour les blancs
                promoteBit(to);
                promoted = true;
            }
        } else if (isBitBlackPawn(to)) {
            int row = to / (sizeBoard / 2);
            if (row == 0) { // première ligne pour les noirs
                promoteBit(to);
                promoted = true;
            }
        }

        // Marquer le move comme promotion pour affichage
        if (promoted) {
            move.setPromotion(true);
        }
    }

    private Map<String, Integer> diagsForIndex(int index) {
        int row = index / (sizeBoard / 2);
        return (row % 2 == 0) ? diagsPair : diagsUnpair;
    }


    // Deplacement simple
    private List<Integer> pawnSimpleTargets(int from) {
        List<Integer> targets = new ArrayList<>();
        Map<String, Integer> diags = diagsForIndex(from);

        for (Map.Entry<String, Integer> entry : diags.entrySet()) {
            String dir = entry.getKey();
            int delta = entry.getValue();
            int to = from + delta;

            // Vérifier les bordures
            if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(from)) continue;
            if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(from)) continue;

            if (to >= 0 && to < indexMax && !isOccupied(to)) {
                targets.add(to);
            }
        }

        return targets;
    }
    private List<Integer> checkerSimpleTargets(int from) {
        List<Integer> targets = new ArrayList<>();

        // Parcours de toutes les directions diagonales
        for (String dir : diagsPair.keySet()) {
            int current = from;

            while (true) {
                Map<String, Integer> diags = diagsForIndex(current);
                int delta = diags.get(dir);

                // Vérification des bords
                if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(current)) break;
                if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(current)) break;

                int next = current + delta;

                // Hors plateau
                if (next < 0 || next >= indexMax) break;

                if (isOccupied(next)) break; // case occupée → stop

                targets.add(next);
                current = next; // continuer dans la même direction
            }
        }

        return targets;
    }


    /*
              Valides Moves    
    */
    public List<Move> getWhiteValidMoves() {
        return getValidMoves(true);
    }

    public List<Move> getBlackValidMoves() {
        return getValidMoves(false);
    }

    private List<Move> getValidMoves(boolean isWhite) {

        List<Move> captures = new ArrayList<>();
        List<Move> simples  = new ArrayList<>();

        for (int i = 0; i < indexMax; i++) {

            if (isWhite) {
                if (isBitWhitePawn(i))
                    captures.addAll(getBestPawnCaptures(i, true));

                if (isBitWhiteChecker(i))
                    captures.addAll(getBestCheckerCaptures(i, true));
            } else {
                if (isBitBlackPawn(i))
                    captures.addAll(getBestPawnCaptures(i, false));

                if (isBitBlackChecker(i))
                    captures.addAll(getBestCheckerCaptures(i, false));
            }
        }

        if (!captures.isEmpty()) return captures;

        // Otherwise simple moves
        for (int i = 0; i < indexMax; i++) {

            if (isWhite) {
                if (isBitWhitePawn(i))
                    for (int to : pawnSimpleTargets(i))
                        simples.add(new Move(List.of(i, to), List.of()));

                if (isBitWhiteChecker(i))
                    for (int to : checkerSimpleTargets(i))
                        simples.add(new Move(List.of(i, to), List.of()));
            } else {
                if (isBitBlackPawn(i))
                    for (int to : pawnSimpleTargets(i))
                        simples.add(new Move(List.of(i, to), List.of()));

                if (isBitBlackChecker(i))
                    for (int to : checkerSimpleTargets(i))
                        simples.add(new Move(List.of(i, to), List.of()));
            }
        }

        return simples;
    }
    
  /* BEST MOVES PAWNS CAPTURE */
    private List<Move> getBestPawnCaptures(int from, boolean isWhite) {

      List<CapturePath> all = pawnMultiCaptures(from, isWhite);
      if (all.isEmpty()) return List.of();

      int max = all.stream()
              .mapToInt(c -> c.captures)
              .max()
              .orElse(0);

      List<Move> best = new ArrayList<>();

      for (CapturePath c : all) {
          if (c.captures == max) {
              best.add(new Move(c.path, new ArrayList<>(c.captured)));
          }
      }

      return best;
    }

    private List<CapturePath> pawnMultiCaptures(int from, boolean isWhite) {
        List<CapturePath> results = new ArrayList<>();

        dfsPawn(from, isWhite, new ArrayList<>(List.of(from)), new HashSet<>(), results,0);

        return results;
    }

    private boolean canCapturePawn(int from, int over, int to, boolean isWhite) {
        // On bloque si sur bordures
        if ((over < from && onLeftEdge(from)) || (over > from && onRightEdge(from))) return false;
        if (to < 0 || to >= indexMax || isOccupied(to)) return false;

        boolean enemy = isWhite
                ? (isBitBlackPawn(over) || isBitBlackChecker(over))
                : (isBitWhitePawn(over) || isBitWhiteChecker(over));

        return enemy;
    }

    private void dfsPawn(int current, boolean isWhite, List<Integer> path,
                     Set<Integer> captured, List<CapturePath> results, int captureCount) {

    boolean foundNext = false;

    // Récupérer les bonnes diagonales selon la ligne actuelle
    Map<String, Integer> diags = diagsForIndex(current);

    for (Map.Entry<String, Integer> entry : diags.entrySet()) {
        String dir = entry.getKey();
        int delta = entry.getValue();

        // Vérification des bords
        if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(current)) continue;
        if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(current)) continue;

        int mid = current + delta;

        // Hors plateau
        if (mid < 0 || mid >= indexMax) continue;

        // Case déjà capturée
        if (captured.contains(mid)) continue;

        // Vérifier que c'est un ennemi
        boolean enemy = isWhite ? (isBitBlackPawn(mid) || isBitBlackChecker(mid))
                                : (isBitWhitePawn(mid) || isBitWhiteChecker(mid));
        if (!enemy) continue;

        // Calculer la destination après le saut
        Map<String, Integer> nextDiags = diagsForIndex(mid);
        int to = mid + nextDiags.get(dir);

        // Vérifications finales
        if (to < 0 || to >= indexMax) continue;
        if (isOccupied(to)) continue;
        if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(mid)) continue;
        if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(mid)) continue;

        // Saut valide
        foundNext = true;
        captured.add(mid);
        path.add(to);

        // Appel récursif pour multi-captures
        dfsPawn(to, isWhite, path, captured, results, captureCount + 1);

        // Backtrack
        path.remove(path.size() - 1);
        captured.remove(mid);
    }

    // Stocker le chemin si aucune capture suivante
    if (!foundNext && captureCount > 0) {
        results.add(new CapturePath(path, new ArrayList<>(captured)));
    }
}



  /* BEST MOVES CHECKERS CAPTURE */
  private List<Move> getBestCheckerCaptures(int from, boolean isWhite) {

      List<CapturePath> all = checkerMultiCaptures(from, isWhite);
      if (all.isEmpty()) return List.of();

      int max = all.stream()
              .mapToInt(c -> c.captures)
              .max()
              .orElse(0);

      List<Move> best = new ArrayList<>();

      for (CapturePath c : all) {
          if (c.captures == max) {
              best.add(new Move(c.path, c.captured));
          }
      }

      return best;
  }


    private List<CapturePath> checkerMultiCaptures(int from, boolean isWhite) {

        List<CapturePath> results = new ArrayList<>();

        dfsChecker(from, isWhite, new ArrayList<>(List.of(from)), new HashSet<>(), results, 0 );

        return results;
    }

    private void dfsChecker(int current, boolean isWhite, List<Integer> path,
                        Set<Integer> captured, List<CapturePath> results, int captureCount) {

        boolean foundNext = false;

        for (String dir : diagsPair.keySet()) {
            int pos = current;
            boolean enemyFound = false;
            int enemyIndex = -1;

            while (true) {
                Map<String, Integer> diags = diagsForIndex(pos);
                int delta = diags.get(dir);

                // Vérification des bords
                if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(pos)) break;
                if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(pos)) break;

                int next = pos + delta;

                // Hors plateau
                if (next < 0 || next >= indexMax) break;

                if (!enemyFound) {
                    if (isOccupied(next)) {
                        // Vérifier si c'est un ennemi et pas déjà capturé
                        boolean enemy = isWhite ? (isBitBlackPawn(next) || isBitBlackChecker(next))
                                                : (isBitWhitePawn(next) || isBitWhiteChecker(next));
                        if (!enemy || captured.contains(next)) break;

                        enemyFound = true;
                        enemyIndex = next;
                        pos = next;
                    } else {
                        pos = next;
                    }
                } else {
                    // Après avoir trouvé un ennemi, chercher la case vide pour sauter
                    if (isOccupied(next)) break;

                    foundNext = true;
                    captured.add(enemyIndex);
                    path.add(next);

                    // Appel récursif pour multi-captures
                    dfsChecker(next, isWhite, path, captured, results, captureCount + 1);

                    // Backtrack
                    path.remove(path.size() - 1);
                    captured.remove(enemyIndex);

                    pos = next;
                }
            }
        }

        if (!foundNext && captureCount > 0) {
            results.add(new CapturePath(path, new ArrayList<>(captured)));
        }
    }




    /*
          Classes internes
    */
    private static class CapturePath {
        List<Integer> path;
        List<Integer> captured;
        int captures;

        CapturePath(List<Integer> path, List<Integer> captured) {
            this.path = new ArrayList<>(path);
            this.captured = new ArrayList<>(captured);
            this.captures = captured.size();
        }
    }

    /*
            Traduction des d'uen case en index ou inverse
    */
    public String indexToSquare(int index) {
        int row = index / (sizeBoard / 2);
        int col = (index % (sizeBoard / 2)) * 2 + (row % 2 == 0 ? 0 : 1);

        char rowChar = (char) ('A' + row);
        return "" + rowChar + (col + 1);
    }

    public int squareToIndex(String square) {
        char rowChar = Character.toUpperCase(square.charAt(0));
        int row = rowChar - 'A';
        int col = Integer.parseInt(square.substring(1)) - 1;

        // Vérifier que c'est une case noire
        if ((row + col) % 2 != 0) {
            throw new IllegalArgumentException("Case blanche invalide: " + square);
        }

        return (row * sizeBoard + col)/2;
    }

    /*
          Affichages String
     */
    public String diagsToString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Diagonales (lignes paires) :\n");
        for (Map.Entry<String, Integer> entry : diagsPair.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        sb.append("\n");

        sb.append("Diagonales (lignes impaires) :\n");
        for (Map.Entry<String, Integer> entry : diagsUnpair.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    private int boardToBitIndex(int row, int col) {
        // A1 (0,0) doit être NOIRE
        if ((row + col) % 2 != 0) {
            return -1; // case blanche, non stockée
        }

        int blackBeforeRow = row * (sizeBoard / 2);
        int blackInRow = col / 2;

        return blackBeforeRow + blackInRow;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        // Affichage des lignes de haut en bas (L -> A)
        for (int row = sizeBoard - 1; row >= 0; row--) {

            // Lettre de ligne (A en bas)
            char rowChar = (char) ('A' + row);
            sb.append(rowChar).append("  ");

            for (int col = 0; col < sizeBoard; col++) {
                int bitIndex = boardToBitIndex(row, col);

                if (bitIndex == -1) {
                    sb.append("_  "); // case blanche
                } else if (isBitWhitePawn(bitIndex)) {
                    sb.append("w  ");
                } else if (isBitWhiteChecker(bitIndex)) {
                    sb.append("W  ");
                } else if (isBitBlackPawn(bitIndex)) {
                    sb.append("b  ");
                } else if (isBitBlackChecker(bitIndex)) {
                    sb.append("B  ");
                } else {
                    sb.append("_  "); // case noire vide
                }
            }
            sb.append("\n");
        }

        // En-tête colonnes (1..size)
        sb.append("  ");
        for (int col = 1; col <= sizeBoard; col++) {
            sb.append(String.format("%2d ", col));
        }
        sb.append("\n");

        return sb.toString();
    }


}


