package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;

/**
 * CLIENT Command: {@code help}
 * 
 * <p>Displays the list of available commands in the network client.
 * Visually distinguishes the three categories of commands:
 * <ul>
 *  <li>Client commands (connect/disconnect)</li>
 * <li>Server network commands (start/stop local server)</li>
 * <li>Game commands (sent to server once connected)</li>
 * </ul>
 * 
 * <p>Separation of concerns:
 * - This class has no dependencies on {@code ClientSession},
 * {@code GameController}, or {@code GameServer}.
 * It only displays static documentation.
 * 
 */
public class HelpClientCommand implements ClientCommand, Helpable {

  /**
   * Displays the help message for client commands.
   */
  @Override
  public void execute() {
    System.out.println(getHelp());
  }

  /**
   * Returns the help message for client commands.
   *
   * @return the help message
   */
  @Override
  public String getHelp() {
    return "\n[CLIENT]\n"
        + "  join [IP[:PORT]]     Connect to a server (default: localhost:12345)\n"
        + "  ping                 Send PING and display round-trip time\n"
        + "  quit                 Disconnect from server / exit client\n"
        + "  help                 Show this help message\n"
        + "\n[NETWORK — SERVER MANAGEMENT]\n"
        + "  server list          List available servers (30-second scan)\n"
        + "  server start [PORT]  Start a local game server (default port: 12345)\n"
        + "  server stop          Stop the local game server\n"
        + "  players              List connected players and their status\n"
        + "  scoreboard           Display win/loss statistics\n"
        + "  status               Show server port, clients, and active games\n";

  }
}
