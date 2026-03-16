package fr.ubordeaux.pdp.model.core;

import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.HumanPlayer;
import fr.ubordeaux.pdp.model.player.Player;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.History;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.ManagerUndoRedo;
import fr.ubordeaux.pdp.view.GameView;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the core logic, rules, and state transitions for the Checkers game.
 *
 * <p>This class acts as the central model in the MVC architecture, coordinating
 * interactions between the board, players, and game state. It implements the
 * {@link Subject} interface to notify registered views of state changes.
 */
public class GameCheckers implements Subject {

  /** The current state of the game. */
  private State state;
  /** The game board. */
  private Board board;
  /** The white player. */
  private Player whitePlayer;
  /** The black player. */
  private Player blackPlayer;
  /** Indicates whether it is the white player's turn. */
  private boolean isWhiteTurn;
  /** The list of observers (views) interested in game state changes. */
  private List<GameView> observers;
  /** The configuration options for the game. */
  private Configuration configuration;
  /** The manager for handling undo and redo operations. */
  private ManagerUndoRedo managerUndoRedo;

  /**
   * Initializes a new game instance with the specified configuration.
   *
   * @param cfg the configuration settings for the game
   */
  public GameCheckers(Configuration cfg) {
    this.configuration = cfg;
    this.board = new Board(cfg.getSize());
    this.isWhiteTurn = true;
    this.state = State.IN_GAME;
    managerUndoRedo = new ManagerUndoRedo(this.board);

    // MODE IA
    if (cfg.iswhiteAi() == true) {
      this.whitePlayer = new AiPlayer(Internationalization.get("game.white_ai_player"));
    } else {
      this.whitePlayer = new HumanPlayer(Internationalization.get("game.white_player"));
    }
    if (cfg.isblackAi() == true) {
      this.blackPlayer = new AiPlayer(Internationalization.get("game.black_ai_player"));
    } else {
      this.blackPlayer = new HumanPlayer(Internationalization.get("game.black_player"));
    }

    // MODE BLITZ
    if (cfg.isBlitz() == true) {
      int timeInSeconds = cfg.getTime() * 60;

      this.whitePlayer.setPlayTime(timeInSeconds);
      this.blackPlayer.setPlayTime(timeInSeconds);
    }
  }

  /**
   * Initializes a new game instance with the default configuration.
   */
  public GameCheckers() {
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
   * @return {@code true} if it is the player's turn and the move is valid;
   *         {@code false} otherwise.
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
   * Validates the move against legal moves to enforce rules like mandatory
   * captures.
   *
   * @param fromS position from.
   * @param toS   position to.
   */
  public void applyMove(String fromS, String toS) {
    Move move = null;
    int from;
    int to;
    PlayerColor currentColor;

    currentColor = isWhiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;

    if (state == State.PAUSE) {
      System.out.println(Internationalization.get("game.game_paused"));
      return;
    }

    if (state == State.FINISHED) {
      System.out.println(Internationalization.get("game.game_is_over"));
      return;
    }

    try {
      from = this.board.squareToIndex(fromS);
      to = this.board.squareToIndex(toS);
    } catch (IllegalArgumentException e) {
      System.err.println(Internationalization.get("game.invalid_square") + " " + e.getMessage());

      return;
    }

    List<Move> possibleMoves = this.getPossibleMoves(this.getCurrentPlayer());
    for (Move m : possibleMoves) {
      if (m.getFrom() == from && m.getTo() == to) {
        move = m;
        break;
      }
    }

    if (move == null) {

      System.err.println(Internationalization.get("game.invalid_move"));
      System.out
          .println(String.format(Internationalization.get("game.display_valid_moves"),
              getCurrentPlayer().getName()));

      for (Move m : possibleMoves) {
        String fromSquare = this.board.indexToSquare(m.getFrom());
        String toSquare = this.board.indexToSquare(m.getTo());
        System.out.println("  -> " + fromSquare + " " + toSquare);
      }

      return;
    }

    board.applyMove(move);
    managerUndoRedo.registerMove(currentColor, move);
    this.isWhiteTurn = !this.isWhiteTurn;
    notifyObservers();
  }

  /**
   * Evaluates if the game has reached an end condition.
   *
   * 
   * <p>Currently checks if the active player has any legal moves remaining.
   * If not, the game transitions to FinishedState.
   *
   * @return The new state if the game is over, otherwise the current state.
   */
  public State checkGameOver() {
    Player currentPlayer = isWhiteTurn ? whitePlayer : blackPlayer;

    // A player loses immediately if they cannot make a move.
    if (getPossibleMoves(currentPlayer).isEmpty()) {
      setState(State.FINISHED);
      return this.state;
    }

    // If both players have no moves, the game is also finished (draw).
    if (getPossibleMoves(whitePlayer).isEmpty() && getPossibleMoves(blackPlayer).isEmpty()) {
      setState(State.FINISHED);
      return this.state;
    }

    return this.state;
  }

  /**
   * Notifies all registered observers (views) of a state change, prompting them to
   * update their display accordingly.
   */
  @Override
  public void notifyObservers() {
    // Guard clause to prevent NullPointerException if no observers are registered
    // yet.
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

  /**
   * Returns whether it is currently the white player's turn.
   *
   * @return {@code true} if it is the white player's turn
   */
  public boolean isWhiteTurn() {
    return isWhiteTurn;
  }

  /**
   * Sets the turn to the specified player color.
   *
   * @param isWhite if {@code true}, sets the turn to the white player
   */
  public void setWhiteTurn(boolean isWhite) {
    this.isWhiteTurn = isWhite;
  }

  /**
   * Returns the current game configuration.
   *
   * @return The active {@link Configuration} instance containing game settings.
   */
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

  /**
   * Updates the play time of the current player by decrementing it by 1 second.
   */
  public void timerPlayer() {
    if (isWhiteTurn) {
      int newTime = whitePlayer.getPlayTime() - 1;
      whitePlayer.setPlayTime(newTime);
    } else {
      int newTime = blackPlayer.getPlayTime() - 1;
      blackPlayer.setPlayTime(newTime);
    }
  }

  /**
   * Manages the undo operation by reverting the last move made by the current
   * player.
   */
  public void undoManage() {
    if (managerUndoRedo.undo(this.isWhiteTurn)) {
      this.isWhiteTurn = !this.isWhiteTurn;
      notifyObservers();
    }
  }

  /**
   * Manages the redo operation by reapplying the last undone move for the current
   * player.
   */
  public void redoManage() {
    if (managerUndoRedo.redo(this.isWhiteTurn)) {
      this.isWhiteTurn = !this.isWhiteTurn;
      notifyObservers();
    }
  }

  /**
   * Returns the current history of moves, which can be used for undo/redo operations or
   * for displaying move history to the user.
   *
   * @return The current {@link History} instance containing the sequence of moves made in the game.
   */
  public History getHistory() {
    return managerUndoRedo.getHistory();
  }

  /**
   * Sets the history of moves to a specific state, allowing for features like loading a game
   * from a saved state or resetting the move history.
   *
   * @param h The {@link History} instance to set as the current move history.
   */
  public void setHistory(History h) {
    managerUndoRedo.setHistory(h);
  }

  public ManagerUndoRedo getManagerUndoRedo() {
    return managerUndoRedo;
  }

  public PlayerColor getCurrentColor() {
    return isWhiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
  }

}