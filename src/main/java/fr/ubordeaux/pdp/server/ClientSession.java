package fr.ubordeaux.pdp.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Holds the TCP connection state and current operating mode for the game client.
 *
 * <p>All {@link fr.ubordeaux.pdp.controller.commands.JoinCommand} and other client
 * commands receive a reference to this session to read or modify state without
 * depending on the {@code client} runner class directly.
 *
 * <p>The {@link ClientMode} controls which commands are permitted at any time:
 *
 * <ul>
 *   <li>{@link ClientMode#LOCAL} — all commands available (default).
 *   <li>{@link ClientMode#SERVER} — server is running; client commands blocked.
 *   <li>{@link ClientMode#CONNECTED} — connected to a server; server-start blocked.
 * </ul>
 */
public class ClientSession {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 12345;

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private boolean connected = false;
  private String currentServer = null;

  /** Current operating mode — drives command availability in the client loop. */
  private ClientMode mode = ClientMode.LOCAL;

  /**
   * Opens a TCP connection to the given host and port, sets the mode to {@link
   * ClientMode#CONNECTED}, then starts a background listener thread.
   *
   * @param host target host.
   * @param port target port.
   */
  public void connect(String host, int port) {
    if (connected) {
      System.out.println(
            "Already connected to " + currentServer + ". Type 'quit' to disconnect first.");
      return;
    }

    System.out.println("Connecting to " + host + ":" + port + "...");

    try {
      socket = new Socket(host, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out =
            new PrintWriter(
                  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
      connected = true;
      currentServer = host + ":" + port;
      mode = ClientMode.CONNECTED;

      System.out.println("Connected to " + currentServer);
      startListenerThread();

    } catch (IOException e) {
      System.out.println("Connection failed: " + e.getMessage());
    }
  }

  /**
   * Closes the TCP socket, resets connection state, and returns the mode to {@link
   * ClientMode#LOCAL}.
   */
  public void disconnect() {
    connected = false;
    currentServer = null;
    mode = ClientMode.LOCAL;
    try {
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      System.out.println("Error while closing client connection: " + e.getMessage());
    }
    System.out.println("Disconnected from server.");
  }

  /**
   * Sends a text line to the connected server.
   *
   * @param message the line to send.
   */
  public void send(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  /**
   * Switches the mode to {@link ClientMode#SERVER}. Called by {@link
   * fr.ubordeaux.pdp.controller.commands.ServerStartCommand} once the server is successfully
   * started.
   */
  public void enterServerMode() {
    mode = ClientMode.SERVER;
    System.out.println(
          "[mode] Now in SERVER mode. Client commands are disabled.\n"
                + "       Use 'server stop' to return to local mode.");
  }

  /**
   * Returns the mode to {@link ClientMode#LOCAL}. Called by {@link
   * fr.ubordeaux.pdp.controller.commands.ServerStopCommand}.
   */
  public void exitServerMode() {
    mode = ClientMode.LOCAL;
    System.out.println("[mode] Server stopped. Back to LOCAL mode.");
  }

  /**
   * Returns the current operating mode.
   *
   * @return the current operating mode
   */
  public ClientMode getMode() {
    return mode;
  }

  /**
   * Returns whether the TCP socket is currently open.
   *
   * @return {@code true} if the TCP socket is currently open
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Returns the current server address.
   *
   * @return the {@code "host:port"} string of the current server, or {@code null}
   */
  public String getCurrentServer() {
    return currentServer;
  }

  /**
   * Returns the default host used by the join command.
   *
   * @return the default host used when no address is given to {@code join}
   */
  public String getDefaultHost() {
    return DEFAULT_HOST;
  }

  /**
   * Returns the default port used by the join command.
   *
   * @return the default port used when no address is given to {@code join}
   */
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  /**
   * Starts a daemon thread that continuously reads lines from the server and prints them to
   * stdout. Handles unexpected server shutdown by calling {@link #disconnect()}.
   */
  private void startListenerThread() {
    Thread listener =
          new Thread(
                () -> {
                  try {
                    String response;
                    while ((response = in.readLine()) != null) {
                      System.out.println("\nServer: " + response);
                      if (connected) {
                        System.out.print("[" + currentServer + "] > ");
                      }
                    }
                  } catch (IOException e) {
                    // Ignore read errors; disconnect handling is done in finally.
                  } finally {
                    if (connected) {
                      System.out.println("\n[!] Server stopped unexpectedly.");
                      disconnect();
                      System.out.print("[local] > ");
                    }
                  }
                },
                "server-listener");
    listener.setDaemon(true);
    listener.start();
  }
}