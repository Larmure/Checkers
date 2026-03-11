package fr.ubordeaux.pdp.serveur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Encapsule l'état de la connexion TCP du client.
 *
 * Toutes les {@link fr.ubordeaux.pdp.controller.commands.ClientCommand} reçoivent
 * une référence à cette session pour lire/modifier l'état de connexion sans
 * dépendre directement de la classe {@code client}.
 *
 * Cela permet de respecter le principe de responsabilité unique :
 * - {@code client}       → boucle de lecture et dispatch des commandes
 * - {@code ClientSession} → état réseau (socket, flux, serveur courant)
 * - {@code XxxCommand}   → logique de chaque action
 */
public class ClientSession {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 12345;

    private Socket         socket;
    private BufferedReader in;
    private PrintWriter    out;
    private boolean        connected     = false;
    private String         currentServer = null;

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    /**
     * Ouvre la connexion TCP vers {@code host:port}.
     * Lance le thread d'écoute des réponses serveur en arrière-plan.
     *
     * @param host Hôte cible (défaut : {@value #DEFAULT_HOST}).
     * @param port Port TCP   (défaut : {@value #DEFAULT_PORT}).
     */
    public void connect(String host, int port) {
        if (connected) {
            System.out.println("Already connected to " + currentServer
                    + ". Type 'quit' to disconnect first.");
            return;
        }

        System.out.println("Connecting to " + host + ":" + port + "...");

        try {
            socket        = new Socket(host, port);
            in            = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out           = new PrintWriter(
                                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            connected     = true;
            currentServer = host + ":" + port;

            System.out.println("✓ Connected to " + currentServer);

            // Thread d'écoute des messages serveur
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        // Affichage propre de la réponse serveur
                        if (response.startsWith("PONG")) {
                            System.out.println("\n✓ Server: " + response);
                        } else if ("BYE".equals(response)) {
                            System.out.println("\nServer: BYE");
                        } else {
                            System.out.println("\nServer: " + response);
                        }
                        if (connected) System.out.print("[" + currentServer + "] > ");
                    }
                } catch (IOException ignored) {
                    // Socket fermé normalement ou par le serveur
                } finally {
                    if (connected) {
                        System.out.println("\n[!] Server stopped unexpectedly.");
                        disconnect();
                        System.out.print("[local] > ");
                    }
                }
            }, "server-listener").start();

        } catch (IOException e) {
            System.out.println("✗ Connection failed: " + e.getMessage());
        }
    }

    /**
     * Ferme la connexion TCP et réinitialise l'état.
     */
    public void disconnect() {
        connected     = false;
        currentServer = null;
        try {
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) { }
        System.out.println("Disconnected from server.");
    }

    // -------------------------------------------------------------------------
    // Envoi de messages
    // -------------------------------------------------------------------------

    /**
     * Envoie une ligne de texte au serveur.
     *
     * @param message La commande ou le texte à envoyer.
     */
    public void send(String message) {
        if (out != null) out.println(message);
    }

   
    public boolean isConnected()  {
         return connected; 
    }
    public String  getCurrentServer(){
         return currentServer; 
    }

    public String  getDefaultHost()  {
         return DEFAULT_HOST; 
    }
    public int     getDefaultPort() { 
        return DEFAULT_PORT; 
    }
}