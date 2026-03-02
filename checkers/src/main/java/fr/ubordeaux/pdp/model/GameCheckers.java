package fr.ubordeaux.pdp.model;

import java.util.ArrayList;
import java.util.List;

import fr.ubordeaux.pdp.view.GameView;

/**
 * Manages the core logic, rules, and state transitions for the Checkers game.
 *
 * <p>This class acts as the central model in the MVC architecture, coordinating
 * interactions between the board, players, and game state. It implements the
 * {@link Subject} interface to notify registered views of state changes.
 */
public class GameCheckers implements Subject {

  private State state;
  private Board board;
  private Player whitePlayer;
  private Player blackPlayer;
  private boolean isWhiteTurn;
  private List<GameView> observers;
  private  Configuration configuration;

  /**
   * Constructs a new game instance.
   * * <p>Initializes a standard 12x12 board, sets the initial state to {@link InGameState},
   * and grants the first turn to the white player.
   * Player types (Human or AI) are assigned based on the provided flags
   * 
   */

  public GameCheckers(Configuration configuration) {
    this .configuration=configuration;
    this.board = new Board(configuration.getSize());
    this.isWhiteTurn = true;
    this.state = new InGameState(this);
    if (configuration.isWhiteIsAI() == true) {
      this.whitePlayer = new AIPlayer("White AI");
    } else {
      this.whitePlayer = new HumanPlayer("White Player");
    }
    if (configuration.isBlackIsAI() == true) {
      this.blackPlayer = new AIPlayer("Black AI");
    } else {
      this.blackPlayer = new HumanPlayer("Black Player");
    }
  }

  public GameCheckers(){
    this(Configuration.getDefaultConfiguration());
  }

  /**
   * Returns the current state of the game engine.
   *
   * @return The current {@link State} instance.
   */
  public State getState() {
    return this.state;
  }

  /**
   * Transitions the game to a new state.
   *
   * @param newState The new state to apply.
   */
  public void setState(State newState) {
    this.state = newState;
  }

  /**
   * Identifies the player whose turn it currently is.
   *
   * @return The {@link Player} object for the current turn.
   */
  public Player getCurrentPlayer() {
    return isWhiteTurn ? whitePlayer : blackPlayer;
  }

  /**
   * Returns the game board.
   *
   * @return The active {@link Board} instance.
   */
  public Board getBoard() {
    return this.board;
  }

  /**
   * Returns the boolean of white turn.
   *
   * @return The {@link isWhiteTurn} boolean for the current turn.
   */
  public boolean getIsWhiteTurn() {
    return this.isWhiteTurn;
  }

  /**
   * Retrieves all legal moves available for the specified player.
   *
   * <p>This method delegates to the board logic, which enforces rules such as
   * mandatory captures.
   *
   * @param player The player to retrieve moves for.
   * @return A list of valid {@link Move} objects.
   */
  public List<Move> getPossibleMoves(Player player) {
    boolean isWhite = (player == whitePlayer);
    return isWhite ? board.getWhiteValidMoves() : board.getBlackValidMoves();
  }

  /**
   * Verifies if a specific move is legally allowed in the current context.
   *
   * @param move   The move to validate.
   * @param player The player attempting the move.
   * @return {@code true} if it is the player's turn and the move is valid; {@code false} otherwise.
   */
  public boolean isValidMove(Move move, Player player) {
    // Prevent moves if it is not the requesting player's turn.
    if ((isWhiteTurn && player != whitePlayer) || (!isWhiteTurn && player != blackPlayer)) {
      return false;
    }
    return getPossibleMoves(player).contains(move);
  }

  /**
   * Applies a move using algebraic notation (e.g., "32-28").
   * Validates the move against legal moves to enforce rules like mandatory captures.
   *
   * @param fromS   position from.
   * @param toS position to.
   */
  public void applyMove(String fromS, String toS) {
    Move move = null;
    int from, to;
    try {
        from = this.board.squareToIndex(fromS);
        to = this.board.squareToIndex(toS);
    } catch (IllegalArgumentException e) {
        System.err.println("Invalid square: " + e.getMessage());
        return;
    }

    for (Move m : this.getPossibleMoves(this.getCurrentPlayer())) {
      if (m.getFrom() == from && m.getTo() == to) {
        move = m;
        break;
      }
    }

    if (move == null) {
      System.err.println("Invalid move: Rule violation or mandatory capture missing.");
      System.out.println("Here are all valid moves for " + getCurrentPlayer().getName() + ":");

      List<Move> possibleMoves = this.getPossibleMoves(this.getCurrentPlayer());
      for (Move m : possibleMoves) {
        String fromSquare = this.board.indexToSquare(m.getFrom());
        String toSquare   = this.board.indexToSquare(m.getTo());
        System.out.println("  -> " + fromSquare + " " + toSquare);
      }

      return;
    }

    board.applyMove(move);
    this.isWhiteTurn = !this.isWhiteTurn;
    notifyObservers();
  }

  /**
   * Evaluates if the game has reached an end condition.
   *
   * <p>Currently checks if the active player has any legal moves remaining.
   * If not, the game transitions to {@link FinishedState}.
   *
   * @return The new state if the game is over, otherwise the current state.
   */
  public State checkGameOver() {
    Player currentPlayer = isWhiteTurn ? whitePlayer : blackPlayer;

    // A player loses immediately if they cannot make a move.
    if (getPossibleMoves(currentPlayer).isEmpty()) {
      return new FinishedState();
    }

    return this.state;
  }

  @Override
  public void notifyObservers() {
    // Guard clause to prevent NullPointerException if no observers are registered yet.
    if (this.observers == null) {
      return;
    }

    for (GameView v : observers) {
      v.update(this);
    }
  }

  /**
   * Registers a view to receive updates when the game state changes.
   *
   * @param observer The view implementing the {@link GameView} interface.
   */
  public void addObserver(GameView observer) {
    // Lazy initialization of the observer list.
    if (this.observers == null) {
      this.observers = new ArrayList<>();
    }
    this.observers.add(observer);
  }

  public boolean isWhiteTurn() {
    return isWhiteTurn;
  }

  public void setWhiteTurn(boolean isWhite) {
    this.isWhiteTurn = isWhite;
  }
  public Configuration getConfiguration() {
    return configuration;
  }


  /**
 * Returns the white player instance.
 *
 * @return The white player.
 */
  public Player getWhitePlayer() {
    return this.whitePlayer;
  }

  /**
   * Returns the black player instance.
   *
   * @return The black player.
   */
  public Player getBlackPlayer() {
    return this.blackPlayer;
  }
}