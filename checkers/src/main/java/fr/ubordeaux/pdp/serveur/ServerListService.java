package fr.ubordeaux.pdp.serveur;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;


public class ServerListService {

    private static final int DISCOVERY_PORT = 12346;
    private static final int TIMEOUT_MS = 5000; // attendre 5 secondes maximum

    /**
     * Écoute les broadcasts UDP et retourne la liste des serveurs disponibles
     * @return Liste des informations serveur (nom:port)
     */
    public static List<String> discoverServers() {
        List<String> servers = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            socket.setSoTimeout(TIMEOUT_MS);

            byte[] buffer = new byte[1024];
            long startTime = System.currentTimeMillis();

            System.out.println("Listening for server broadcasts...");

            // Écouter pendant 5 secondes
            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    // On vérifie si c'est un nouveau serveur
                    if (!servers.contains(message)) {
                        servers.add(message);

                        // Affichage immédiat lors de la découverte
                        String[] parts = message.split(":");
                        if (parts.length == 3) {
                            System.out.println("Nouveau serveur trouvé : " + parts[0] + " à l'adresse " + parts[1] + ":" + parts[2]);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    continue;
                }
            }

            if (servers.isEmpty()) {
                System.out.println("No servers found.");
            }

        } catch (Exception e) {
            System.err.println("Error during server discovery: " + e.getMessage());
        }

        return servers;
    }

    /**
     * Affiche la liste des serveurs disponibles
     */
    public static void displayAvailableServers() {
        List<String> servers = discoverServers();

        if (servers.isEmpty()) {
            System.out.println("No game servers available.");
        } else {
            System.out.println("\n=== Available Game Servers ===");
            for (String s : servers) {
                String[] parts = s.split(":");
                if (parts.length == 3) {
                    System.out.println("- " + parts[0] + " | Adresse à taper: " + parts[1] + ":" + parts[2]);
                }
            }
            System.out.println("==============================\n");
        }
    }

    public static void main(String[] args) {
        // Test de découverte de serveurs
        displayAvailableServers();
    }
}