package fr.ubordeaux.pdp.view.gui.dialogs;

import fr.ubordeaux.pdp.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Converts keyboard shortcut strings from {@link ConfigManager} into JavaFX
 * {@link KeyCombination} objects, and writes changes back.
 *
 * <p>All persistence is delegated to {@link ConfigManager} — this class
 * contains no file I/O.
 */
public class ShortcutManager {

  /** The config manager used for reading and writing shortcut strings. */
  private final ConfigManager configManager;

  /**
   * Creates a ShortcutManager backed by the given ConfigManager.
   *
   * @param configManager the config manager; must not be {@code null}
   */
  public ShortcutManager(ConfigManager configManager) {
    this.configManager = configManager;
  }

  /**
   * Returns the {@link KeyCombination} for the given action key.
   *
   * @param action the action key (e.g. {@code "new-game"})
   * @return the corresponding {@link KeyCombination}, or {@code null}
   */
  public KeyCombination get(String action) {
    return parse(configManager.getShortcut(action));
  }

  /**
   * Updates the shortcut for the given action in memory.
   * Call {@link #save()} to persist.
   *
   * @param action the action key (e.g. {@code "new-game"})
   * @param kc     the new shortcut
   */
  public void set(String action, KeyCombination kc) {
    configManager.setShortcut(action, serialize(kc));
  }

  /**
   * Persists all shortcuts to {@code .checkersrc} via {@link ConfigManager}.
   */
  public void save() {
    configManager.saveShortcuts();
  }

  /**
   * Resets all shortcuts to their built-in defaults in memory.
   * Call {@link #save()} to persist.
   */
  public void resetToDefaults() {
    configManager.setShortcut("new-game", "Ctrl+N");
    configManager.setShortcut("load-game", "Ctrl+L");
    configManager.setShortcut("save-game", "Ctrl+S");
    configManager.setShortcut("configuration", "Ctrl+Comma");
    configManager.setShortcut("info", "Ctrl+I");
    configManager.setShortcut("quit", "Ctrl+Q");
    configManager.setShortcut("undo", "Ctrl+U");
    configManager.setShortcut("redo", "Ctrl+R");
    configManager.setShortcut("pause", "Ctrl+P");
    configManager.setShortcut("hint", "Ctrl+H");
  }

  /**
   * Parses a shortcut string (e.g. {@code "Ctrl+N"}) into a
   * {@link KeyCombination}.
   *
   * @param value the shortcut string from {@code .checkersrc}
   * @return the parsed combination, or {@code null} if unparseable
   */
  private KeyCombination parse(String value) {
    if (value == null) {
      return null;
    }
    try {
      String[] tokens = value.split("\\+");
      boolean ctrl = false;
      boolean shift = false;
      boolean alt = false;
      KeyCode code = null;
      for (String token : tokens) {
        switch (token.trim().toLowerCase(Locale.ROOT)) {
          case "ctrl" -> ctrl = true;
          case "shift" -> shift = true;
          case "alt" -> alt = true;
          default -> code = KeyCode.valueOf(token.trim().toUpperCase(Locale.ROOT));
        }
      }
      if (code == null) {
        return null;
      }
      List<KeyCombination.Modifier> mods = new ArrayList<>();
      if (ctrl) {
        mods.add(KeyCombination.CONTROL_DOWN);
      }
      if (shift) {
        mods.add(KeyCombination.SHIFT_DOWN);
      }
      if (alt) {
        mods.add(KeyCombination.ALT_DOWN);
      }
      return new KeyCodeCombination(code, mods.toArray(new KeyCombination.Modifier[0]));
    } catch (Exception e) {
      System.err.println("ShortcutManager: cannot parse shortcut: " + value);
      return null;
    }
  }

  /**
   * Serialises a {@link KeyCombination} to the string format used in
   * {@code .checkersrc} (e.g. {@code "Ctrl+N"}).
   *
   * @param kc the combination to serialise
   * @return the string representation
   */
  private String serialize(KeyCombination kc) {
    if (!(kc instanceof KeyCodeCombination kcc)) {
      return kc.toString();
    }
    StringBuilder sb = new StringBuilder();
    if (kcc.getControl() == KeyCombination.ModifierValue.DOWN) {
      sb.append("Ctrl+");
    }
    if (kcc.getShift() == KeyCombination.ModifierValue.DOWN) {
      sb.append("Shift+");
    }
    if (kcc.getAlt() == KeyCombination.ModifierValue.DOWN) {
      sb.append("Alt+");
    }
    sb.append(kcc.getCode().getName());
    return sb.toString();
  }
}