package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.serveur.ClientSession;

/**
 * Commande CLIENT : {@code ping}
 *
 * Envoie un message {@code PING} au serveur et mesure le RTT (Round-Trip Time)
 * côté client. Le serveur répond {@code PONG TIME=<ms>} (temps de traitement
 * serveur). Le thread d'écoute de {@link ClientSession} affiche cette réponse.
 *
 * Distinction des responsabilités :
 * - Cette classe mesure le RTT côté client et envoie la commande PING.
 * - Elle ne gère pas la réception de la réponse (délégué au thread d'écoute
 *   de {@link ClientSession}).
 */
public class PingCommand implements ClientCommand, Helpable {

    private final ClientSession session;

    public PingCommand(ClientSession session) {
        this.session = session;
    }

    @Override
    public void execute() {
        if (!session.isConnected()) {
            System.out.println("Not connected to any server. Use 'join' first.");
            return;
        }

        long t0 = System.currentTimeMillis();
        session.send("PING");
        long rtt = System.currentTimeMillis() - t0;

        // La réponse "PONG TIME=..." du serveur est affichée par le thread d'écoute.
        // On affiche ici le RTT mesuré côté client (temps aller-retour réseau).
        System.out.println("→ PING sent. RTT (client-side): " + rtt + "ms");
    }

    @Override
    public String getHelp() {
        return "ping — Sends a PING to the server and displays the round-trip time (RTT).";
    }
}