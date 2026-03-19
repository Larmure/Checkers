package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads a saved game from a structured text file.
 *
 * <p>The file contains three mandatory sections:
 *
 * <ul>
 *   <li>{@code [settings]} for configuration values</li>
 *   <li>{@code [game]} for the board layout</li>
 *   <li>{@code [history]} for move history</li>
 * </ul>
 *
 * <p>Comments are removed before parsing:
 *
 * <ul>
 *   <li>{@code # ...} for inline comments</li>
 *   <li>{@code { ... }} for block comments</li>
 * </ul>
 */
public class LoadBoard {

  /** The directory where save files are stored. */
  private static final String SAVE_DIRECTORY = System.getProperty("user.dir") + File.separator
      + "Sauvegarde";

  /** The game being loaded. */
  private final GameCheckers game;
  /** The board being loaded. */
  private final Board board;
  /** The state of the loaded game. */
  private boolean seenGame = false;
  /** The state of the loaded settings. */
  private boolean seenSettings = false;
  /** The state of the loaded history. */
  private boolean seenHistory = false;

  /** The state of the game section initialization. */
  private boolean gameSectionInitialized = false;
  /** The current row being processed in the board section. */
  private int currentBoardRow = 0;

  /** The loaded configuration. */
  private Configuration loadedConfiguration = null;
  /** The loaded starting color. */
  private Boolean loadedStartingWhite = null;
  /** The loaded board size. */
  private int loadedBoardSize = 0;
  /** The loaded blitz mode flag. */
  private Boolean loadedBlitz = null;
  /** The loaded debug mode flag. */
  private Boolean loadedDebug = null;
  /** The loaded verbose mode flag. */
  private Boolean loadedVerbose = null;

  /** The buffer for storing move history. */
  private StringBuilder historyBuffer = new StringBuilder();

  /**
   * Creates a loader for the given game.
   *
   * @param game the game to restore
   */
  public LoadBoard(GameCheckers game) {
    this.game = game;
    this.board = game.getBoard();
  }

  /**
   * Loads game data from a file in the save directory.
   *
   * <p>Stops at the first format error and prints a descriptive message.
   *
   * @param fileName the save file name
   */
  public void loadGameData(String fileName) {
    Path path = Paths.get(SAVE_DIRECTORY, fileName);
    File file = path.toFile();

    resetState();

    if (!file.exists()) {
      System.err.println("Loading error: file not found at " + path);
      return;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String currentSection = "";
      String line;
      int lineNum = 0;

      while ((line = reader.readLine()) != null) {
        lineNum++;
        String clean = stripComments(line);
        if (clean.isEmpty()) {
          continue;
        }

        if (clean.startsWith("[") && clean.endsWith("]")) {
          currentSection = clean.toLowerCase();
          switch (currentSection) {
            case "[settings]" -> seenSettings = true;
            case "[game]" -> seenGame = true;
            case "[history]" -> seenHistory = true;
            default -> {
              // Unknown sections are ignored.
            }
          }
          continue;
        }

        try {
          processSectionData(currentSection, clean);
        } catch (Exception e) {
          System.err.println(
              "Format error at line "
                  + lineNum
                  + " ["
                  + currentSection
                  + "]: "
                  + e.getMessage());
          return;
        }
      }

      if (!validateSections()) {
        return;
      }

      loadedConfiguration = buildLoadedConfiguration();
      game.setHistory(new History(historyBuffer.toString()));

    } catch (IOException e) {
      System.err.println("Critical I/O error: " + e.getMessage());
    }
  }

  /**
   * Returns the loaded configuration.
   *
   * <p>Valid only after a successful call to {@link #loadGameData(String)}.
   *
   * @return the reconstructed configuration
   */
  public Configuration getLoadedConfiguration() {
    return loadedConfiguration;
  }

  /** Resets parser state before loading a new file. */
  private void resetState() {
    historyBuffer = new StringBuilder();
    gameSectionInitialized = false;
    currentBoardRow = 0;
    seenGame = false;
    seenSettings = false;
    seenHistory = false;
    loadedConfiguration = null;
    loadedStartingWhite = null;
    loadedBoardSize = 0;
    loadedBlitz = null;
    loadedDebug = null;
    loadedVerbose = null;
  }

  /**
   * Removes inline and block comments from a line.
   *
   * @param line the raw input line
   * @return the cleaned line
   */
  private String stripComments(String line) {
    line = line.replaceAll("\\{.*?\\}", "");
    int hashIndex = line.indexOf('#');
    if (hashIndex != -1) {
      line = line.substring(0, hashIndex);
    }
    return line.trim();
  }

  /**
   * Dispatches a line to the correct section parser.
   *
   * @param section the current section
   * @param data the cleaned line content
   * @throws Exception if the data is invalid
   */
  private void processSectionData(String section, String data) throws Exception {
    if (section == null || section.isEmpty()) {
      throw new Exception("Data found outside any section header.");
    }

    switch (section) {
      case "[settings]" -> {
        parseSetting(data);
      }
      case "[game]" -> {
        if (!gameSectionInitialized) {
          board.clearBoard();
          currentBoardRow = 0;
          gameSectionInitialized = true;
        }
        parseBoardLine(data);
      }
      case "[history]" -> {
        historyBuffer.append(data).append("\n");
      }
      default -> {
        // Unknown sections are ignored.
      }
    }
  }

  /**
   * Checks that all mandatory sections were found.
   *
   * @return {@code true} if the file is valid
   */
  private boolean validateSections() {
    if (!seenGame) {
      System.err.println("Format error: missing [game] section.");
      return false;
    }
    if (!seenSettings) {
      System.err.println("Format error: missing [settings] section.");
      return false;
    }
    if (!seenHistory) {
      System.err.println("Format error: missing [history] section.");
      return false;
    }
    if (gameSectionInitialized && currentBoardRow != board.getSizeBoard()) {
      System.err.println(
          "Format error: incomplete board - expected "
              + board.getSizeBoard()
              + " rows, got "
              + currentBoardRow
              + ".");
      return false;
    }
    return true;
  }

  /**
   * Parses one {@code key=value} line from the settings section.
   *
   * @param data the input line
   * @throws Exception if the format or value is invalid
   */
  private void parseSetting(String data) throws Exception {
    String[] parts = data.split("=", 2);
    if (parts.length < 2) {
      throw new Exception("Invalid key-value format (missing '=').");
    }

    String key = parts[0].trim();
    String value = parts[1].trim();

    switch (key) {
      case "starting-player" -> {
        if (value.equalsIgnoreCase("white")) {
          loadedStartingWhite = true;
          game.setWhiteTurn(true);
        } else if (value.equalsIgnoreCase("black")) {
          loadedStartingWhite = false;
          game.setWhiteTurn(false);
        } else {
          throw new Exception("Invalid starting player: '" + value + "'.");
        }
      }
      case "board-size" -> {
        int size = Integer.parseInt(value);
        loadedBoardSize = size;
        if (size != board.getSizeBoard()) {
          throw new Exception(
              "Board size mismatch: file has "
                  + size
                  + ", current board is "
                  + board.getSizeBoard()
                  + ".");
        }
      }
      case "time-mode" -> {
        if (value.equalsIgnoreCase("blitz")) {
          loadedBlitz = true;
        } else if (value.equalsIgnoreCase("classic")) {
          loadedBlitz = false;
        } else {
          throw new Exception("Invalid time-mode: '" + value + "'.");
        }
      }
      case "debug" -> {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
          loadedDebug = Boolean.valueOf(value);
        } else {
          throw new Exception(
              "Invalid debug value: '" + value + "' (expected true/false).");
        }
      }
      case "verbose" -> {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
          loadedVerbose = Boolean.valueOf(value);
        } else {
          throw new Exception(
              "Invalid verbose value: '" + value + "' (expected true/false).");
        }
      }
      default -> {
        // Unknown keys are ignored.
      }
    }
  }

  /**
   * Rebuilds one board row from an ASCII line.
   *
   * @param data one board row
   * @throws Exception if the row is invalid
   */
  private void parseBoardLine(String data) throws Exception {
    String cells = data.replace(" ", "");
    int n = board.getSizeBoard();

    if (currentBoardRow >= n) {
      throw new Exception("Too many board rows (expected " + n + ").");
    }
    if (cells.length() != n) {
      throw new Exception(
            "Board row must have " + n + " cells, got " + cells.length() + ".");
    }

    int boardRow = n - 1 - currentBoardRow;

    for (int col = 0; col < n; col++) {
      char c = cells.charAt(col);
      boolean playable = ((boardRow + col) % 2 == 0);

      if (!playable) {
        if (c != '_') {
          throw new Exception(
                "Piece '" + c + "' on non-playable square at row "
                      + currentBoardRow
                      + ", col "
                      + col
                      + ".");
        }
        continue;
      }

      if ("xoXO_".indexOf(c) == -1) {
        throw new Exception("Invalid board character: '" + c + "'.");
      }

      if (c != '_') {
        int index = (boardRow * n + col) / 2;
        switch (c) {
          case 'x' -> board.restorePiece(index, "BP");
          case 'o' -> board.restorePiece(index, "WP");
          case 'X' -> board.restorePiece(index, "BC");
          case 'O' -> board.restorePiece(index, "WC");
          default -> {
          }
        }
      }
    }

    currentBoardRow++;
  }
  /**
   * Builds a configuration from loaded values.
   *
   * <p>Missing values fall back to defaults.
   *
   * @return the reconstructed configuration
   */

  public Configuration buildLoadedConfiguration() {
    Configuration defaults = Configuration.getDefaultConfiguration();

    boolean blitz = loadedBlitz != null ? loadedBlitz : defaults.isBlitz();
    boolean verbose = loadedVerbose != null ? loadedVerbose : defaults.isVerbose();
    boolean debug = loadedDebug != null ? loadedDebug : defaults.isDebug();
    int size = loadedBoardSize > 0 ? loadedBoardSize : defaults.getSize();

    return new Configuration(
        blitz,
        defaults.getTime(),
        defaults.isContest(),
        size,
        verbose,
        debug,
        defaults.iswhiteAi(),
        defaults.isblackAi(),
        defaults.getAiTime());
  }
}