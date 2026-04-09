package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;
import java.util.Scanner;

/**
 * Client command: {@code join [IP[:PORT]]}.
 *
 * <p>Connects to a game server and performs the mandatory {@code REGISTER}
 * handshake. On success, the session switches to
 * {@link fr.ubordeaux.pdp.server.ClientMode#CONNECTED}.
 *
 * <h2>Protocol sequence</h2>
 *
 * <pre>
 * Client → Server : (TCP connect)
 * Client → Server : REGISTER &lt;id&gt; &lt;name&gt; &lt;GUI|CLI&gt;
 * Server → Client : WELCOME &lt;id&gt; mode=&lt;GUI|ANY&gt;   (success)
 * Server → Client : ERROR …                         (ID taken / bad format)
 * </pre>
 *
 * <p>The interface mode token ({@code GUI} or {@code CLI}) is chosen at
 * connection time, independently from local game startup. This allows the user
 * to decide which kind of network game to declare when connecting.
 *
 * <p>After the socket is open, the player is prompted for an ID and for the
 * network interface mode to register. The server response is handled by the
 * background listener thread started inside
 * {@link ClientSession#connect(String, int)}.
 *
 * <p>Defaults to {@code localhost:12345} if no address is provided.
 *
 * <p>Category: [CLIENT]
 */
public class JoinCommand implements ClientCommand, Helpable {

  private final ClientSession session;
  private final String address;

  /**
   * Creates a join command.
   *
   * @param session the current client session
   * @param address target address as {@code "host:port"}, {@code "host"}, or
   *     {@code null} to use the defaults ({@code localhost:12345})
   */
  public JoinCommand(ClientSession session, String address) {
    this.session = session;
    this.address = address;
  }

  /**
   * Executes the join sequence.
   *
   * <ol>
   *   <li>Resolve host and port from {@code address}.</li>
   *   <li>Open the TCP connection via {@link ClientSession#connect(String, int)}.</li>
   *   <li>Prompt the user for a player ID.</li>
   *   <li>Prompt the user for the network mode ({@code GUI} or {@code CLI}).</li>
   *   <li>Send {@code REGISTER <id> <name> <GUI|CLI>}.</li>
   * </ol>
   */
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
              "Invalid port in '" + address + "'. Using default: "
                  + session.getDefaultPort());
        }
      }
    }

    session.connect(host, port);

    if (!session.isConnected()) {
      return;
    }

    Scanner scanner = new Scanner(System.in);

    System.out.print("Player ID: ");
    String id = scanner.nextLine().trim();

    if (id.isBlank()) {
      System.out.println("Player ID cannot be empty. Disconnecting.");
      session.disconnect();
      return;
    }

    session.send("REGISTER " + id + " " + id);
    System.out.println("Registration sent. Waiting for server response...");
  }

  @Override
  public String getHelp() {
    return "join [IP[:PORT]] — Connects to a server and registers "
        + "(default: localhost:12345).";
  }
}