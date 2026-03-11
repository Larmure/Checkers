package fr.ubordeaux.pdp.controller;

/**
 * Marqueur sémantique pour les commandes propres au client réseau.
 *
 * Hiérarchie des commandes :
 * <pre>
 * Command (interface de base)
 * ├── [Jeu]           NewCommand, UndoCommand, RedoCommand, SetCommand...
 * │                   → agissent sur GameController / GameCheckers
 * ├── [Réseau serveur] ServerListCommand, ServerStartCommand, ServerStopCommand
 * │                   → agissent sur GameServer / ServerListService
 * └── [Client]        JoinCommand, PingCommand, QuitClientCommand, HelpClientCommand
 *                     → agissent sur ClientSession (état de connexion du client)
 * </pre>
 *
 * Les commandes client reçoivent un {@link ClientSession} pour manipuler
 * l'état de connexion sans couplage direct à la classe {@code client}.
 */
public interface ClientCommand extends Command {
    // Interface marqueur — toutes les commandes client implémentent ceci
    // en plus de Command, ce qui permet de les identifier et filtrer facilement.
}