package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.GameRegistry;
import fr.ubordeaux.pdp.server.PlayerSession;
import java.util.Comparator;
import java.util.List;

/**
 * Server management command: {@code scoreboard}.
 *
 * <p>Displays win/loss/games-played statistics for all players who have completed at
 * least one game, sorted by number of victories in descending order.
 *
 * <p>Category: [NETWORK — SERVER MANAGEMENT] — reads {@link GameRegistry}.
 */
public class ScoreboardCommand implements Command, Helpable {

  private final GameRegistry registry;

  /**
   * Creates a scoreboard command.
   *
   * @param registry the shared player and session registry.
   */
  public ScoreboardCommand(GameRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void execute() {
    List<PlayerSession> ranked =
        registry.getAllPlayers().stream()
            .filter(p -> p.getGamesPlayed() > 0)
            .sorted(Comparator.comparingInt(PlayerSession::getWins).reversed())
            .toList();

    if (ranked.isEmpty()) {
      System.out.println("No games have been played yet.");
      return;
    }

    System.out.printf("%-4s  %-10s  %-15s  %5s  %5s  %5s%n",
        "RANK", "ID", "NAME", "WINS", "LOSS", "GAMES");
    System.out.println("-".repeat(53));

    int rank = 1;
    for (PlayerSession player : ranked) {
      System.out.printf(
          "%-4d  %-10s  %-15s  %5d  %5d  %5d%n",
          rank++,
          player.getId(),
          player.getName(),
          player.getWins(),
          player.getLosses(),
          player.getGamesPlayed());
    }
  }

  @Override
  public String getHelp() {
    return "scoreboard — Displays win/loss statistics for all players who have played,"
        + " sorted by victories.";
  }
}