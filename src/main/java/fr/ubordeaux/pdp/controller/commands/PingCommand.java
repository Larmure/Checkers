package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.ClientCommand;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.server.ClientSession;

/**
 * Client command: {@code ping}.
 *
 * <p>Sends a {@code PING} message to the server and measures the round-trip
 * time (RTT) on the client side. The server replies with
 * {@code PONG TIME=<ms>} (server processing time). The listener thread in
 * {@link ClientSession} displays that response.
 *
 * <p>Responsibility split:
 * <ul>
 *   <li>This class measures the client-side RTT and sends the {@code PING}
 *       command.</li>
 *   <li>It does not handle the response reception, which is delegated to the
 *       listener thread in {@link ClientSession}.</li>
 * </ul>
 */
public class PingCommand implements ClientCommand, Helpable {

  private final ClientSession session;

  /**
   * Creates a ping command bound to a client session.
   *
   * @param session the current client session
   */
  public PingCommand(ClientSession session) {
    this.session = session;
  }

  @Override
  public void execute() {
    if (!session.isConnected()) {
      System.out.println("Not connected to any server. Use 'join' first.");
      return;
    }

    long t0 = System.currentTimeMillis();
    session.send("PING");
    long rtt = System.currentTimeMillis() - t0;

    // The "PONG TIME=..." response is displayed by the listener thread.
    // This line shows the client-side measured RTT.
    System.out.println("PING sent. RTT (client-side): " + rtt + "ms");
  }

  @Override
  public String getHelp() {
    return "ping - Sends a PING to the server and displays the round-trip time (RTT).";
  }
}