package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.Evaluator;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Monte Carlo Tree Search (MCTS) implementation for the checkers AI.
 *
 * <p>Unlike MinMax-based approaches, MCTS does not exhaustively explore a fixed depth. Instead, it
 * repeats four phases in a loop until the time budget is exhausted:
 *
 * <ol>
 *   <li><b>Selection</b> — traverse the tree from the root, choosing at each node the child that
 *       maximises the UCB1 score, until reaching a node that has unexplored children.
 *   <li><b>Expansion</b> — add one new child node for one unvisited move.
 *   <li><b>Simulation</b> — play the game out randomly from the new node until a terminal state is
 *       reached or a depth cap is hit.
 *   <li><b>Backpropagation</b> — walk back up to the root, incrementing visit counts and win
 *       scores at every ancestor.
 * </ol>
 *
 * <p>After the time limit, the child of the root with the highest visit count is returned.
 *
 * <p>The UCB1 formula balances exploration and exploitation:
 *
 * <pre>UCB1(child) = w/n + C * sqrt(ln(N) / n)</pre>
 *
 * <p>where {@code w} = child win score, {@code n} = child visit count, {@code N} = parent visit
 * count, and {@code C} is the exploration constant (default).
 */
public class Mcts extends Ai {

  /**
   * Default UCB1 exploration constant (√2).
   *
   * <p>Increase this to explore more broadly; decrease it to focus on already-promising moves.
   */
  public static final double DEFAULT_EXPLORATION = Math.sqrt(2);

  /**
   * Maximum number of moves per random simulation.
   *
   * <p>Caps runaway rollouts on positions where neither side wins quickly.
   */
  private static final int SIMULATION_DEPTH = 60;

  /** Score credited to a win during backpropagation. */
  private static final double WIN_SCORE = 1.0;

  /**
   * Score credited to a draw during backpropagation.
   *
   * <p>Halfway between a win and a loss so draws are not treated as worthless.
   */
  private static final double DRAW_SCORE = 0.5;

  /** The default selection mode for this instance. */
  public static final SelectionMode DEFAULT_SELECTION_MODE = SelectionMode.UCT;

  /** UCB1 exploration constant for this instance. */
  final double explorationConstant;

  /** The selection mode for this instance. */
  private SelectionMode selectionMode;

  /** The learned weights for the logistic regression model. */
  private static double[] mlWeights = null;

  /** The learned bias for the logistic regression model. */
  private static double mlBias = 0.0;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /** Creates an MCTS AI with default time limit and exploration constant. */
  public Mcts() {
    super();
    this.explorationConstant = DEFAULT_EXPLORATION;
    this.selectionMode = DEFAULT_SELECTION_MODE;
  }

  /**
   * Creates an MCTS AI with the specified depth and default exploration constant.
   *
   * <p>The {@code depth} parameter is accepted for API compatibility with {@link Ai} but is unused
   * — MCTS is time-driven, not depth-driven.
   *
   * @param depth ignored; MCTS uses the time limit instead
   */
  public Mcts(int depth) {
    super(depth);
    this.explorationConstant = DEFAULT_EXPLORATION;
    this.selectionMode = DEFAULT_SELECTION_MODE;
  }

  /**
   * Creates an MCTS AI with the specified depth and time limit.
   *
   * @param depth ignored; MCTS uses the time limit instead
   * @param maxTimeMs the maximum thinking time in milliseconds
   */
  public Mcts(int depth, long maxTimeMs) {
    super(depth, maxTimeMs);
    this.explorationConstant = DEFAULT_EXPLORATION;
    this.selectionMode = DEFAULT_SELECTION_MODE;
  }

  /**
   * Creates an MCTS AI with the specified depth, time limit, and exploration constant.
   *
   * @param depth ignored; MCTS uses the time limit instead
   * @param maxTimeMs the maximum thinking time in milliseconds
   * @param explorationConstant UCB1 exploration constant C (must be positive)
   * @throws IllegalArgumentException if {@code explorationConstant} is not positive
   */
  public Mcts(int depth, long maxTimeMs, double explorationConstant) {
    super(depth, maxTimeMs);
    if (explorationConstant <= 0) {
      throw new IllegalArgumentException(
          "Exploration constant must be positive, got: " + explorationConstant);
    }
    this.explorationConstant = explorationConstant;
    this.selectionMode = DEFAULT_SELECTION_MODE;
  }

  // -------------------------------------------------------------------------
  // Node — inner class
  // -------------------------------------------------------------------------

  /**
   * A node in the MCTS search tree.
   *
   * <p>Each node stores the move that produced it, statistics collected by simulations passing
   * through it, and references to its parent and children.
   */
  private class Node {

    /** Move that produced this node's board state from the parent; {@code null} at the root. */
    final Move move;

    /**
     * The player who made {@link #move}.
     *
     * <p>At the root this is set to {@code opponent(currentPlayer)} so that
     * {@code opponent(node.player)} always gives the correct next player at every level.
     */
    final PlayerColor player;

    /** Parent node; {@code null} for the root. */
    final Node parent;

    /** Children discovered so far (one per expanded move). */
    final List<Node> children = new ArrayList<>();

    /**
     * Moves reachable from this node not yet expanded into children.
     *
     * <p>Populated lazily on first access via {@link #getUnexploredMoves(Board)}.
     */
    List<Move> unexploredMoves = null;

    /** Number of simulations that have passed through this node. */
    int visits = 0;

    /**
     * Accumulated win score from simulations that passed through this node.
     *
     * <p>Each win adds {@value #WIN_SCORE}, each draw adds {@value #DRAW_SCORE}, a loss adds 0.
     */
    double wins = 0.0;

    /**
     * Creates a root node (no parent, no move).
     *
     * @param player the player who "just moved" before the search starts, i.e. the opponent of the
     *     current player
     */
    Node(PlayerColor player) {
      this.move = null;
      this.player = player;
      this.parent = null;
    }

    /**
     * Creates a child node.
     *
     * @param move the move that produced this node's position
     * @param player the player who made {@code move}
     * @param parent the parent node
     */
    Node(Move move, PlayerColor player, Node parent) {
      this.move = move;
      this.player = player;
      this.parent = parent;
    }

    /**
     * Returns the win rate of this node, or 0 when unvisited.
     *
     * @return {@code wins / visits}, or 0 if {@code visits == 0}
     */
    double winRate() {
      return visits == 0 ? 0 : wins / visits;
    }

    /**
     * Returns true when every legal move from this position has been expanded into a child node.
     *
     * @param board the board in this node's position (used to compute legal moves on first call)
     * @return true if fully expanded
     */
    boolean isFullyExpanded(Board board) {
      return getUnexploredMoves(board).isEmpty();
    }

    /**
     * Returns the list of unexplored moves, computing it lazily on first call.
     *
     * @param board the board in this node's position
     * @return mutable list of moves not yet expanded into children
     */
    List<Move> getUnexploredMoves(Board board) {
      if (unexploredMoves == null) {
        unexploredMoves = new ArrayList<>(getValidMoves(board, opponent(player)));
      }
      return unexploredMoves;
    }

    /**
     * Selects the child with the highest UCB1 score.
     *
     * <p>UCB1(child) = {@code winRate + C * sqrt(ln(visits) / child.visits)}. Unvisited children
     * return {@link Double#MAX_VALUE} so they are always visited before any visited sibling.
     *
     * @return the best child by UCB1, or {@code null} if there are no children
     */
    Node bestChildByUcb1() {
      Node best = null;
      double bestScore = Double.NEGATIVE_INFINITY;
      double logVisits = Math.log(visits);

      for (Node child : children) {
        double score = child.visits == 0
            ? Double.MAX_VALUE
            : child.winRate() + explorationConstant * Math.sqrt(logVisits / child.visits);
        if (score > bestScore) {
          bestScore = score;
          best = child;
        }
      }
      return best;
    }

    /**
     * Returns the child with the highest visit count (the final move-selection criterion).
     *
     * <p>Visit count is preferred over win rate because it is more robust to variance in shallow
     * simulations.
     *
     * @return most-visited child, or {@code null} if there are no children
     */
    Node mostVisitedChild() {
      Node best = null;
      int bestVisits = -1;
      for (Node child : children) {
        if (child.visits > bestVisits) {
          bestVisits = child.visits;
          best = child;
        }
      }
      return best;
    }

    /**
     * Selects the child with the highest predicted score from the TensorFlow model.
     *
     * @param currentBoard the current board state at this node 
     *     (used to extract features for the model)
     * @param undo the undo manager used to apply and revert the child's move for evaluation
     * @return the child with the highest predicted score, or {@code null} if there are no children
     */
    Node bestChildByMl(Board currentBoard, ManagerUndoRedo undo) {
      Node best = null;
      double bestScore = Double.NEGATIVE_INFINITY;

      for (Node child : children) {
        double score = evaluateMl(child, currentBoard, undo);

        if (score > bestScore) {
          bestScore = score;
          best = child;
        }
      }
      return best;
    }
  }

  // -------------------------------------------------------------------------
  // Main entry point
  // -------------------------------------------------------------------------

  /**
   * Calculates the best move using MCTS within the configured time budget.
   *
   * <p>Builds a search tree rooted at the current position, running selection → expansion →
   * simulation → backpropagation iterations until time runs out, then returns the move leading to
   * the most-visited child of the root.
   *
   * @param undo the undo manager used to apply and revert moves
   * @param board the current state of the game board
   * @param player the color of the current player
   * @param evaluator unused — MCTS uses random rollouts rather than a heuristic evaluator
   * @param validMoves the list of valid moves available to the current player
   * @return the move leading to the most-visited child of the root
   */
  @Override
  protected Move calculateBestMove(ManagerUndoRedo undo, Board board, PlayerColor player,
      Evaluator evaluator, List<Move> validMoves) {

    long startTime = System.currentTimeMillis();
    // The root's player field is set to opponent(player) so that opponent(root.player) == player
    // at the first expansion level, keeping the turn-alternation logic uniform throughout the tree.
    Node root = new Node(opponent(player));
    root.unexploredMoves = new ArrayList<>(validMoves);

    while (!isTimeExceeded(startTime)) {
      // 1. Selection
      Node node = select(root, board, undo);

      // 2. Expansion — only expand visited nodes; simulate unvisited nodes in-place first.
      if (!node.isFullyExpanded(board) && node.visits > 0) {
        node = expand(node, board, undo);
      }

      // 3. Simulation
      double result = simulate(board, undo, player, node.player);

      // 4. Backpropagation
      backpropagate(node, result, player, undo);
    }

    Node best = root.mostVisitedChild();
    return best != null ? best.move : validMoves.get(0);
  }

  // -------------------------------------------------------------------------
  // MCTS phases
  // -------------------------------------------------------------------------

  /**
   * Selection phase: traverse the tree using UCB1 until an incompletely expanded node is reached.
   *
   * <p>At each fully expanded node the child with the highest UCB1 score is entered and its move
   * is applied to the board. All applied moves are registered with {@code undo} so they can be
   * reverted during backpropagation.
   *
   * @param root the root node to start from
   * @param board the live board (mutated during traversal)
   * @param undo the undo manager used to register each applied move
   * @return the deepest node that still has unexplored children (or an unvisited leaf)
   */
  private Node select(Node root, Board board, ManagerUndoRedo undo) {
    Node node = root;
    while (node.isFullyExpanded(board) && !node.children.isEmpty()) {
      switch (selectionMode) {
        case UCT -> node = node.bestChildByUcb1();
        case ML -> node = node.bestChildByMl(board, undo);
        default -> throw new IllegalStateException(Internationalization.get("ai.unknown_selection",
            selectionMode));
      }
      undo.registerMove(node.player, node.move);
      board.applyMove(node.move);
    }
    return node;
  }

  /**
   * Expansion phase: pick one unexplored move, create a child node, and apply it to the board.
   *
   * <p>The chosen move is removed from the parent's unexplored list so it is never expanded twice.
   *
   * @param node the node to expand (must have at least one unexplored move)
   * @param board the live board at the node's position (mutated by applying the new move)
   * @param undo the undo manager used to register the applied move
   * @return the newly created child node
   */
  private Node expand(Node node, Board board, ManagerUndoRedo undo) {
    List<Move> unexplored = node.getUnexploredMoves(board);
    Move move = unexplored.remove(unexplored.size() - 1);
    PlayerColor nextPlayer = opponent(node.player);

    Node child = new Node(move, nextPlayer, node);
    node.children.add(child);

    undo.registerMove(nextPlayer, move);
    board.applyMove(move);
    return child;
  }

  /**
   * Simulation phase: play random moves until a terminal state or the depth cap is reached.
   *
   * <p>Every move applied during the simulation is registered with {@code undo} and fully reverted
   * before this method returns, leaving the board in exactly the state it was in after expansion.
   *
   * @param board the live board at the expanded node's position (mutated then fully restored)
   * @param undo the undo manager used to revert simulation moves
   * @param rootPlayer the player for whom a win is a positive result
   * @param lastExpandedPlayer the player who made the last expansion move
   * @return {@value #WIN_SCORE} for a win, {@value #DRAW_SCORE} for a draw, 0 for a loss
   */
  private double simulate(Board board, ManagerUndoRedo undo, PlayerColor rootPlayer,
      PlayerColor lastExpandedPlayer) {

    PlayerColor turn = opponent(lastExpandedPlayer);
    int movesPlayed = 0;

    while (movesPlayed < SIMULATION_DEPTH) {
      if (board.noPiecesLeft(PlayerColor.WHITE) || board.noPiecesLeft(PlayerColor.BLACK)) {
        break;
      }
      List<Move> moves = getValidMoves(board, turn);
      if (moves.isEmpty()) {
        break;
      }
      Move randomMove = moves.get((int) (Math.random() * moves.size()));
      undo.registerMove(turn, randomMove);
      board.applyMove(randomMove);
      movesPlayed++;
      turn = opponent(turn);
    }

    double result = evaluateResult(board, rootPlayer);

    // Revert all simulation moves in reverse order.
    for (int i = 0; i < movesPlayed; i++) {
      turn = opponent(turn);
      undo.undo(turn == PlayerColor.WHITE);
    }
    return result;
  }

  /**
   * Backpropagation phase: update statistics from the leaf back to the root.
   *
   * <p>Each ancestor has its visit count incremented and its win score updated from
   * {@code rootPlayer}'s perspective: nodes where {@code rootPlayer} moved receive {@code result};
   * nodes where the opponent moved receive {@code WIN_SCORE - result}. Board moves registered
   * during selection and expansion are also undone here, restoring the board to the root position.
   *
   * @param node the leaf node from which to start (the expanded or selected node)
   * @param result the simulation outcome ({@value #WIN_SCORE}, {@value #DRAW_SCORE}, or 0)
   * @param rootPlayer the player for whom a high result is good
   * @param undo the undo manager used to revert selection and expansion moves
   */
  private void backpropagate(Node node, double result, PlayerColor rootPlayer,
      ManagerUndoRedo undo) {
    Node current = node;
    while (current != null) {
      current.visits++;
      if (current.player == rootPlayer) {
        current.wins += result;
      } else {
        current.wins += (WIN_SCORE - result);
      }
      if (current.move != null) {
        undo.undo(current.player == PlayerColor.WHITE);
      }
      current = current.parent;
    }
  }

  // -------------------------------------------------------------------------
  // Machine Learning
  // -------------------------------------------------------------------------

  /**
  * Extracts a feature vector from the given board state for input into the TensorFlow model.
  *
  * @param board the board state to extract features from
  * @return a float array containing the extracted features in the order expected by the model
  */
  public static float[] extractFeatures(Board board) {
    int whitePawns = board.whitePawnsCount();
    int blackPawns = board.blackPawnsCount();
    int whiteKings = board.whiteCheckersCount();
    int blackKings = board.blackCheckersCount();

    return new float[] {
        whitePawns,
        blackPawns,
        whiteKings,
        blackKings,
        whitePawns - blackPawns,
        whiteKings - blackKings
    };
  }

  /**
   * Evaluates the given child node's board state using the logistic regression model.
   * This methode loads the model weights from file on first call and caches them
   * for subsequent calls. If the model is not loaded successfully, an exception is thrown to
   * prevent silent failures.
   *
   * @param child the node whose board state is to be evaluated
   * @param currentBoard the current board state at the node (
   *     used to extract features for the model)
   * @param undo the undo manager used to apply and revert the child's move for evaluation
   * @return the predicted probability of victory for the root player, or 0 if the model 
   *     is not loaded
   */
  private double evaluateMl(Node child, Board board, ManagerUndoRedo undo) {

    if (mlWeights == null) {
      loadMlWeights(LogisticRegressionTrainer.OUTPUT_FILEPATH);

      if (mlWeights == null) {
        throw new IllegalStateException("ML weights not loaded; cannot evaluate"
            + "\n Be sure to use -tr option before running MCTS with ML selection mode.");
      }
    }

    board.applyMove(child.move);
    float[] features = extractFeatures(board);
    undo.registerMove(child.player, child.move);
    undo.undo(child.player == PlayerColor.WHITE);
    double[] w = mlWeights;
    double b = mlBias;

    double z = b;
    for (int i = 0; i < w.length; i++) {
      z += w[i] * features[i];
    }

    return LogisticRegressionTrainer.sigmoid(z);
  }

  /**
   * Loads the logistic regression model weights from a file. 
   * The file should have the following format:
   * - First line: comma-separated weight values (one per feature)
   * - Second line: bias value
   *
   * @param filepath the path to the weights file
   * @throws IllegalStateException if the weights cannot be loaded successfully
   */
  public static void loadMlWeights(String filepath) {
    try {
      List<String> lines = Files.readAllLines(Paths.get(filepath));
      if (lines.size() >= 2) {
        String[] firstLine = lines.get(0).split(",");
        double[] tempWeights = new double[firstLine.length];
        for (int i = 0; i < firstLine.length; i++) {
          tempWeights[i] = Double.parseDouble(firstLine[i]);
        }
        double tempBias = Double.parseDouble(lines.get(1));

        mlWeights = tempWeights;
        mlBias = tempBias;

        System.out.println(Internationalization.get("ai.training.weights_loaded", filepath));
      }
    } catch (IOException | NumberFormatException e) {
      System.err.println(Internationalization.get("ai.training.load_error", e.getMessage()));
    }
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * Returns the opponent of the given player.
   *
   * @param player a player color
   * @return the other player color
   */
  private static PlayerColor opponent(PlayerColor player) {
    return player == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  /**
   * Evaluates the terminal (or depth-capped) board from {@code rootPlayer}'s perspective.
   *
   * <p>Returns {@value #WIN_SCORE} if the opponent has no pieces, 0 if {@code rootPlayer} has no
   * pieces, and {@value #DRAW_SCORE} if neither side has won (depth cap reached with both sides
   * still alive).
   *
   * @param board the board at the end of the simulation
   * @param rootPlayer the player to evaluate for
   * @return the numeric outcome for backpropagation
   */
  private static double evaluateResult(Board board, PlayerColor rootPlayer) {
    PlayerColor opp = opponent(rootPlayer);
    if (board.noPiecesLeft(opp)) {
      return WIN_SCORE;
    }
    if (board.noPiecesLeft(rootPlayer)) {
      return 0.0;
    }
    return DRAW_SCORE;
  }

  /**
   * Sets the selection mode for this MCTS instance, determining how child nodes are selected during
   * the selection phase.
   *
   * @param selectionMode the selection mode to use
   */
  public void setSelectionMode(SelectionMode selectionMode) {
    this.selectionMode = selectionMode;
  }
}