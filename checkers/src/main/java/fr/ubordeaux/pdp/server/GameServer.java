package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.CommandProtocol;
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

/**
 * TCP game server.
 *
 * <p>Responsibility: network connections only. Game logic is entirely delegated
 * to the {@link GameController}.
 *
 * <p>Network features:
 * <ul>
 *   <li>Accepts multiple clients simultaneously</li>
 *   <li>Unexpected disconnection timeout: 1 minute</li>
 *   <li>{@code server stop} notifies all connected clients before closing</li>
 *   <li>Explicit error if the TCP port is already in use</li>
 * </ul>
 */
public class GameServer {

  private static final int DEFAULT_PORT = 12345;

  /** Client socket timeout: 1 minute. */
  private static final int CLIENT_TIMEOUT_MS = 60_000;

  private final int tcpPort;
  private final String serverName;
  private final GameController controller;

  private Thread discoveryThread;
  private ServerSocket serverSocket;
  private volatile boolean running = false;

  /** Active writers used to notify all clients when the server stops. */
  private final Set<PrintWriter> connectedClients =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Creates a game server.
   *
   * @param serverName the name broadcast through UDP
   * @param tcpPort the TCP listening port
   * @param controller the game controller injected from outside
   */
  public GameServer(String serverName, int tcpPort, GameController controller) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
    this.controller = controller;
  }

  /**
   * Starts the TCP server and the UDP discovery service.
   *
   * <p>Throws an {@link IOException} with an explicit message if the port is
   * already in use.
   *
   * @throws IOException if the server cannot start
   */
  public void start() throws IOException {
    if (running) {
      System.out.println("Server is already running on port " + tcpPort + "!");
      return;
    }

    try {
      serverSocket = new ServerSocket(tcpPort);
    } catch (java.net.BindException e) {
      throw new IOException(
            "Port " + tcpPort + " is already in use. "
                  + "Choose another port or stop the existing server.",
            e);
    }

    running = true;

    DiscoveryService discoveryService = new DiscoveryService(serverName, tcpPort);
    discoveryThread = new Thread(discoveryService, "discovery-thread");
    discoveryThread.setDaemon(true);
    discoveryThread.start();

    System.out.println("Game server '" + serverName + "' started on port " + tcpPort);
    System.out.println("Discovery broadcasting on UDP 12346");

    while (running) {
      try {
        Socket client = serverSocket.accept();
        client.setSoTimeout(CLIENT_TIMEOUT_MS);
        System.out.println("Client connected: " + client.getInetAddress());

        Thread thread =
              new Thread(
                    () -> handleClient(client),
                    "client-" + client.getInetAddress());
        thread.start();

      } catch (IOException e) {
        if (running) {
          System.err.println("Error accepting client: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Stops the server gracefully.
   *
   * <p>All connected clients receive {@code BYE} before the server socket is
   * closed.
   */
  public void stop() {
    if (!running) {
      System.out.println("Server is not running.");
      return;
    }

    running = false;

    for (PrintWriter clientOut : connectedClients) {
      try {
        clientOut.println(CommandProtocol.BYE);
      } catch (RuntimeException e) {
        // Ignore client notification failure during shutdown.
      }
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

  public boolean isRunning() {
    return running;
  }

  public int getPort() {
    return tcpPort;
  }

  /**
   * Handles communication with a connected client.
   *
   * @param client the connected socket
   */
  private void handleClient(Socket client) {
    try (
          BufferedReader in =
                new BufferedReader(new InputStreamReader(client.getInputStream()));
          PrintWriter out =
                new PrintWriter(
                      new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),
                      true)) {
      connectedClients.add(out);
      String line;

      while ((line = in.readLine()) != null) {
        System.out.println("Received: " + line);

        CommandProtocol netCmd = tryParseProtocol(line.split("\\s+")[0]);

        if (netCmd != null) {
          switch (netCmd) {
            case PING:
              long t0 = System.currentTimeMillis();
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

        String[] tokens = line.trim().split("\\s+", 2);
        String commandName = tokens[0];
        String[] args = tokens.length > 1
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

  /**
   * Tries to parse a protocol command token.
   *
   * @param token the raw command token
   * @return the matching protocol command, or {@code null} if invalid
   */
  private CommandProtocol tryParseProtocol(String token) {
    try {
      return CommandProtocol.valueOf(token.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}