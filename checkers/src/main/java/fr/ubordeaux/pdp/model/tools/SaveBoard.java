package fr.ubordeaux.pdp.model.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;

public class SaveBoard {

    private Board b;
    private final int n;
    private GameCheckers GH;
    private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";

    public SaveBoard(Board b, GameCheckers g) {
        this.b = b;
        this.n = b.getSizeBoard();
        this.GH = g;
    }

    /**
     * MAIN SAVE METHOD
     * F21: Mandatory order [settings] -> [game] -> [history]
     */
    public void saveToFile(String fileName) throws Exception{
        createSaveDirectory();
        File file = new File(saveDirectory + File.separator + fileName);
        Configuration config = GH.getConfiguration();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {

            // --- SECTION 1: [settings] (F23) ---
            writer.write("[settings] # Game configuration parameters\n");

            if (GH.isWhiteTurn()) {
                writer.write("starting-player=white # Can be black or white\n");
            } else {
                writer.write("starting-player=black # Can be black or white\n");
            }

            String timeMode;
            if (config.isBlitz()) {
                timeMode = "blitz";
            } else {
                timeMode = "classic";
            }
            writer.write("time-mode=" + timeMode + "\n");

            writer.write("ai-mode=None\n");
            writer.write("ai-depth=2\n");

            writer.write("debug=" + config.isDebug() + "\n");
            writer.write("board-size=" + b.getSizeBoard() + "\n");
            writer.write("\n");

            // --- SECTION 2: [game] (F21) ---
            writer.write("[game] # Current board state\n");
            writer.write(generateBoardString());
            writer.write("\n");

            // --- SECTION 3: [history] (F21) ---
            writer.write("[history] # Move history\n");
            writer.write(GH.getHistory().historyString());

            System.out.println("Save successful: " + file.getPath());

        }
    }

    /**
     * Generates the ASCII board string
     */
    public String generateBoardString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                if ((row + col) % 2 != 0) {
                    sb.append("-"); // Light squares
                } else {
                    int cellIndex = (row * n + col) / 2;
                    sb.append(getCharForCell(cellIndex));
                }
                if (col < n - 1)
                    sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Fills the board with pieces based on bitboards
     */
    private char getCharForCell(int index) {
        if (b.isBitBlackChecker(index)) {
            return 'X';
        }
        if (b.isBitWhiteChecker(index)) {
            return 'O';
        }
        if (b.isBitBlackPawn(index)) {
            return 'x';
        }
        if (b.isBitWhitePawn(index)) {
            return 'o';
        }
        return '-';
    }

    private void createSaveDirectory() {
        File dir = new File(saveDirectory);
        if (!dir.exists())
            dir.mkdirs();
    }
}