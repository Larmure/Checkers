package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.controller.GameController;
import java.io.File;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * <h2>Menus</h2>
 * <ul>
 *   <li><b>File</b>: New Game, Load Game, Save Game, Configuration, Info,
 *       Quit.</li>
 *   <li><b>Game</b>: Undo, Redo, Pause, Hint.</li>
 * </ul>
 *
 * <h2>Load / Save dialogs</h2>
 * Both dialogs use a {@link TextInputDialog} (rather than a native
 * {@code FileChooser}) to avoid a WSL2 / Windows path incompatibility where
 * the native Windows file picker cannot list files stored under a
 * {@code /mnt/c/…} WSL path.
 *
 * <ul>
 *   <li>The <em>Save</em> dialog prompts the user for a file name and
 *       forwards it to {@code controller.executeCommand("save", …)}. The
 *       directory is managed by {@code SaveBoard}.</li>
 *   <li>The <em>Load</em> dialog lists existing saves in {@code Sauvegarde/}
 *       and prompts the user for a file name, then forwards it to
 *       {@code controller.executeCommand("load", …)}.</li>
 * </ul>
 *
 * <h2>Stage reference</h2>
 * Call {@link #setStage(Stage)} after {@code stage.show()} so that
 * {@link Alert} dialogs are owned by the main window (modal behaviour).
 */
public class MenuView extends MenuBar {

  /**
   * Directory where {@code SaveBoard} and {@code LoadBoard} write and read
   * save files. Mirrored here so the Load dialog can list available saves
   * without duplicating the path logic.
   */
  private static final File SAVE_DIR = new File(System.getProperty("user.dir")
      + File.separator + "Sauvegarde");

  /** Controller that receives all menu action commands. */
  private final GameController controller;

  /** GUI entry point — used to delegate the quit flow. */
  private GraphicalUserInterface gui;

  /**
   * Primary application stage. Set by {@link #setStage(Stage)} after
   * {@code stage.show()} so that modal dialogs have a proper owner window.
   * May be {@code null} before that call, but all dialog-opening code is only
   * triggered by user interaction (after the stage is visible).
   */
  private Stage stage;

  /**
   * Creates the menu bar and populates it with the File and Game menus.
   *
   * @param controller the game controller; must not be {@code null}
   */
  public MenuView(GameController controller) {
    this.controller = controller;
    initMenus();
    // Delegate to the OS native menu bar on macOS for a native look.
    this.setUseSystemMenuBar(true);
  }

  /**
   * Sets the primary {@link Stage} so that dialogs opened by this menu bar are
   * modal with respect to the main window.
   *
   * <p>Must be called after {@code stage.show()} (from
   * {@link MainView#passStageToMenu(Stage)}).
   *
   * @param stage the application's primary stage; must not be {@code null}
   */
  public void setStage(Stage stage) {
    this.stage = stage;
  }

  /**
   * Sets the GUI reference so the Quit item can delegate to
   * {@link GraphicalUserInterface#requestQuit()}.
   *
   * @param gui the application's GUI entry point
   */
  public void setGui(GraphicalUserInterface gui) {
    this.gui = gui;
  }

  /** Adds the File and Game menus to this menu bar. */
  private void initMenus() {
    this.getMenus().addAll(buildFileMenu(), buildGameMenu());
  }

  /**
   * Builds the <em>File</em> menu.
   *
   * <p>Items and their default shortcuts:
   * <ul>
   *   <li>New Game — {@code Ctrl+N}</li>
   *   <li>Load Game — {@code Ctrl+L}</li>
   *   <li>Save Game — {@code Ctrl+S}</li>
   *   <li>Configuration — {@code Ctrl+,}</li>
   *   <li>Info — {@code Ctrl+I}</li>
   *   <li>Quit — {@code Ctrl+Q}</li>
   * </ul>
   *
   * @return the configured {@link Menu}
   */
  private Menu buildFileMenu() {
    MenuItem newItem = new MenuItem("New Game");
    newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
    newItem.setOnAction(e -> openConfigDialog());

    MenuItem loadItem = new MenuItem("Load Game");
    loadItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));
    loadItem.setOnAction(e -> handleLoad());

    MenuItem saveItem = new MenuItem("Save Game");
    saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
    saveItem.setOnAction(e -> openSaveDialog());

    MenuItem configItem = new MenuItem("Configuration");
    configItem.setAccelerator(
        new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
    configItem.setOnAction(e -> showConfigDialog());

    MenuItem infoItem = new MenuItem("Info");
    infoItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
    infoItem.setOnAction(e -> showInfoDialog());

    MenuItem quitItem = new MenuItem("Quit");
    quitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
    // Delegate to the GUI so the JavaFX confirmation dialog is shown.
    // Falls back to controller.executeCommand if gui is not yet set.
    quitItem.setOnAction(e -> {
      if (gui != null) {
        gui.requestQuit();
      } else {
        controller.executeCommand("quit", new String[0]);
      }
    });

    Menu fileMenu = new Menu("_File");
    fileMenu.getItems().addAll(
        newItem, loadItem, saveItem,
        new SeparatorMenuItem(),
        configItem, infoItem,
        new SeparatorMenuItem(),
        quitItem);
    return fileMenu;
  }

  /**
   * Builds the <em>Game</em> menu.
   *
   * <p>Items and their default shortcuts:
   * <ul>
   *   <li>Undo — {@code Ctrl+U}</li>
   *   <li>Redo — {@code Ctrl+R}</li>
   *   <li>Pause — {@code Ctrl+P}</li>
   *   <li>Hint — {@code Ctrl+H}</li>
   * </ul>
   *
   * @return the configured {@link Menu}
   */
  private Menu buildGameMenu() {
    MenuItem undoItem = new MenuItem("Undo");
    undoItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
    undoItem.setOnAction(e -> controller.executeCommand("undo", new String[] { "1" }));

    MenuItem redoItem = new MenuItem("Redo");
    redoItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
    redoItem.setOnAction(e -> controller.executeCommand("redo", new String[] { "1" }));

    MenuItem pauseItem = new MenuItem("Pause");
    pauseItem.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN));
    pauseItem.setOnAction(e -> controller.executeCommand("pause", new String[0]));

    MenuItem hintItem = new MenuItem("Hint");
    hintItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
    hintItem.setOnAction(e -> controller.executeCommand("hint", new String[0]));

    Menu gameMenu = new Menu("_Game");
    gameMenu.getItems().addAll(
        undoItem, redoItem, new SeparatorMenuItem(), pauseItem, hintItem);
    return gameMenu;
  }

  /**
   * Entry point for the Load action.
   *
   * <p>If a game is in progress with unsaved changes, the user is asked
   * whether to save first (Yes / No / Cancel). Only "Cancel" aborts the load.
   * In all other cases {@link #openLoadDialog()} is called.
   */
  private void handleLoad() {
    if (controller.getGame() != null && controller.hasUnsavedChanges()) {
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.setTitle("Unsaved changes");
      confirm.setHeaderText("Current game has unsaved changes.");
      confirm.setContentText("Save before loading another game?");
      confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

      confirm.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
          openSaveDialog();
          ;
          openLoadDialog();
        } else if (response == ButtonType.NO) {
          openLoadDialog();
        }
        // ButtonType.CANCEL — do nothing.
      });
    } else {
      openLoadDialog();
    }
  }

  /**
   * Shows a {@link TextInputDialog} listing the saves available in
   * {@link #SAVE_DIR} and forwards the chosen file name to
   * {@code controller.executeCommand("load", …)}.
   *
   * <p>Using a text dialog.
   */
  private void openLoadDialog() {
    // Build the header text: list available save files if any exist.
    String headerText;
    if (SAVE_DIR.exists()) {
      File[] files = SAVE_DIR.listFiles();
      if (files != null && files.length > 0) {
        StringBuilder sb = new StringBuilder("Available saves:\n");
        for (File f : files) {
          sb.append("  - ").append(f.getName()).append("\n");
        }
        headerText = sb.toString();
      } else {
        headerText = "No saves found in: " + SAVE_DIR.getPath();
      }
    } else {
      headerText = "Load from: " + SAVE_DIR.getPath();
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Load Game");
    dialog.setHeaderText(headerText);
    dialog.setContentText("File name:");

    dialog.showAndWait().ifPresent(name -> {
      name = name.trim();
      if (!name.isEmpty()) {
        // LoadBoard reconstructs the full path from the file name alone.
        controller.executeCommand("load", new String[] { name });
      }
    });
  }

  /**
   * Shows a {@link TextInputDialog} prompting the user for a file name, then
   * forwards it to {@code controller.executeCommand("save", …)}.
   *
   * <p>A success or failure alert is displayed after the command completes,
   * based on whether the expected file was actually created on disk.
   */
  public void openSaveDialog() {
    if (controller.getGame() == null) {
      showError("No game in progress", "Start a new game before saving.");
      return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Save Game");
    dialog.setHeaderText("Save to: " + SAVE_DIR.getPath());
    dialog.setContentText("File name:");

    dialog.showAndWait().ifPresent(name -> {
      name = name.trim();
      if (name.isEmpty()) {
        return;
      }

      // SaveBoard reconstructs the full path from the file name alone.
      controller.executeCommand("save", new String[] { name });

      // Confirm that the file was actually written to disk.
      File saved = new File(SAVE_DIR, name);
      if (saved.exists()) {
        showInfo("Saved", "Game saved as: " + name);
      } else {
        showError("Save failed", "Could not write to: " + SAVE_DIR.getPath());
      }
    });
  }

  /**
   * Opens the game configuration dialog and starts a new game if the user
   * confirms. Called by both the "New Game" menu item and the Configuration
   * menu item.
   *
   * <p>Blocks until the user closes the dialog. If the user clicks
   * "Start Game", the resulting {@link Configuration} is forwarded to
   * {@link fr.ubordeaux.pdp.controller.GameController#startNewGame}.
   */
  private void openConfigDialog() {
    ConfigDialog dialog = new ConfigDialog();
    dialog.showAndWait().ifPresent(cfg -> controller.startNewGame(cfg));
  }

  /**
  * Shows the game configuration dialog (File › Configuration, {@code Ctrl+,}).
  */
  private void showConfigDialog() {
    openConfigDialog();
  }

  /**
   * Shows the About dialog with project and version information.
   */
  private void showInfoDialog() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("About Checkers");
    alert.setHeaderText("Checkers — v1.0");
    alert.setContentText(
        "Universite de Bordeaux\n"
            + "Master Informatique — Projet de Programmation 2025-2026\n\n"
            + "A checkers game with CLI and GUI interfaces.\n"
            + "Built with Java 17 + JavaFX.");
    alert.showAndWait();
  }

  /**
   * Shows a modal error alert.
   *
   * @param header  short summary shown in the dialog header
   * @param content detailed message shown in the dialog body
   */
  private void showError(String header, String content) {
    Alert a = new Alert(Alert.AlertType.ERROR);
    a.setTitle("Error");
    a.setHeaderText(header);
    a.setContentText(content);
    a.showAndWait();
  }

  /**
   * Shows a modal informational alert.
   *
   * @param header  short summary shown in the dialog header
   * @param content detailed message shown in the dialog body
   */
  private void showInfo(String header, String content) {
    Alert a = new Alert(Alert.AlertType.INFORMATION);
    a.setTitle("Info");
    a.setHeaderText(header);
    a.setContentText(content);
    a.showAndWait();
  }
}