package fr.ubordeaux.pdp.controller.bridge;

import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.MaxEvaluator;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.MinMaxAlphaBeta;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.LoadBoard;
import java.nio.file.Paths;

/**
 * Bridge between CLI entry point and contest analysis internals.
 *
 * <p>This class isolates contest-mode orchestration (load save, compute best move,
 * print result) from App so the entry point remains focused on argument parsing
 * and startup flow.</p>
 */
public final class ContestAnalysisBridge {

  private static final int CONTEST_DEPTH = 10;

  private ContestAnalysisBridge() {
    // Utility class.
  }

  /**
   * Executes contest analysis only when contest mode and a save file are provided.
   *
   * <p>The recommended move is computed with AlphaBeta depth 10 and MaxEvaluator,
   * then printed in Manoury notation.</p>
   *
   * @param contestEnabled whether contest mode is enabled
   * @param startupSaveFile save file path from CLI
   * @return {@code true} if analysis was executed (successfully or not), {@code false} otherwise
   */
  public static boolean runIfRequested(boolean contestEnabled, String startupSaveFile) {
    if (!contestEnabled || startupSaveFile == null || startupSaveFile.isBlank()) {
      return false;
    }

    String fileName = Paths.get(startupSaveFile.trim()).getFileName().toString();

    LoadBoard loader = new LoadBoard();
    try {
      loader.loadGameData(fileName);
    } catch (IllegalStateException e) {
      // Loader already emitted detailed diagnostics.
    }

    GameCheckers game = loader.getLoadedGame();
    if (game == null) {
      System.err.println(Internationalization.get("contest.load_error", startupSaveFile));
      return true;
    }

    MinMaxAlphaBeta alphaBeta = new MinMaxAlphaBeta(CONTEST_DEPTH, Ai.DEFAULT_MAX_TIME_MS);
    MaxEvaluator evaluator = new MaxEvaluator();
    Move bestMove = alphaBeta.getBestMove(
        game.getManagerUndoRedo(),
        game.getBoard(),
        game.getCurrentColor(),
        evaluator);

    if (bestMove == null) {
      System.out.println(Internationalization.get("contest.no_legal_move"));
      return true;
    }

    // Internal move indices match the Manoury format used by this project.
    System.out.println(Internationalization.get("contest.best_move", bestMove.toString()));
    return true;
  }
}
