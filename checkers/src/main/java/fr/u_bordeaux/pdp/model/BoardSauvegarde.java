package fr.u_bordeaux.pdp.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BoardSauvegarde {

    private  Board b;
    private final int n;

    // Répertoire de sauvegarde
    private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";


    public BoardSauvegarde(Board b) {
        this.b = b;
        this.n = b.getSizeBoard();

    }

    /* ===================== */
    /* === SAUVEGARDE ====== */
    /* ===================== */

    /**
     * Génère la chaîne représentant le plateau
     */
    public String generateBoardString() {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {

                if ((row + col) % 2 == 0) {
                    sb.append("-");
                } else {
                    int cellIndex = (row * n + col) / 2;
                    sb.append(getCharForCell(cellIndex));
                }

                if (col < n - 1) sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private char getCharForCell(int index) {
        long mask = 1L << (index % 64);
        boolean part2 = index >= 64;

        if (part2) {
            //if ((b.getBlackCheckers2() & mask) != 0) return 'X';
            //if ((b.getWhiteCheckers2() & mask) != 0) return 'O';
            if ((b.getBlackPawns2() & mask) != 0) return 'x';
            if ((b.getWhitePawns2() & mask) != 0) return 'o';
        } else {
            //if ((b.getBlackCheckers1() & mask) != 0) return 'X';
            //if ((b.getWhiteCheckers1() & mask) != 0) return 'O';
            if ((b.getBlackPawns1() & mask) != 0) return 'x';
            if ((b.getWhitePawns1() & mask) != 0) return 'o';
        }

        return '-';
    }

    /**
     * Sauvegarde dans un fichier (écrase si existe)
     */
    public void saveToFile(String fileName) {
        File file = new File(saveDirectory + File.separator + fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(generateBoardString());
            System.out.println("Sauvegarde réussie : " + file.getPath());
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde : " + e.getMessage());
        }
    }
    private void createSaveDirectory() {
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs(); // OBLIGATOIRE
        }
    }


}




