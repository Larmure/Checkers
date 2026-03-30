package fr.ubordeaux.pdp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers game servers broadcasting on the local network via UDP.
 *
 * <p>Listens on the well-known discovery port {@value #DISCOVERY_PORT} for up to
 * {@value #LISTEN_DURATION_MS} ms. A server is considered alive as long as it sent a
 * broadcast packet within the last {@value #SERVER_EXPIRY_MS} ms.
 *
 * <p>Also prints the local machine's IP address before scanning so the user knows which
 * address to share with other players for the {@code join} command.
 */
public class ServerListService {

  private static final int DISCOVERY_PORT = 12346;
  private static final int LISTEN_DURATION_MS = 30_000;
  private static final int SOCKET_TIMEOUT_MS = 1_000;
  private static final int SERVER_EXPIRY_MS = 10_000;

  private ServerListService() {}

  /**
   * Listens for UDP broadcasts and returns servers that are still active.
   *
   * <p>Broadcast packet format: {@code name:ip:port}
   *
   * <p>Prints the local machine's IP address before scanning so the user can share it
   * with other players.
   *
   * @return list of server descriptors in {@code "name:ip:port"} format.
   */
  public static List<String> discoverServers() {
    printLocalAddress();

    Map<String, Long> serverTimestamps = new ConcurrentHashMap<>();

    try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
      socket.setSoTimeout(SOCKET_TIMEOUT_MS);

      byte[] buffer = new byte[1024];
      long startTime = System.currentTimeMillis();

      System.out.println("Scanning for servers (30s)...");

      while (System.currentTimeMillis() - startTime < LISTEN_DURATION_MS) {
        try {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          socket.receive(packet);

          String message = new String(packet.getData(), 0, packet.getLength()).trim();
          long now = System.currentTimeMillis();
          boolean isNew = !serverTimestamps.containsKey(message);
          serverTimestamps.put(message, now);

          if (isNew) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
              System.out.println("  Found: " + parts[0] + " at " + parts[1] + ":" + parts[2]);
            }
          }
        } catch (SocketTimeoutException ignored) {
          // Keep listening until the scan window expires.
        }

        long now = System.currentTimeMillis();
        serverTimestamps.entrySet().removeIf(
              entry -> {
                boolean expired = (now - entry.getValue()) > SERVER_EXPIRY_MS;
                if (expired) {
                  System.out.println("  Expired: " + entry.getKey());
                }
                return expired;
              });
      }
    } catch (java.net.BindException e) {
      System.err.println("Port " + DISCOVERY_PORT + " already in use.");
    } catch (Exception e) {
      System.err.println("Discovery error: " + e.getMessage());
    }

    return new ArrayList<>(serverTimestamps.keySet());
  }

  /**
   * Prints the local machine's IP address so the user knows which address to share
   * with other players for the {@code join} command.
   */
  private static void printLocalAddress() {
    try {
      String localIp = InetAddress.getLocalHost().getHostAddress();
      System.out.println("Your IP address : " + localIp);
      System.out.println("Share with others: join " + localIp + ":<port>");
    } catch (Exception e) {
      System.out.println("Your IP address : (could not determine)");
    }
  }
}