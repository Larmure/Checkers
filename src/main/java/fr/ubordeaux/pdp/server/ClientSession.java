package fr.ubordeaux.pdp.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Encapsulates the TCP connection state of the client.
 *
 * <p>All client commands receive a reference to this session so they can read
 * and update the connection state without depending directly on the client
 * entry-point class.
 *
 * <p>This helps keep responsibilities separated:
 * <ul>
 *   <li>{@code Client} handles the input loop and command dispatch</li>
 *   <li>{@code ClientSession} stores network state</li>
 *   <li>Command classes implement individual actions</li>
 * </ul>
 */
public class ClientSession {
  /** The default host to connect to. */
  private static final String DEFAULT_HOST = "localhost";
  /** The default port to connect to. */
  private static final int DEFAULT_PORT = 12345;

  /** The active TCP socket for the connection. */
  private Socket socket;
  /** The input stream for reading server responses. */
  private BufferedReader in;
  /** The output stream for sending messages to the server. */
  private PrintWriter out;
  /** Indicates whether the client is currently connected to a server. */
  private boolean connected = false;
  /** The address of the currently connected server. */
  private String currentServer = null;

  /**
   * Opens a TCP connection to {@code host:port}.
   *
   * <p>Starts a background listener thread for server responses.
   *
   * @param host target host
   * @param port target TCP port
   */
  public void connect(String host, int port) {
    if (connected) {
      System.out.println(
          "Already connected to " + currentServer
              + ". Type 'quit' to disconnect first.");
      return;
    }

    System.out.println("Connecting to " + host + ":" + port + "...");

    try {
      socket = new Socket(host, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(
          new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
          true);
      connected = true;
      currentServer = host + ":" + port;

      System.out.println("Connected to " + currentServer);

      new Thread(
          () -> {
            try {
              String response;
              while ((response = in.readLine()) != null) {
                if (response.startsWith("PONG")) {
                  System.out.println("\nServer: " + response);
                } else if ("BYE".equals(response)) {
                  System.out.println("\nServer: BYE");
                } else {
                  System.out.println("\nServer: " + response);
                }

                if (connected) {
                  System.out.print("[" + currentServer + "] > ");
                }
              }
            } catch (IOException e) {
              // Socket closed normally or by the server.
            } finally {
              if (connected) {
                System.out.println("\nServer stopped unexpectedly.");
                disconnect();
                System.out.print("[local] > ");
              }
            }
          },
          "server-listener").start();

    } catch (IOException e) {
      System.out.println("Connection failed: " + e.getMessage());
    }
  }

  /** Closes the TCP connection and resets the state. */
  public void disconnect() {
    connected = false;
    currentServer = null;

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
      // Ignore close failure during cleanup.
    }

    System.out.println("Disconnected from server.");
  }

  /**
   * Sends one line of text to the server.
   *
   * @param message the text to send
   */
  public void send(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  /**
   * Checks if the client is currently connected to a server.
   *
   * @return true if connected, false otherwise.
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Returns the address of the currently connected server, or null if not connected.
   *
   * @return the current server address in "host:port" format, or null if not connected.
   */
  public String getCurrentServer() {
    return currentServer;
  }

  /**
   * Returns the default host for connections.
   *
   * @return the default host as a string.
   */
  public String getDefaultHost() {
    return DEFAULT_HOST;
  }

  /**
   * Returns the default port for connections.
   *
   * @return the default port as an integer.
   */
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }
}