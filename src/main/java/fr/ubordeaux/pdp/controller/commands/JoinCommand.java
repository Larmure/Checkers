package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Commande CLIENT : {@code join [IP[:PORT]]}
 * Se connecte au serveur de jeu à l'adresse indiquée.
 * Par défaut : {@code localhost:12345}.
 * Distinction des responsabilités :
 * - Cette classe gère la logique de parsing de l'adresse et délègue
 *   la connexion TCP à {@link ClientSession}.
 * - Elle n'a aucune connaissance de {@code GameController} ou {@code GameServer}.
 */
public class JoinCommand implements ClientCommand, Helpable {

  /** The client session to which this command belongs. */
  private final ClientSession session;
  /** The address provided by the user. */
  private final String address; // peut être null → valeurs par défaut

  /**
   * Creates a join command for the current client session.
   *
   * @param session current client session
   * @param address address provided by the user ({@code "host:port"},
   *     {@code "host"}, or {@code null} for default values)
   */
  public JoinCommand(ClientSession session, String address) {
    this.session = session;
    this.address = address;
  }

  /** Executes the join command. */
  @Override
  public void execute() {
    String host = session.getDefaultHost();
    int port = session.getDefaultPort();

    if (address != null && !address.isBlank()) {
      String[] parts = address.split(":");
      host = parts[0].trim();
      if (parts.length >= 2) {
        try {
          port = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
          System.out.println("Invalid port in '" + address
              + "'. Using default: " + session.getDefaultPort());
        }
      }
    }

    session.connect(host, port);
  }

  /** Returns the help message for this command. */
  @Override
  public String getHelp() {
    return "join [IP[:PORT]] — Connects to a game server. "
        + "Defaults to localhost:12345 if no address is given.";
  }
}