package fr.ubordeaux.pdp.view.gui.layout;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.State;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import fr.ubordeaux.pdp.view.gui.dialogs.ConfigDialog;
import fr.ubordeaux.pdp.view.gui.dialogs.ShortcutManager;
import java.io.File;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * <h2>Menus</h2>
 * <ul>
 * <li><b>File</b>: New Game, Load Game, Save Game, Configuration, Info,
 * Quit.</li>
 * <li><b>Game</b>: Undo, Redo, Pause, Hint.</li>
 * </ul>
 *
 * <h2>Load / Save dialogs</h2>
 * Both dialogs use a {@link TextInputDialog} (rather than a native
 * {@code FileChooser}) to avoid a WSL2 / Windows path incompatibility where
 * the native Windows file picker cannot list files stored under a
 * {@code /mnt/c/…} WSL path.
 *
 * <ul>
 * <li>The <em>Save</em> dialog prompts the user for a file name and
 * forwards it to {@code controller.executeCommand("save", …)}. The
 * directory is managed by {@code SaveBoard}.</li>
 * <li>The <em>Load</em> dialog lists existing saves in {@code Sauvegarde/}
 * and prompts the user for a file name, then forwards it to
 * {@code controller.executeCommand("load", …)}.</li>
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

  /* Shorcut keyboard manager */
  private ShortcutManager shortcutManager;

  /** Game menu item: undo last move. */
  private MenuItem undoItem;

  /** Game menu item: redo last undone move. */
  private MenuItem redoItem;

  /**
   * Primary application stage. Set by {@link #setStage(Stage)} after
   * {@code stage.show()} so that modal dialogs have a proper owner window.
   * May be {@code null} before that call, but all dialog-opening code is only
   * triggered by user interaction (after the stage is visible).
   */
  @SuppressWarnings("unused")
  private Stage stage;

  /**
   * Creates the menu bar and populates it with the File and Game menus.
   *
   * @param controller      the game controller; must not be {@code null}
   * @param shortcutManager the shortcut manager for keyboard bindings
   */
  public MenuView(GameController controller, ShortcutManager shortcutManager) {
    this.controller = controller;
    this.shortcutManager = shortcutManager;
    initMenus();
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
   * <li>New Game — {@code Ctrl+N}</li>
   * <li>Load Game — {@code Ctrl+L}</li>
   * <li>Save Game — {@code Ctrl+S}</li>
   * <li>Configuration — {@code Ctrl+,}</li>
   * <li>Info — {@code Ctrl+I}</li>
   * <li>Quit — {@code Ctrl+Q}</li>
   * </ul>
   *
   * @return the configured {@link Menu}
   */
  private Menu buildFileMenu() {
    MenuItem newItem = new MenuItem(Internationalization.get("menu.new_game"));
    newItem.setAccelerator(shortcutManager.get("new-game"));
    newItem.setOnAction(e -> controller.executeCommand("new", new String[0]));

    MenuItem loadItem = new MenuItem(Internationalization.get("menu.load_game"));
    loadItem.setAccelerator(shortcutManager.get("load-game"));
    loadItem.setOnAction(e -> executeWithPause(() -> handleLoad()));

    MenuItem saveItem = new MenuItem(Internationalization.get("menu.save_game"));
    saveItem.setAccelerator(shortcutManager.get("save-game"));
    saveItem.setOnAction(e -> executeWithPause(() -> openSaveDialog()));

    MenuItem configItem = new MenuItem(Internationalization.get("menu.configuration"));
    configItem.setAccelerator(shortcutManager.get("configuration"));
    configItem.setOnAction(e -> executeWithPause(() -> showConfigDialog()));

    MenuItem infoItem = new MenuItem(Internationalization.get("menu.info"));
    infoItem.setAccelerator(shortcutManager.get("info"));
    infoItem.setOnAction(e -> executeWithPause(() -> showInfoDialog()));

    MenuItem quitItem = new MenuItem(Internationalization.get("menu.quit"));
    quitItem.setAccelerator(shortcutManager.get("quit"));
    // Delegate to the GUI so the JavaFX confirmation dialog is shown.
    // Falls back to controller.executeCommand if gui is not yet set.
    quitItem.setOnAction(e -> {
      if (gui != null) {
        gui.requestQuit();
      } else {
        controller.executeCommand("quit", new String[0]);
      }
    });

    Menu fileMenu = new Menu(Internationalization.get("menu.file"));
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
   * <li>Undo — {@code Ctrl+U}</li>
   * <li>Redo — {@code Ctrl+R}</li>
   * <li>Pause — {@code Ctrl+P}</li>
   * <li>Hint — {@code Ctrl+H}</li>
   * </ul>
   *
   * @return the configured {@link Menu}
   */
  private Menu buildGameMenu() {
    undoItem = new MenuItem(Internationalization.get("menu.undo"));
    undoItem.setAccelerator(shortcutManager.get("undo"));
    undoItem.setOnAction(e -> {
      int steps = (controller.isWhiteAi() != controller.isBlackAi()) ? 2 : 1;
      controller.executeCommand("undo", new String[] { String.valueOf(steps) });
    });

    redoItem = new MenuItem(Internationalization.get("menu.redo"));
    redoItem.setAccelerator(shortcutManager.get("redo"));
    redoItem.setOnAction(e -> {
      int steps = (controller.isWhiteAi() != controller.isBlackAi()) ? 2 : 1;
      controller.executeCommand("redo", new String[] { String.valueOf(steps) });
    });

    MenuItem pauseItem = new MenuItem(Internationalization.get("menu.pause"));
    pauseItem.setAccelerator(shortcutManager.get("pause"));
    pauseItem.setOnAction(e -> {
      if (controller.getGame() != null && controller.getGame().getState() == State.IN_GAME) {
        controller.executeCommand("pause", new String[0]);

        Alert pauseAlert = new Alert(Alert.AlertType.INFORMATION);
        pauseAlert.setTitle(Internationalization.get("dialog.pause_title"));
        pauseAlert.setHeaderText(Internationalization.get("dialog.pause_header"));

        ButtonType btnResume = new ButtonType(Internationalization.get("dialog.pause_resume"),
            javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        pauseAlert.getButtonTypes().setAll(btnResume);
        pauseAlert.showAndWait();

        controller.executeCommand("continue", new String[0]);
      }
    });

    MenuItem hintItem = new MenuItem(Internationalization.get("menu.hint"));
    hintItem.setAccelerator(shortcutManager.get("hint"));
    hintItem.setOnAction(e -> controller.executeCommand("hint", new String[0]));

    Menu gameMenu = new Menu(Internationalization.get("menu.game"));
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
      confirm.setTitle(Internationalization.get("dialog.unsaved_changes"));
      confirm.setHeaderText(Internationalization.get("dialog.unsaved_changes_header"));
      confirm.setContentText(Internationalization.get("dialog.unsaved_changes_content"));
      confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

      confirm.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
          openSaveDialog();
          ;
          openLoadDialog();
        } else if (response == ButtonType.NO) {
          openLoadDialog();
        }
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
        StringBuilder sb = new StringBuilder(Internationalization.get("dialog.available_saves")
            + "\n");
        for (File f : files) {
          sb.append("  - ").append(f.getName()).append("\n");
        }
        headerText = sb.toString();
      } else {
        headerText = Internationalization.get("dialog.no_saves") + SAVE_DIR.getPath();
      }
    } else {
      headerText = Internationalization.get("dialog.load_from") + SAVE_DIR.getPath();
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(Internationalization.get("dialog.load_game"));
    dialog.setHeaderText(headerText);
    dialog.setContentText(Internationalization.get("dialog.file_name"));

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
    if (controller.getGame() == null || controller.getGame().checkGameOver() == State.FINISHED) {
      showError(Internationalization.get("dialog.no_game"), Internationalization.get(
          "dialog.no_game_content"));
      return;
    }

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(Internationalization.get("dialog.save_game"));
    dialog.setHeaderText(Internationalization.get("dialog.save_to") + SAVE_DIR.getPath());
    dialog.setContentText(Internationalization.get("dialog.file_name"));

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
        showInfo(Internationalization.get("dialog.saved"), Internationalization.get(
            "dialog.saved_content") + name);
      } else {
        showError(Internationalization.get("dialog.save_failed"),
            Internationalization.get("dialog.save_failed_content") + SAVE_DIR.getPath());
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
    ConfigDialog dialog = new ConfigDialog(shortcutManager, () -> {
      this.getMenus().clear();
      initMenus();
    });
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
    alert.setTitle(Internationalization.get("dialog.about_title"));
    alert.setHeaderText(Internationalization.get("dialog.about_header"));
    alert.setContentText(Internationalization.get("dialog.about_content"));
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
    a.setTitle(Internationalization.get("dialog.error"));
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
    a.setTitle(Internationalization.get("dialog.save_game"));
    a.setHeaderText(header);
    a.setContentText(content);
    a.showAndWait();
  }

  /**
   * Helper method to automatically pause the game before opening a dialog,
   * and resume it after the dialog is closed.
   *
   * @param action The method to execute (opening the Load, Save, or Info dialog)
   */
  private void executeWithPause(Runnable action) {
    boolean wasInGame = controller.getGame() != null
        && controller.getGame().getState() == State.IN_GAME;

    if (wasInGame) {
      controller.executeCommand("pause", new String[0]);
    }

    action.run();

    if (wasInGame && controller.getGame() != null
        && controller.getGame().getState() == State.PAUSE) {
      controller.executeCommand("continue", new String[0]);
    }
  }

  /**
   * Enables or disables the Undo and Redo menu items. Called by the controller
   *
   * @param disable true to disable the items, false to enable them
   */
  public void setDisableUndoRedo(boolean disable) {
    if (undoItem != null) {
      undoItem.setDisable(disable);
    }
    if (redoItem != null) {
      redoItem.setDisable(disable);
    }
  }
}