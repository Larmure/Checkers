package fr.ubordeaux.pdp.view.gui.board;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.gui.layout.PlayView;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * JavaFX component that renders an interactive checkers board.
 *
 * <p>The board is displayed as a {@link GridPane} where each dark square is a
 * clickable cell. Coordinate labels (A–H along the rows, 1–8 along the columns)
 * are drawn around the grid.
 *
 * <h2>Coordinate mapping</h2>
 * <ul>
 *   <li>Grid row 0 = model row {@code size - 1} = letter 'H' (top of the board).</li>
 *   <li>Grid row {@code size - 1} = model row 0 = letter 'A' (bottom of the board).</li>
 *   <li>Only dark squares ({@code (modelRow + modelCol) % 2 == 0}) are playable.</li>
 *   <li>Bitboard index = {@code (modelRow * size + modelCol) / 2}.</li>
 * </ul>
 *
 * <h2>Click handling</h2>
 * Two-click selection: first click selects a piece (highlighted in blue), second
 * click on a destination triggers {@link GameController#executeMove(String, String, boolean)}.
 *
 * <h2>Responsive sizing</h2>
 * Call {@link #bindCellSize(DoubleBinding)} to make cell size track the window
 * dimensions. The board redraws automatically whenever the binding value changes.
 *
 * <p>Visual styles are defined in {@code style.css} (classes:
 * {@code board-grid}, {@code coord-label}).
 * Square and piece colours remain in Java because {@link Rectangle#setFill}
 * and {@link Circle#setFill} are not controllable via CSS.
 */
public class BoardView extends GridPane {

  /** Background colour of light (non-playable) squares. */
  private static final Color LIGHT_SQ = Color.web("#f0bab5");

  /** Background colour of dark (playable) squares. */
  private static final Color DARK_SQ = Color.web("#b56363");

  /** Highlight colour applied to the currently selected square. */
  private static final Color SELECTED_SQ = Color.web("#beecf8", 0.75);

  /** Semi-transparent white tint shown when hovering over a dark square. */
  private static final Color HOVER_TINT = Color.web("#FFFFFF", 0.08);

  /** Fill colour for white pawns and kings. */
  private static final Color WHITE_FILL = Color.WHITE;

  /** Fill colour for black pawns and kings. */
  private static final Color BLACK_FILL = Color.web("#1C1C2E");

  /** Fill colour for the crown marker drawn on king pieces. */
  private static final Color CROWN_FILL = Color.web("#FFD700");

  /** Default cell size in pixels, used before any responsive binding is set. */
  private static final double DEFAULT_CELL = 68.0;

  /**
   * Size of coordinate labels relative to the cell size.
   * A value of {@code 0.30} means labels are 30 % of one cell wide/tall.
   */
  private static final double LABEL_RATIO = 0.30;

  /**
   * Observable cell size in pixels.
   *
   * <p>A listener on this property calls {@link #redraw()} whenever the value
   * changes, so the board scales live with the window.
   */
  private final DoubleProperty cellSize = new SimpleDoubleProperty(DEFAULT_CELL);

  /** Controller used to forward user moves. */
  private final GameController controller;

  /**
   * Reference to the current model board, used by {@link #getPieceChar} to
   * query piece positions via bitboards. {@code null} when no game is active.
   */
  private Board board;

  /** Number of rows/columns on the board (8, 10, or 12). */
  private int size = 8;

  /**
   * Grid row of the currently selected cell, or {@code -1} when nothing is
   * selected.
   */
  private int selRow = -1;

  /**
   * Grid column of the currently selected cell, or {@code -1} when nothing is
   * selected.
   */
  private int selCol = -1;

  /**
   * Creates a {@code BoardView} for the given controller.
   *
   * <p>An initial empty 8×8 board is drawn immediately so that the component
   * has a visible size before any game starts.
   *
   * @param controller the game controller that receives move commands
   */
  public BoardView(GameController controller) {
    this.controller = controller;
    this.setAlignment(Pos.CENTER);
    // style.css: .board-grid (drop shadow)
    this.getStyleClass().add("board-grid");
    cellSize.addListener((obs, oldV, newV) -> redraw());
    drawEmpty(8);
  }

  /**
   * Binds the cell size to an external {@link DoubleBinding}.
   *
   * <p>Typically called from {@link PlayView#bindToScene} so that the board
   * tracks the available window space. The board redraws automatically whenever
   * the binding value changes.
   *
   * @param binding a binding that computes the desired cell size in pixels
   */
  public void bindCellSize(DoubleBinding binding) {
    cellSize.bind(binding);
  }

  /**
   * Refreshes the board from the current state of a running game.
   *
   * <p>Must be called on the JavaFX Application Thread (e.g. inside a
   * {@code Platform.runLater} block).
   *
   * @param game the current game state
   */
  public void refresh(GameCheckers game) {
    this.board = game.getBoard();
    this.size = board.getSizeBoard();
    redraw();
  }

  /**
   * Draws an empty board with no pieces.
   *
   * <p>Useful for the initial display before a game has started, or to reset
   * the visual state.
   *
   * @param size the number of rows/columns (typically 8, 10, or 12)
   */
  public void drawEmpty(int size) {
    this.board = null;
    this.size = size;
    redraw();
  }

  /**
   * Returns the current board size (number of rows/columns).
   *
   * @return board dimension, e.g. {@code 8}
   */
  public int getSize() {
    return size;
  }

  /**
   * Rebuilds the entire grid from scratch.
   *
   * <p>Clears all children and constraints, then adds coordinate labels and
   * one {@link StackPane} cell per square. Called automatically when
   * {@link #cellSize} changes, or when {@link #refresh} / {@link #drawEmpty}
   * is called.
   */
  private void redraw() {
    this.getChildren().clear();
    this.getColumnConstraints().clear();
    this.getRowConstraints().clear();

    // Clamp to a minimum of 30 px so labels remain readable at small sizes.
    double cell = Math.max(30, cellSize.get());
    double label = cell * LABEL_RATIO;

    addCoordinateLabels(cell, label);

    for (int row = 0; row < size; row++) {
      for (int col = 0; col < size; col++) {
        // Convert grid coordinates to model coordinates.
        // Grid row 0 is the top of the screen (= model row size-1 = letter H).
        int modelRow = (size - 1) - row;
        int modelCol = col;
        this.add(buildCell(row, col, modelRow, modelCol, cell), col + 1, row + 1);
      }
    }
  }

  /**
   * Builds a single board cell as a {@link StackPane}.
   *
   * <p>Dark squares receive a piece (if any), a hover overlay, and a click
   * handler. Light squares are purely decorative.
   *
   * @param row     grid row (0 = top)
   * @param col     grid column (0 = left)
   * @param modelRow model row used to look up piece data and square notation
   * @param modelCol model column used to look up piece data and square notation
   * @param cell     current cell size in pixels
   * @return a fully configured {@link StackPane} representing this square
   */
  private StackPane buildCell(
      int row, int col, int modelRow, int modelCol, double cell) {

    StackPane pane = new StackPane();
    pane.setPrefSize(cell, cell);
    pane.setMinSize(cell, cell);
    pane.setMaxSize(cell, cell);

    boolean isDark = (modelRow + modelCol) % 2 == 0;
    boolean isSelected = (row == selRow && col == selCol);

    // Background rectangle — blue tint when selected, otherwise normal colour.
    // Square colours stay in Java because Rectangle.setFill() is not CSS-controllable.
    Rectangle bg = new Rectangle(cell, cell);
    bg.setFill(isSelected ? SELECTED_SQ : (isDark ? DARK_SQ : LIGHT_SQ));
    pane.getChildren().add(bg);

    if (isDark) {
      // Add a piece if one is present on this square.
      char piece = getPieceChar(modelRow, modelCol);
      if (piece != '_') {
        pane.getChildren().add(buildPiece(piece, cell));
      }

      // Hover overlay — visible only while the mouse is over this cell.
      Rectangle hover = new Rectangle(cell, cell, HOVER_TINT);
      hover.setVisible(false);
      pane.getChildren().add(hover);

      // Capture grid/model coords in effectively-final locals for the lambdas.
      final int fr = row;
      final int fc = col;
      final int mr = modelRow;
      final int mc = modelCol;

      pane.setOnMouseEntered(e -> hover.setVisible(true));
      pane.setOnMouseExited(e -> hover.setVisible(false));
      pane.setOnMouseClicked(e -> handleClick(fr, fc, mr, mc));
    }

    return pane;
  }

  /**
   * Builds the visual representation of a single piece.
   *
   * <p>Each piece is composed of two layered {@link Circle}s:
   * <ol>
   *   <li>A slightly larger, semi-transparent black circle acting as a drop
   *       shadow.</li>
   *   <li>The piece body filled with {@link #WHITE_FILL} or
   *       {@link #BLACK_FILL}.</li>
   * </ol>
   * Kings additionally receive a small golden circle in the centre to indicate
   * their promoted status.
   *
   * @param ch   piece character: {@code 'o'} white pawn, {@code 'O'} white king,
   *             {@code 'x'} black pawn, {@code 'X'} black king
   * @param cell current cell size in pixels, used to scale the piece radius
   * @return a {@link StackPane} containing all circles for this piece
   */
  private StackPane buildPiece(char ch, double cell) {
    double r = cell * 0.36;

    // Drop shadow: slightly oversized circle shifted downward.
    Circle shadow = new Circle(r + 2);
    shadow.setFill(Color.web("#000000", 0.30));
    shadow.setTranslateY(cell * 0.04);

    // Piece body.
    boolean isWhite = (ch == 'o' || ch == 'O');
    Circle body = new Circle(r);
    body.setFill(isWhite ? WHITE_FILL : BLACK_FILL);

    StackPane stack = new StackPane();
    stack.getChildren().addAll(shadow, body);

    // Crown marker for king pieces.
    boolean isKing = (ch == 'O' || ch == 'X');
    if (isKing) {
      Circle crown = new Circle(r * 0.27);
      crown.setFill(CROWN_FILL);
      stack.getChildren().add(crown);
    }

    return stack;
  }

  /**
   * Adds row letters (A–H) and column numbers (1–8) around all four sides of
   * the board.
   *
   * <p>Labels occupy grid column 0 and column {@code size + 1} for rows, and
   * grid row 0 and row {@code size + 1} for columns.
   *
   * @param cell  cell size in pixels
   * @param label label cell size in pixels (a fraction of {@code cell})
   */
  private void addCoordinateLabels(double cell, double label) {
    // Column numbers — top and bottom.
    for (int c = 0; c < size; c++) {
      this.add(coordLabel(String.valueOf(c + 1), cell, label), c + 1, 0);
      this.add(coordLabel(String.valueOf(c + 1), cell, label), c + 1, size + 1);
    }
    // Row letters — left and right.
    for (int row = 0; row < size; row++) {
      char letter = (char) ('A' + (size - 1 - row));
      this.add(coordLabel(String.valueOf(letter), label, label), 0, row + 1);
      this.add(coordLabel(String.valueOf(letter), label, label), size + 1, row + 1);
    }
  }

  /**
   * Creates a single coordinate label centred in a square of the given size.
   *
   * <p>Font size is computed dynamically (proportional to {@code size}) and
   * stays in Java; the remaining style is declared in {@code style.css}
   * via the {@code coord-label} class.
   *
   * @param text the label text (letter or digit)
   * @param size the width and height of the label cell in pixels
   * @return a configured {@link Label}
   */
  private Label coordLabel(String text, double width, double height) {
    Label lbl = new Label(text);
    lbl.setPrefSize(width, height);
    lbl.setMinSize(width, height);
    lbl.setAlignment(Pos.CENTER);
    lbl.setPadding(new Insets(1));
    // Dynamic font size — cannot be expressed in static CSS.
    double fontSize = Math.max(8, size * 0.50);
    lbl.setStyle("-fx-font-size: " + fontSize + "px;");
    // style.css: .coord-label (font-weight, text-fill, font-family)
    lbl.getStyleClass().add("coord-label");
    return lbl;
  }

  /**
   * Handles a click on a dark square.
   *
   * <p>Two-click selection protocol:
   * <ol>
   *   <li>First click on a square containing a piece → mark it as selected and
   *       redraw (highlight).</li>
   *   <li>Second click on any square → compute algebraic notation for both
   *       squares and forward the move to
   *       {@link GameController#executeMove(String, String)}.</li>
   * </ol>
   *
   * @param row     grid row of the clicked cell
   * @param col     grid column of the clicked cell
   * @param modelRow model row of the clicked cell
   * @param modelCol model column of the clicked cell
   */
  private void handleClick(int row, int col, int modelRow, int modelCol) {
    if (board == null) {
      return;
    }

    if (selRow == -1) {
      // No piece selected yet — select this cell only if it contains a piece.
      if (getPieceChar(modelRow, modelCol) != '_') {
        selRow = row;
        selCol = col;
        redraw();
      }
    } else {
      // A piece is already selected — treat this click as the destination.
      String from = toSquare((size - 1) - selRow, selCol);
      String to = toSquare(modelRow, modelCol);
      selRow = -1;
      selCol = -1;
      controller.executeMove(from, to, false);
    }
  }

  /**
   * Returns the piece character at the given model position, querying the
   * bitboards of the current {@link Board}.
   *
   * <p>Character mapping:
   * <ul>
   *   <li>{@code 'O'} — white king</li>
   *   <li>{@code 'o'} — white pawn</li>
   *   <li>{@code 'X'} — black king</li>
   *   <li>{@code 'x'} — black pawn</li>
   *   <li>{@code '_'} — empty or non-playable square</li>
   * </ul>
   *
   * @param modelRow model row (0 = bottom / letter A)
   * @param modelCol model column (0 = left / digit 1)
   * @return the piece character, or {@code '_'} if the square is empty or
   *         non-playable
   */
  private char getPieceChar(int modelRow, int modelCol) {
    if (board == null || (modelRow + modelCol) % 2 != 0) {
      return '_';
    }
    int index = (modelRow * size + modelCol) / 2;
    if (board.isBitWhiteChecker(index)) {
      return 'O';
    }
    if (board.isBitWhitePawn(index)) {
      return 'o';
    }
    if (board.isBitBlackChecker(index)) {
      return 'X';
    }
    if (board.isBitBlackPawn(index)) {
      return 'x';
    }
    return '_';
  }

  /**
   * Converts model coordinates to algebraic square notation.
   *
   * <p>Example: model row 0, model col 0 → {@code "A1"}.
   *
   * @param modelRow model row (0 = letter A)
   * @param modelCol model column (0 = digit 1)
   * @return the algebraic notation string, e.g. {@code "E3"}
   */
  private String toSquare(int modelRow, int modelCol) {
    return "" + (char) ('A' + modelRow) + (modelCol + 1);
  }
}