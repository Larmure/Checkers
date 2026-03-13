package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Concrete implementation of {@link ClientCommand} used to quit the client or disconnect 
 * from the server.
 * 
 * <p>This command allows the user to either disconnect from the currently connected server 
 * (if any) or exit the client application if not connected. It updates the client's session 
 * state accordingly and provides feedback to the user about the action taken.
 *
 * @version 1.0
 */
public class QuitClientCommand implements ClientCommand, Helpable {
  /** The client session to which this command belongs. */
  private final ClientSession session;
  /** Indicates whether the client should exit. */
  private boolean exit = false;

  /**
   * Constructs a QuitClientCommand with the given client session.
   *
   * @param session the current client session, used to manage connection state
   */
  public QuitClientCommand(ClientSession session) {
    this.session = session;
  }

  /**
   * Executes the quit command by disconnecting from the server if currently connected, or
   * exiting the client application if not connected. 
   */
  @Override
  public void execute() {
    if (session.isConnected()) {
      // Notifie le serveur puis ferme la connexion locale
      session.send("QUIT");
      session.disconnect();
      // On reste dans la boucle principale (retour en [local])
    } else {
      // En mode local → signal de sortie du programme
      System.out.println("Exiting client. Goodbye!");
      exit = true;
    }
  }

  /**
   * Returns whether the client should exit.
   *
   * @return true if the client should exit, false otherwise.
   */
  public boolean shouldExit() {
    return exit;
  }

  /**
   * Returns the help message for this command.
   *
   * @return a string describing how to use the quit command
   */
  @Override
  public String getHelp() {
    return "quit — Disconnects from the server (if connected) or exits the client.";
  }
}