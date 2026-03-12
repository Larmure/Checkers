package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.model.core.GameCheckers;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Side panel displayed to the right of the board inside {@link PlayView}.
 *
 * <p>The panel is divided into three sections:
 * <ol>
 *   <li>A player card for the BLACK player, showing a colour dot and a
 *       live timer.</li>
 *   <li>A player card for the WHITE player, showing a colour dot and a
 *       live timer.</li>
 *   <li>A scrollable move-history log that auto-scrolls to the latest
 *       entry.</li>
 * </ol>
 *
 * <p>Call {@link #update(GameCheckers)} on every model notification to
 * refresh timers and move history. This method must be called on the
 * JavaFX Application Thread.
 */
public class LogView extends VBox {

  /** Fixed width of this panel in pixels. Exposed so {@link PlayView} can
   *  subtract it when computing the available width for the board. */
  public static final double WIDTH = 250;

  /** Timer label for the black player. Updated by {@link #update}. */
  private Label blackTimeLabel;

  /** Timer label for the white player. Updated by {@link #update}. */
  private Label whiteTimeLabel;

  /** Read-only text area displaying the full move history. */
  private TextArea logArea;

  /**
   * Creates the log panel and builds all child nodes.
   */
  public LogView() {
    super(16);
    this.setPrefWidth(WIDTH);
    this.setMinWidth(WIDTH);
    this.setMaxWidth(WIDTH);
    this.setPadding(new Insets(16));
    this.setStyle("-fx-background-color: #1C1C1E;");

    this.getChildren().addAll(
        buildPlayerCard("BLACK", true),
        buildPlayerCard("WHITE", false),
        buildDivider(),
        buildLogPanel()
    );
  }

  /**
   * Refreshes timers and move history from the current game state.
   *
   * <p>Timer display is only meaningful in blitz mode; in classic mode the
   * underlying {@code getPlayTime()} value stays at zero and shows
   * {@code "00:00"}.
   *
   * <p>Both blocks are wrapped in a {@code try/catch} so that a missing
   * feature in the model (e.g. timers not implemented) does not crash the UI.
   *
   * @param game the current game state; must not be {@code null}
   */
  public void update(GameCheckers game) {
    // Refresh player timers.
    try {
      whiteTimeLabel.setText(formatTime(game.getWhitePlayer().getPlayTime()));
      blackTimeLabel.setText(formatTime(game.getBlackPlayer().getPlayTime()));
    } catch (Exception ignored) {
      // Timer feature may not be available in all game modes.
    }

    // Refresh move history and auto-scroll to the bottom.
    try {
      logArea.setText(game.getHistory().historyString());
      logArea.setScrollTop(Double.MAX_VALUE);
    } catch (Exception ignored) {
      // History may be empty at game start.
    }
  }

  /**
   * Builds a player card containing a colour dot, a player name label, and a
   * timer label.
   *
   * @param name    display name of the player (e.g. {@code "BLACK"})
   * @param isBlack {@code true} for the black player, {@code false} for white
   * @return the constructed card as a {@link VBox}
   */
  private VBox buildPlayerCard(String name, boolean isBlack) {
    VBox card = new VBox(6);
    card.setPadding(new Insets(10, 12, 10, 12));
    card.setStyle(
        "-fx-background-color: #6d4751; "
            + "-fx-background-radius: 8; "
            + "-fx-border-color: #3A3A3C; "
            + "-fx-border-radius: 8; "
            + "-fx-border-width: 1;");

    // Colour dot — a plain JavaFX circle avoids emoji rendering issues on Linux.
    Circle dot = new Circle(7);
    dot.setFill(isBlack ? Color.web("#1C1C2E") : Color.WHITE);

    Label nameLabel = new Label(name);
    nameLabel.setStyle(
        "-fx-text-fill: #EBEBF5; -fx-font-size: 13px; -fx-font-weight: bold;");

    HBox header = new HBox(8, dot, nameLabel);
    header.setAlignment(Pos.CENTER_LEFT);

    Label timeLabel = new Label("--:--");
    timeLabel.setStyle(
        "-fx-text-fill: #EBEBF5; -fx-font-size: 12px; "
            + "-fx-font-family: 'Courier New'; -fx-padding: 0 0 0 22;");

    // Store a reference so update() can setText() on it later.
    if (isBlack) {
      blackTimeLabel = timeLabel;
    } else {
      whiteTimeLabel = timeLabel;
    }

    card.getChildren().addAll(header, timeLabel);
    return card;
  }

  /**
   * Builds the move-history section containing a title label and a scrollable
   * {@link TextArea}.
   *
   * @return a {@link VBox} that grows vertically to fill remaining space
   */
  private VBox buildLogPanel() {
    VBox box = new VBox(6);
    VBox.setVgrow(box, Priority.ALWAYS);

    Label title = new Label("Move History");
    title.setStyle(
        "-fx-text-fill: #8E8E93; -fx-font-size: 11px; -fx-font-weight: bold;");

    logArea = new TextArea();
    logArea.setEditable(false);
    logArea.setWrapText(true);
    logArea.setPromptText("No moves yet...");
    VBox.setVgrow(logArea, Priority.ALWAYS);
    logArea.setStyle(
        "-fx-control-inner-background: #2e2c2d; "
            + "-fx-text-fill: #EBEBF5; "
            + "-fx-font-family: 'Courier New'; "
            + "-fx-font-size: 12px; "
            + "-fx-border-color: #3A3A3C; "
            + "-fx-border-radius: 6; "
            + "-fx-background-radius: 6;");

    box.getChildren().addAll(title, logArea);
    return box;
  }

  /**
   * Builds a 1-pixel horizontal divider between the player cards and the
   * history log.
   *
   * @return an empty {@link Region} styled as a thin line
   */
  private Region buildDivider() {
    Region sep = new Region();
    sep.setPrefHeight(1);
    sep.setStyle("-fx-background-color: #3A3A3C;");
    return sep;
  }

  /**
   * Formats a duration expressed in seconds as {@code mm:ss}.
   *
   * @param totalSeconds total elapsed seconds (non-negative)
   * @return a string in the form {@code "02:35"}, never {@code null}
   */
  private String formatTime(int totalSeconds) {
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
  }
}