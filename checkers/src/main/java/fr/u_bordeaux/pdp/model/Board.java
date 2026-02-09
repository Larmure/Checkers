package fr.u_bordeaux.pdp.model;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
     * Represents a checkers board using bitboards.
     * 
     * <p>
     * This class supports:
     * <ul>
     *   <li>Different board sizes: 8x8, 10x10, and 12x12</li>
     *   <li>Two bitboards per player to handle up to 128 squares (board1 and board2)</li>
     *   <li>Initial positions of white and black pawns</li>
     *   <li>Diagonal moves for even and odd rows</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Each square of the board corresponds to a bit in a long:
     * <ul>
     *   <li>Board1: squares 0 to 63</li>
     *   <li>Board2: squares 64 to 127 (for larger boards)</li>
     * </ul>
     * </p>
*/
public class Board {

    private static final Set<Integer> VALID_SIZES = Set.of(8, 10,12);

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

    /**
     * Creates a board of the given size and initializes the positions.
     *
     * @param size the size of the board (8, 10, or 12)
     * @throws IllegalArgumentException if the size is invalid
     */
    public Board(int size) {
        if (!VALID_SIZES.contains(size)){
            throw new IllegalArgumentException("Invalid board size: " + size);
        }
        this.sizeBoard = size;
        this.indexMax = (this.sizeBoard * this.sizeBoard)/2;
        initPosition();

        initDiag();
    }

    /**
     * Initializes the starting positions of the pawns on the board.
     * 
     * <p>
     * White pawns are placed at the top, black pawns at the bottom.
     * Depending on the board size, pawns may overflow into the second bitboard.
     * </p>
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

    /**
     * Initializes diagonal shifts for even and odd rows.
     */
    private void initDiag() {
        this.diagsPair.put("NW", (this.sizeBoard/2)-1);
        this.diagsPair.put("NE", (this.sizeBoard/2));
        this.diagsPair.put("SW", -((this.sizeBoard/2)+1));
        this.diagsPair.put("SE", -(this.sizeBoard/2));

        this.diagsUnpair.put("NW", (this.sizeBoard/2));
        this.diagsUnpair.put("NE", (this.sizeBoard/2)+1);
        this.diagsUnpair.put("SW", -((this.sizeBoard/2)));
        this.diagsUnpair.put("SE", -((this.sizeBoard/2)+1));
    }

    /**
     * Convertit une case textuelle (ex: "C5") en index linéaire
     * sur le plateau size x size.
     *
     * @param square position sous forme lettre+nombre
     * @return index linéaire correspondant
     */
    private int squareToIndex(String square) {
        char rowChar = Character.toUpperCase(square.charAt(0));
        int row = rowChar - 'A';
        int col = Integer.parseInt(square.substring(1)) - 1;
        System.out.println((row * sizeBoard + col)/2);
        return (row * sizeBoard + col)/2;
    }

     /**
     * Teste la présence d'un bit à un index donné
     * dans une paire de bitboards (0-63 / 64-127).
     *
     * @param index index global
     * @param bitboard1 bits 0-63
     * @param bitboard2 bits 64-127
     * @return true si le bit est à 1
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

    /**
     * Indique si un pion blanc est présent à l'index donné.
     *
     * @param index index bitboard
     * @return true si un pion blanc est présent
     */
    public boolean isWhitePawn(String square) {
        int index = squareToIndex(square);
        return isBitBlackPawn(index);
    }

    /**
     * Indique si un pion noir est présent à l'index donné.
     *
     * @param index index bitboard
     * @return true si un pion noir est présent
     */
    public boolean isBlackPawn(String square) {
        int index = squareToIndex(square);
        return isBitBlackPawn(index);
    }

    /**
     * Indique si une dame blanche est présente à l'index donné.
     *
     * @param index index bitboard
     * @return true si une dame blanche est présente
     */
    public boolean isWhiteChecker(String square) {
        int index = squareToIndex(square);
        return isBitWhiteChecker(index);
    }

    /**
     * Indique si une dame noire est présente à l'index donné.
     *
     * @param index index bitboard
     * @return true si une dame noire est présente
     */
    public boolean isBlackChecker(String square) {
        int index = squareToIndex(square);
        return isBitBlackChecker(index);
    }

    /**
     * Indique si un pion blanc est présent à l'index donné.
     *
     * @param index index bitboard
     * @return true si un pion blanc est présent
     */
    private boolean isBitWhitePawn(int index) {
        return hasPawn(index, whitePawns1, whitePawns2);
    }

    /**
     * Indique si un pion noir est présent à l'index donné.
     *
     * @param index index bitboard
     * @return true si un pion noir est présent
     */
    private boolean isBitBlackPawn(int index) {
        return hasPawn(index, blackPawns1, blackPawns2);
    }

    /**
     * Indique si une dame blanche est présente à l'index donné.
     *
     * @param index index bitboard
     * @return true si une dame blanche est présente
     */
    private boolean isBitWhiteChecker(int index) {
        return hasPawn(index, whiteCheckers1, whiteCheckers2);
    }

    /**
     * Indique si une dame noire est présente à l'index donné.
     *
     * @param index index bitboard
     * @return true si une dame noire est présente
     */
    private boolean isBitBlackChecker(int index) {
        return hasPawn(index, blackCheckers1, blackCheckers2);
    }

    /**
     * Indique si une pièce quelconque est présente à l'index donné.
     *
     * @param index index bitboard
     * @return true si une pièce est présente
     */
    public boolean something(String square) {
        int index = squareToIndex(square);
        return isOccupied(index);
    }

    private boolean isOccupied(int index) {
        return isBitWhitePawn(index) || isBitBlackPawn(index)
                || isBitWhiteChecker(index) || isBitBlackChecker(index);
    }

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

    /**
     * Splits a move string like "A9-L12" into its two squares.
     *
     * @param move a string representing a move, e.g. "A9-L12"
     * @return a string array of length 2: { "A9", "L12" }
     * @throws IllegalArgumentException if the format is invalid
     */
    public void move(String move) {
        if (move == null || !move.contains("-")) {
            throw new IllegalArgumentException("Invalid move format: " + move);
        }
        
        String[] parts = move.split("-");
        
        if (parts.length != 2) {
            throw new IllegalArgumentException("Move should have exactly two squares: " + move);
        }
        int from = this.squareToIndex(parts[0]);
        int to = this.squareToIndex(parts[1]);
        
        this.moveAux(from, to);
    }



    /**
     * Moves a piece from one square to another.
     * 
     * <p>
     * Square indices can exceed 63 for larger boards:
     * <ul>
     *   <li>00-63 → bitboard 1</li>
     *   <li>64-74  → bitboard 2</li>
     * </ul>
     * </p>
     *
     * @param from the starting square index
     * @param to the target square index
     * @throws IllegalArgumentException if no piece is present at the "from" position
     */
    private void moveAux(int from, int to) {
        if (from < 0 || from >= indexMax || to < 0 || to >= indexMax) {
            throw new IllegalArgumentException("Move out of board bounds");
        }

        if (from == to) {
            throw new IllegalArgumentException("Source and destination are identical");
        }

        if (isOccupied(to)) {
            throw new IllegalArgumentException("Destination square is not empty: " + to);
        }

        if (isBitWhitePawn(from)) {
            removeWhitePawn(from);
            addWhitePawn(to);
            return;
        }

        if (isBitBlackPawn(from)) {
            removeBlackPawn(from);
            addBlackPawn(to);
            return;
        }

        if (isBitWhiteChecker(from)) {
            removeWhiteChecker(from);
            addWhiteChecker(to);
            return;
        }

        if (isBitBlackChecker(from)) {
            removeBlackChecker(from);
            addBlackChecker(to);
            return;
        }

        throw new IllegalArgumentException("No piece at source index: " + from);
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


    /**
     * Convertit une position (row, col) du plateau réel
     * en index de bitboard.
     *
     * @param row ligne du plateau
     * @param col colonne du plateau
     * @return index bitboard ou -1 si case blanche
     */
    private int boardToBitIndex(int row, int col) {
        // A1 (0,0) doit être NOIRE
        if ((row + col) % 2 != 0) {
            return -1; // case blanche, non stockée
        }

        int blackBeforeRow = row * (sizeBoard / 2);
        int blackInRow = col / 2;

        return blackBeforeRow + blackInRow;
    }


    /**
     * Affiche le plateau de jeu complet (cases blanches et noires),
     * avec les pièces placées selon les bitboards.
     */
    public void printBoard() {

        System.out.println();

        // Affichage des lignes de haut en bas (L -> A)
        for (int row = sizeBoard - 1; row >= 0; row--) {

            // Lettre de ligne (A en bas)
            char rowChar = (char) ('A' + row);
            System.out.print(rowChar + "  ");

            for (int col = 0; col < sizeBoard; col++) {
                int bitIndex = boardToBitIndex(row, col);

                if (bitIndex == -1) {
                    System.out.print("_  "); // case blanche
                } else if (isBitWhitePawn(bitIndex)) {
                    System.out.print("w  ");
                } else if (isBitWhiteChecker(bitIndex)) {
                    System.out.print("W  ");
                } else if (isBitBlackPawn(bitIndex)) {
                    System.out.print("b  ");
                } else if (isBitBlackChecker(bitIndex)) {
                    System.out.print("B  ");
                } else {
                    System.out.print("_  "); // case noire vide
                }
            }
            System.out.println();
            
        }
        // En-tête colonnes (1..size)
        System.out.print("  ");
        for (int col = 1; col <= sizeBoard; col++) {
            System.out.printf("%2d ", col);
        }
        System.out.println();
    }

}

