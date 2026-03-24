package fr.ubordeaux.pdp.model.player.ai;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.MaxEvaluator;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;

class AiMatchmakingTest {

  private static final int MAX_MOVES = 100;

  @Test
  @DisplayName("MinMaxAlphaBeta (depth 3) should win against MinMax (depth 1)")
  void testMinMaxAlphaBetaBeatsMinMax() {
    AiPlayer white = new AiPlayer("AlphaBeta", new MinMaxAlphaBeta(3, 500), new MaxEvaluator());
    AiPlayer black = new AiPlayer("MinMax", new MinMax(1, 500), new MaxEvaluator());

    MatchResult result = runMatch(white, black, PlayerColor.WHITE);

    assertEquals(PlayerColor.WHITE, result.winner, "White should have a stronger search on average");
  }

  @Test
  @DisplayName("MinMaxAlphaBeta (depth 3) should beat MCTS (short time) in repeated matches")
  void testMinMaxAlphaBetaVersusMcts() {
    int whiteWins = 0;
    int blackWins = 0;
    int draws = 0;

    for (int i = 0; i < 3; i++) {
      AiPlayer white = new AiPlayer("AlphaBeta", new MinMaxAlphaBeta(3, 500), new MaxEvaluator());
      AiPlayer black = new AiPlayer("MCTS", new Mcts(1, 200, Mcts.DEFAULT_EXPLORATION), new MaxEvaluator());

      MatchResult result;
      if (i % 2 == 0) {
        result = runMatch(white, black, PlayerColor.WHITE);
      } else {
        result = runMatch(black, white, PlayerColor.WHITE);
      }

      if (result.winner == null) {
        draws++;
      } else if (result.winner == PlayerColor.WHITE) {
        if (i % 2 == 0) {
          whiteWins++;
        } else {
          blackWins++;
        }
      } else {
        if (i % 2 == 0) {
          blackWins++;
        } else {
          whiteWins++;
        }
      }
    }

    assertTrue(whiteWins < blackWins, "MinMaxAlphaBeta should win more games than MCTS");
    assertEquals(3, whiteWins + blackWins + draws);
  }

  @Test
  @DisplayName("MCTS should outperform a random move generator in repeated matches")
  void testMctsVersusRandom() {
    int mctsWins = 0;
    int randomWins = 0;
    int draws = 0;

    for (int i = 0; i < 3; i++) {
      RandomPlayer randomPlayer = new RandomPlayer(i + 123);
      AiPlayer mctsAiPlayer = new AiPlayer("MCTS", new Mcts(1, 200, Mcts.DEFAULT_EXPLORATION), new MaxEvaluator());

      MatchResult result;
      if (i % 2 == 0) {
        result = runMatch(mctsAiPlayer, randomPlayer, PlayerColor.WHITE);
      } else {
        result = runMatch(randomPlayer, mctsAiPlayer, PlayerColor.WHITE);
      }

      if (result.winner == null) {
        draws++;
      } else if (result.winner == PlayerColor.WHITE) {
        if (i % 2 == 0) {
          mctsWins++;
        } else {
          randomWins++;
        }
      } else {
        if (i % 2 == 0) {
          randomWins++;
        } else {
          mctsWins++;
        }
      }
    }

    assertTrue(mctsWins > randomWins, "MCTS should win more games than random");
    assertTrue(mctsWins + randomWins + draws == 3);
  }

  // --- Helpers ---

  private MatchResult runMatch(PlayerWrapper whitePlayer, PlayerWrapper blackPlayer,
      PlayerColor startingColor) {
    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    boolean whiteTurn = (startingColor == PlayerColor.WHITE);

    int moveCount = 0;

    while (moveCount < MAX_MOVES) {
      PlayerColor currentColor = whiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
      PlayerWrapper currentPlayer = whiteTurn ? whitePlayer : blackPlayer;

      Move move = currentPlayer.getBestMove(board, undo, currentColor);
      if (move == null) {
        // Current player has no moves -> game over
        return new MatchResult(whiteTurn ? opponent(currentColor) : currentColor);
      }

      board.applyMove(move);
      undo.registerMove(currentColor, move);

      // Fin de partie si un joueur a perdu toutes ses pièces
      if (board.noPiecesLeft(PlayerColor.WHITE)) {
        return new MatchResult(PlayerColor.BLACK);
      }
      if (board.noPiecesLeft(PlayerColor.BLACK)) {
        return new MatchResult(PlayerColor.WHITE);
      }

      whiteTurn = !whiteTurn;
      moveCount++;
    }

    // Draw threshold
    return new MatchResult(null);
  }

  private PlayerColor opponent(PlayerColor color) {
    return color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  private interface PlayerWrapper {
    Move getBestMove(Board board, ManagerUndoRedo undo, PlayerColor player);
  }

  private static class AiWrapper implements PlayerWrapper {
    private final AiPlayer aiPlayer;

    AiWrapper(AiPlayer aiPlayer) {
      this.aiPlayer = aiPlayer;
    }

    @Override
    public Move getBestMove(Board board, ManagerUndoRedo undo, PlayerColor player) {
      return aiPlayer.getBestMove(undo, board, player);
    }
  }

  private static class RandomPlayer implements PlayerWrapper {
    private final Random random;

    RandomPlayer(long seed) {
      this.random = new Random(seed);
    }

    @Override
    public Move getBestMove(Board board, ManagerUndoRedo undo, PlayerColor player) {
      List<Move> moves = player == PlayerColor.WHITE ? board.getWhiteValidMoves() : board.getBlackValidMoves();
      if (moves.isEmpty()) {
        return null;
      }
      return moves.get(random.nextInt(moves.size()));
    }
  }

  private static class MatchResult {
    final PlayerColor winner;

    MatchResult(PlayerColor winner) {
      this.winner = winner;
    }
  }

  // Support constructors for top-level test delegation when using AiPlayer/RandomPlayer
  private MatchResult runMatch(AiPlayer white, AiPlayer black, PlayerColor start) {
    return runMatch(new AiWrapper(white), new AiWrapper(black), start);
  }

  private MatchResult runMatch(AiPlayer white, RandomPlayer black, PlayerColor start) {
    return runMatch(new AiWrapper(white), black, start);
  }

  private MatchResult runMatch(RandomPlayer white, AiPlayer black, PlayerColor start) {
    return runMatch(white, new AiWrapper(black), start);
  }
}
