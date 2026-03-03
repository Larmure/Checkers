package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

public class AdvancedEvaluator implements Evaluator {

    @Override
    public int evaluate(Board board) {

        int score = 0;

        int pawn = 100;
        int checker = 350;
        int avancement = 8;

        int half = board.getSizeBoard() / 2;

        for (int i = 0; i < board.getIndexMax(); i++) {

            int row = i / half;

            if (board.isBitWhitePawn(i)) {
                score += pawn;
                score += row * avancement;
            }

            if (board.isBitWhiteChecker(i)) {
                score += checker;
            }

            if (board.isBitBlackPawn(i)) {
                score -= pawn;
                score -= (board.getSizeBoard() - 1 - row) * avancement;
            }

            if (board.isBitBlackChecker(i)) {
                score -= checker;
            }
        }

        return score;
    }
}