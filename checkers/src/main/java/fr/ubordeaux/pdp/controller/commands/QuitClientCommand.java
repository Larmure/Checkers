package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.serveur.ClientSession;

/**
 * Commande CLIENT : {@code quit}
 *
 * Comportement contextuel :
 * - Si connecté à un serveur → envoie {@code QUIT} (le serveur répond {@code BYE})
 *   puis ferme la connexion locale. Le client revient en mode local.
 * - Si déjà en mode local → retourne {@code true} via {@link #shouldExit()}
 *   pour signaler à la boucle principale qu'il faut quitter l'application.
 *
 * Distinction des responsabilités :
 * - Cette classe gère la logique de déconnexion et le signal de sortie.
 * - La fermeture effective du socket est déléguée à {@link ClientSession}.
 */
public class QuitClientCommand implements ClientCommand, Helpable {

  private final ClientSession session;
  /** Indique si la boucle principale doit terminer le programme. */
  private boolean exit = false;

  public QuitClientCommand(ClientSession session) {
    this.session = session;
  }

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
   * @return {@code true} si la boucle principale doit terminer.
   *         À appeler après {@link #execute()}.
   */
  public boolean shouldExit() {
    return exit;
  }

  @Override
  public String getHelp() {
    return "quit — Disconnects from the server (if connected) or exits the client.";
  }
}