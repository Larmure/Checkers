package fr.u_bordeaux.pdp.model;

import java.util.Set;

public class Board {

    private static final Set<Integer> VALID_SIZES = Set.of(8, 10,12);

    private int sizeBoard;

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
        initPosition();
    }

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


    public long getBlackPawns1() {
        return this.blackPawns1;
    }

    public long getBlackPawns2() {
        return this.blackPawns2;
    }
 
    public long getWhitePawns1() {
        return this.whitePawns1;
    }

    public long getWhitePawns2() {
        return this.whitePawns2;
    }

    public int getSizeBoard() { return sizeBoard; }
}

