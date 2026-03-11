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
        return  "╔══════════════════════════════════════════════════════════════╗\n"
              + "║                  GAME CLIENT — COMMANDS                      ║\n"
              + "╠══════════════════════════════════════════════════════════════╣\n"
              + "║ [CLIENT]                                                     ║\n"
              + "║  join [IP[:PORT]]    Connect to a server (default localhost) ║\n"
              + "║  ping                Send PING, display RTT                  ║\n"
              + "║  quit                Disconnect from server / exit client    ║\n"
              + "║  help                Show this help                          ║\n"
              + "╠══════════════════════════════════════════════════════════════╣\n"
              + "║ [NETWORK — SERVER MANAGEMENT]                                ║\n"
              + "║  server list         List available servers (30s scan)       ║\n"
              + "║  server start [PORT] Start a local game server (def. 12345)  ║\n"
              + "║  server stop         Stop the local game server              ║\n"
              + "╚══════════════════════════════════════════════════════════════╝\n";
    }
}
