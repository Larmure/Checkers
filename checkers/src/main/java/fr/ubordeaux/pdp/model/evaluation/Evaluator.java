package fr.ubordeaux.pdp.model.evaluation;

import fr.ubordeaux.pdp.model.core.Board;

public interface Evaluator {
    int evaluate(Board board);
}