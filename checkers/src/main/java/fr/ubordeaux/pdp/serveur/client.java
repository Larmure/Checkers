package fr.ubordeaux.pdp.serveur;

import java.util.Scanner;

import fr.ubordeaux.pdp.controller.commands.HelpClientCommand;
import fr.ubordeaux.pdp.controller.commands.JoinCommand;
import fr.ubordeaux.pdp.controller.commands.PingCommand;
import fr.ubordeaux.pdp.controller.commands.QuitClientCommand;
import fr.ubordeaux.pdp.controller.commands.ServerListCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStartCommand;
import fr.ubordeaux.pdp.controller.commands.ServerStopCommand;

/**
 * Point d'entrée du client de jeu réseau.
 *
 * <p>Rôle unique : lire les saisies utilisateur et les dispatcher
 * vers la {@link fr.ubordeaux.pdp.controller.commands.ClientCommand} appropriée.
 * Toute la logique est encapsulée dans les classes Command correspondantes.
 *
 * <p>Hiérarchie des commandes dispatchées :
 * <pre>
 * [CLIENT]          JoinCommand · PingCommand · QuitClientCommand · HelpClientCommand
 * [RÉSEAU SERVEUR]  ServerListCommand · ServerStartCommand · ServerStopCommand
 * [JEU]             Transmises telles quelles au serveur via ClientSession.send()
 * </pre>
 *
 * @see ClientSession
 */
public class client {

    /** État de connexion partagé entre toutes les commandes client. */
    private final ClientSession session = new ClientSession();

    // -------------------------------------------------------------------------
    // Boucle principale
    // -------------------------------------------------------------------------

    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  GAME CLIENT — WELCOME                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("Type 'help' to see available commands\n");

        while (true) {
            // Prompt contextuel
            System.out.print(session.isConnected()
                    ? "[" + session.getCurrentServer() + "] > "
                    : "[local] > ");

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // Découpe : mot1 [mot2 [reste...]]
            String[] tokens = input.split("\\s+", 3);
            String   word1  = tokens[0].toLowerCase();
            String   word2  = tokens.length > 1 ? tokens[1].toLowerCase() : "";
            String   rest   = tokens.length > 2 ? tokens[2] : "";

            // ------------------------------------------------------------------
            // Commandes à deux mots : server <sous-commande>
            // ------------------------------------------------------------------
            if ("server".equals(word1)) {
                dispatchServerCommand(word2, rest);
                continue;
            }

            // ------------------------------------------------------------------
            // Commandes client à un mot
            // ------------------------------------------------------------------
            switch (word1) {

                case "join" -> {
                    // Reconstitue l'adresse (ex: "join 192.168.1.1 12345" → "192.168.1.1:12345")
                    String addr = word2.isBlank() ? null : word2 + (rest.isBlank() ? "" : ":" + rest);
                    new JoinCommand(session, addr).execute();
                }

                case "ping" ->
                    new PingCommand(session).execute();

                case "help" ->
                    new HelpClientCommand().execute();

                case "quit" -> {
                    QuitClientCommand quitCmd = new QuitClientCommand(session);
                    quitCmd.execute();
                    if (quitCmd.shouldExit()) {
                        scanner.close();
                        return; // Sortie propre du programme
                    }
                }

                default -> {
                    if (session.isConnected()) {
                        // Commande de jeu → transmise au serveur (GameController s'en charge)
                        session.send(input);
                    } else {
                        System.out.println("Unknown command: '" + word1 + "'.");
                        System.out.println("Type 'help' to see available commands.");
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dispatch server <sous-commande>
    // -------------------------------------------------------------------------

    /**
     * Distribue les commandes {@code server list / start / stop}.
     *
     * @param sub  Sous-commande ({@code list}, {@code start}, {@code stop}).
     * @param args Arguments supplémentaires (ex. numéro de port).
     */
    private void dispatchServerCommand(String sub, String args) {
        switch (sub) {

            case "list" ->
                new ServerListCommand().execute();

            case "start" -> {
                String portArg = args.isBlank() ? null : args.trim();
                String[] startArgs = portArg == null ? new String[0] : new String[]{ portArg };
                // null = pas de GameController côté client autonome.
                // Pour un serveur complet avec logique de jeu, lancer GameServer directement.
                new ServerStartCommand(null, startArgs).execute();
            }

            case "stop" ->
                new ServerStopCommand().execute();

            default ->
                System.out.println("Unknown server command: '" + sub + "'."
                        + " Use: server list | server start [PORT] | server stop");
        }
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        new client().run();
    }
}