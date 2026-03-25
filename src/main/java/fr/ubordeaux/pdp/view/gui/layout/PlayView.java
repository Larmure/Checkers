package fr.ubordeaux.pdp.view.gui.layout;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import fr.ubordeaux.pdp.view.gui.board.BoardView;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/**
 * Central game area displayed in the middle of {@link MainView}.
 *
 * <h2>Responsive sizing</h2>
 * Call {@link #bindToScene(Scene)} once after the {@link Scene} has been
 * created. This sets up a {@link DoubleBinding} that keeps the board's cell
 * size proportional to the available window area, recomputing live whenever
 * the user resizes the window.
 *
 * <h2>Update flow</h2>
 * {@link #update(GameCheckers)} is the single entry point for model changes.
 * It delegates to both {@link BoardView#refresh} and {@link LogView#update}.
 *
 * <p>Visual styles are defined in {@code style.css} (class:
 * {@code board-wrapper}).
 */
public class PlayView extends HBox {

  /**
   * Estimated height of the menu bar in pixels.
   * Subtracted from the scene height when computing available board space.
   */
  private static final double MENUBAR_H = 28;

  /**
   * Estimated height of the bottom toolbar in pixels.
   * Subtracted from the scene height when computing available board space.
   */
  private static final double TOOLBAR_H = 46;

  /**
   * Total vertical and horizontal padding around the board wrapper in pixels.
   * Accounts for the {@link StackPane} padding (24 px × 2 sides = 48 px) plus
   * a small margin.
   */
  private static final double PADDING = 64;

  /** The interactive board grid. */
  private final BoardView boardView;

  /** The side panel showing player info and move history. */
  private final LogView logView;

  /**
   * Creates the play area with an initial empty 8×8 board.
   *
   * @param controller the game controller passed down to {@link BoardView}
   */
  public PlayView(GameController controller) {
    // Spacing 0: LogView provides its own distinct background colour.
    super(0);

    boardView = new BoardView(controller);
    boardView.drawEmpty(8);

    logView = new LogView();

    // The board wrapper grows to fill all horizontal space left by LogView.
    StackPane boardWrapper = new StackPane(boardView);
    boardWrapper.setPadding(new Insets(24));
    boardWrapper.setAlignment(Pos.CENTER);
    // style.css : .board-wrapper
    boardWrapper.getStyleClass().add("board-wrapper");
    HBox.setHgrow(boardWrapper, Priority.ALWAYS);

    this.getChildren().addAll(boardWrapper, logView);
  }

  /**
   * Binds the board's cell size to the scene dimensions.
   *
   * <p>The cell size is computed as:
   * <pre>
   *   cellSize = min(availableWidth, availableHeight) / boardSize - 2
   * </pre>
   * where {@code availableWidth = sceneWidth - LogView.WIDTH - PADDING} and
   * {@code availableHeight = sceneHeight - MENUBAR_H - TOOLBAR_H - PADDING}.
   *
   * <p>Must be called after the {@link Scene} is created (typically from
   * {@link GraphicalUserInterface#start()}).
   *
   * @param scene the application's primary scene; must not be {@code null}
   */
  public void bindToScene(Scene scene) {
    int boardSize = boardView.getSize();

    DoubleBinding availW = scene.widthProperty()
        .subtract(LogView.WIDTH + PADDING);

    DoubleBinding availH = scene.heightProperty()
        .subtract(MENUBAR_H + TOOLBAR_H + PADDING);

    // Take the smaller dimension so the board always fits both axes.
    DoubleBinding cellSize = (DoubleBinding) Bindings.min(availW, availH)
        .divide(boardSize)
        .subtract(2); // 2 px inter-cell gap

    boardView.bindCellSize(cellSize);
  }

  /**
   * Refreshes the board and the log panel from the current game state.
   *
   * <p>Must be called on the JavaFX Application Thread (e.g. inside
   * {@code Platform.runLater}).
   *
   * @param game the current game state; must not be {@code null}
   */
  public void update(GameCheckers game) {
    boardView.refresh(game);
    logView.update(game);
  }

  /**
   * Returns the {@link BoardView} instance managed by this play area.
   *
   * @return the board view; never {@code null}
   */
  public BoardView getBoardView() {
    return boardView;
  }

  /**
   * Returns the {@link LogView} instance managed by this play area.
   *
   * @return the log view; never {@code null}
   */
  public LogView getLogView() {
    return logView;
  }
}