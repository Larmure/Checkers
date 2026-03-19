package fr.ubordeaux.pdp.controller.commands;

import java.util.Scanner;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Client command: {@code join [IP[:PORT]]}.
 *
 * <p>Connects to a game server and performs the mandatory {@code REGISTER} handshake.
 * On success, the session switches to {@link fr.ubordeaux.pdp.server.ClientMode#CONNECTED}.
 *
 * <p>Protocol sequence:
 *
 * <pre>
 * Client → Server: (TCP connect)
 * Client → Server: REGISTER &lt;id&gt; &lt;id&gt;
 * Server → Client: WELCOME &lt;id&gt;   (on success)
 * Server → Client: ERROR ...       (if ID already taken)
 * </pre>
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
   *     to use the defaults.
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

    // Step 1: open TCP connection.
    session.connect(host, port);

    if (!session.isConnected()) {
      return;
    }

    // Step 2: REGISTER handshake — server requires REGISTER <id> <name>.
    // The player ID is also used as the display name.
    Scanner scanner = new Scanner(System.in);
    System.out.print("Player ID: ");
    String id = scanner.nextLine().trim();

    if (id.isBlank()) {
      System.out.println("Player ID cannot be empty. Disconnecting.");
      session.disconnect();
      return;
    }

    session.send("REGISTER " + id + " " + id);
    System.out.println("Waiting for server response...");
    // Server response (WELCOME or ERROR) is printed by the listener thread.
  }

  @Override
  public String getHelp() {
    return "join [IP[:PORT]] — Connects to a server and registers (default: localhost:12345).";
  }
}