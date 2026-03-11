package fr.ubordeaux.pdp.controller.commands;



import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;

/**
 * Commande CLIENT : {@code help}
 *
 * Affiche la liste des commandes disponibles dans le client réseau.
 * Distingue visuellement les trois catégories de commandes :
 * <ul>
 *   <li>Commandes client (connexion/déconnexion)</li>
 *   <li>Commandes réseau serveur (démarrage/arrêt du serveur local)</li>
 *   <li>Commandes de jeu (transmises au serveur une fois connecté)</li>
 * </ul>
 *
 * Distinction des responsabilités :
 * - Cette classe n'a aucune dépendance sur {@code ClientSession},
 *   {@code GameController} ou {@code GameServer}.
 *   Elle affiche uniquement de la documentation statique.
 */
public class HelpClientCommand implements ClientCommand, Helpable {

  @Override
  public void execute() {
    System.out.println(getHelp());
  }

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
