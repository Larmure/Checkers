package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the configuration file for the Checkers game.
 *
 * <p>Handles the {@code .checkersrc} file located in the user's home directory.
 * It strictly enforces the presence of a {@code [defaults]} header and provides
 * fallback mechanisms for invalid or missing keys and values.
 *
 * @version 1.0
 */
public class ConfigManager {
  /** The name of the configuration file. */
  private static final String CONFIG_FILE = ".checkersrc";

  /** The default values for the configuration settings. */
  private boolean verbose = Utils.DEFAULT_VERBOSE;
  /** The default value for the blitz setting. */
  private boolean blitz = Utils.DEFAULT_BLITZ;
  /** The default value for the time setting. */
  private int time = Utils.DEFAULT_TIME;
  /** The default value for the contest setting. */
  private boolean contest = Utils.DEFAULT_CONTEST;
  /** The default value for the board size setting. */
  private int size = Utils.DEFAULT_BOARD_SIZE;
  /** The default value for the debug setting. */
  private boolean debug = Utils.DEFAULT_DEBUG;
  /** The default value for keyboard shortcuts. */
  private String shortcutNewGame = Utils.DEFAULT_SHORTCUT_NEW_GAME;
  private String shortcutLoadGame = Utils.DEFAULT_SHORTCUT_LOAD_GAME;
  private String shortcutSaveGame = Utils.DEFAULT_SHORTCUT_SAVE_GAME;
  private String shortcutConfig = Utils.DEFAULT_SHORTCUT_CONFIGURATION;
  private String shortcutInfo = Utils.DEFAULT_SHORTCUT_INFO;
  private String shortcutQuit = Utils.DEFAULT_SHORTCUT_QUIT;
  private String shortcutUndo = Utils.DEFAULT_SHORTCUT_UNDO;
  private String shortcutRedo = Utils.DEFAULT_SHORTCUT_REDO;
  private String shortcutPause = Utils.DEFAULT_SHORTCUT_PAUSE;
  private String shortcutHint = Utils.DEFAULT_SHORTCUT_HINT;

  /**
   * Loads configuration settings from the {@code .checkersrc} file.
   *
   * <p>If the file does not exist, a default one is created. If the file is
   * corrupted (missing header), it is reset. For specific invalid values,
   * it logs a warning and uses safe defaults from {@link Utils}.
   */
  public void load() {
    Path configPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE);

    if (!Files.exists(configPath)) {
      createDefaultConfig(configPath);
    }

    try {
      List<String> lines = Files.readAllLines(configPath);
      boolean inDefaultsSection = false;
      boolean inShortcutsSection = false;
      boolean foundHeader = false;
      boolean foundVerbose = false;
      boolean foundContest = false;
      boolean foundDebug = false;
      boolean foundBlitz = false;
      boolean foundTimeout = false;

      for (String line : lines) {
        line = line.trim();

        // Skip empty lines or comments
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        if (line.startsWith("[") && line.endsWith("]")) {
          // Check if we are entering the [defaults] section
          if (line.equalsIgnoreCase("[defaults]")) {
            inDefaultsSection = true;
            foundHeader = true;
          } else if (line.equalsIgnoreCase("[shortcuts]")) {
            inShortcutsSection = true;
            inDefaultsSection = false;
          } else {
            inDefaultsSection = false;
            inShortcutsSection = false;
          }
          continue;
        }

        // Parse key-value pairs only if we are inside the [defaults] section
        if (inDefaultsSection) {
          String[] parts = line.split("=", 2);
          if (parts.length < 2) {
            continue; // Ignore lines that don't follow the 'key = value' format
          }

          String key = parts[0].trim();
          String value = parts[1].trim();

          switch (key) {
            case "verbose":
              if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                this.verbose = Boolean.parseBoolean(value);
                foundVerbose = true;
              } else {
                System.err.println(Internationalization.get("config.warn.invalid_value")
                    + value);
                this.verbose = Utils.DEFAULT_VERBOSE;
              }
              break;

            case "blitz":
              if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                this.blitz = Boolean.parseBoolean(value);
                foundBlitz = true;
              } else {
                System.err.println(Internationalization.get(
                    "config.warn.invalid_generic", "blitz", value));
                this.blitz = Utils.DEFAULT_BLITZ;
              }
              break;

            case "timeout":
              try {
                this.time = Integer.parseInt(value);
                foundTimeout = true;
              } catch (NumberFormatException e) {
                System.err.println(Internationalization.get("config.warn.invalid_generic",
                    "timeout", value));
                this.time = Utils.DEFAULT_TIME;
              }
              break;

            case "contest":
              if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                this.contest = Boolean.parseBoolean(value);
                foundContest = true;
              } else {
                System.err.println(Internationalization.get("config.warn.invalid_generic",
                    "contest", value));
                this.contest = Utils.DEFAULT_CONTEST;
              }
              break;

            case "debug":
              if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                this.debug = Boolean.parseBoolean(value);
                foundDebug = true;
              } else {
                System.err.println(Internationalization.get("config.warn.invalid_generic",
                    "debug", value));
                this.debug = Utils.DEFAULT_DEBUG;
              }
              break;

            case "size":
              if (Utils.VALID_SIZES.contains(Integer.valueOf(value))) {
                this.size = Integer.parseInt(value);
              } else {
                System.err.println(Internationalization.get("config.warn.invalid_size", value));
                this.size = Utils.DEFAULT_BOARD_SIZE;
              }
              break;

            default:
              System.err.println(Internationalization.get("config.warn.unknown_key") + key);
              break;

          }
        }
        if (inShortcutsSection) {
          String[] parts = line.split("=", 2);
          if (parts.length < 2) {
            continue;
          }
          String key = parts[0].trim();
          String value = parts[1].trim();
          switch (key) {
            case "new-game" -> shortcutNewGame = value;
            case "load-game" -> shortcutLoadGame = value;
            case "save-game" -> shortcutSaveGame = value;
            case "configuration" -> shortcutConfig = value;
            case "info" -> shortcutInfo = value;
            case "quit" -> shortcutQuit = value;
            case "undo" -> shortcutUndo = value;
            case "redo" -> shortcutRedo = value;
            case "pause" -> shortcutPause = value;
            case "hint" -> shortcutHint = value;
            default -> System.err.println("ShortcutManager: unknown key: " + key);
          }
        }
      }
      if (!foundHeader) {
        throw new IOException(Internationalization.get("config.error.missing_header"));
      }
      // Check if keys were found
      if (!foundVerbose) {
        System.err.println(Internationalization.get("config.warn.key_not_found",
            "verbose", Utils.DEFAULT_VERBOSE));
        this.verbose = Utils.DEFAULT_VERBOSE;
      }

      if (!foundContest) {
        System.err.println(Internationalization.get("config.warn.key_not_found",
            "contest", Utils.DEFAULT_VERBOSE));
        this.contest = Utils.DEFAULT_CONTEST;
      }

      if (!foundDebug) {
        System.err.println(Internationalization.get("config.warn.key_not_found",
            "debug", Utils.DEFAULT_VERBOSE));
        this.debug = Utils.DEFAULT_DEBUG;
      }

      if (!foundTimeout) {
        System.err.println(Internationalization.get("config.warn.key_not_found",
            "timeout", Utils.DEFAULT_VERBOSE));
        this.time = Utils.DEFAULT_TIME;
      }

      if (!foundBlitz) {
        System.err.println(Internationalization.get("config.warn.key_not_found",
            "blitz", Utils.DEFAULT_VERBOSE));
        this.blitz = Utils.DEFAULT_BLITZ;
      }

    } catch (Exception e) {
      System.err.println(Internationalization.get("config.warn.invalid_file") + e.getMessage());
      System.err.println(Internationalization.get("config.info.reset"));
      createDefaultConfig(configPath);
      this.verbose = Utils.DEFAULT_VERBOSE;
      this.contest = Utils.DEFAULT_CONTEST;
      this.debug = Utils.DEFAULT_DEBUG;
      this.blitz = Utils.DEFAULT_BLITZ;
      this.time = Utils.DEFAULT_TIME;
    }
  }

  /**
   * Creates a default {@code .checkersrc} file with predefined values from
   * {@link Utils}.
   *
   * @param path The path where the configuration file should be created.
   */
  private void createDefaultConfig(Path path) {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
      writer.println("[defaults]");
      writer.println("verbose = " + Utils.DEFAULT_VERBOSE);
      writer.println("blitz = " + Utils.DEFAULT_BLITZ);
      writer.println("timeout = " + Utils.DEFAULT_TIME);
      writer.println("contest = " + Utils.DEFAULT_CONTEST);
      writer.println("size = " + Utils.DEFAULT_BOARD_SIZE);
      writer.println("debug = " + Utils.DEFAULT_DEBUG);
      writer.println("");
      writer.println("[shortcuts]");
      writer.println("new-game = " + Utils.DEFAULT_SHORTCUT_NEW_GAME);
      writer.println("load-game = " + Utils.DEFAULT_SHORTCUT_LOAD_GAME);
      writer.println("save-game = " + Utils.DEFAULT_SHORTCUT_SAVE_GAME);
      writer.println("configuration = " + Utils.DEFAULT_SHORTCUT_CONFIGURATION);
      writer.println("info = " + Utils.DEFAULT_SHORTCUT_INFO);
      writer.println("quit = " + Utils.DEFAULT_SHORTCUT_QUIT);
      writer.println("undo = " + Utils.DEFAULT_SHORTCUT_UNDO);
      writer.println("redo = " + Utils.DEFAULT_SHORTCUT_REDO);
      writer.println("pause = " + Utils.DEFAULT_SHORTCUT_PAUSE);
      writer.println("hint = " + Utils.DEFAULT_SHORTCUT_HINT);
      System.err.println(Internationalization.get("config.info.created", path.toString()));
    } catch (IOException e) {
      System.err.println(Internationalization.get("config.info.created"));
    }
  }

  /**
   * Persists the current shortcuts into the {@code [shortcuts]} section of
   * {@code .checkersrc}, replacing the existing section if present.
   *
   * <p>Called by {@link fr.ubordeaux.pdp.view.gui.dialogs.ShortcutDialog} after the
   * user confirms changes.
   */
  public void saveShortcuts() {
    Path configPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE);
    try {
      List<String> lines;
      if (Files.exists(configPath)) {
        lines = Files.readAllLines(configPath);
      } else {
        lines = new ArrayList<>();
      }

      // Remove old [shortcuts] section.
      List<String> kept = new java.util.ArrayList<>();
      boolean inSection = false;
      for (String raw : lines) {
        if (raw.trim().equalsIgnoreCase("[shortcuts]")) {
          inSection = true;
          continue;
        }
        if (inSection && raw.trim().startsWith("[")) {
          inSection = false;
        }
        if (!inSection) {
          kept.add(raw);
        }
      }

      // Append updated [shortcuts] section.
      if (!kept.isEmpty() && !kept.get(kept.size() - 1).isBlank()) {
        kept.add("");
      }
      kept.add("[shortcuts]");
      kept.add("new-game = " + shortcutNewGame);
      kept.add("load-game = " + shortcutLoadGame);
      kept.add("save-game = " + shortcutSaveGame);
      kept.add("configuration = " + shortcutConfig);
      kept.add("info = " + shortcutInfo);
      kept.add("quit = " + shortcutQuit);
      kept.add("undo = " + shortcutUndo);
      kept.add("redo = " + shortcutRedo);
      kept.add("pause = " + shortcutPause);
      kept.add("hint = " + shortcutHint);

      Files.write(configPath, kept,
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);

    } catch (IOException e) {
      System.err.println("ConfigManager: could not save shortcuts — " + e.getMessage());
    }
  }

  /**
   * Checks if verbose mode is enabled.
   *
   * @return true if verbose mode is active, false otherwise.
   */
  public boolean isVerbose() {
    return this.verbose;
  }

  /**
   * Checks if blitz mode is enabled.
   *
   * @return true if blitz mode is active, false otherwise.
   */
  public boolean isBlitz() {
    return this.blitz;
  }

  /**
   * Returns the configured time limit for blitz mode.
   *
   * @return the time limit in seconds, or the default if not set or invalid.
   */
  public int getTime() {
    return this.time;
  }

  /**
   * Checks if contest mode is enabled.
   *
   * @return true if contest mode is active, false otherwise.
   */
  public boolean isContest() {
    return this.contest;
  }

  /**
   * Returns the configured board size.
   *
   * @return the board size, or the default if not set or invalid.
   */
  public int getSize() {
    return this.size;
  }

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug mode is active, false otherwise.
   */
  public boolean isDebug() {
    return this.debug;
  }

  /**
   * Retrieves the keyboard shortcut string for the specified action.
   *
   * @param action the action key (e.g., "new-game", "save-game")
   * @return the shortcut string from {@code .checkersrc}, or {@code null} if not found
   */
  public String getShortcut(String action) {
    return switch (action) {
      case "new-game" -> shortcutNewGame;
      case "load-game" -> shortcutLoadGame;
      case "save-game" -> shortcutSaveGame;
      case "configuration" -> shortcutConfig;
      case "info" -> shortcutInfo;
      case "quit" -> shortcutQuit;
      case "undo" -> shortcutUndo;
      case "redo" -> shortcutRedo;
      case "pause" -> shortcutPause;
      case "hint" -> shortcutHint;
      default -> null;
    };
  }

  /**
   * Sets the keyboard shortcut string for the specified action.
   *
   * @param action the action key (e.g., "new-game", "save-game")
   * @param value the new shortcut string from {@code .checkersrc}
   */
  public void setShortcut(String action, String value) {
    switch (action) {
      case "new-game" -> shortcutNewGame = value;
      case "load-game" -> shortcutLoadGame = value;
      case "save-game" -> shortcutSaveGame = value;
      case "configuration" -> shortcutConfig = value;
      case "info" -> shortcutInfo = value;
      case "quit" -> shortcutQuit = value;
      case "undo" -> shortcutUndo = value;
      case "redo" -> shortcutRedo = value;
      case "pause" -> shortcutPause = value;
      case "hint" -> shortcutHint = value;
      default -> System.err.println("ConfigManager: unknown shortcut: " + action);
    }
  }
}