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

/**
 * Holds the TCP connection state and current operating mode for the game client.
 *
 * <h2>Handshake</h2>
 *
 * <p>{@link fr.ubordeaux.pdp.controller.commands.JoinCommand} calls
 * {@link #connect(String, int)} to open the socket, then sends
 * {@code REGISTER <id> <name> <GUI|CLI>} via {@link #send(String)}.
 * The server replies with {@code WELCOME <id> mode=<GUI|ANY>}. The listener
 * thread parses that response and sets {@link #serverRequiresGui} accordingly.
 *
 * <h2>Mode flag flow</h2>
 *
 * <ol>
 *   <li>{@code App} calls {@code session.setGuiMode(true)} when
 *       {@code --gui} is present.</li>
 *   <li>{@code JoinCommand.execute()} appends {@code GUI} or {@code CLI}
 *       to the {@code REGISTER} line.</li>
 *   <li>The listener thread reads {@code WELCOME … mode=GUI} and sets
 *       {@link #serverRequiresGui} so the application can warn or adapt the UI.</li>
 *   <li>When {@code GAME_START … mode=GUI} arrives and the client is GUI,
 *       a graphical window is opened automatically before the local game starts.</li>
 * </ol>
 */
public class ClientSession {

  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 12345;

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private boolean connected = false;
  private String currentServer = null;

  /** Current operating mode — drives command availability in the client shell. */
  private ClientMode mode = ClientMode.LOCAL;

  /**
   * {@code true} when this client was started with {@code --gui}.
   * Must be set before {@link #connect(String, int)} is called so
   * {@code JoinCommand} can read it when building the {@code REGISTER} line.
   */
  private boolean guiMode = false;

  /**
   * Set to {@code true} by the listener thread when the server
   * {@code WELCOME} response contains {@code mode=GUI}.
   */
  private volatile boolean serverRequiresGui = false;

  /** Prevents opening the GUI window more than once per connection. */
  private boolean guiWindowOpened = false;

  /** Local controller — used to apply opponent moves on the local board. */
  private GameController controller;

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
    if (connected) {
      System.out.println(
          "Already connected to " + currentServer + ". Type 'quit' to disconnect first.");
      return;
    }

    System.out.println("Connecting to " + host + ":" + port + "...");

    try {
      socket = new Socket(host, port);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out = new PrintWriter(
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

  /** Closes the TCP socket and resets state to {@link ClientMode#LOCAL}. */
  public void disconnect() {
    connected = false;
    currentServer = null;
    serverRequiresGui = false;
    guiWindowOpened = false;
    mode = ClientMode.LOCAL;
    closeQuietly();
    System.out.println("Disconnected from server.");
  }

  /**
   * Sends a text line to the connected server.
   *
   * @param message the line to send
   */
  public void send(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  /** Switches to {@link ClientMode#SERVER} mode. */
  public void enterServerMode() {
    mode = ClientMode.SERVER;
    System.out.println("[mode] Now in SERVER mode. Client commands are disabled.\n"
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
    return connected;
  }

  /** Returns the {@code "host:port"} string, or {@code null} when not connected. */
  public String getCurrentServer() {
    return currentServer;
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

  /** Starts the background listener thread. */
  private void startListenerThread() {
    Thread listener = new Thread(() -> {
      try {
        String response;
        while ((response = in.readLine()) != null) {
          handleServerMessage(response);
          if (connected) {
            System.out.print("[" + currentServer + "] > ");
          }
        }
      } catch (IOException ignored) {
        // Connection cleanup is handled in finally.
      } finally {
        if (connected) {
          System.out.println("\n[!] Connection closed by server.");
          disconnect();
          System.out.print("[local] > ");
        }
      }
    }, "server-listener");
    listener.setDaemon(true);
    listener.start();
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

    } else if (message.startsWith("GAME_START")) {
      boolean requiresGui = message.contains("mode=GUI");



      if (requiresGui && guiMode) {
        openGuiWindow();
      }

      if (controller != null) {
        controller.stopBlitzTimer();
        controller.startNewGame(Configuration.getDefaultConfiguration());
      }

      System.out.println("\nGame started! " + message);

    } else if (message.startsWith("MOVE_OK ")) {
      String moveArg = message.substring("MOVE_OK ".length()).trim();
      String[] parts = moveArg.split("-");
      if (parts.length == 2 && controller != null) {
        System.out.println("\nYou played: " + moveArg);
        try {
          controller.executeMove(parts[0].trim(), parts[1].trim(), false);
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
          controller.executeMove(parts[0].trim(), parts[1].trim(), false);
        } catch (Exception e) {
          System.out.println("[warning] Could not apply opponent move: " + e.getMessage());
        }
      } else {
        System.out.println("\nServer: " + message);
      }

    } else if (message.startsWith("INVITATION_RECEIVED")) {
      System.out.println("\n╔══ INVITATION ══════════════════════════════════╗");
      System.out.println("║  " + message);
      System.out.println("║  Type 'accept' to accept or 'decline' to refuse.");
      System.out.println("╚════════════════════════════════════════════════╝");

    } else if (message.startsWith("INVITATION_SENT")) {
      System.out.println("\n[invitation] Invitation sent. "
          + message.substring("INVITATION_SENT".length()).trim());
      System.out.println("[invitation] Waiting for a response... (type 'cancel' to withdraw)");

    } else if (message.startsWith("INVITATION_ACCEPTED")) {
      System.out.println("\n[invitation] Your invitation was accepted! "
          + message.substring("INVITATION_ACCEPTED".length()).trim());

    } else if (message.startsWith("INVITATION_DECLINED")) {
      System.out.println("\n[invitation] "
          + message.substring("INVITATION_DECLINED".length()).trim()
          + " declined your invitation.");

    } else if (message.startsWith("INVITATION_CANCELLED")) {
      System.out.println("\n[invitation] The invitation was cancelled. "
          + message.substring("INVITATION_CANCELLED".length()).trim());

    } else if (message.startsWith("INVITATION_EXPIRED")) {
      System.out.println("\n[invitation] An invitation expired: "
          + message.substring("INVITATION_EXPIRED".length()).trim());

    } else if (message.startsWith("STATUS_CHANGED")) {
      System.out.println("\n[status] Your status is now: "
          + message.substring("STATUS_CHANGED".length()).trim());

    } else {
      System.out.println("\nServer: " + message);
    }
  }

  /**
   * Opens the JavaFX window for a network GUI game.
   *
   * <p>The same {@link GameController} instance is reused so subsequent
   * {@code MOVE_OK} and {@code OPPONENT_MOVE} updates keep refreshing the
   * same local model.
   */
  private void openGuiWindow() {
    if (guiWindowOpened) {
      return;
    }

    guiWindowOpened = true;

    GraphicalUserInterface gui = new GraphicalUserInterface(false, this);
    gui.setController(controller);
    gui.start();

    System.out.println("[GUI] Window opened for network game.");
  }

  /** Closes all socket resources quietly. */
  private void closeQuietly() {
    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException ignored) {
      // Ignore close failure.
    }
    try {
      if (out != null) {
        out.close();
      }
    } catch (Exception ignored) {
      // Ignore close failure.
    }
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException ignored) {
      // Ignore close failure.
    }
  }
}