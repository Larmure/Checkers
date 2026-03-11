package fr.ubordeaux.pdp.serveur;

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

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 12345;

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private boolean connected = false;
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
      out =
            new PrintWriter(
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

  public boolean isConnected() {
    return connected;
  }

  public String getCurrentServer() {
    return currentServer;
  }

  public String getDefaultHost() {
    return DEFAULT_HOST;
  }

  public int getDefaultPort() {
    return DEFAULT_PORT;
  }
}