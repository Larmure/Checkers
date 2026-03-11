package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.serveur.GameServer;

/**
 * Network command: {@code server start [PORT]}
 *
 * Starts a TCP game server on the specified port (default: 12345).
 * Displays an explicit error message if the port is already in use.
 * The server runs in a daemon thread and delegates game logic
 * to the current {@link GameController}.
 */
public class ServerStartCommand implements Command, Helpable {

    private static final int DEFAULT_PORT = 12345;

    private final GameController controller;
    private final String[] args;

    /** Static reference allowing {@link ServerStopCommand} to stop the server. */
    public static GameServer activeServer = null;

    public ServerStartCommand(GameController controller, String[] args) {
        this.controller = controller;
        this.args = args;
    }

    @Override
    public void execute() {
        if (activeServer != null && activeServer.isRunning()) {
            System.out.println("A server is already running on port "
                    + activeServer.getPort() + ".");
            return;
        }

        int port = DEFAULT_PORT;
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0]
                        + "'. Using default port: " + DEFAULT_PORT);
            }
        }

        final int finalPort = port;
        activeServer = new GameServer("GameServer", finalPort, controller);

        GameServer ref = activeServer;
        Thread serverThread = new Thread(() -> {
            try {
                ref.start();
            } catch (java.io.IOException e) {
                // Explicit message returned by GameServer if the port is already in use
                System.out.println("✗ Unable to start the server: " + e.getMessage());
                activeServer = null;
            }
        }, "game-server-thread");
        serverThread.setDaemon(true);
        serverThread.start();

        // Short delay to give the server time to initialize
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}

        if (activeServer != null && activeServer.isRunning()) {
            System.out.println("✓ Server started on port " + finalPort);
        }
    }

    @Override
    public String getHelp() {
        return "server start [PORT] — Starts a game server on port PORT "
             + "(default: 12345). Displays an error if the port is already in use.";
    }
}