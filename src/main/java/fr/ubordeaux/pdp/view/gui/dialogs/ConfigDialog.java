package fr.ubordeaux.pdp.view.gui.dialogs;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.view.gui.layout.MenuView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Modal configuration dialog shown before starting a new game.
 *
 * <p>Lets the user configure all game options that would otherwise be set
 * via CLI flags:
 * <ul>
 *   <li>Board size — {@code -s 8|10|12}</li>
 *   <li>Blitz mode + time limit — {@code -b -t TIME}</li>
 *   <li>AI players — {@code -a W|B|A}</li>
 *   <li>Contest mode — {@code -c}</li>
 *   <li>Debug / verbose — {@code -d -v}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   ConfigDialog dialog = new ConfigDialog();
 *   dialog.showAndWait().ifPresent(cfg -> controller.startNewGame(cfg));
 * </pre>
 *
 * <p>Returns an {@link java.util.Optional} containing the built
 * {@link Configuration} when the user clicks "Start Game", or empty when
 * the user cancels.
 */
public class ConfigDialog extends Dialog<Configuration> {

  /** Board size selector: 8, 10 or 12. */
  private final ComboBox<Integer> sizeCombo = new ComboBox<>();

  /** Enables blitz mode. Enabling it also enables the time spinner. */
  private final CheckBox blitzCheck = new CheckBox("Blitz mode");

  /**
   * Time limit per player in minutes (only meaningful when blitz is on).
   * Default: 30 min, range 1–120.
   */
  private final Spinner<Integer> timeSpinner = new Spinner<>(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 30));

  /** Enables AI for the white player. */
  private final CheckBox whiteAiCheck = new CheckBox("White player (AI)");

  /** Enables AI for the black player. */
  private final CheckBox blackAiCheck = new CheckBox("Black player (AI)");

  /** Enables contest mode. */
  private final CheckBox contestCheck = new CheckBox("Contest mode");

  /** Enables verbose output. */
  private final CheckBox verboseCheck = new CheckBox("Verbose");

  /** Enables debug output. */
  private final CheckBox debugCheck = new CheckBox("Debug");

  /** AI thinking time in seconds. Default: Utils.DEFAULT_AI_TIME, range 1–30. */
  private final Spinner<Integer> aiTimeSpinner = new Spinner<>(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));

  /** Manages keyboard shortcuts read from and written to {@code .checkersrc}. */
  private final ShortcutManager shortcutManager;

  /** Called after the user saves changes in {@link ShortcutDialog} so that
  *  {@link MenuView} can re-apply the updated accelerators. */
  private final Runnable onShortcutsChanged;

  /**
   * Builds the configuration dialog.
   *
   * @param shortcutManager   the shortcut manager; must not be {@code null}
   * @param onShortcutsChanged callback invoked after shortcuts are saved,
   *                           used to refresh menu accelerators
   */
  public ConfigDialog(ShortcutManager shortcutManager, Runnable onShortcutsChanged) {
    super();
    this.shortcutManager = shortcutManager;
    this.onShortcutsChanged = onShortcutsChanged;

    setTitle("New Game — Configuration");
    setHeaderText("Configure the game options before starting.");

    ButtonType startButton = new ButtonType(Internationalization.get("dialog.start.game"),
        ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().addAll(startButton, ButtonType.CANCEL);

    Configuration defaults = Configuration.getDefaultConfiguration();
    sizeCombo.getItems().addAll(8, 10, 12);
    sizeCombo.setValue(defaults.getSize());

    blitzCheck.setSelected(defaults.isBlitz());
    timeSpinner.getValueFactory().setValue(defaults.getTime() / 60);
    timeSpinner.setDisable(!defaults.isBlitz());
    timeSpinner.setPrefWidth(80);

    whiteAiCheck.setSelected(defaults.iswhiteAi());
    blackAiCheck.setSelected(defaults.isblackAi());
    contestCheck.setSelected(defaults.isContest());
    verboseCheck.setSelected(defaults.isVerbose());
    debugCheck.setSelected(defaults.isDebug());

    aiTimeSpinner.getValueFactory().setValue((int) defaults.getAiTime());
    aiTimeSpinner.setPrefWidth(80);
    aiTimeSpinner.setDisable(!defaults.iswhiteAi() && !defaults.isblackAi());

    // Enable / disable time spinner based on blitz checkbox.
    blitzCheck.selectedProperty().addListener(
        (obs, oldV, newV) -> timeSpinner.setDisable(!newV));

    // Enable / disable spinner if at least one or the other au moins is checked.
    whiteAiCheck.selectedProperty()
        .addListener((obs, oldV, newV) -> aiTimeSpinner.setDisable(!newV
            && !blackAiCheck.isSelected()));

    blackAiCheck.selectedProperty()
        .addListener((obs, oldV, newV) -> aiTimeSpinner.setDisable(!newV
            && !whiteAiCheck.isSelected()));

    getDialogPane().setContent(buildContent());
    getDialogPane().getStyleClass().add("config-dialog");

    setResultConverter(buttonType -> {
      if (buttonType.getButtonData() == ButtonData.OK_DONE) {
        return buildConfiguration();
      }
      return null;
    });
  }

  /**
   * Assembles the full dialog content as a {@link VBox} containing labelled
   * sections separated by {@link Separator}s.
   *
   * @return the root content node
   */
  private VBox buildContent() {
    VBox root = new VBox(16);
    root.setPadding(new Insets(20));
    root.setPrefWidth(360);

    root.getChildren().addAll(
        buildSection("Board"),
        buildBoardGrid(),
        new Separator(),
        buildSection("Blitz"),
        buildBlitzGrid(),
        new Separator(),
        buildSection("Players"),
        buildPlayersGrid(),
        new Separator(),
        buildSection("Advanced"),
        buildAdvancedGrid());
    Button shortcutsBtn = new Button(Internationalization.get("dialog.keyboard.shortcuts"));
    shortcutsBtn.setOnAction(e -> {
      new ShortcutDialog(shortcutManager).showAndWait();
      onShortcutsChanged.run();
    });
    root.getChildren().add(shortcutsBtn);
    return root;
  }

  /**
   * Creates a bold section-title label.
   *
   * @param text the section title
   * @return the styled {@link Label}
   */
  private Label buildSection(String text) {
    Label lbl = new Label(text);
    lbl.getStyleClass().add("config-section-title");
    lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #8E8E93;");
    return lbl;
  }

  /**
   * Builds the board-size row.
   *
   * @return a {@link GridPane} with the size combo
   */
  private GridPane buildBoardGrid() {
    GridPane grid = baseGrid();
    grid.add(new Label("Board size:"), 0, 0);
    grid.add(sizeCombo, 1, 0);
    return grid;
  }

  /**
   * Builds the blitz-mode row (checkbox + time spinner).
   *
   * @return a {@link GridPane} with blitz controls
   */
  private GridPane buildBlitzGrid() {
    GridPane grid = baseGrid();
    grid.add(blitzCheck, 0, 0);

    VBox timeBox = new VBox(4,
        new Label("Time per player (min):"),
        timeSpinner);
    timeBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(timeBox, 0, 1);

    return grid;
  }

  /**
   * Builds the AI player rows.
   *
   * @return a {@link GridPane} with the two AI checkboxes
   */
  private GridPane buildPlayersGrid() {
    GridPane grid = baseGrid();
    grid.add(whiteAiCheck, 0, 0);
    grid.add(blackAiCheck, 0, 1);

    VBox aiTimeBox = new VBox(4,
        new Label("AI thinking time (sec):"),
        aiTimeSpinner);
    aiTimeBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(aiTimeBox, 0, 2);

    return grid;
  }

  /**
   * Builds the advanced-options rows (contest, verbose, debug).
   *
   * @return a {@link GridPane} with the three checkboxes
   */
  private GridPane buildAdvancedGrid() {
    GridPane grid = baseGrid();
    grid.add(contestCheck, 0, 0);
    grid.add(verboseCheck, 0, 1);
    grid.add(debugCheck, 0, 2);
    return grid;
  }

  /**
   * Returns a pre-configured {@link GridPane} with standard gaps and padding.
   *
   * @return the base grid
   */
  private GridPane baseGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(16);
    grid.setVgap(10);
    grid.setPadding(new Insets(4, 0, 4, 8));
    return grid;
  }

  /**
   * Reads all widget values and constructs the resulting {@link Configuration}.
   *
   * @return the configured {@link Configuration} instance
   */
  private Configuration buildConfiguration() {
    boolean blitz = blitzCheck.isSelected();
    int timeSec = blitz ? timeSpinner.getValue() : 30;
    boolean contest = contestCheck.isSelected();
    int size = sizeCombo.getValue();
    boolean verbose = verboseCheck.isSelected();
    boolean debug = debugCheck.isSelected();
    boolean whiteAi = whiteAiCheck.isSelected();
    boolean blackAi = blackAiCheck.isSelected();
    int aiTime = aiTimeSpinner.getValue();

    return new Configuration(blitz, timeSec, contest, size,
        verbose, debug, whiteAi, blackAi, aiTime,
        Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);
  }
}