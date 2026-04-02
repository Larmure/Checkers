package fr.ubordeaux.pdp.view.gui.layout;

import fr.ubordeaux.pdp.ConfigManager;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.server.ClientMode;
import fr.ubordeaux.pdp.server.ClientSession;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import fr.ubordeaux.pdp.view.gui.dialogs.ShortcutManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
 * <h2>Initialisation sequence</h2>
 * <ol>
 * <li>Instantiate {@code MainView}.</li>
 * <li>Create the {@link Scene} and call {@link #bindToScene(Scene)} so the
 * board scales with the window.</li>
 * <li>Call {@code stage.show()}.</li>
 * <li>Call {@link #passStageToMenu(Stage)} so {@link MenuView} can open
 * modal dialogs.</li>
 * </ol>
 *
 * <p>Visual styles are defined in {@code style.css} (classes:
 * {@code root-pane}, {@code toolbar}, {@code toolbar-button},
 * {@code toolbar-button:hover}, {@code turn-label}).
 */
public class MainView extends BorderPane {

  /** Controller forwarded to child components that need to send commands. */
  private final GameController controller;

  /** Configuration manager used to initialise the {@link ShortcutManager}. */
  private final ConfigManager configManager;

  /** Optional configuration provided from CLI startup flags. */
  private final Configuration cliConfig;

  /** Top menu bar (File / Game menus + keyboard shortcuts). */
  private MenuView menuView;

  /** Central play area containing the board and the log panel. */
  private PlayView playView;

  /** Flag indicating whether the GUI is running in server mode. */
  private final boolean serverMode;

  private final ClientSession session;

  /** Toolbar button: undo. */
  private Button undoButton;

  /** Toolbar button: redo. */
  private Button redoButton;

  /** Toolbar button: pause. */
  private Button pauseButton;

  /** Toolbar button: hint. */
  private Button hintButton;

  /**
   * Toolbar label indicating whose turn it is.
   * Updated by {@link #update(GameCheckers)}.
   */
  private Label turnLabel;

  /**
   * Creates the root layout and assembles all child components.
   *
   * @param controller    the game controller; must not be {@code null}
   * @param configManager the configuration manager used to load and persist
   *                      keyboard shortcuts; must not be {@code null}
   * @param cliConfig     optional CLI configuration used to prefill
   *                      configuration dialogs; may be {@code null}  // style.css : .root-pane
   */
  public MainView(GameController controller, ConfigManager configManager,
                  Configuration cliConfig, boolean serverMode, ClientSession session) {
    this.controller = controller;
    this.configManager = configManager;
    this.cliConfig = cliConfig;
    this.serverMode = serverMode;
    this.session = session;
    buildLayout();
    // style.css : .root-pane
    this.getStyleClass().add("root-pane");
  }

  /**
   * Assembles the three regions of the {@link BorderPane}: menu bar (top),
   * play area (centre), and action toolbar (bottom).
   */
  private void buildLayout() {
    ShortcutManager shortcutManager = new ShortcutManager(configManager);
    if (!serverMode) {
      menuView = new MenuView(controller, shortcutManager, cliConfig);
      this.setTop(menuView);
    } else {
      System.out.println("Mode réseau détecté.");
    }

    playView = new PlayView(controller);
    this.setCenter(playView);

    this.setBottom(buildToolbar());
    refreshToolbarState();
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
    toolbar.getStyleClass().add("toolbar");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    turnLabel = new Label(Internationalization.get("toolbar.turn") + "BLACK");
    turnLabel.getStyleClass().add("turn-label");

    undoButton = toolbarButton(
        Internationalization.get("toolbar.undo"),
        () -> controller.executeCommand("undo", new String[] {"1"}));

    redoButton = toolbarButton(
        Internationalization.get("toolbar.redo"),
        () -> controller.executeCommand("redo", new String[] {"1"}));

    pauseButton = toolbarButton(
        Internationalization.get("toolbar.pause"),
        () -> {
          if (controller.getGame() != null
              && controller.getGame().getState() == State.IN_GAME) {
            controller.executeCommand("pause", new String[0]);

            Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION);
            pauseAlert.setTitle(Internationalization.get("dialog.pause_title"));
            pauseAlert.setHeaderText(Internationalization.get("dialog.pause_header"));

            ButtonType resumeButton = new ButtonType(
                Internationalization.get("dialog.pause_resume"),
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            pauseAlert.getButtonTypes().setAll(resumeButton);
            pauseAlert.showAndWait();

            controller.executeCommand("continue", new String[0]);
          }
        });

    hintButton = toolbarButton(
        Internationalization.get("toolbar.hint"),
        () -> controller.executeCommand("hint", new String[0]));

    toolbar.getChildren().addAll(
        undoButton,
        redoButton,
        pauseButton,
        hintButton,
        spacer,
        turnLabel);
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
    // style.css : .toolbar-button / .toolbar-button:hover
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
    if (menuView != null) {
      menuView.setStage(stage);
    }
  }

  /**
   * Opens the Save dialog from MenuView.
   * Called by {@link GraphicalUserInterface#requestQuit()} when the user
   * chooses to save before quitting.
   */
  public void openSaveDialog() {
    if (menuView != null) {
      menuView.openSaveDialog();
    }
  }

  /**
   * Passes the {@link GraphicalUserInterface} reference to {@link MenuView}
   * so the Quit menu item can delegate to
   * {@link GraphicalUserInterface#requestQuit()} instead of a raw exit.
   *
   * @param gui the GUI instance; must not be {@code null}
   */
  public void passGuiToMenu(GraphicalUserInterface gui) {
    if (menuView != null) {
      menuView.setGui(gui);
    }
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
   * <p>Delegates board and log 0tes to {@link PlayView}, then refreshes the
   * toolbar turn indicator.
   *
   * <p>Must be called on the JavaFX Application Thread (e.g. inside
   * {@code Platform.runLater}).
   *
   * @param game the current game state; must not be {@code null}
   */
  public void update(GameCheckers game) {
    refreshToolbarState();
    if (game != null) {
      playView.update(game);

      String name = game.getCurrentPlayer().getName();
      turnLabel.setText(Internationalization.get("toolbar.turn") + name.toUpperCase());
    }
  }

  /**
   * Opens the configuration dialog managed by {@link MenuView}.
   *
   * <p>This is typically called by the GUI after the primary stage is shown,
   * so the dialog has a valid window owner and appears modally.
   */
  public void openConfigDialog() {
    if (menuView != null) {
      menuView.openConfigDialog();
    }
  }

  private void refreshToolbarState() {
    boolean localMode = session == null || session.getMode() == ClientMode.LOCAL;

    if (undoButton != null) {
      undoButton.setDisable(!localMode);
    }
    if (redoButton != null) {
      redoButton.setDisable(!localMode);
    }
    if (pauseButton != null) {
      pauseButton.setDisable(!localMode);
    }
    if (hintButton != null) {
      hintButton.setDisable(!localMode);
    }
  }
}