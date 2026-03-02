package fr.ubordeaux.pdp.controller.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.BoardChargement;
import fr.ubordeaux.pdp.model.Configuration;
import fr.ubordeaux.pdp.model.GameCheckers;
import fr.ubordeaux.pdp.model.Utils;

public class LoadCommand implements Command, Helpable {

  private final GameController controller;
  private final String[] args;

  private final String saveDirectory = System.getProperty("user.dir") + File.separator + "Sauvegarde";

  public LoadCommand(GameController controller, String[] args) {
    this.controller = controller;
    this.args = args;
  }

  @Override
  public void execute() {
    if (args == null || args.length == 0) {
      System.out.println(getHelp());
      return;
    }

    String fileName = args[0].trim();
    if (fileName.isEmpty()) {
      System.out.println(getHelp());
      return;
    }

    Path path = Paths.get(saveDirectory, fileName);
    File file = path.toFile();
    if (!file.exists()) {
      System.out.println("Loading Error: File not found: " + path);
      return;
    }

    // ---- Read settings first to build Configuration ----
    Integer size = null;
    boolean blitz = Utils.DEFAULT_BLITZ;
    int time = Utils.DEFAULT_TIME;
    boolean contest = Utils.DEFAULT_CONTEST;
    boolean debug = Utils.DEFAULT_DEBUG;
    boolean isWhiteIsAI = Utils.DEFAULT_WHITE_AI;
    boolean isBlackIsAI = Utils.DEFAULT_BLACK_AI;

    // verbose vient du controller (param runtime)
    boolean verbose = controller.isVerbose();

    String section = "";

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        line = stripComments(line);
        if (line.isEmpty()) continue;

        if (line.startsWith("[") && line.endsWith("]")) {
          section = line.toLowerCase();
          // on s'arrête après settings dès qu'on passe à game/history
          if (!section.equals("[settings]") && size != null) break;
          continue;
        }

        if (!section.equals("[settings]")) continue;

        String[] parts = line.split("=", 2);
        if (parts.length < 2) continue;

        String key = parts[0].trim().toLowerCase();
        String value = parts[1].trim();

        switch (key) {
            case "board-size" -> size = Integer.parseInt(value);
            case "time-mode"  -> blitz = value.equalsIgnoreCase("blitz");
            case "debug"      -> debug = Boolean.parseBoolean(value);

            default -> {
                // ignore starting-player, ai-mode, ai-depth, etc.
            }
        }
      }
    } catch (Exception e) {
      System.out.println("Loading Error: cannot read settings: " + e.getMessage());
      return;
    }

    if (size == null) {
      System.out.println("Format Error: Missing board-size in [settings].");
      return;
    }

    Configuration cfg = new Configuration(blitz, time, contest, size, verbose, debug, isWhiteIsAI, isBlackIsAI);

    // ---- Create a new game with correct board size ----
    GameCheckers loadedGame = new GameCheckers(cfg);

    // ---- Load board state into that game ----
    new BoardChargement(loadedGame.getBoard(), loadedGame).loadFromFile(fileName);

    // ---- Bind to controller & view ----
    controller.setGame(loadedGame, cfg);

    System.out.println("Game loaded: " + fileName);
  }

  private String stripComments(String line) {
    line = line.replaceAll("\\{.*?\\}", "");
    int hashIndex = line.indexOf('#');
    if (hashIndex != -1) line = line.substring(0, hashIndex);
    return line.trim();
  }

  @Override
  public String getHelp() {
    return "load FILE : Load a game from a file.\nExample: load mygame.txt";
  }
}