package fr.ubordeaux.pdp.model.tools;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Saves the current game state to a structured text file.
 *
 * <p>The output file follows a mandatory three-section order (F21):
 *
 * <ol>
 *   <li>{@code [settings]} - Game configuration key-value pairs (F23).</li>
 *   <li>{@code [game]} - ASCII board layout.</li>
 *   <li>{@code [history]} - Move history in algebraic notation.</li>
 * </ol>
 *
 * <p>Files are written to the {@code Sauvegarde/} directory under the working
 * directory.
 */
public class SaveBoard {

  private static final String SAVE_DIRECTORY =
        System.getProperty("user.dir") + File.separator + "Sauvegarde";

  private final Board board;
  private final GameCheckers game;

  /**
   * Creates a save helper for the current game.
   *
   * @param board the board to save
   * @param game the game instance to save
   */
  public SaveBoard(Board board, GameCheckers game) {
    this.board = board;
    this.game = game;
  }

  /**
   * Writes the current game state to a file inside the save directory.
   *
   * <p>Sections are written in mandatory order: {@code [settings]},
   * {@code [game]}, then {@code [history]}. Any existing file with the same
   * name is overwritten.
   *
   * @param fileName name of the destination file
   * @throws IOException if the file cannot be created or written
   */
  public void saveToFile(String fileName) throws IOException {
    createSaveDirectory();

    File file = new File(SAVE_DIRECTORY + File.separator + fileName);
    Configuration config = game.getConfiguration();

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
      writeSettings(writer, config);
      writeBoard(writer);
      writeHistory(writer);
    }

    System.out.println("Save successful: " + file.getPath());
  }

  /**
   * Writes the {@code [settings]} section with all configuration key-value
   * pairs.
   *
   * @param writer active writer for the save file
   * @param config current game configuration
   * @throws IOException if the write fails
   */
  private void writeSettings(BufferedWriter writer, Configuration config)
        throws IOException {
    writer.write("[settings] # Game configuration parameters\n");

    String startingPlayer = game.isWhiteTurn() ? "white" : "black";
    writer.write("starting-player=" + startingPlayer + " # Can be white or black\n");

    String timeMode = config.isBlitz() ? "blitz" : "classic";
    writer.write("time-mode=" + timeMode + "\n");

    writer.write("ai-mode=None\n");
    writer.write("ai-depth=2\n");
    writer.write("verbose=" + config.isVerbose() + "\n");
    writer.write("debug=" + config.isDebug() + "\n");
    writer.write("board-size=" + board.getSizeBoard() + "\n");
    writer.write("\n");
  }

  /**
   * Writes the {@code [game]} section with the current ASCII board layout.
   *
   * @param writer active writer for the save file
   * @throws IOException if the write fails
   */
  private void writeBoard(BufferedWriter writer) throws IOException {
    writer.write("[game] # Current board state\n");
    writer.write(board.boardString());
    writer.write("\n");
  }

  /**
   * Writes the {@code [history]} section with the full move history.
   *
   * @param writer active writer for the save file
   * @throws IOException if the write fails
   */
  private void writeHistory(BufferedWriter writer) throws IOException {
    writer.write("[history] # Move history\n");
    writer.write(game.getHistory().historyString());
  }

  /** Creates the save directory if it does not already exist. */
  private void createSaveDirectory() {
    File dir = new File(SAVE_DIRECTORY);
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }
}