package fr.ubordeaux.pdp.model.core;

import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.HumanPlayer;
import fr.ubordeaux.pdp.model.player.Player;
import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.player.ai.Ai;
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
  /** Counts consecutive turns without progress. */
  private int noProgressCount = 0;
  /** Counts turns since the last capture. */
  private int endGameCount = 0;
  /** Stores the history of board positions. */
  private List<String> positionHistory = new ArrayList<>();

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

    // AI MODE INITIALIZATION

    if (cfg.iswhiteAi()) {
      this.whitePlayer = new AiPlayer(Internationalization.get("game.white_ai_player"));
      ((AiPlayer) this.whitePlayer).setAlgorithm(Ai.buildAi(cfg));
    } else {
      this.whitePlayer = new HumanPlayer(Internationalization.get("game.white_player"));
    }

    if (cfg.isblackAi()) {
      this.blackPlayer = new AiPlayer(Internationalization.get("game.black_ai_player"));
      ((AiPlayer) this.blackPlayer).setAlgorithm(Ai.buildAi(cfg));
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
   * @param isManoury boolean of manoury
   */
  public void applyMove(String fromS, String toS, boolean isManoury) {
    Move move = null;
    int from = -1;
    int to = 1;
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

    if (isManoury) {
      try {
        int manouryFrom = Integer.valueOf(fromS);
        int manouryTo = Integer.valueOf(toS);
        from = this.board.manouryToIndex(manouryFrom);
        to = this.board.manouryToIndex(manouryTo);
      } catch (IllegalArgumentException e) {
        System.err.println(Internationalization.get("game.invalid_square") + " "
            + e.getMessage());
        return;
      }
    } else {
      try {
        from = this.board.squareToIndex(fromS);
        to = this.board.squareToIndex(toS);
      } catch (IllegalArgumentException e) {
        System.err.println(Internationalization.get("game.invalid_square") + " "
            + e.getMessage());

        return;
      }
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
      if (isManoury) {
        for (Move m : possibleMoves) {
          String fromSquare = String.valueOf(this.board.indexToManoury(m.getFrom()));
          String toSquare = String.valueOf(this.board.indexToManoury(m.getTo()));
          System.out.println("  -> " + fromSquare + " " + toSquare);
        }
      } else {
        for (Move m : possibleMoves) {
          String fromSquare = this.board.indexToSquare(m.getFrom());
          String toSquare = this.board.indexToSquare(m.getTo());
          System.out.println("  -> " + fromSquare + " " + toSquare);
        }
      }

      return;
    }

    boolean isPawnMove = board.isBitWhitePawn(from) || board.isBitBlackPawn(from);
    boolean isCapture = move.isCapture();

    board.applyMove(move);
    managerUndoRedo.registerMove(currentColor, move);

    if (isCapture || isPawnMove) {
      noProgressCount = 0;
    } else {
      noProgressCount++;
    }

    if (isEndgameScenario()) {
      endGameCount++;
    } else {
      endGameCount = 0;
    }

    positionHistory.add(board.boardString());

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
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(Internationalization.get("game.game_winner") + " "
          + (isWhiteTurn ? blackPlayer.getName() : whitePlayer.getName()));
      setState(State.FINISHED);
      return this.state;
    }

    // If both players have no moves, the game is also finished (draw).
    if (getPossibleMoves(whitePlayer).isEmpty() && getPossibleMoves(blackPlayer).isEmpty()) {
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(Internationalization.get("game.game_draw"));
      setState(State.FINISHED);
      return this.state;
    }

    // 25 turn *2 = 50 half-turns without progress (no captures or pawn moves) is a common rule 
    // for declaring a draw.
    if (noProgressCount >= 50) {
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(Internationalization.get("game.game_draw"));
      System.out.println(Internationalization.get("game.game_over_no_progress"));
      setState(State.FINISHED);
      return this.state;
    }

    // 16 turns *2 = 32 half-turns in an endgame scenario (one player has only one piece left) 
    // is often considered a draw due to insufficient material.
    if (endGameCount >= 32) {
      System.out.println(Internationalization.get("game.game_over"));
      System.out.println(Internationalization.get("game.game_draw"));
      System.out.println(Internationalization.get("game.game_over_endgame"));
      setState(State.FINISHED);
      return this.state;
    }

    // 3-fold repetition rule: if the same board position occurs 3 times, the game is a draw.
    if (!positionHistory.isEmpty()) {
      String currentSignature = board.boardString();
      long occurrences = positionHistory.stream()
          .filter(sig -> sig.equals(currentSignature))
          .count();
      if (occurrences >= 3) {
        System.out.println(Internationalization.get("game.game_over"));
        System.out.println(Internationalization.get("game.game_draw"));
        System.out.println(Internationalization.get("game.game_over_repetition"));
        setState(State.FINISHED);
        return this.state;
      }
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
    }

    if (!positionHistory.isEmpty()) {
      positionHistory.remove(positionHistory.size() - 1);
    }

    notifyObservers();
  }

  /**
   * Manages the redo operation by reapplying the last undone move for the current
   * player.
   */
  public void redoManage() {
    if (managerUndoRedo.redo(this.isWhiteTurn)) {
      this.isWhiteTurn = !this.isWhiteTurn;
    }

    positionHistory.add(board.boardString());
    notifyObservers();
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

  /**
   * Returns the manager responsible for handling undo and redo operations, 
   * which maintains the move history and allows for reverting or reapplying moves as needed.
   *
   * @return The active {@link ManagerUndoRedo} instance managing the undo/redo functionality for 
   *     the game.
   */
  public ManagerUndoRedo getManagerUndoRedo() {
    return managerUndoRedo;
  }

  /**
   * Returns the color of the current player based on whose turn it is.
   *
   * @return  The {@link PlayerColor} corresponding to the current player's turn (WHITE or BLACK).
   */
  public PlayerColor getCurrentColor() {
    return isWhiteTurn ? PlayerColor.WHITE : PlayerColor.BLACK;
  }

  /**
   * Determines if the game has reached an endgame scenario based on the current board state.
   * This method checks for specific configurations of pieces that indicate a likely endgame, 
   * such as one player having only a single checker while the other has multiple pieces.
   *
   * @return {@code true} if the game is in an endgame scenario, {@code false} otherwise.
   */
  private boolean isEndgameScenario() {
    int whitePawns = 0;
    int blackPawns = 0;
    int whiteCheckers = 0;
    int blackCheckers = 0;

    whitePawns = board.countWhitePawns();
    blackPawns = board.countBlackPawns();
    whiteCheckers = board.countWhiteCheckers();
    blackCheckers = board.countBlackCheckers();

    int whiteTotal = whitePawns + whiteCheckers;
    int blackTotal = blackPawns + blackCheckers;

    boolean whiteAdvantage = (whiteTotal == 3 && blackTotal == 1 && blackCheckers == 1
        && blackPawns == 0);
    boolean blackAdvantage = (blackTotal == 3 && whiteTotal == 1 && whiteCheckers == 1
        && whitePawns == 0);

    return whiteAdvantage || blackAdvantage;
  }

}