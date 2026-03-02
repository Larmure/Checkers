package fr.ubordeaux.pdp.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class BoardChargement {

    private Board board;
    private GameCheckers game;
    private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";
    private int currentBoardRow = 0;
    private boolean gameSectionInitialized = false;
    private boolean  seenGame=false;
    private boolean seenSettings=false;
    private boolean seenHistoriy=false;

    public BoardChargement(Board board, GameCheckers game) {
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

    // Reset state for this load
    this.gameSectionInitialized = false;
    this.currentBoardRow = 0;
    this.seenGame = false;
    this.seenSettings = false;
    this.seenHistoriy = false;

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
            if (cleanLine.isEmpty()) continue;

            // Detect Section Headers
            if (cleanLine.startsWith("[") && cleanLine.endsWith("]")) {
                currentSection = cleanLine.toLowerCase();
                continue;
            }

            try {
                processSectionData(currentSection, cleanLine);
            } catch (Exception e) {
                System.err.println(
                        "Format Error at line " + lineNum + " [" + currentSection + "]: " + e.getMessage()
                );
                return; // stop loading on first error
            }
        }

        // --- Final validations (tolerant mode) ---
        if (!seenGame) {
            System.err.println("Format Error: Missing [game] section.");
            return;
        }
        if (!seenGame) {
            System.err.println("Format Error: Missing [game] section.");
            return;
        }
        if (!seenHistoriy) {
            System.err.println("Format Error: Missing [history] section.");
            return;
        }


        if (gameSectionInitialized && currentBoardRow != board.getSizeBoard()) {
            System.err.println("Format Error: Missing board rows in [game] section. Expected "
                    + board.getSizeBoard() + ", got " + currentBoardRow + ".");
            return;
        }

     

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
                seenSettings=true;
                parseSetting(data);
            }
            case "[game]" -> {
                seenGame=true;
                if (!gameSectionInitialized) {
                    resetBoardBitboards();
                    currentBoardRow = 0;
                    gameSectionInitialized = true;
                }
                parseBoardLine(data);
            }
            case "[history]" -> {
                seenHistoriy=true;
            }
            default -> {
                return;
            }

        }
        // parseHistory(data);
            }

    private void resetBoardBitboards() {
        board.setWhitePawns1(0L);
        board.setWhitePawns2(0L);
        board.setBlackPawns1(0L);
        board.setBlackPawns2(0L);

        board.setWhiteCheckers1(0L);
        board.setWhiteCheckers2(0L);
        board.setBlackCheckers1(0L);
        board.setBlackCheckers2(0L);
    }


    /**
     * F23: Parses key-value pairs for settings.
     */
    private void parseSetting(String data) throws Exception {
        String[] parts = data.split("=", 2);
        if (parts.length < 2){
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
                if (size != board.getSizeBoard()) throw new Exception("Board size mismatch");
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

            // Case non jouable
            if (!playable) {
                if (c != '-') {
                    throw new Exception("Piece '" + c + "' on non-playable square at row "
                            + currentBoardRow + ", col " + col);
                }
                continue;
            }

            // Case jouable
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
            if (part2) board.setBlackPawns2(board.getBlackPawns2() | mask);
            else board.setBlackPawns1(board.getBlackPawns1() | mask);
            }

        case 'o' -> {
            if (part2) board.setWhitePawns2(board.getWhitePawns2() | mask);
            else board.setWhitePawns1(board.getWhitePawns1() | mask);
            }

        case 'X' -> {
            if (part2) board.setBlackCheckers2(board.getBlackCheckers2() | mask);
            else board.setBlackCheckers1(board.getBlackCheckers1() | mask);
            }

        case 'O' -> {
            if (part2) board.setWhiteCheckers2(board.getWhiteCheckers2() | mask);
            else board.setWhiteCheckers1(board.getWhiteCheckers1() | mask);
            }

        case '-' -> {
            }

        default -> // caractère inconnu => format invalide
            throw new IllegalArgumentException("Unknown piece char: " + c);
    }
        // vide : ne rien faire
        }
}