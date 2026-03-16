package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Root layout of the graphical user interface.
 *
 * <p>{@code MainView} owns only the toolbar. All game-state rendering is
 * delegated to {@link PlayView}, keeping this class lightweight.
 *
 * <h3>Initialisation sequence</h3>
 * <ol>
 *   <li>Instantiate {@code MainView}.</li>
 *   <li>Create the {@link Scene} and call {@link #bindToScene(Scene)} so the
 *       board scales with the window.</li>
 *   <li>Call {@code stage.show()}.</li>
 *   <li>Call {@link #passStageToMenu(Stage)} so {@link MenuView} can open
 *       modal dialogs.</li>
 * </ol>
 *
 * <p>Visual styles are defined in {@code style.css} (classes:
 * {@code root-pane}, {@code toolbar}, {@code toolbar-button},
 * {@code toolbar-button:hover}, {@code turn-label}).
 */
public class MainView extends BorderPane {

  /** Controller forwarded to child components that need to send commands. */
  private final GameController controller;

  /** Top menu bar (File / Game menus + keyboard shortcuts). */
  private MenuView menuView;

  /** Central play area containing the board and the log panel. */
  private PlayView playView;

  /**
   * Toolbar label indicating whose turn it is.
   * Updated by {@link #update(GameCheckers)}.
   */
  private Label turnLabel;

  /**
   * Creates the root layout and assembles all child components.
   *
   * @param controller the game controller; must not be {@code null}
   */
  public MainView(GameController controller) {
    this.controller = controller;
    buildLayout();
    // style.css : .root-pane
    this.getStyleClass().add("root-pane");
  }

  /**
   * Assembles the three regions of the {@link BorderPane}: menu bar (top),
   * play area (centre), and action toolbar (bottom).
   */
  private void buildLayout() {
    menuView = new MenuView(controller);
    this.setTop(menuView);

    playView = new PlayView(controller);
    this.setCenter(playView);

    this.setBottom(buildToolbar());
  }

  /**
   * Builds the bottom action toolbar.
   *
   * <p>The toolbar contains four action buttons (Undo, Redo, Pause, Hint) on
   * the left, and a turn indicator label on the right. A growing spacer
   * separates the two groups.
   *
   * @return the configured {@link HBox} toolbar
   */
  private HBox buildToolbar() {
    HBox toolbar = new HBox(10);
    toolbar.setPadding(new Insets(10, 20, 10, 20));
    toolbar.setAlignment(Pos.CENTER_LEFT);
    // style.css : .toolbar
    toolbar.getStyleClass().add("toolbar");

    // Spacer pushes the turn label to the right edge.
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    turnLabel = new Label("Turn: BLACK");
    // style.css : .turn-label
    turnLabel.getStyleClass().add("turn-label");

    Button undoBtn = toolbarButton("Undo", () -> controller.executeCommand("undo",
        new String[] { "1" }));
    Button redoBtn = toolbarButton("Redo", () -> controller.executeCommand("redo",
        new String[] { "1" }));
    Button pauseBtn = toolbarButton("Pause", () -> controller.executeCommand("pause",
        new String[0]));
    Button hintBtn = toolbarButton("Hint", () -> controller.executeCommand("hint",
        new String[0]));

    toolbar.getChildren().addAll(undoBtn, redoBtn, pauseBtn, hintBtn, spacer, turnLabel);
    return toolbar;
  }

  /**
   * Creates a styled toolbar button.
   *
   * <p>Hover effect is handled by the {@code .toolbar-button:hover} rule in
   * {@code style.css}, so no {@code setOnMouseEntered}/{@code setOnMouseExited}
   * handlers are needed here.
   *
   * @param text   button label
   * @param action action executed when the button is clicked
   * @return the configured {@link Button}
   */
  private Button toolbarButton(String text, Runnable action) {
    Button btn = new Button(text);
    btn.setOnAction(e -> action.run());
    // style.css : .toolbar-button  /  .toolbar-button:hover
    btn.getStyleClass().add("toolbar-button");
    return btn;
  }

  /**
   * Passes the primary {@link Stage} reference to {@link MenuView} so that
   * its {@code TextInputDialog} and {@code Alert} instances are owned by the
   * main window (making them modal).
   *
   * <p>Must be called after {@code stage.show()}.
   *
   * @param stage the application's primary stage; must not be {@code null}
   */
  public void passStageToMenu(Stage stage) {
    menuView.setStage(stage);
  }

  /**
   * Opens the Save dialog from MenuView.
   * Called by {@link GraphicalUserInterface#requestQuit()} when the user
   * chooses to save before quitting.
   */
  public void openSaveDialog() {
    menuView.openSaveDialog();
  }

  /**
   * Passes the {@link GraphicalUserInterface} reference to {@link MenuView}
   * so the Quit menu item can delegate to
   * {@link GraphicalUserInterface#requestQuit()} instead of a raw exit.
   *
   * @param gui the GUI instance; must not be {@code null}
   */
  public void passGuiToMenu(GraphicalUserInterface gui) {
    menuView.setGui(gui);
  }

  /**
   * Delegates responsive cell-size binding to {@link PlayView}.
   *
   * <p>Must be called after the {@link Scene} is created so that the board's
   * cell size tracks window resizing.
   *
   * @param scene the application's primary scene; must not be {@code null}
   */
  public void bindToScene(Scene scene) {
    playView.bindToScene(scene);
  }

  /**
   * Refreshes the entire view from the current game state.
   *
   * <p>Delegates board and log updates to {@link PlayView}, then refreshes the
   * toolbar turn indicator.
   *
   * <p>Must be called on the JavaFX Application Thread (e.g. inside
   * {@code Platform.runLater}).
   *
   * @param game the current game state; must not be {@code null}
   */
  public void update(GameCheckers game) {
    playView.update(game);

    String name = game.getCurrentPlayer().getName();
    turnLabel.setText("Turn: " + name.toUpperCase());
  }
}