package fr.ubordeaux.pdp.server;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.view.gui.GraphicalUserInterface;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds the TCP connection state and current operating mode for the game client.
 *
 * <h2>Handshake</h2>
 *
 * <p>{@link fr.ubordeaux.pdp.controller.commands.JoinCommand} calls
 * {@link #connect(String, int)} to open the socket, then sends
 * {@code REGISTER <id> <name> } via {@link #send(String)}.
 * The server replies with {@code WELCOME <id> mode=<GUI|ANY>}. The listener
 * thread parses that response and sets {@link #serverRequiresGui} accordingly.
 *
 * <h2>Mode flag flow</h2>
 *
 * <ol>
 *   <li>{@code App} calls {@code session.setGuiMode(true)} when
 *       {@code --gui} is present.</li>
 *   <li>The listener thread reads {@code WELCOME … mode=GUI} and sets
 *       {@link #serverRequiresGui} so the application can warn or adapt the UI.</li>
 * </ol>
 *
 * <p>Thread-safety: all accesses to connection-state fields are protected by
 * {@link #connectionLock} to prevent races between the shell thread and the
 * background listener thread.
 */
public class ClientSession {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 12345;
  private GameServer hostedServer;

  /**
   * Protects all connection-state fields.
   */
  private final ReentrantLock connectionLock = new ReentrantLock();

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private boolean connected = false;
  private String currentServer = null;

  /** Current operating mode — drives command availability in the client shell. */
  private volatile ClientMode mode = ClientMode.LOCAL;

  private volatile boolean guiMode = false;

  /**
   * Set to {@code true} by the listener thread when the server
   * {@code WELCOME} response contains {@code mode=GUI}.
   */
  private volatile boolean serverRequiresGui = false;

  /** Prevents opening the GUI window more than once per connection. */
  private boolean guiWindowOpened = false;

  /** Local controller — used to apply opponent moves on the local board. */
  private volatile GameController controller;

  /**
   * Injects the local game controller.
   * Must be called before any game-related server messages can arrive.
   *
   * @param controller the local game controller
   */
  public void setController(GameController controller) {
    this.controller = controller;
  }

  /**
   * Opens a TCP socket to the server and starts the background listener thread.
   *
   * <p>This method only opens the connection. The {@code REGISTER} handshake
   * is performed by {@link fr.ubordeaux.pdp.controller.commands.JoinCommand}
   * via {@link #send(String)} immediately after this call returns.
   *
   * @param host target host
   * @param port target port
   */
  public void connect(String host, int port) {
    connectionLock.lock();
    try {
      if (connected) {
        System.out.println(
            "Already connected to " + currentServer
                + ". Type 'quit' to disconnect first.");
        return;
      }

      System.out.println("Connecting to " + host + ":" + port + "...");

      Socket newSocket = null;
      BufferedReader newIn = null;
      PrintWriter newOut = null;

      try {
        newSocket = new Socket(host, port);
        newIn =
            new BufferedReader(
                new InputStreamReader(newSocket.getInputStream()));
        newOut =
            new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(newSocket.getOutputStream())),
                true);

        socket = newSocket;
        in = newIn;
        out = newOut;
        connected = true;
        currentServer = host + ":" + port;
        serverRequiresGui = false;
        guiWindowOpened = false;
        mode = ClientMode.CONNECTED;

        System.out.println("Connected to " + currentServer);
        startListenerThread(newIn);
      } catch (IOException e) {
        closeQuietly(newIn, newOut, newSocket);
        System.out.println("Connection failed: " + e.getMessage());
      }
    } finally {
      connectionLock.unlock();
    }
  }

  /** Closes the TCP socket and resets state to {@link ClientMode#LOCAL}. */
  public void disconnect() {
    connectionLock.lock();
    try {
      connected = false;
      currentServer = null;
      serverRequiresGui = false;
      guiWindowOpened = false;
      mode = ClientMode.LOCAL;
      closeCurrentConnection();
      System.out.println("Disconnected from server.");
    } finally {
      connectionLock.unlock();
    }
  }

  /**
   * Sends a text line to the connected server.
   *
   * @param message the line to send
   */
  public void send(String message) {
    connectionLock.lock();
    try {
      if (connected && out != null) {
        out.println(message);
      }
    } finally {
      connectionLock.unlock();
    }
  }

  /** Switches to {@link ClientMode#SERVER} mode. */
  public void enterServerMode() {
    mode = ClientMode.SERVER;
    System.out.println(
        "[mode] Now in SERVER mode. Client commands are disabled.\n"
            + "       Use 'server stop' to return to local mode.");
  }

  /** Returns to {@link ClientMode#LOCAL} mode. */
  public void exitServerMode() {
    mode = ClientMode.LOCAL;
    System.out.println("[mode] Server stopped. Back to LOCAL mode.");
  }

  /** Returns the current operating mode. */
  public ClientMode getMode() {
    return mode;
  }

  /** Returns {@code true} if the TCP socket is open. */
  public boolean isConnected() {
    connectionLock.lock();
    try {
      return connected;
    } finally {
      connectionLock.unlock();
    }
  }

  /** Returns the {@code "host:port"} string, or {@code null} when not connected. */
  public String getCurrentServer() {
    connectionLock.lock();
    try {
      return currentServer;
    } finally {
      connectionLock.unlock();
    }
  }

  /** Returns the default host used when no address is given to {@code join}. */
  public String getDefaultHost() {
    return DEFAULT_HOST;
  }

  /** Returns the default port used when no address is given to {@code join}. */
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  /**
   * Sets whether this client was launched with {@code --gui}.
   *
   * @param guiMode {@code true} if the client uses the graphical interface
   */
  public void setGuiMode(boolean guiMode) {
    this.guiMode = guiMode;
  }

  /**
   * Returns whether this client was launched with {@code --gui}.
   *
   * @return {@code true} if the client was launched in GUI mode
   */
  public boolean isGuiMode() {
    return guiMode;
  }

  /**
   * Returns {@code true} if the server sent {@code mode=GUI} in its
   * {@code WELCOME} response.
   *
   * @return {@code true} if the connected server requires GUI games
   */
  public boolean isServerRequiresGui() {
    return serverRequiresGui;
  }

  /**
   * Starts the background listener thread.
   *
   * @param reader the active reader bound to the current socket
   */
  private void startListenerThread(BufferedReader reader) {
    Thread listener =
        new Thread(
            () -> {
              try {
                String response;
                while ((response = reader.readLine()) != null) {
                  handleServerMessage(response);
                  printRemotePromptIfConnected();
                }
              } catch (IOException ignored) {
                // Cleanup is handled below.
              } finally {
                handleUnexpectedServerStop();
              }
            },
            "server-listener");
    listener.setDaemon(true);
    listener.start();
  }

  /**
   * Prints the remote prompt if the client is still connected.
   */
  private void printRemotePromptIfConnected() {
    connectionLock.lock();
    try {
      if (connected) {
        System.out.print("[" + currentServer + "] > ");
      }
    } finally {
      connectionLock.unlock();
    }
  }

  /**
   * Handles an unexpected remote disconnection.
   */
  private void handleUnexpectedServerStop() {
    connectionLock.lock();
    try {
      if (!connected) {
        return;
      }

      System.out.println("\n[!] Connection closed by server.");
      connected = false;
      currentServer = null;
      serverRequiresGui = false;
      guiWindowOpened = false;
      mode = ClientMode.LOCAL;
      closeCurrentConnection();
      System.out.print("[local] > ");
    } finally {
      connectionLock.unlock();
    }
  }

  /**
   * Processes a single line received from the server.
   *
   * <ul>
   *   <li>{@code WELCOME … mode=GUI} — sets {@link #serverRequiresGui}.</li>
   *   <li>{@code GAME_START … mode=GUI} — opens the GUI for GUI clients, then starts
   *       the local game.</li>
   *   <li>{@code MOVE_OK / OPPONENT_MOVE} — applies the move on the local board.</li>
   *   <li>{@code INVITATION_*} — prints invitation prompts and status.</li>
   *   <li>{@code STATUS_CHANGED} — prints presence update.</li>
   *   <li>Everything else — printed as-is.</li>
   * </ul>
   *
   * @param message the raw line received from the server
   */
  private void handleServerMessage(String message) {
    if ("BYE".equals(message)) {
      System.out.println("\nServer: " + message);
      disconnect();
      System.out.print("[local] > ");
      return;
    }

    if (message.startsWith("WELCOME")) {
      serverRequiresGui = message.contains("mode=GUI");
      if (serverRequiresGui && !guiMode) {
        System.out.println(
            "\n[!] Warning: this server is in GUI-only mode "
                + "but you are using a CLI client.\n"
                + "    Games will not render graphically. "
                + "Reconnect with --gui to play properly.");
      }
      System.out.println("\nServer: " + message);
      return;
    }

    if (message.startsWith("GAME_START")) {
      boolean requiresGui = message.contains("mode=GUI");

      if (requiresGui && guiMode) {
        openGuiWindow();
      }

      GameController localController = controller;
      if (localController != null) {
        localController.stopBlitzTimer();
        localController.startNewGame(Configuration.getDefaultConfiguration());
      }

      System.out.println("\nGame started! " + message);
      return;
    }

    if (message.startsWith("MOVE_OK ")) {
      applyMoveMessage(message, "MOVE_OK ", "You played: ");
      return;
    }

    if (message.startsWith("OPPONENT_MOVE ")) {
      applyMoveMessage(message, "OPPONENT_MOVE ", "Opponent played: ");
      return;
    }

    if (message.startsWith("INVITATION_RECEIVED")) {
      System.out.println("\n╔══ INVITATION ══════════════════════════════════╗");
      System.out.println("║  " + message);
      System.out.println("║  Type 'accept' to accept or 'decline' to refuse.");
      System.out.println("╚════════════════════════════════════════════════╝");
    } else if (message.startsWith("INVITATION_SENT")) {
      System.out.println(
          "\n[invitation] Invitation sent. "
              + message.substring("INVITATION_SENT".length()).trim());
      System.out.println(
          "[invitation] Waiting for a response... (type 'cancel' to withdraw)");
    } else if (message.startsWith("INVITATION_ACCEPTED")) {
      System.out.println(
          "\n[invitation] Your invitation was accepted! "
              + message.substring("INVITATION_ACCEPTED".length()).trim());
    } else if (message.startsWith("INVITATION_DECLINED")) {
      System.out.println(
          "\n[invitation] "
              + message.substring("INVITATION_DECLINED".length()).trim()
              + " declined your invitation.");
    } else if (message.startsWith("INVITATION_CANCELLED")) {
      System.out.println(
          "\n[invitation] The invitation was cancelled. "
              + message.substring("INVITATION_CANCELLED".length()).trim());
    } else if (message.startsWith("INVITATION_EXPIRED")) {
      System.out.println(
          "\n[invitation] An invitation expired: "
              + message.substring("INVITATION_EXPIRED".length()).trim());
    } else if (message.startsWith("STATUS_CHANGED")) {
      System.out.println(
          "\n[status] Your status is now: "
              + message.substring("STATUS_CHANGED".length()).trim());
    } else {
      System.out.println("\nServer: " + message);
    }
  }

  /**
   * Applies a move sent by the server to the local controller.
   *
   * @param message raw server message
   * @param prefix command prefix
   * @param humanPrefix text shown to the user
   */
  private void applyMoveMessage(
      String message, String prefix, String humanPrefix) {
    String moveArg = message.substring(prefix.length()).trim();
    String[] parts = moveArg.split("-");
    GameController localController = controller;

    if (parts.length == 2 && localController != null) {
      System.out.println("\n" + humanPrefix + moveArg);
      try {
        localController.executeMove(
            parts[0].trim(),
            parts[1].trim(),
            false);
      } catch (Exception e) {
        System.out.println(
            "[warning] Could not apply local move: " + e.getMessage());
      }
    } else {
      System.out.println("\nServer: " + message);
    }
  }

  /**
   * Opens the JavaFX window for a network GUI game once per connection.
   */
  private void openGuiWindow() {
    boolean shouldOpen = false;

    connectionLock.lock();
    try {
      if (!guiWindowOpened) {
        guiWindowOpened = true;
        shouldOpen = true;
      }
    } finally {
      connectionLock.unlock();
    }

    if (!shouldOpen) {
      return;
    }

    GraphicalUserInterface gui = new GraphicalUserInterface(false, this);
    gui.setController(controller);
    gui.start();

    System.out.println("[GUI] Window opened for network game.");
  }

  /**
   * Closes the current connection resources.
   *
   * <p>Must be called while holding {@link #connectionLock}.
   */
  private void closeCurrentConnection() {
    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException ignored) {
      // Ignore close failure.
    }

    if (out != null) {
      out.close();
    }

    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException ignored) {
      // Ignore close failure.
    } finally {
      in = null;
      out = null;
      socket = null;
    }
  }

  /**
   * Closes partially initialized resources without throwing.
   *
   * @param reader temporary reader
   * @param writer temporary writer
   * @param socketToClose temporary socket
   */
  private void closeQuietly(
      BufferedReader reader,
      PrintWriter writer,
      Socket socketToClose) {
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (IOException ignored) {
      // Ignore partial cleanup failure.
    }

    if (writer != null) {
      writer.close();
    }

    try {
      if (socketToClose != null && !socketToClose.isClosed()) {
        socketToClose.close();
      }
    } catch (IOException ignored) {
      // Ignore partial cleanup failure.
    }
  }
}