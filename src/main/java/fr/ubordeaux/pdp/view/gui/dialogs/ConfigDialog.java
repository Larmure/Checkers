package fr.ubordeaux.pdp.view.gui.dialogs;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.view.gui.layout.MenuView;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

  /** Display label for Minimax AI mode. */
  private static final String MINIMAX = "Minimax";

  /** Board size selector: 8, 10 or 12. */
  private final ComboBox<Integer> sizeCombo = new ComboBox<>();

  /** White AI type selector. */
  private final ComboBox<String> whiteAiModeCombo = new ComboBox<>();

  /** Black AI type selector. */
  private final ComboBox<String> blackAiModeCombo = new ComboBox<>();

  /** MCTS type selector. */
  private final ComboBox<String> mctsCombo = new ComboBox<>();

  /** Enables blitz mode. Enabling it also enables the time spinner. */
  private final CheckBox blitzCheck = new CheckBox("Blitz mode");

  /**
   * Time limit per player in minutes (only meaningful when blitz is on).
   * range 1–120.
   */
  private final Spinner<Integer> timeSpinner = new Spinner<>(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120));

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

  /** AI thinking time in seconds. range 1–30. */
  private final Spinner<Integer> aiTimeSpinner = new Spinner<>(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30));

  /** AI thinking depth. range 1–15. */
  private final Spinner<Integer> aiDepthSpinner = new Spinner<>(
      new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15));

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
   * @param initialConfig    optional initial configuration to populate the fields;
   *                         if {@code null}, default configuration values are used
   */
  public ConfigDialog(ShortcutManager shortcutManager, Runnable onShortcutsChanged,
      Configuration initialConfig) {
    super();
    this.shortcutManager = shortcutManager;
    this.onShortcutsChanged = onShortcutsChanged;

    setTitle("New Game — Configuration");
    setHeaderText("Configure the game options before starting.");

    ButtonType startButton = new ButtonType(Internationalization.get("dialog.start.game"),
        ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().addAll(startButton, ButtonType.CANCEL);

    Configuration defaults = initialConfig != null
        ? initialConfig
        : Configuration.getDefaultConfiguration();
    sizeCombo.getItems().addAll(8, 10, 12);
    sizeCombo.setValue(defaults.getSize());

    blitzCheck.setSelected(defaults.isBlitz());
    timeSpinner.getValueFactory().setValue(defaults.getTime());
    timeSpinner.setDisable(!defaults.isBlitz());
    timeSpinner.setPrefWidth(80);

    whiteAiCheck.setSelected(defaults.iswhiteAi());
    blackAiCheck.setSelected(defaults.isblackAi());
    whiteAiModeCombo.getItems().addAll("Minimax", "Alpha-Beta", "MCTS");
    whiteAiModeCombo.setValue(toDisplayAiMode(defaults.getWhiteAiMode()));
    blackAiModeCombo.getItems().addAll("Minimax", "Alpha-Beta", "MCTS");
    blackAiModeCombo.setValue(toDisplayAiMode(defaults.getBlackAiMode()));
    mctsCombo.getItems().addAll("UCT", "ML");
    mctsCombo.setValue(defaults.getSelectionMode().name());

    contestCheck.setSelected(defaults.isContest());
    verboseCheck.setSelected(defaults.isVerbose());
    debugCheck.setSelected(defaults.isDebug());

    aiTimeSpinner.getValueFactory().setValue((int) (defaults.getAiTime() / 1000));
    aiTimeSpinner.setPrefWidth(80);

    aiDepthSpinner.getValueFactory().setValue((int) defaults.getAiDepth());
    aiDepthSpinner.setPrefWidth(80);
    updateAiControlsState();

    // Enable / disable time spinner based on blitz checkbox.
    blitzCheck.selectedProperty().addListener(
        (obs, oldV, newV) -> timeSpinner.setDisable(!newV));

    // Enable / disable the per-color selectors based on the matching checkbox.
    whiteAiCheck.selectedProperty()
        .addListener((obs, oldV, newV) -> {
          updateAiControlsState();
        });

    blackAiCheck.selectedProperty()
        .addListener((obs, oldV, newV) -> {
          updateAiControlsState();
        });

    whiteAiModeCombo.valueProperty().addListener((obs, oldV, newV) -> updateAiControlsState());
    blackAiModeCombo.valueProperty().addListener((obs, oldV, newV) -> updateAiControlsState());

    ScrollPane scrollPane = new ScrollPane(buildContent());
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(500);
    scrollPane.setStyle("-fx-control-inner-background: #f5f5f5;");
    getDialogPane().setContent(scrollPane);
    getDialogPane().getStyleClass().add("config-dialog");
    getDialogPane().setPrefHeight(600);
    getDialogPane().setMaxHeight(600);

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
    root.setPrefWidth(380);
    root.setStyle("-fx-padding: 20px; -fx-spacing: 16px;");

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

    VBox whiteAiModeBox = new VBox(4,
        new Label("White AI mode:"),
        whiteAiModeCombo);
    whiteAiModeBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(whiteAiModeBox, 0, 2);

    VBox blackAiModeBox = new VBox(4,
        new Label("Black AI mode:"),
        blackAiModeCombo);
    blackAiModeBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(blackAiModeBox, 0, 3);

    VBox aiTimeBox = new VBox(4,
        new Label("AI thinking time (sec):"),
        aiTimeSpinner);
    aiTimeBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(aiTimeBox, 0, 4);

    VBox aiDepthBox = new VBox(4,
        new Label("AI thinking depth:"),
        aiDepthSpinner);
    aiDepthBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(aiDepthBox, 0, 5);

    VBox mctsBox = new VBox(4,
        new Label("MCTS selection mode:"),
        mctsCombo);
    mctsBox.setAlignment(Pos.CENTER_LEFT);
    grid.add(mctsBox, 0, 6);

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

  /** Updates enabled state of AI-related controls from player selection and AI mode. */
  private void updateAiControlsState() {
    boolean whiteEnabled = whiteAiCheck.isSelected();
    boolean blackEnabled = blackAiCheck.isSelected();
    boolean aiEnabled = whiteEnabled || blackEnabled;

    aiTimeSpinner.setDisable(!aiEnabled);

    whiteAiModeCombo.setDisable(!whiteEnabled);
    blackAiModeCombo.setDisable(!blackEnabled);

    String whiteMode = whiteAiModeCombo.getValue();
    String blackMode = blackAiModeCombo.getValue();
    boolean whiteDepthSupported = MINIMAX.equals(whiteMode) || "Alpha-Beta".equals(whiteMode);
    boolean blackDepthSupported = MINIMAX.equals(blackMode) || "Alpha-Beta".equals(blackMode);
    aiDepthSpinner.setDisable(!aiEnabled || (!whiteDepthSupported && !blackDepthSupported));

    boolean whiteMctsSelected = "MCTS".equals(whiteMode);
    boolean blackMctsSelected = "MCTS".equals(blackMode);
    mctsCombo.setDisable(!aiEnabled || (!whiteMctsSelected && !blackMctsSelected));
  }

  /**
   * Reads all widget values and constructs the resulting {@link Configuration}.
   *
   * @return the configured {@link Configuration} instance
   */
  private Configuration buildConfiguration() {
    boolean blitz = blitzCheck.isSelected();
    Integer timeValue = timeSpinner.getValue();
    int timeSec = blitz ? (timeValue != null ? timeValue : Utils.DEFAULT_TIME) : Utils.DEFAULT_TIME;
    boolean contest = contestCheck.isSelected();
    Integer sizeValue = sizeCombo.getValue();
    int size = sizeValue != null ? sizeValue : Utils.DEFAULT_BOARD_SIZE;
    boolean verbose = verboseCheck.isSelected();
    boolean debug = debugCheck.isSelected();
    boolean whiteAi = whiteAiCheck.isSelected();
    boolean blackAi = blackAiCheck.isSelected();
    Integer aiTimeValue = aiTimeSpinner.getValue();
    int aiTime = aiTimeValue != null ? aiTimeValue * 1000 : (int) (Ai.DEFAULT_MAX_TIME_MS);

    String whiteAiMode = normalizeAiMode(whiteAiModeCombo.getValue());
    String blackAiMode = normalizeAiMode(blackAiModeCombo.getValue());
    int aiDepth = normalizeAiDepth(aiDepthSpinner.getValue());
    SelectionMode mctsMode = normalizeSelectionMode(mctsCombo.getValue());

    return new Configuration(blitz, timeSec, contest, size,
        verbose, debug, whiteAi, blackAi, aiTime,
        whiteAiMode, blackAiMode, aiDepth, mctsMode);
  }

  private static String toDisplayAiMode(String aiMode) {
    if (aiMode == null) {
      return MINIMAX;
    }
    return switch (aiMode.toLowerCase(Locale.ROOT)) {
      case "alphabeta" -> "Alpha-Beta";
      case "mcts" -> "MCTS";
      case "minimax" -> MINIMAX;
      default -> MINIMAX;
    };
  }

  private static String normalizeAiMode(String aiMode) {
    if (aiMode == null) {
      return Utils.DEFAULT_AI_MODE;
    }
    return switch (aiMode.trim().toLowerCase(Locale.ROOT)) {
      case "alpha-beta", "alphabeta" -> "alphabeta";
      case "mcts" -> "mcts";
      case "minimax" -> "minimax";
      default -> Utils.DEFAULT_AI_MODE;
    };
  }

  private static int normalizeAiDepth(Integer aiDepth) {
    if (aiDepth == null || aiDepth <= 0) {
      return Ai.DEFAULT_DEPTH;
    }
    return aiDepth;
  }

  private static SelectionMode normalizeSelectionMode(String mode) {
    if (mode == null || mode.isBlank()) {
      return Mcts.DEFAULT_SELECTION_MODE;
    }
    try {
      return SelectionMode.valueOf(mode.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return Mcts.DEFAULT_SELECTION_MODE;
    }
  }
}