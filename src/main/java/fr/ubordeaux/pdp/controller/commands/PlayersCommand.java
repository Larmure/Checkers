package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.GameRegistry;
import fr.ubordeaux.pdp.server.PlayerSession;

/**
 * Server management command: {@code players}.
 *
 * <p>Lists every player currently connected to the server, along with their unique ID,
 * display name, and status ({@code idle} or {@code ingame}).
 *
 * <p>Category: [NETWORK — SERVER MANAGEMENT] — reads {@link GameRegistry}.
 */
public class PlayersCommand implements Command, Helpable {

  private final GameRegistry registry;

  /**
   * Creates a players command.
   *
   * @param registry the shared player and session registry.
   */
  public PlayersCommand(GameRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void execute() {
    if (registry.getPlayerCount() == 0) {
      System.out.println("No players currently connected.");
      return;
    }

    System.out.printf("%-10s  %-15s  %s%n", "ID", "NAME", "STATUS");
    System.out.println("-".repeat(38));
    for (PlayerSession player : registry.getAllPlayers()) {
      System.out.printf(
            "%-10s  %-15s  %s%n",
            player.getId(),
            player.getName(),
            player.getStatus().name().toLowerCase());
    }
  }

  @Override
  public String getHelp() {
    return "players — Lists all connected players with their ID, name,"
          + " and status (idle or ingame).";
  }
}