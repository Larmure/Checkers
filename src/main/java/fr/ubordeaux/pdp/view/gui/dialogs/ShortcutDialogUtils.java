package fr.ubordeaux.pdp.view.gui.dialogs;

import java.util.List;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;

/**
 * Pure utility class extracted from {@link ShortcutDialog} to make its
 * logic independently testable without requiring a JavaFX runtime.
 *
 * <p>None of these methods touch the UI — they operate only on strings,
 * key codes, and key combinations.
 */
public class ShortcutDialogUtils {

  /** Private constructor — this class is not meant to be instantiated. */
  private ShortcutDialogUtils() {
  }

  /**
   * Formats a hyphenated action key into a human-readable label.
   * Example: {@code "new-game"} → {@code "New Game"}.
   *
   * @param action the action key (e.g. {@code "new-game"})
   * @return the display string (e.g. {@code "New Game"})
   */
  public static String formatAction(String action) {
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
   * Returns {@code true} if the given key code is a lone modifier key
   * (Ctrl, Shift, Alt, Meta, etc.) with no regular key attached.
   *
   * @param code the key code to check
   * @return {@code true} if the key is a modifier-only key
   */
  public static boolean isModifierOnly(KeyCode code) {
    return switch (code) {
      case CONTROL, SHIFT, ALT, META, COMMAND, WINDOWS -> true;
      default -> false;
    };
  }

  /**
   * Returns {@code true} if the given key code is reserved by the system
   * and cannot be used as a menu shortcut.
   *
   * <p>JavaFX intercepts {@code Ctrl+A} (select all), {@code Ctrl+C} (copy),
   * and {@code Ctrl+Z} (undo) before they reach menu accelerators.
   *
   * @param code the key code to check
   * @return {@code true} if the key is reserved
   */
  public static boolean isReserved(KeyCode code) {
    return switch (code) {
      case A, C, Z -> true;
      default -> false;
    };
  }

  /**
   * Returns {@code true} if the given key combination is already assigned
   * to another action in the provided shortcut manager.
   *
   * <p>The {@code currentAction} is excluded from the check so that the user
   * can re-confirm the same shortcut without triggering a duplicate error.
   *
   * @param kc            the key combination to check
   * @param currentAction the action being remapped (excluded from the check)
   * @param actions       the full list of action keys to check against
   * @param manager       the shortcut manager holding current assignments
   * @return {@code true} if the combination is already used by another action
   */
  public static boolean isDuplicate(
      KeyCombination kc,
      String currentAction,
      List<String> actions,
      ShortcutManager manager) {

    for (String action : actions) {
      if (action.equals(currentAction)) {
        continue;
      }
      KeyCombination existing = manager.get(action);
      if (existing != null
          && existing.getDisplayText().equals(kc.getDisplayText())) {
        return true;
      }
    }
    return false;
  }
}