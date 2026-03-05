package fr.ubordeaux.pdp.model.player.ia;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

public interface AI {
  Move getBestMove(ManagerUndoRedo m, Board b, PlayerColor joueur,
    Evaluator eval
  );
} 
