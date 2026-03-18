package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Client command: {@code quit}.
 *
 * <p>Context-sensitive behaviour:
 * <ul>
 *   <li>If connected to a server — sends {@code QUIT}, disconnects, and returns the
 *       session to {@link fr.ubordeaux.pdp.server.ClientMode#LOCAL}.</li>
 *   <li>If in LOCAL mode — signals the main loop to exit the program via
 *       {@link #shouldExit()}.</li>
 * </ul>
 *
 * <p>Category: [CLIENT]
 */
public class QuitClientCommand implements ClientCommand, Helpable {

  private final ClientSession session;

  /** Set to {@code true} when the program should terminate. */
  private boolean exit = false;

  /**
   * Creates a quit command for the current client session.
   *
   * @param session the current client session.
   */
  public QuitClientCommand(ClientSession session) {
    this.session = session;
  }

  @Override
  public void execute() {
    if (session.isConnected()) {
      // Notify the server then close the local socket.
      // disconnect() internally resets the mode to LOCAL.
      session.send("QUIT");
      session.disconnect();
    } else {
      System.out.println("Exiting client. Goodbye!");
      exit = true;
    }
  }

  /**
   * Returns whether the main loop should terminate the program.
   *
   * <p>This method should be called after {@link #execute()}.
   *
   * @return {@code true} if the main loop should terminate the program
   */
  public boolean shouldExit() {
    return exit;
  }

  @Override
  public String getHelp() {
    return "quit — Disconnects from the server (if connected) or exits the client.";
  }
}