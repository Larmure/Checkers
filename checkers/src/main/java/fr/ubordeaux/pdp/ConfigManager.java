package fr.ubordeaux.pdp;

import fr.ubordeaux.pdp.model.Utils;
import fr.ubordeaux.pdp.model.Internationalization;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fr.ubordeaux.pdp.model.Utils;

/**
 * Manages the configuration file for the Checkers game.
 *
 * Handles the {@code .checkersrc} file located in the user's home directory.
 * It strictly enforces the presence of a {@code [defaults]} header and provides
 * fallback mechanisms for invalid or missing keys and values.
 * 
 * @version 1.0
 */
public class ConfigManager {
  private static final String CONFIG_FILE = ".checkersrc";

  private boolean verbose = Utils.DEFAULT_VERBOSE;
  private boolean blitz = Utils.DEFAULT_BLITZ;
  private int time = Utils.DEFAULT_TIME;
  private boolean contest = Utils.DEFAULT_CONTEST;
  private int size = Utils.DEFAULT_BOARD_SIZE;
  private boolean debug = Utils.DEFAULT_DEBUG;

  /**
   * Loads configuration settings from the {@code .checkersrc} file.
   *
   * If the file does not exist, a default one is created. If the file is 
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

      // Strict validation of the Header on the first line
      if (lines.isEmpty() || !lines.get(0).trim().equals("[defaults]")) {
        throw new IOException("Missing [defaults] header.");
      }

      boolean foundVerbose = false;
      boolean foundContest = false;
      boolean foundDebug = false;

      // Iterate through lines after the header
      for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i).trim();

        // Skip empty lines or comments
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        // Split the line at the first '=' sign
        String[] parts = line.split("=", 2);
        if (parts.length < 2) {
          continue;
        }

        String key = parts[0].trim();
        String value = parts[1].trim();

        switch (key) {
          case "verbose":
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
              this.verbose = Boolean.parseBoolean(value);
              foundVerbose = true;
            } else {
              System.err.println(Internationalization.get("config.warn.invalid_value") + value);
              this.verbose = Utils.DEFAULT_VERBOSE;
            }
            break;

          case "blitz":
            this.blitz = Boolean.parseBoolean(value);
            break;

          case "timeout":
            this.time = Integer.parseInt(value);
            break;

          case "contest":
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
              this.contest = Boolean.parseBoolean(value);
              foundContest = true;
            } else {
              System.err.println("Warning: Invalid value for 'contest': " + value);
              this.contest = Utils.DEFAULT_CONTEST;
            }
            break;
          
          case "debug":
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
              this.debug = Boolean.parseBoolean(value);
              foundDebug = true;
            } else {
              System.err.println("Warning: Invalid value for 'debug': " + value);
              this.debug = Utils.DEFAULT_DEBUG;
            }
            break;
          
          case "size":
            if(Utils.VALID_SIZES.contains(Integer.valueOf(value))) {
              this.size = Integer.parseInt(value);
            } else {
              System.err.println("Warning: Invalid size for 'size': " + value);
              this.size = Utils.DEFAULT_BOARD_SIZE;
            }
            break;

          default:
            System.err.println(Internationalization.get("config.warn.unknown_key") + key);
            break;
        }
      }

      // Check if keys were found
      if (!foundVerbose) {
        System.err.println("Note: 'verbose' key not found. Using default: " + Utils.DEFAULT_VERBOSE);
        this.verbose = Utils.DEFAULT_VERBOSE;
      }

      if (!foundContest) {
        System.err.println("Note: 'contest' key not found. Using default: " + Utils.DEFAULT_CONTEST);
        this.contest = Utils.DEFAULT_CONTEST;
      }

      if (!foundDebug) {
        System.err.println("Note: 'debug' key not found. Using default: " + Utils.DEFAULT_CONTEST);
        this.debug = Utils.DEFAULT_DEBUG;
      }

    } catch (Exception e) {
      System.err.println(Internationalization.get("config.warn.invalid_file") + e.getMessage());
      System.err.println(Internationalization.get("config.info.reset"));
      createDefaultConfig(configPath);
      this.verbose = Utils.DEFAULT_VERBOSE;
      this.contest = Utils.DEFAULT_CONTEST;
      this.debug = Utils.DEFAULT_DEBUG;
    }
  }

  /**
   * Creates a default {@code .checkersrc} file with predefined values from {@link Utils}.
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
      System.out.println("Default configuration file created successfully in : "+ path.toString());
    } catch (IOException e) {
      System.err.println("Critical Error: Could not create configuration file.");
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

  public boolean isBlitz() {
    return this.blitz;
  }

  public int getTime() {
    return this.time;
  }

  public boolean isContest() {
    return this.contest;
  }

  public int getSize() {
    return this.size;
  }

  public boolean isDebug() {
    return this.debug;
  }
}