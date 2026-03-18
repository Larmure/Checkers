package fr.ubordeaux.pdp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers game servers broadcasting on the local network via UDP.
 *
 * <p>Listens on the well-known discovery port {@value #DISCOVERY_PORT} for 30 seconds.
 * A server is considered alive as long as it sent a broadcast packet within the last
 * {@value #SERVER_EXPIRY_MS} ms. Servers that exceed this threshold are removed from
 * the result set.
 */
public class ServerListService {

  private static final int DISCOVERY_PORT = 12346;
  private static final int LISTEN_DURATION_MS = 30_000;
  private static final int SOCKET_TIMEOUT_MS = 1_000;
  private static final int SERVER_EXPIRY_MS = 30_000;

  private ServerListService() {}

  /**
   * Listens for UDP broadcasts for {@value #LISTEN_DURATION_MS} ms and returns the
   * servers that are still considered active.
   *
   * <p>Broadcast packet format: {@code name:ip:port}
   *
   * @return list of server descriptors in {@code "name:ip:port"} format.
   */
  public static List<String> discoverServers() {
    Map<String, Long> serverTimestamps = new ConcurrentHashMap<>();

    try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
      socket.setSoTimeout(SOCKET_TIMEOUT_MS);

      byte[] buffer = new byte[1024];
      long startTime = System.currentTimeMillis();

      System.out.println("Listening for server broadcasts (30s)...");

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
              System.out.println("Found: " + parts[0] + " at " + parts[1] + ":" + parts[2]);
            }
          }
        } catch (SocketTimeoutException ignored) {
        }

        long now = System.currentTimeMillis();
        serverTimestamps.entrySet().removeIf(
              entry -> {
                boolean expired = (now - entry.getValue()) > SERVER_EXPIRY_MS;
                if (expired) {
                  System.out.println("Expired: " + entry.getKey());
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
}