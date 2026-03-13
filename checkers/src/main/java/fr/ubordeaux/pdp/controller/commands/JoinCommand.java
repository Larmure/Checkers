package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.serveur.ClientSession;

/**
 * Client command: {@code join [IP[:PORT]]}
 *
 * <p>Connects to a game server. On success, the session switches to
 * {@link fr.ubordeaux.pdp.serveur.ClientMode#CONNECTED}, which blocks
 * {@code server start} until the client disconnects.
 *
 * <p>Defaults to {@code localhost:12345} if no address is provided.
 *
 * <p>Category: [CLIENT]
 */
public class JoinCommand implements ClientCommand, Helpable {

  private final ClientSession session;
  private final String address;

  /**
   * @param session the current client session.
   * @param address target address as {@code "host:port"}, {@code "host"}, or {@code null}
   *                to use the defaults.
   */
  public JoinCommand(ClientSession session, String address) {
    this.session = session;
    this.address = address;
  }

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
          System.out.println(
            "Invalid port in '" + address + "'. Using default: " + session.getDefaultPort());
        }
      }
    }

    // connect() internally sets mode to CONNECTED on success
    session.connect(host, port);
  }

  @Override
  public String getHelp() {
    return "join [IP[:PORT]] — Connects to a game server (default: localhost:12345).";
  }
}