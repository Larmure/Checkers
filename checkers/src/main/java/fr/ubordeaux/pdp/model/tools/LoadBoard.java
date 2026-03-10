package fr.ubordeaux.pdp.model.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.GameCheckers;

public class LoadBoard {

    private Board board;
    private GameCheckers game;
    private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";
    private int currentBoardRow = 0;
    private boolean gameSectionInitialized = false;
    private boolean seenGame = false;
    private boolean seenSettings = false;
    private boolean seenHistory = false;
    private StringBuilder historyBuffer = new StringBuilder();
    private long wp1, wp2, bp1, bp2, wc1, wc2, bc1, bc2;

    public LoadBoard(Board board, GameCheckers game) {
        this.board = board;
        this.game = game;
    }

    /**
     * F22: Removes inline (#) and block ({}) comments.
     */
    private String stripComments(String line) {
        // Remove block comments { ... }
        line = line.replaceAll("\\{.*?\\}", "");
        // Remove inline comments # ...
        int hashIndex = line.indexOf('#');
        if (hashIndex != -1) {
            line = line.substring(0, hashIndex);
        }
        return line.trim();
    }

    /**
     * F21: Loads the file and handles sections [settings], [game], [history].
     */
    public void loadFromFile(String fileName) {
        Path path = Paths.get(saveDirectory, fileName);
        File file = path.toFile();
        this.historyBuffer = new StringBuilder();
        // Reset state for this load
        this.gameSectionInitialized = false;
        this.currentBoardRow = 0;
        this.seenGame = false;
        this.seenSettings = false;
        this.seenHistory = false;

        if (!file.exists()) {
            System.err.println("Loading Error: File not found at " + path);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentSection = "";
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                String cleanLine = stripComments(line);
                if (cleanLine.isEmpty())
                    continue;

                // Detect Section Headers
                if (cleanLine.startsWith("[") && cleanLine.endsWith("]")) {
                    currentSection = cleanLine.toLowerCase();
                    if (currentSection.equals("[history]")) {
                        seenHistory = true;
                    }
                    continue;
                }

                try {
                    processSectionData(currentSection, cleanLine);
                } catch (Exception e) {
                    System.err.println(
                            "Format Error at line " + lineNum + " [" + currentSection + "]: " + e.getMessage());
                    return; // stop loading on first error
                }
            }

            // --- Final validations (tolerant mode) ---
            if (!seenGame) {
                System.err.println("Format Error: Missing [game] section.");
                return;
            }
            if (!seenSettings) {
                System.err.println("Format Error: Missing [game] section.");
                return;
            }
            if (!seenHistory) {
                System.err.println("Format Error: Missing [history] section.");
                return;
            }

            if (gameSectionInitialized && currentBoardRow != board.getSizeBoard()) {
                System.err.println("Format Error: Missing board rows in [game] section. Expected "
                        + board.getSizeBoard() + ", got " + currentBoardRow + ".");
                return;
            }

            game.setHistory(new History(historyBuffer.toString()));
            System.out.println("Game successfully restored from: " + fileName);

        } catch (IOException e) {
            System.err.println("Critical IO Error: " + e.getMessage());
        }
    }

    /**
     * Dispatches data to specific parsers.
     */
    private void processSectionData(String section, String data) throws Exception {
        if (section == null || section.isEmpty()) {
            throw new Exception("Data found outside any section header");
        }
        switch (section) {
            case "[settings]" -> {
                seenSettings = true;
                parseSetting(data);
            }
            case "[game]" -> {
                seenGame = true;
                if (!gameSectionInitialized) {
                    resetBoardBitboards();
                    wp1 = wp2 = bp1 = bp2 = wc1 = wc2 = bc1 = bc2 = 0L;
                    currentBoardRow = 0;
                    gameSectionInitialized = true;
                }
                parseBoardLine(data);
            }
            case "[history]" -> {
                seenHistory = true;
                historyBuffer.append(data).append("\n");
            }
            default -> {
                return;
            }

        }
        // parseHistory(data);
    }

    private void resetBoardBitboards() {
        board.clearBoard();
    }

    /**
     * F23: Parses key-value pairs for settings.
     */
    private void parseSetting(String data) throws Exception {
        String[] parts = data.split("=", 2);
        if (parts.length < 2) {
            throw new Exception("Invalid key-value format (missing '=')");
        }
        String key = parts[0].trim();
        String value = parts[1].trim();

        switch (key) {
            case "starting-player" -> {
                if (value.equalsIgnoreCase("white")) {
                    game.setWhiteTurn(true);
                } else if (value.equalsIgnoreCase("black")) {
                    game.setWhiteTurn(false);
                } else {
                    // F21: Robustness - if the value is wrong, we report it
                    throw new Exception("Invalid player color: " + value);
                }
            }
            case "board-size" -> {
                int size = Integer.parseInt(value);
                if (size != board.getSizeBoard())
                    throw new Exception("Board size mismatch");
            }
            case "time-mode" -> {
                //
            }
            case "debug" -> {
                //
            }
            default -> {

            }

        }
    }

    /**
     * Reconstructs the board bitboards from ASCII characters.
     */
    private void parseBoardLine(String data) throws Exception {
        String cells = data.replace(" ", "");
        int n = board.getSizeBoard();

        if (currentBoardRow >= n) {
            throw new Exception("Too many board rows (expected " + n + ")");
        }

        if (cells.length() != n) {
            throw new Exception("Board line must have " + n + " cells, got " + cells.length());
        }

        for (int col = 0; col < n; col++) {
            char c = cells.charAt(col);
            boolean playable = ((currentBoardRow + col) % 2 == 0);

            if (!playable) {
                if (c != '-') {
                    throw new Exception("Piece '" + c + "' on non-playable square at row "
                            + currentBoardRow + ", col " + col);
                }
                continue;
            }

            if ("xoXO-".indexOf(c) == -1) {
                throw new Exception("Invalid board character: '" + c + "'");
            }

            if (c != '-') {
                int index = (currentBoardRow * n + col) / 2;
                updateBitboard(c, index);
            }
        }

        currentBoardRow++;
    }

    private void updateBitboard(char c, int index) {
        long mask = 1L << (index % 64);
        boolean part2 = index >= 64;

        switch (c) {
            case 'x' -> {
                if (part2) {
                    bp2 |= mask;
                } else {
                    bp1 |= mask;
                }
            }
            case 'o' -> {
                if (part2) {
                    wp2 |= mask;
                } else {
                    wp1 |= mask;
                }
            }
            case 'X' -> {
                if (part2) {
                    bc2 |= mask;
                } else {
                    bc1 |= mask;
                }
            }
            case 'O' -> {
                if (part2) {
                    wc2 |= mask;
                } else {
                    wc1 |= mask;
                }
            }
            case '-' -> {
                // no-op
            }
            default -> throw new IllegalArgumentException("Unknown piece char: " + c);
        }
    }
}