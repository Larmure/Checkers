package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ServerListService;
import java.util.List;

/**
 * Network command: {@code server list}.
 *
 * <p>Listens for UDP broadcasts for 30 seconds and displays active servers
 * whose last message was received less than 30 seconds ago.
 */
public class ServerListCommand implements Command, Helpable {

  @Override
  public void execute() {
    List<String> servers = ServerListService.discoverServers();

    if (servers.isEmpty()) {
      System.out.println("No game servers found on the network.");
      return;
    }

    System.out.println("\n╔════════════════════════════════════════╗");
    System.out.println("║   Available Game Servers               ║");
    System.out.println("╠════════════════════════════════════════╣");
    for (int i = 0; i < servers.size(); i++) {
      String[] parts = servers.get(i).split(":");
      String name = parts.length > 0 ? parts[0] : "Unknown";
      String ip = parts.length > 1 ? parts[1] : "?";
      String port = parts.length > 2 ? parts[2] : "?";
      System.out.printf("║ %d. %-15s %s:%-6s ║%n", i + 1, name, ip, port);
    }
    System.out.println("╚════════════════════════════════════════╝");
    System.out.println("Use 'join <ip>:<port>' to connect\n");
  }

  @Override
  public String getHelp() {
    return "server list - Lists all game servers on the local network (30-second scan).";
  }
}