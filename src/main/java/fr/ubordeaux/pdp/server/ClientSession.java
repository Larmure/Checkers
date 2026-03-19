package fr.ubordeaux.pdp.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import fr.ubordeaux.pdp.controller.GameController;

/**
 * Holds the TCP connection state and current operating mode for the game client.
 *
 * <p>The background listener thread intercepts specific server messages:
 *
 * <ul>
 *   <li>{@code OPPONENT_MOVE <from>-<to>} — applies the move on the local {@link GameController}
 *       so the board updates visually for both players.
 *   <li>{@code GAME_START ...} — printed as-is; the game is already initialised locally.
 *   <li>{@code GAME_OVER ...} — printed as-is.
 *   <li>All other messages — printed as-is.
 * </ul>
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
   * Local game controller — used to apply opponent moves so the board stays in sync.
   * Set via {@link #setController(GameController)} after construction.
   */
  private GameController controller;

  /**
   * Injects the local game controller so the listener thread can call
   * {@link GameController#executeMove(String, String)} when an {@code OPPONENT_MOVE} arrives.
   *
   * @param controller the local game controller.
   */
  public void setController(GameController controller) {
    this.controller = controller;
  }

  /**
   * Opens a TCP connection, sets the mode to {@link ClientMode#CONNECTED}, and starts the
   * background listener thread.
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
   * Closes the TCP socket, resets connection state, and returns the mode to
   * {@link ClientMode#LOCAL}.
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
    } catch (IOException ignored) {
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
   * Switches the mode to {@link ClientMode#SERVER}. Called by
   * {@link fr.ubordeaux.pdp.controller.commands.ServerStartCommand}.
   */
  public void enterServerMode() {
    mode = ClientMode.SERVER;
    System.out.println(
        "[mode] Now in SERVER mode. Client commands are disabled.\n"
            + "       Use 'server stop' to return to local mode.");
  }

  /**
   * Returns the mode to {@link ClientMode#LOCAL}. Called by
   * {@link fr.ubordeaux.pdp.controller.commands.ServerStopCommand}.
   */
  public void exitServerMode() {
    mode = ClientMode.LOCAL;
    System.out.println("[mode] Server stopped. Back to LOCAL mode.");
  }

  /** @return the current operating mode. */
  public ClientMode getMode() {
    return mode;
  }

  /** @return {@code true} if the TCP socket is currently open. */
  public boolean isConnected() {
    return connected;
  }

  /** @return the {@code "host:port"} string of the current server, or {@code null}. */
  public String getCurrentServer() {
    return currentServer;
  }

  /** @return the default host used when no address is given to {@code join}. */
  public String getDefaultHost() {
    return DEFAULT_HOST;
  }

  /** @return the default port used when no address is given to {@code join}. */
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  /**
   * Starts a daemon thread that reads server messages and reacts to them:
   *
   * <ul>
   *   <li>{@code OPPONENT_MOVE <from>-<to>} — applies the move locally via the controller
   *       so both players see the board update in real time.
   *   <li>All other messages — printed to stdout as {@code Server: <message>}.
   * </ul>
   */
  private void startListenerThread() {
    Thread listener =
        new Thread(
            () -> {
              try {
                String response;
                while ((response = in.readLine()) != null) {
                  handleServerMessage(response);
                  if (connected) {
                    System.out.print("[" + currentServer + "] > ");
                  }
                }
              } catch (IOException ignored) {
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

  /**
   * Processes a single message received from the server.
   *
   * <p>If the message is {@code OPPONENT_MOVE <from>-<to>}, the move is applied on the
   * local controller so the board refreshes for this player. All other messages are
   * printed to stdout.
   *
   * @param message the raw message line from the server.
   */
  private void handleServerMessage(String message) {
    if (message.startsWith("GAME_START")) {
      // Stop any running local game (e.g. the default game started by App)
      // before initializing the network game, to avoid timer conflicts.
      if (controller != null) {
        controller.stopBlitzTimer();
        controller.startNewGame(
            fr.ubordeaux.pdp.model.core.Configuration.getDefaultConfiguration());
      }
      System.out.println("\nGame started! " + message);
    } else if (message.startsWith("MOVE_OK ")) {
      String moveArg = message.substring("MOVE_OK ".length()).trim();
      String[] parts = moveArg.split("-");
      if (parts.length == 2 && controller != null) {
        System.out.println("\nYou played: " + moveArg);
        try {
          controller.executeMove(parts[0].trim(), parts[1].trim());
        } catch (Exception e) {
          System.out.println("[warning] Could not apply local move: " + e.getMessage());
        }
      } else {
        System.out.println("\nServer: " + message);
      }
    } else if (message.startsWith("OPPONENT_MOVE ")) {
      String moveArg = message.substring("OPPONENT_MOVE ".length()).trim();
      String[] parts = moveArg.split("-");
      if (parts.length == 2 && controller != null) {
        System.out.println("\nOpponent played: " + moveArg);
        try {
          controller.executeMove(parts[0].trim(), parts[1].trim());
        } catch (Exception e) {
          System.out.println("[warning] Could not apply opponent move: " + e.getMessage());
        }
      } else {
        System.out.println("\nServer: " + message);
      }
    } else {
      System.out.println("\nServer: " + message);
    }
  }
}