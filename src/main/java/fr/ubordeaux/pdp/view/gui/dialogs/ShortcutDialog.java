package fr.ubordeaux.pdp.view.gui.dialogs;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;

/**
 * Modal dialog that lets the user remap keyboard shortcuts for all menu
 * actions.
 *
 * <p>Each row shows the action name, its current shortcut, and a "Change"
 * button. Clicking "Change" opens a small capture dialog — the next key
 * combination typed becomes the new shortcut.
 *
 * <p>Changes are written to {@link ShortcutManager} and persisted via
 * {@link fr.ubordeaux.pdp.ConfigManager} when the user clicks "Save".
 *
 * <p>Usage:
 * <pre>
 *   new ShortcutDialog(shortcutManager).showAndWait();
 * </pre>
 */
public class ShortcutDialog extends Dialog<Void> {

  /** All configurable action keys, in display order. */
  private static final List<String> ACTIONS = List.of(
      "new-game", "load-game", "save-game", "configuration",
      "info", "quit", "undo", "redo", "pause", "hint");

  /** The shortcut manager to read from and write to. */
  private final ShortcutManager shortcutManager;

  /**
   * Builds the dialog for the given {@link ShortcutManager}.
   *
   * @param shortcutManager the manager to read from and write to;
   *                        must not be {@code null}
   */
  public ShortcutDialog(ShortcutManager shortcutManager) {
    this.shortcutManager = shortcutManager;

    setTitle("Keyboard Shortcuts");
    setHeaderText("Click 'Change' next to an action to remap it.");

    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    getDialogPane().setContent(buildGrid());

    setResultConverter(btn -> {
      if (btn == ButtonType.OK) {
        shortcutManager.save();
      }
      return null;
    });
  }

  /**
   * Builds the shortcut grid: one row per action with its current shortcut
   * and a "Change" button.
   *
   * @return the configured {@link GridPane}
   */
  private GridPane buildGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(16);
    grid.setVgap(10);
    grid.setPadding(new Insets(16));

    for (int i = 0; i < ACTIONS.size(); i++) {
      String action = ACTIONS.get(i);

      Label nameLabel = new Label(formatAction(action));
      nameLabel.setMinWidth(120);

      KeyCombination kc = shortcutManager.get(action);
      Label currentLabel = new Label(kc != null ? kc.getDisplayText() : "—");
      currentLabel.setMinWidth(100);

      Button changeBtn = new Button("Change");
      changeBtn.setOnAction(e -> {
        KeyCombination captured = captureKey(action);
        if (captured != null) {
          shortcutManager.set(action, captured);
          currentLabel.setText(captured.getDisplayText());
        }
      });

      grid.add(nameLabel, 0, i);
      grid.add(currentLabel, 1, i);
      grid.add(changeBtn, 2, i);
    }

    // Reset to defaults button at the bottom.
    Button resetBtn = new Button("Reset to defaults");
    resetBtn.setOnAction(e -> {
      shortcutManager.resetToDefaults();
      // Rebuild the grid content by closing and reopening is overkill —
      // just refresh all the current labels.
      grid.getChildren().clear();
      grid.getChildren().addAll(buildGrid().getChildren());
    });
    grid.add(resetBtn, 0, ACTIONS.size(), 3, 1);

    return grid;
  }

  /**
   * Opens a small capture dialog and waits for the user to press a key
   * combination.
   *
   * <p>The captured combination is validated before being accepted:
   * <ul>
   *   <li>Lone modifier keys (Ctrl, Shift, Alt) are ignored.</li>
   *   <li>System-reserved combinations (e.g. {@code Ctrl+A}) are rejected
   *       with an error alert.</li>
   *   <li>Combinations already assigned to another action are rejected
   *       with an error alert.</li>
   * </ul>
   *
   * @param action the action being remapped (used in the dialog header
   *               and for duplicate detection)
   * @return the captured {@link KeyCombination}, or {@code null} if
   *         cancelled or rejected
   */
  private KeyCombination captureKey(String action) {
    Alert capture = new Alert(Alert.AlertType.INFORMATION);
    capture.setTitle("Press shortcut");
    capture.setHeaderText("Action: " + formatAction(action));
    capture.setContentText("Press the key combination to assign…");
    capture.getButtonTypes().setAll(ButtonType.CANCEL);

    KeyCombination[] result = { null };
    capture.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (isModifierOnly(e)) {
        return;
      }
      e.consume();

      if (isReserved(e.getCode())) {
        showError("Reserved shortcut",
            "Ctrl+" + e.getCode().getName() + " is reserved by the system.");
        capture.close();
        return;
      }

      List<KeyCombination.Modifier> mods = new ArrayList<>();
      if (e.isControlDown()) {
        mods.add(KeyCombination.CONTROL_DOWN);
      }
      if (e.isShiftDown()) {
        mods.add(KeyCombination.SHIFT_DOWN);
      }
      if (e.isAltDown()) {
        mods.add(KeyCombination.ALT_DOWN);
      }

      KeyCombination kc = new KeyCodeCombination(
          e.getCode(), mods.toArray(new KeyCombination.Modifier[0]));

      if (isDuplicate(kc, action)) {
        showError("Duplicate shortcut",
            kc.getDisplayText() + " is already used by another action.");
        capture.close();
        return;
      }
      result[0] = kc;
      capture.close();
    });

    capture.showAndWait();
    return result[0];
  }

  /**
   * Formats an action key into a human-readable label.
   * Example: {@code "new-game"} → {@code "New Game"}.
   *
   * @param action the action key
   * @return the display string
   */
  private String formatAction(String action) {
    String[] words = action.split("-");
    StringBuilder sb = new StringBuilder();
    for (String w : words) {
      if (!w.isEmpty()) {
        sb.append(Character.toUpperCase(w.charAt(0)))
            .append(w.substring(1))
            .append(' ');
      }
    }
    return sb.toString().trim();
  }

  /**
   * Returns {@code true} if the event is a lone modifier key with no regular
   * key attached (Ctrl, Shift, Alt, etc.).
   *
   * @param e the key event to test
   * @return {@code true} if only a modifier was pressed
   */
  private boolean isModifierOnly(KeyEvent e) {
    return switch (e.getCode()) {
      case CONTROL, SHIFT, ALT, META, COMMAND, WINDOWS -> true;
      default -> false;
    };
  }

  /**
   * Returns {@code true} if the given key code is reserved by the system
   * and cannot be used as a menu shortcut.
   *
   * <p>JavaFX intercepts {@code Ctrl+A} (select all), {@code Ctrl+C} (copy)
   * and {@code Ctrl+Z} (undo) before they reach menu accelerators.
   *
   * @param code the key code to check
   * @return {@code true} if the key is reserved
   */
  private boolean isReserved(KeyCode code) {
    return switch (code) {
      case A, C, Z -> true;
      default -> false;
    };
  }

  /**
   * Returns {@code true} if the given key combination is already assigned
   * to another action.
   *
   * <p>The {@code currentAction} is excluded from the check so that the
   * user can "re-assign" the same shortcut to the same action without
   * triggering a duplicate error.
   *
   * @param kc            the key combination to check
   * @param currentAction the action being remapped (excluded from the check)
   * @return {@code true} if the combination is already used by another action
   */
  private boolean isDuplicate(KeyCombination kc, String currentAction) {
    for (String action : ACTIONS) {
      if (action.equals(currentAction)) {
        continue;
      }
      KeyCombination existing = shortcutManager.get(action);
      if (existing != null && existing.getDisplayText().equals(kc.getDisplayText())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Shows a modal error alert with the given title and message.
   *
   * @param title   short summary shown in the dialog header
   * @param message detailed message shown in the dialog body
   */
  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(message);
    alert.showAndWait();
  }
}