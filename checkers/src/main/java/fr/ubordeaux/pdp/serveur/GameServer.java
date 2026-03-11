package fr.ubordeaux.pdp.serveur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.CommandProtocol;

/**
 * TCP game server.
 *
 * Responsibility: network connections only.
 * Game logic is entirely delegated to the {@link GameController}.
 *
 * Network features:
 * - Accepts multiple clients simultaneously.
 * - Unexpected disconnection timeout: 1 minute (SO_TIMEOUT).
 * - {@code server stop} notifies all connected clients before closing.
 * - Explicit error if the TCP port is already in use.
 */
public class GameServer {

    private static final int DEFAULT_PORT    = 12345;
    /** Client socket timeout: 1 minute as specified. */
    private static final int CLIENT_TIMEOUT_MS = 60_000;

    private final int    tcpPort;
    private final String serverName;

    private Thread       discoveryThread;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    /** Set of active writers used to notify all clients when the server stops. */
    private final Set<PrintWriter> connectedClients =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final GameController controller;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param serverName Name broadcast through UDP.
     * @param tcpPort    TCP listening port.
     * @param controller Already initialized game controller (injected from outside).
     */
    public GameServer(String serverName, int tcpPort, GameController controller) {
        this.serverName = serverName;
        this.tcpPort    = tcpPort;
        this.controller = controller;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the TCP server and the UDP discovery service.
     * Throws an {@link IOException} with an explicit message if the port is already in use.
     */
    public void start() throws IOException {
        if (running) {
            System.out.println("Server is already running on port " + tcpPort + "!");
            return;
        }

        // Explicit check for occupied port
        try {
            serverSocket = new ServerSocket(tcpPort);
        } catch (java.net.BindException e) {
            throw new IOException(
                "Port " + tcpPort + " is already in use. "
                + "Choose another port or stop the existing server.", e);
        }

        running = true;

        // UDP discovery service (daemon → stops with the JVM)
        DiscoveryService discoveryService = new DiscoveryService(serverName, tcpPort);
        discoveryThread = new Thread(discoveryService, "discovery-thread");
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        System.out.println("Game server '" + serverName + "' started on port " + tcpPort);
        System.out.println("Discovery broadcasting on UDP 12346");

        // Accept loop
        while (running) {
            try {
                Socket client = serverSocket.accept();
                // 1-minute timeout for unexpected disconnections
                client.setSoTimeout(CLIENT_TIMEOUT_MS);
                System.out.println("Client connected: " + client.getInetAddress());

                Thread t = new Thread(() -> handleClient(client), "client-" + client.getInetAddress());
                t.start();

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
                // If running == false, stop() closed the socket → normal behavior
            }
        }
    }

    /**
     * Gracefully stops the server:
     * 1. Notifies all connected clients with {@code BYE}.
     * 2. Closes the server socket.
     * 3. Interrupts the UDP discovery thread.
     */
    public void stop() {
        if (!running) {
            System.out.println("Server is not running.");
            return;
        }

        running = false;

        // Notify all connected clients
        for (PrintWriter clientOut : connectedClients) {
            try {
                clientOut.println(CommandProtocol.BYE);
            } catch (Exception ignored) { }
        }
        connectedClients.clear();

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

    public boolean isRunning() { return running; }
    public int     getPort()   { return tcpPort;  }

    // -------------------------------------------------------------------------
    // Connected client handling
    // -------------------------------------------------------------------------

    private void handleClient(Socket client) {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            PrintWriter    out = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true)
        ) {
            connectedClients.add(out);
            String line;

            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);

                // --- Network protocol commands ---
                CommandProtocol netCmd = tryParseProtocol(line.split("\\s+")[0]);

                if (netCmd != null) {
                    switch (netCmd) {

                        case PING:
                            // The client sends PING, we reply with PONG TIME=<ms>
                            long t0 = System.currentTimeMillis();
                            // (server processing time — client-side RTT will be measured separately)
                            long elapsed = System.currentTimeMillis() - t0;
                            out.println("PONG TIME=" + elapsed + "ms");
                            System.out.println("Sent: PONG TIME=" + elapsed + "ms");
                            break;

                        case QUIT:
                            out.println(CommandProtocol.BYE);
                            System.out.println("Client disconnected gracefully.");
                            connectedClients.remove(out);
                            return;

                        default:
                            out.println(CommandProtocol.ERROR);
                    }
                    continue;
                }

                // --- Game commands → delegated to the GameController ---
                String[] tokens      = line.trim().split("\\s+", 2);
                String   commandName = tokens[0];
                String[] args        = tokens.length > 1
                                       ? tokens[1].split("\\s+")
                                       : new String[0];
                try {
                    controller.executeCommand(commandName, args);
                    out.println("OK");
                } catch (Exception e) {
                    out.println("ERROR: " + e.getMessage());
                    System.err.println("Command error: " + e.getMessage());
                }
            }

        } catch (SocketTimeoutException e) {
            System.out.println("Client timed out after 1 minute of inactivity.");
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private CommandProtocol tryParseProtocol(String token) {
        try {
            return CommandProtocol.valueOf(token.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}