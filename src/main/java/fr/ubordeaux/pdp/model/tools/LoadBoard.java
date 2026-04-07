package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Piece;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

  /** One parsed logical line with its original source line. */
  private static class ParsedLine {
    private final int sourceLine;
    private final String text;

    ParsedLine(int sourceLine, String text) {
      this.sourceLine = sourceLine;
      this.text = text;
    }
  }

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
  private Integer loadedBoardSize = null;
  /** The loaded blitz mode flag. */
  private Boolean loadedBlitz = null;
  /** The loaded debug mode flag. */
  private Boolean loadedDebug = null;
  /** The loaded verbose mode flag. */
  private Boolean loadedVerbose = null;
  /** The loaded AI time. */
  private Long loadedAiTime = null;
  /** The loaded white AI flag. */
  private Boolean loadedWhiteAi = null;
  /** The loaded black AI flag. */
  private Boolean loadedBlackAi = null;
  /** The loaded AI algorithm name. */
  private String loadedAiMode = null;
  /** The loaded AI depth. */
  private Integer loadedAiDepth = null;

  /** Buffered move history. */
  private StringBuilder historyBuffer = new StringBuilder();
  /** Buffered board lines from the [game] section. */
  private List<String> loadedBoardLines = new ArrayList<>();
  private List<Integer> loadedBoardLineNumbers = new ArrayList<>();
  /** Line where the [game] section header was found. */
  private int gameSectionHeaderLine = -1;

  /** Creates a loader. */
  public LoadBoard() {
  }

  /**
   * Loads game data from a file in the save directory.
   *
   * @param fileName the save file name
   */
  public void loadGameData(String fileName) {
    Path path = Paths.get(SAVE_DIRECTORY, fileName);
    File file = path.toFile();

    resetState();

    if (!file.exists()) {
      failLoad("file not found at " + path);
      return;
    }

    try {
      String rawContent = Files.readString(path);
      List<ParsedLine> parsedLines = stripCommentsFromContent(rawContent);

      String currentSection = "";
      for (ParsedLine parsed : parsedLines) {
        int lineNum = parsed.sourceLine;
        String line = parsed.text;

        String clean = line.trim();

        if (clean.isEmpty()) {
          continue;
        }

        if (clean.startsWith("[") && clean.endsWith("]")) {
          currentSection = clean.toLowerCase();
          switch (currentSection) {
            case "[settings]" -> seenSettings = true;
            case "[game]" -> {
              seenGame = true;
              gameSectionHeaderLine = lineNum;
            }
            case "[history]" -> seenHistory = true;
            default -> {
              // Unknown sections are ignored.
            }
          }
          continue;
        }

        try {
          processSectionData(currentSection, clean, lineNum);
        } catch (Exception e) {
          failLoadAtLine(lineNum,
              "format error [" + currentSection + "]: " + e.getMessage());
          return;
        }
      }

      if (!validateSections()) {
        return;
      }

      loadedConfiguration = buildLoadedConfiguration();
      loadedGame = new GameCheckers(loadedConfiguration);

      if (loadedStartingWhite != null) {
        loadedGame.setWhiteTurn(loadedStartingWhite);
      }

      Board board = loadedGame.getBoard();
      board.clearBoard();

      applyBufferedBoard(board);

      loadedGame.setHistory(new History(historyBuffer.toString()));
      loadedGame.checkGameOver();

    } catch (IllegalStateException e) {
      if ("LOAD_ABORTED".equals(e.getMessage())) {
        throw e;
      }
      failLoad("critical loading state error: " + e.getMessage());
    } catch (Exception e) {
      failLoad("critical I/O error: " + e.getMessage());
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
    loadedAiTime = null;
    loadedStartingWhite = null;
    loadedBoardSize = null;
    loadedBlitz = null;
    loadedDebug = null;
    loadedVerbose = null;
    loadedAiDepth = null;
    loadedWhiteAi = null;
    loadedBlackAi = null;
    loadedAiMode = null;
    historyBuffer = new StringBuilder();
    loadedBoardLines = new ArrayList<>();
    loadedBoardLineNumbers = new ArrayList<>();
    gameSectionHeaderLine = -1;
  }

  /**
   * Exits the program on load error.
   *
   * <p>Kept protected so tests can override it.
   */
  protected void exitOnLoadError() {
    System.exit(1);
  }

  /**
   * Prints an error, resets state, and stops execution.
   *
   * @param message the error message
   */
  private void failLoad(String message) {
    System.err.println("[LOAD ERROR] " + message);
    resetState();
    exitOnLoadError();
  }

  /**
   * Prints an error with its source line, resets state, and stops execution.
   *
   * @param lineNum source line number in the save file
   * @param message error message
   */
  private void failLoadAtLine(int lineNum, String message) {
    System.err.println("[LOAD ERROR] line " + lineNum + ": " + message);
    resetState();
    exitOnLoadError();
  }

  /**
   * Prints a warning for a missing optional setting.
   *
   * @param key the missing setting key
   */
  private void warnMissingSetting(String key) {
    System.err.println("Warning: missing setting '" + key + "'. Default value will be used.");
  }

  /**
   * Removes inline and block comments from the whole file content.
   *
   * <p>Block comments delimited by '{' and '}' may span multiple lines.
   * Newlines inside such comments are removed as part of the comment block,
   * allowing values split by a comment to be reconstructed.
   *
   * @param content raw file content
   * @return parsed logical lines with their original source line numbers
   * @throws Exception if comment delimiters are invalid
   */
  private List<ParsedLine> stripCommentsFromContent(String content) throws Exception {
    List<ParsedLine> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inBlockComment = false;
    boolean inInlineComment = false;
    int blockStartLine = -1;
    int lineNum = 1;
    int logicalLineStart = 1;

    for (int i = 0; i < content.length(); i++) {
      char c = content.charAt(i);

      if (c == '\n') {
        if (inInlineComment) {
          inInlineComment = false;
        }

        if (!inBlockComment) {
          lines.add(new ParsedLine(logicalLineStart, current.toString()));
          current.setLength(0);
          logicalLineStart = lineNum + 1;
        }
        lineNum++;
        continue;
      }

      if (inInlineComment) {
        continue;
      }

      if (inBlockComment) {
        if (c == '}') {
          inBlockComment = false;
          blockStartLine = -1;
        }
        continue;
      }

      if (c == '#') {
        inInlineComment = true;
        continue;
      }

      if (c == '{') {
        inBlockComment = true;
        blockStartLine = lineNum;
        continue;
      }

      if (c == '}') {
        throw new Exception("line " + lineNum + ": unexpected '}' without matching '{'.");
      }

      current.append(c);
    }

    if (inBlockComment) {
      throw new Exception("format error: unclosed block comment starting at line "
          + blockStartLine + ".");
    }

    if (inInlineComment) {
      inInlineComment = false;
    }

    if (current.length() > 0) {
      lines.add(new ParsedLine(logicalLineStart, current.toString()));
    }

    return lines;
  }

  /**
   * Dispatches a line to the correct section parser.
   *
   * @param section the current section
   * @param data the cleaned line content
   * @throws Exception if the data is invalid
   */
  private void processSectionData(String section, String data, int lineNum) throws Exception {
    if (section == null || section.isEmpty()) {
      throw new Exception("Data found outside any section header.");
    }

    switch (section) {
      case "[settings]" -> parseSetting(data);
      case "[game]" -> {
        loadedBoardLines.add(data);
        loadedBoardLineNumbers.add(lineNum);
      }
      case "[history]" -> historyBuffer.append(data).append("\n");
      default -> {
        // Unknown sections are ignored.
      }
    }
  }

  /**
   * Checks whether at least one AI player is active.
   *
   * @return {@code true} if white or black is controlled by an AI
   */
  private boolean hasActiveAi() {
    return Boolean.TRUE.equals(loadedWhiteAi) || Boolean.TRUE.equals(loadedBlackAi);
  }

  /**
   * Checks the validity of the loaded sections and settings.
   *
   * @return {@code true} if the file is valid
   */
  private boolean validateSections() {
    if (!seenGame) {
      failLoad("format error: missing [game] section.");
      return false;
    }
    if (!seenSettings) {
      failLoad("format error: missing [settings] section.");
      return false;
    }
    if (!seenHistory) {
      failLoad("format error: missing [history] section.");
      return false;
    }
    if (loadedBoardSize == null) {
      failLoad("format error: missing or invalid board-size.");
      return false;
    }

    // Report the first malformed board row before reporting missing/extra row count.
    for (int i = 0; i < loadedBoardLines.size(); i++) {
      String[] cells = loadedBoardLines.get(i).trim().split("\\s+");
      if (cells.length != loadedBoardSize) {
        int rowLine = loadedBoardLineNumbers.get(i);
        failLoadAtLine(rowLine,
            "board row must have " + loadedBoardSize + " cells, got " + cells.length + ".");
        return false;
      }
    }

    if (loadedBoardLines.size() != loadedBoardSize) {
      if (loadedBoardLines.size() < loadedBoardSize) {
        int missingFromLine;
        if (!loadedBoardLineNumbers.isEmpty()) {
          missingFromLine = loadedBoardLineNumbers.get(loadedBoardLineNumbers.size() - 1) + 1;
        } else {
          missingFromLine = gameSectionHeaderLine + 1;
        }
        failLoadAtLine(missingFromLine,
            "incomplete board - expected "
                + loadedBoardSize
                + " rows, got "
                + loadedBoardLines.size()
                + ".");
      } else {
        int firstUnexpectedLine = loadedBoardLineNumbers.get(loadedBoardSize);
        failLoadAtLine(firstUnexpectedLine,
            "too many board rows - expected "
                + loadedBoardSize
                + " rows, got "
                + loadedBoardLines.size()
                + ".");
      }
      return false;
    }

    if (hasActiveAi()) {
      if (loadedAiMode == null || loadedAiMode.equals("none")) {
        failLoad("format error: active AI requires a valid ai-mode.");
        return false;
      }
      if (loadedAiDepth == null || loadedAiDepth <= 0) {
        failLoad("format error: active AI requires ai-depth > 0.");
        return false;
      }
      if (loadedAiTime == null || loadedAiTime <= 0) {
        failLoad("format error: active AI requires ai-time > 0.");
        return false;
      }
    }

    if (loadedStartingWhite == null) {
      warnMissingSetting("starting-player");
    }

    if (loadedBlitz == null) {
      warnMissingSetting("time-mode");
    }

    if (loadedDebug == null) {
      warnMissingSetting("debug");
    }

    if (loadedVerbose == null) {
      warnMissingSetting("verbose");
    }

    if (loadedAiMode == null) {
      warnMissingSetting("ai-mode");
    }

    if (loadedAiDepth == null) {
      warnMissingSetting("ai-depth");
    }

    if (loadedAiTime == null) {
      warnMissingSetting("ai-time");
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
          throw new Exception("Invalid size: '" + value + "'.");
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
          loadedAiMode = "minimax";

        } else if (value.startsWith("white-")) {
          String algo = normalizeAiAlgorithm(value.substring("white-".length()));
          loadedWhiteAi = true;
          loadedBlackAi = false;
          loadedAiMode = algo;

        } else if (value.startsWith("black-")) {
          String algo = normalizeAiAlgorithm(value.substring("black-".length()));
          loadedWhiteAi = false;
          loadedBlackAi = true;
          loadedAiMode = algo;

        } else {
          throw new Exception("Invalid ai-mode value: '" + value + "'.");
        }
      }
      case "ai-depth" -> {
        int depth = Integer.parseInt(value);
        if (depth < 0 || depth >= 15) {
          throw new Exception("Invalid ai-depth: '" + value + "'.");
        }
        loadedAiDepth = depth;
      }
      case "ai-time" -> {
        long aiTime = Long.parseLong(value);
        if (aiTime <= Ai.MIN_TIME_MS || aiTime > Ai.MAX_TIME_MS) {
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
   * Normalizes supported AI algorithm names.
   *
   * @param raw the raw name from the file
   * @return the normalized algorithm name
   * @throws Exception if the algorithm is not supported
   */
  private String normalizeAiAlgorithm(String raw) throws Exception {
    String algo = raw.trim().toLowerCase();

    if (algo.equals("minimax") || algo.equals("minmax")) {
      return "minimax";
    } else if (algo.equals("alphabeta")
        || algo.equals("minmaxalphabeta")
        || algo.equals("alpha-beta")) {
      return "alphabeta";
    } else if (algo.equals("mcts")) {
      return "mcts";
    } else {
      throw new Exception("Unknown AI algorithm: '" + raw + "'.");
    }
  }

  /**
   * Applies the buffered [game] lines to the given board.
   *
   * @param board the board to fill
   */
  private void applyBufferedBoard(Board board) {
    int n = loadedBoardSize;

    for (int rowIndex = 0; rowIndex < loadedBoardLines.size(); rowIndex++) {
      String data = loadedBoardLines.get(rowIndex).trim();
      int fileLine = loadedBoardLineNumbers.get(rowIndex);

      String[] cells = data.split("\\s+");

      if (cells.length != n) {
        failLoadAtLine(fileLine, "board row must have " + n + " cells, got "
            + cells.length + ".");
        return;
      }

      int boardRow = n - 1 - rowIndex;

      for (int col = 0; col < n; col++) {
        String token = cells[col];
        String square = toSquare(boardRow, col);

        if (token.length() != 1) {
          failLoadAtLine(fileLine, "invalid board token '" + token
              + "' at square " + square + ".");
          return;
        }

        char c = token.charAt(0);
        boolean playable = ((boardRow + col) % 2 == 0);

        if ("xoXO_".indexOf(c) == -1) {
          failLoadAtLine(fileLine, "invalid board character '" + c
              + "' at square " + square + ".");
          return;
        }

        if (!playable) {
          if (c != '_') {
            failLoadAtLine(fileLine, "piece '" + c
                + "' on non-playable square " + square + ".");
            return;
          }
          continue;
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

  private String toSquare(int boardRow, int col) {
    return "" + (char) ('A' + boardRow) + (col + 1);
  }

  /**
   * Builds a configuration from loaded values.
   *
   * <p>Missing non-critical values fall back to defaults.
   *
   * @return the reconstructed configuration
   */
  public Configuration buildLoadedConfiguration() {
    Configuration defaults = Configuration.getDefaultConfiguration();

    boolean blitz = loadedBlitz != null ? loadedBlitz : defaults.isBlitz();
    boolean verbose = loadedVerbose != null ? loadedVerbose : defaults.isVerbose();
    boolean debug = loadedDebug != null ? loadedDebug : defaults.isDebug();
    int size = loadedBoardSize != null ? loadedBoardSize : defaults.getSize();
    boolean whiteAi = loadedWhiteAi != null ? loadedWhiteAi : defaults.iswhiteAi();
    boolean blackAi = loadedBlackAi != null ? loadedBlackAi : defaults.isblackAi();
    int aiDepth = loadedAiDepth != null ? loadedAiDepth : defaults.getAiDepth();
    long aiTime = loadedAiTime != null ? loadedAiTime : defaults.getAiTime();
    String aiMode = loadedAiMode != null ? loadedAiMode : defaults.getAiMode();
    String whiteAiMode = whiteAi ? aiMode : defaults.getWhiteAiMode();
    String blackAiMode = blackAi ? aiMode : defaults.getBlackAiMode();

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
        whiteAiMode,
        blackAiMode,
        aiDepth,
        defaults.getSelectionMode());
  }
}