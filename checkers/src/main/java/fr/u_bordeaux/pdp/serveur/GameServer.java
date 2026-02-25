package fr.u_bordeaux.pdp.serveur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import fr.ubordeaux.pdp.model.CommandProtocol;


public class GameServer {

    private final int tcpPort;
    private final String serverName;
    private Thread discoveryThread;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public GameServer(String serverName, int tcpPort) {
        this.serverName = serverName;
        this.tcpPort = tcpPort;
    }

    /**
     * Démarre le serveur de jeu avec le service de découverte
     */
    public void start() throws IOException {
        if (running) {
            System.out.println("Server is already running!");
            return;
        }

        serverSocket = new ServerSocket(tcpPort);
        running = true;

        // Démarrer le service de découverte UDP
        DiscoveryService discoveryService = new DiscoveryService(serverName, tcpPort);
        discoveryThread = new Thread(discoveryService);
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        System.out.println("Game server '" + serverName + "' started on port " + tcpPort);
        System.out.println("Discovery service broadcasting on UDP port 12346");

        // Boucle d'acceptation des clients
        while (running) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());

                // Gérer chaque client dans un thread séparé
                Thread clientThread = new Thread(() -> handleClient(client));
                clientThread.start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Arrête le serveur de jeu
     */
    public void stop() {
        if (!running) {
            System.out.println("Server is not running!");
            return;
        }

        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }

        System.out.println("Game server stopped.");
    }

    /**
     * Gère la communication avec un client connecté
     */
    private void handleClient(Socket client) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
        ) {
            String line;

            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);

                try {
                    CommandProtocol command = CommandProtocol.valueOf(line.toUpperCase());

                    switch (command) {
                        case PING:
                            out.println(CommandProtocol.PONG);
                            System.out.println("Sent: PONG");
                            break;

                        case QUIT:
                            out.println(CommandProtocol.BYE);
                            System.out.println("Sent: BYE");
                            System.out.println("Client disconnected gracefully");
                            return;

                        default:
                            out.println(CommandProtocol.ERROR);
                            System.out.println("Sent: ERROR (unknown command)");
                    }

                } catch (IllegalArgumentException e) {
                    out.println(CommandProtocol.ERROR);
                    System.out.println("Sent: ERROR (invalid command: " + line + ")");
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String serverName = "GameServer-1";
        int port = 12345;

        // Parser les arguments de ligne de commande si fournis
        if (args.length >= 1) {
            serverName = args[0];
        }
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 12345");
            }
        }

        GameServer server = new GameServer(serverName, port);

        // Ajouter un shutdown hook pour arrêter proprement le serveur
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}