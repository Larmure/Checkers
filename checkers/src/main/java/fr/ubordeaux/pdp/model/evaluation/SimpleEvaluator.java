package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.Board;

public class SimpleEvaluator implements Evaluator {

    @Override
    public int evaluate(Board board) {

        int score = 0;

        int pawn = 100;
        int checker = 350;

        for (int i = 0; i < board.getIndexMax(); i++) {

            if (board.isBitWhitePawn(i)) score += pawn;
            if (board.isBitWhiteChecker(i)) score += checker;

            if (board.isBitBlackPawn(i)) score -= pawn;
            if (board.isBitBlackChecker(i)) score -= checker;
        }

        return score;
    }
}