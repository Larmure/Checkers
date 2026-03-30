package fr.ubordeaux.pdp.model.player.ai;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.MaxEvaluator;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import java.util.List;

/**
 * Simple Logistic Regression trainer using gradient descent.
 */
public class LogisticRegressionTrainer {

  /** Maximum number of moves in a game for training. */
  private static final int MAX_MOVES = 100;

  /** The weights for each feature. */
  private final double[] weights;
  /** The learning rate for gradient descent. */
  private final double learningRate;

  /** The bias term (intercept) for the logistic regression model. */
  private double bias;

  /**
   * Initializes the logistic regression model with the specified number of features and learning 
   * rate.
   *
   * @param numFeatures Number of features (6 in this case)
   * @param learningRate Learning rate (e.g., 0.01)
   */
  public LogisticRegressionTrainer(int numFeatures, double learningRate) {
    this.weights = new double[numFeatures];
    this.bias = 0.0;
    this.learningRate = learningRate;
  }

  /**
    * Sigmoid activation function.
    * Transforms any number into a probability between 0.0 and 1.0.
    *
    * @param z The input value (linear combination of features and weights)
    * @return The output of the sigmoid function, representing the predicted probability of winning
   */
  private double sigmoid(double z) {
    return 1.0 / (1.0 + Math.exp(-z));
  }

  /**
   * Predicts the probability of winning (between 0 and 1) for a given board.
   *
   * @param features The 6 float values extracted from the board representing the current state
   * @return The predicted probability of winning for the current player
   */
  public double predict(float[] features) {
    double z = bias;
    for (int i = 0; i < weights.length; i++) {
      z += weights[i] * features[i];
    }
    return sigmoid(z);
  }

  /**
    * Trains the model on a game history dataset.
    *
    * @param x List of feature vectors (the 6 float values extracted from the board)
    * @param y List of ground-truth outcomes (1.0 for win, 0.0 for loss)
    * @param epochs Number of full passes over the dataset
   */
  public void train(List<float[]> x, List<Double> y, int epochs) {
    System.out.println("Début de l'entraînement...");

    for (int epoch = 0; epoch < epochs; epoch++) {
      double totalLoss = 0;

      for (int i = 0; i < x.size(); i++) {
        float[] features = x.get(i);
        double label = y.get(i); // Ground-truth outcome

        // 1. Prediction with current weights
        double prediction = predict(features);

        // 2. Error computation (difference between prediction and reality)
        double error = prediction - label;

        // Compute log loss (only for progress display)
        // Avoid log(0) by clipping prediction between 1e-15 and 1 - 1e-15
        double p = Math.max(1e-15, Math.min(1 - 1e-15, prediction));
        totalLoss += -label * Math.log(p) - (1 - label) * Math.log(1 - p);

        // 3. Weight update (gradient descent)
        for (int j = 0; j < weights.length; j++) {
          weights[j] -= learningRate * error * features[j];
        }
        bias -= learningRate * error;
      }

      // Display progress every 100 epochs
      if (epoch % 100 == 0) {
        System.out.printf("Epoch %d | Loss moyenne: %.4f%n", epoch, (totalLoss / x.size()));
      }
    }
    System.out.println("Entraînement terminé !");
  }

  /**
    * Prints the final weights that can be copied into Mcts.java.
   */
  private void printFinalWeights() {
    System.out.println("\n=== Results to use in Mcts.java ===");
    System.out.print("double[] learnedWeights = new double[] {");
    for (int i = 0; i < weights.length; i++) {
      System.out.print(weights[i] + (i < weights.length - 1 ? ", " : ""));
    }
    System.out.println("};");
    System.out.println("double learnedBias = " + bias + ";");
    System.out.println("=========================================\n");
  }

  /**
   * Runs a match between two MCTS AIs and collects training data for the logistic regression model.
   *
   * @return the result of the match (winner or draw)
   */
  private MatchResult runMatch() {
    Board board = new Board(8);
    ManagerUndoRedo undo = new ManagerUndoRedo(board);
    boolean whiteTurn = true;
    AiPlayer whiteAi = new AiPlayer("MCTS", new Mcts(1, 200,
        Mcts.DEFAULT_EXPLORATION), new MaxEvaluator());
    AiPlayer blackAi = new AiPlayer("MCTS", new Mcts(1, 200,
        Mcts.DEFAULT_EXPLORATION), new MaxEvaluator());

    int moveCount = 0;
    List<float[]> matchHistory = new java.util.ArrayList<>();

    while (moveCount < MAX_MOVES) {
      PlayerColor currentColor = whiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
      AiPlayer currentPlayer = whiteTurn ? whiteAi : blackAi;

      matchHistory.add(extractFeatures(board));

      Move move = currentPlayer.getBestMove(undo, board, currentColor);
      if (move == null) {
        // Current player has no moves -> game over
        return new MatchResult(whiteTurn ? opponent(currentColor) : currentColor,
            matchHistory);
      }

      board.applyMove(move);
      undo.registerMove(currentColor, move);

      // Fin de partie si un joueur a perdu toutes ses pièces
      if (board.noPiecesLeft(PlayerColor.WHITE)) {
        return new MatchResult(PlayerColor.BLACK, matchHistory);
      }
      if (board.noPiecesLeft(PlayerColor.BLACK)) {
        return new MatchResult(PlayerColor.WHITE, matchHistory);
      }

      whiteTurn = !whiteTurn;
      moveCount++;
    }

    // Draw threshold
    return new MatchResult(null, matchHistory);
  }

  /**
   * Simple class to represent the result of a match, including the winner (or null for a draw).
   * This is used to collect training data for the logistic regression model.
   */
  private static class MatchResult {
    /**
     * The winner of the match (or null for a draw).
     */
    final PlayerColor winner;

    final List<float[]> featuresHistory;

    /**
     * Initializes a MatchResult with the specified winner.
     *
     * @param winner the winner of the match (WHITE, BLACK, or null for draw)
     * @param featuresHistory the list of feature vectors collected during the match for training
     *     the logistic regression model
     * 
     */
    MatchResult(PlayerColor winner, List<float[]> featuresHistory) {
      this.winner = winner;
      this.featuresHistory = featuresHistory;
    }
  }

  /**
   * Returns the opponent color.
   *
   * @param color the current player's color
   * @return the opponent player's color
   */
  private PlayerColor opponent(PlayerColor color) {
    return color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  /**
  * Extracts a feature vector from the given board state for input into the TensorFlow model.
  */
  private float[] extractFeatures(Board board) {
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
   * Launches the training process by running multiple matches and collecting
   * results to evaluate the performance of the logistic regression model. 
   * It prints the outcomes of each match and a summary at the end.
   */
  public static void lauchTraining() {
    int whiteWins = 0;
    int blackWins = 0;
    int draws = 0;
    int numGames = 10;

    LogisticRegressionTrainer trainer = new LogisticRegressionTrainer(6, 0.01);
    List<float[]> allX = new java.util.ArrayList<>();
    List<Double> allY = new java.util.ArrayList<>();

    System.out.println("Lancement de " + numGames + " parties pour la collecte de données...");

    for (int i = 0; i < numGames; i++) {
      MatchResult result = trainer.runMatch();

      if (result.winner == null) {
        draws++;
      } else {
        // Determine the label (1.0 if White wins, 0.0 if Black wins)
        // Because our extractFeatures always counts White - Black
        double label = (result.winner == PlayerColor.WHITE) ? 1.0 : 0.0;

        // Add all feature vectors from this match to the training dataset with the same label
        for (float[] features : result.featuresHistory) {
          allX.add(features);
          allY.add(label);
        }

        if (result.winner == PlayerColor.WHITE) {
          whiteWins++;
        } else {
          blackWins++;
        }
      }

      System.out.printf("Partie %d terminée : %s (Positions extraites : %d)%n",
          i + 1, result.winner == null ? "Match nul" : (result.winner + " gagne"),
          result.featuresHistory.size());
    }

    System.out.printf("Bilan : %d victoires Blancs, %d victoires Noirs, %d nuls%n",
        whiteWins, blackWins, draws);
    System.out.println("Total des positions récoltées : " + allX.size());

    if (!allX.isEmpty()) {
      trainer.train(allX, allY, 1000);
      trainer.printFinalWeights();
    } else {
      System.out.println("Aucune donnée récoltée (que des matchs nuls).");
    }
  }
}