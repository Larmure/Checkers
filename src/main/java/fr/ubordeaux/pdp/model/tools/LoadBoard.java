package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Piece;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a saved game from a structured text file in a single file pass.
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
  private static final String SAVE_DIRECTORY = System.getProperty("user.dir")
      + File.separator + "Sauvegarde";

  /** Whether the [game] section was found. */
  private boolean seenGame = false;
  /** Whether the [settings] section was found. */
  private boolean seenSettings = false;
  /** Whether the [history] section was found. */
  private boolean seenHistory = false;

  /** The loaded configuration. */
  private Configuration loadedConfiguration = null;
  /** The loaded game. */
  private GameCheckers loadedGame = null;

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
  /** The loaded Time of AI. */
  private long loadedAiTime = 0;
  /** The loaded white AI flag. */
  private Boolean loadedWhiteAi = null;
  /** The loaded black AI flag. */
  private Boolean loadedBlackAi = null;
  /** The loaded white AI algorithm name. */
  private String loadedWhiteAiAlgorithm = null;
  /** The loaded black AI algorithm name. */
  private String loadedBlackAiAlgorithm = null;
  /**the loaded depth of AI.*/
  private int loadedAiDepth = 0;
  /** Buffered move history. */
  private StringBuilder historyBuffer = new StringBuilder();
  /** Buffered board lines from the [game] section. */
  private List<String> loadedBoardLines = new ArrayList<>();

  /** Creates a loader. */
  public LoadBoard() {
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
          resetState();
          return;
        }
      }

      if (!validateSections()) {
        resetState();
        return;
      }

      loadedConfiguration = buildLoadedConfiguration();
      loadedGame = new GameCheckers(loadedConfiguration);

      if (loadedStartingWhite != null) {
        loadedGame.setWhiteTurn(loadedStartingWhite);
      }

      Board board = loadedGame.getBoard();
      board.clearBoard();

      try {
        applyBufferedBoard(board);
      } catch (Exception e) {
        System.err.println("Loading error: " + e.getMessage());
        resetState();
        return;
      }

      loadedGame.setHistory(new History(historyBuffer.toString()));
      loadedGame.checkGameOver();

    } catch (IOException e) {
      System.err.println("Critical I/O error: " + e.getMessage());
      resetState();
    }
  }

  /**
   * Returns the reconstructed configuration.
   *
   * @return the loaded configuration, or {@code null} if loading failed
   */
  public Configuration getLoadedConfiguration() {
    return loadedConfiguration;
  }

  /**
   * Returns the reconstructed game.
   *
   * @return the loaded game, or {@code null} if loading failed
   */
  public GameCheckers getLoadedGame() {
    return loadedGame;
  }

  /** Resets parser state before loading a new file. */
  private void resetState() {
    seenGame = false;
    seenSettings = false;
    seenHistory = false;
    loadedConfiguration = null;
    loadedGame = null;
    loadedAiTime = 0;
    loadedStartingWhite = null;
    loadedBoardSize = 0;
    loadedBlitz = null;
    loadedDebug = null;
    loadedVerbose = null;
    loadedAiDepth = 0;
    loadedWhiteAi = null;
    loadedBlackAi = null;
    loadedWhiteAiAlgorithm = null;
    loadedBlackAiAlgorithm = null;
    historyBuffer = new StringBuilder();
    loadedBoardLines = new ArrayList<>();
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
      case "[settings]" -> parseSetting(data);
      case "[game]" -> loadedBoardLines.add(data);
      case "[history]" -> historyBuffer.append(data).append("\n");
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
    if (loadedBoardSize == 0) {
      System.err.println("Format error: missing or invalid board-size.");
      return false;
    }
    if (loadedBoardLines.size() != loadedBoardSize) {
      System.err.println(
          "Format error: incomplete board - expected "
              + loadedBoardSize
              + " rows, got "
              + loadedBoardLines.size()
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
        } else if (value.equalsIgnoreCase("black")) {
          loadedStartingWhite = false;
        } else {
          throw new Exception("Invalid starting player: '" + value + "'.");
        }
      }
      case "board-size" -> {
        int size = Integer.parseInt(value);
        if (size == 8 || size == 10 || size == 12) {
          loadedBoardSize = size;
        } else {
          throw new Exception("Invalid size");
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
      case "ai-mode" -> {
        if (value.equalsIgnoreCase("none")) {
          loadedWhiteAi = false;
          loadedBlackAi = false;
          loadedWhiteAiAlgorithm = null;
          loadedBlackAiAlgorithm = null;

        } else if (value.startsWith("white-")) {
          String[] tokens = value.split("-", 2);
          if (tokens.length != 2 || tokens[1].isBlank()) {
            throw new Exception("Invalid ai-mode format: '" + value + "'.");
          }

          loadedWhiteAi = true;
          loadedBlackAi = false;
          loadedBlackAiAlgorithm = null;

          if (tokens[1].equals("MinMax")) {
            loadedWhiteAiAlgorithm = "minimax";
          } else if (tokens[1].equals("MinMaxAlphaBeta")) {
            loadedWhiteAiAlgorithm = "alphabeta";
          } else if (tokens[1].equals("Mcts")) {
            loadedWhiteAiAlgorithm = "mcts";
          } else {
            throw new Exception("Unknown white AI algorithm: '" + tokens[1] + "'.");
          }

        } else if (value.startsWith("black-")) {
          String[] tokens = value.split("-", 2);
          if (tokens.length != 2 || tokens[1].isBlank()) {
            throw new Exception("Invalid ai-mode format: '" + value + "'.");
          }

          loadedWhiteAi = false;
          loadedBlackAi = true;
          loadedWhiteAiAlgorithm = null;

          if (tokens[1].equals("MinMax")) {
            loadedBlackAiAlgorithm = "minimax";
          } else if (tokens[1].equals("MinMaxAlphaBeta")) {
            loadedBlackAiAlgorithm = "alphabeta";
          } else if (tokens[1].equals("Mcts")) {
            loadedBlackAiAlgorithm = "mcts";
          } else {
            throw new Exception("Unknown black AI algorithm: '" + tokens[1] + "'.");
          }

        } else if (value.startsWith("both-")) {
          String[] tokens = value.split("-", 3);
          if (tokens.length != 3 || tokens[1].isBlank() || tokens[2].isBlank()) {
            throw new Exception("Invalid ai-mode format: '" + value + "'.");
          }

          loadedWhiteAi = true;
          loadedBlackAi = true;

          if (tokens[1].equals("MinMax")) {
            loadedWhiteAiAlgorithm = "minimax";
          } else if (tokens[1].equals("MinMaxAlphaBeta")) {
            loadedWhiteAiAlgorithm = "alphabeta";
          } else if (tokens[1].equals("Mcts")) {
            loadedWhiteAiAlgorithm = "mcts";
          } else {
            throw new Exception("Unknown white AI algorithm: '" + tokens[1] + "'.");
          }

          if (tokens[2].equals("MinMax")) {
            loadedBlackAiAlgorithm = "minimax";
          } else if (tokens[2].equals("MinMaxAlphaBeta")) {
            loadedBlackAiAlgorithm = "alphabeta";
          } else if (tokens[2].equals("Mcts")) {
            loadedBlackAiAlgorithm = "mcts";
          } else {
            throw new Exception("Unknown black AI algorithm: '" + tokens[2] + "'.");
          }

        } else {
          throw new Exception("Invalid ai-mode value: '" + value + "'.");
        }
      }

      case "ai-depth" -> {
        int depth = Integer.parseInt(value);
        if (depth <= 0) {
          throw new Exception("Invalid ai-depth: '" + value + "'.");
        }
        loadedAiDepth = depth;
      }
      case "ai-time" -> {
        long aiTime = Long.parseLong(value);
        if (aiTime <= 0) {
          throw new Exception("Invalid ai-time: '" + value + "'.");
        }
        loadedAiTime = aiTime;
      }

      default -> {
        // Unknown keys are ignored.
      }
    }
  }

  /**
   * Applies the buffered [game] lines to the given board.
   *
   * @param board the board to fill
   * @throws Exception if a buffered row is invalid
   */
  private void applyBufferedBoard(Board board) throws Exception {
    int n = loadedBoardSize;

    for (int rowIndex = 0; rowIndex < loadedBoardLines.size(); rowIndex++) {
      String data = loadedBoardLines.get(rowIndex);
      String cells = data.replace(" ", "");

      if (cells.length() != n) {
        throw new Exception(
            "Board row must have " + n + " cells, got " + cells.length() + ".");
      }

      int boardRow = n - 1 - rowIndex;

      for (int col = 0; col < n; col++) {
        char c = cells.charAt(col);
        boolean playable = ((boardRow + col) % 2 == 0);

        if (!playable) {
          if (c != '_') {
            throw new Exception(
                "Piece '" + c + "' on non-playable square at row "
                    + rowIndex
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
            case 'x' -> board.restorePiece(index, Piece.BLACK_PAWN);
            case 'o' -> board.restorePiece(index, Piece.WHITE_PAWN);
            case 'X' -> board.restorePiece(index, Piece.BLACK_CHECKER);
            case 'O' -> board.restorePiece(index, Piece.WHITE_CHECKER);
            default -> {
            }
          }
        }
      }
    }
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
    boolean whiteAi = loadedWhiteAi != null ? loadedWhiteAi : defaults.iswhiteAi();
    boolean blackAi = loadedBlackAi != null ? loadedBlackAi : defaults.isblackAi();
    int aiDepth = loadedAiDepth != 0 ? loadedAiDepth : defaults.getAiDepth();
    long aiTime = loadedAiTime != 0 ? loadedAiTime : defaults.getAiTime();

    String aiMode = defaults.getAiMode();
    if (loadedWhiteAiAlgorithm != null) {
      aiMode = loadedWhiteAiAlgorithm;
    } else if (loadedBlackAiAlgorithm != null) {
      aiMode = loadedBlackAiAlgorithm;
    }

    return new Configuration(
        blitz,
        defaults.getTime(),
        defaults.isContest(),
        size,
        verbose,
        debug,
        whiteAi,
        blackAi,
        aiTime,
        aiMode,
        aiDepth,
        defaults.getSelectionMode());
  }
}