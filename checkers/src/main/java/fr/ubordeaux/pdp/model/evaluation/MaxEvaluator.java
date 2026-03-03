package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

public class MaxEvaluator implements Evaluator {

    @Override
    public int evaluate(Board board) {

        int score = 0;

        int pawn = 100;
        int checker = 350;
        int mobility = 5;
        int center = 15;
        int avancement = 8;

        int sizeBoard = board.getSizeBoard();
        int half = sizeBoard / 2;

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
                score -= (sizeBoard - 1 - row) * avancement;
            }

            if (board.isBitBlackChecker(i)) {
                score -= checker;
            }

            int col = (i % half) * 2 + (row % 2 == 0 ? 0 : 1);

            boolean inCenter = row >= sizeBoard / 2 - 2 && row <= sizeBoard / 2 + 1
                    && col >= sizeBoard / 2 - 2 && col <= sizeBoard / 2 + 1;

            if (inCenter) {
                if (board.isBitWhitePawn(i) || board.isBitWhiteChecker(i))
                    score += center;
                if (board.isBitBlackPawn(i) || board.isBitBlackChecker(i))
                    score -= center;
            }
        }

        score += board.getWhiteValidMoves().size() * mobility;
        score -= board.getBlackValidMoves().size() * mobility;

        return score;
    }
}