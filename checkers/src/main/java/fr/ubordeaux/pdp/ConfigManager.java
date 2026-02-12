package fr.ubordeaux.pdp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages the configuration file for the Checkers game.
 * Handles loading, creating default settings, and retrieving properties.
 * The configuration is stored in a file named .checkersrc in the user's home directory.
 */
public class ConfigManager {

  /** Name of the configuration file. */
  private static final String CONFIG_FILE = ".checkersrc";

  /** Properties object to store configuration key-value pairs. */
  private final Properties props = new Properties();

  /**
   * Loads the configuration from the .checkersrc file in the user's home directory.
   * If the file does not exist, a default one is created.
   * If the file is invalid, a warning is displayed and default values are used.
   */
  public void load() {
    Path configPath = Paths.get(System.getProperty("user.home"), CONFIG_FILE);

    if (!Files.exists(configPath)) {
      createDefaultConfig(configPath);
    }

    InputStream input = null;
    try {
      input = new FileInputStream(configPath.toFile());
      props.load(input);
    } catch (IOException e) {
      System.err.println("Warning: Configuration file is invalid.");
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException ex) {
          // Silent catch for closing stream
        }
      }
    }
  }

  /**
   * Creates a default configuration file with initial settings.
   *
   * @param path The path where the configuration file should be created.
   */
  private void createDefaultConfig(Path path) {
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
      writer.println("[defaults]");
      writer.println("verbose = false");
      writer.println("blitz = false");
      writer.println("timeout = 30");
      System.out.println("Default configuration created in: " + path.toString());
    } catch (IOException e) {
      System.err.println("Error creating default configuration: " + e.getMessage());
    }
  }

  /**
   * Retrieves a configuration property by its key.
   *
   * @param key The configuration key to look for.
   * @param defaultValue The value to return if the key is not found.
   * @return The value associated with the key, or the default value.
   */
  public String getProperty(String key, String defaultValue) {
    return props.getProperty(key, defaultValue);
  }
}