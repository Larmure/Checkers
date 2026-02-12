package fr.u_bordeaux.pdp.serveur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * Client de jeu avec support de la découverte de serveurs
 * Commandes disponibles:
 * - server_list: Affiche la liste des serveurs disponibles
 * - join [IP:PORT]: Se connecte à un serveur
 * - ping: Teste la connexion
 * - quit: Quitte le serveur (ou mode local si non connecté)
 */
public class client {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private String currentServer = null;

    /**
     * Affiche la liste des serveurs disponibles
     */
    private void listServers() {
        System.out.println("\nSearching for available servers...");
        List<String> servers = ServerListService.discoverServers();

        if (servers.isEmpty()) {
            System.out.println("No game servers found on the network.");
            System.out.println("Make sure at least one server is running.");
        } else {
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║   Available Game Servers          ║");
            System.out.println("╠════════════════════════════════════╣");
            for (int i = 0; i < servers.size(); i++) {
                String[] parts = servers.get(i).split(":");
                String name = parts.length > 0 ? parts[0] : "Unknown";
                String port = parts.length > 1 ? parts[1] : "?";
                System.out.printf("║ %d. %-20s Port: %-6s║%n", (i + 1), name, port);
            }
            System.out.println("╚════════════════════════════════════╝\n");
            System.out.println("Use 'join <host>:<port>' to connect to a server");
            System.out.println("Example: join localhost:12345");
        }
    }

    /**
     * Se connecte à un serveur
     * @param address Format: "host:port" ou juste "localhost:12345"
     */
    private void joinServer(String address) {
        if (connected) {
            System.out.println("Already connected to " + currentServer);
            System.out.println("Use 'quit' to disconnect first.");
            return;
        }

        try {
            String[] parts = address.split(":");
            if (parts.length != 2) {
                System.out.println("Invalid address format. Use: host:port");
                System.out.println("Example: localhost:12345");
                return;
            }

            String host = parts[0].trim();
            int port = Integer.parseInt(parts[1].trim());

            System.out.println("Connecting to " + host + ":" + port + "...");

            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                    true
            );

            connected = true;
            currentServer = address;

            System.out.println("✓ Successfully connected to server!");
            System.out.println("You can now use 'ping' to test the connection or 'quit' to disconnect.");

        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " );
        } catch (IOException e) {
            System.out.println("✗ Connection failed: " + e.getMessage());
            System.out.println("Make sure the server is running and the address is correct.");
        }
    }

    /**
     * Envoie une commande ping au serveur
     */
    private void ping() {
        if (!connected) {
            System.out.println("Not connected to any server. Use 'join <host>:<port>' first.");
            return;
        }

        try {
            out.println("PING");
            String response = in.readLine();

            if ("PONG".equals(response)) {
                System.out.println("✓ Server responded: PONG");
                System.out.println("Connection is active.");
            } else {
                System.out.println("Unexpected response: " + response);
            }

        } catch (IOException e) {
            System.out.println("✗ Ping failed: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Déconnecte du serveur ou quitte le client
     */
    private void quit() {
        if (connected) {
            try {
                out.println("QUIT");
                String response = in.readLine();
                System.out.println("Server: " + response);
            } catch (IOException e) {
                System.out.println("Error during disconnect: " + e.getMessage());
            } finally {
                disconnect();
            }
        } else {
            System.out.println("Not connected to any server.");
        }
    }

    /**
     * Ferme la connexion
     */
    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignorer les erreurs de fermeture
        }

        connected = false;
        currentServer = null;
        System.out.println("Disconnected from server.");
    }

    /**
     * Affiche l'aide des commandes disponibles
     */
    private void showHelp() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║              GAME CLIENT - COMMANDS                ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║ server_list          - List available servers      ║");
        System.out.println("║ join <host>:<port>   - Connect to a server         ║");
        System.out.println("║ ping                 - Test server connection      ║");
        System.out.println("║ quit                 - Disconnect/Exit client      ║");
        System.out.println("║ help                 - Show this help message      ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");
    }

    /**
     * Boucle principale du client
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║           GAME CLIENT - WELCOME                    ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println("Type 'help' to see available commands\n");

        while (true) {
            if (connected) {
                System.out.print("[" + currentServer + "] > ");
            } else {
                System.out.print("[local] > ");
            }

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "server_list":
                    listServers();
                    break;

                case "join":
                    if (parts.length < 2) {
                        System.out.println("Usage: join <host>:<port>");
                        System.out.println("Example: join localhost:12345");
                    } else {
                        joinServer(parts[1]);
                    }
                    break;

                case "ping":
                    ping();
                    break;

                case "quit":
                    quit();
                    if (!connected) {
                        System.out.println("Exiting client. Goodbye!");
                        scanner.close();
                        return;
                    }
                    break;

                case "help":
                    showHelp();
                    break;

                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' to see available commands");
            }
        }
    }

    public static void main(String[] args) {
        client client = new client();
        client.run();
    }
}