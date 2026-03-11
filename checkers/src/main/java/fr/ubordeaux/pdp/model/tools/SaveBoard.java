package fr.ubordeaux.pdp.model.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;

public class SaveBoard {

    private Board b;
    private GameCheckers GH;
    private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";

    public SaveBoard(Board b, GameCheckers g) {
        this.b = b;
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
            writer.write(b.boardString());
            writer.write("\n");

            // --- SECTION 3: [history] (F21) ---
            writer.write("[history] # Move history\n");
            writer.write(GH.getHistory().historyString());

            System.out.println("Save successful: " + file.getPath());

        }
    }

    private void createSaveDirectory() {
        File dir = new File(saveDirectory);
        if (!dir.exists())
            dir.mkdirs();
    }
}