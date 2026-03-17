package fr.ubordeaux.pdp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovery service for game servers available on the local network.
 * Listens for UDP broadcasts for 30 seconds and automatically removes
 * servers whose last message is older than 30 seconds.
 */
public class ServerListService {

  /** The UDP port for discovery broadcasts. */
  private static final int DISCOVERY_PORT = 12346;
  /** Total listening duration: 30 seconds as specified. */
  private static final int LISTEN_DURATION_MS = 30_000;
  /** Short socket timeout to remain responsive in the loop. */
  private static final int SOCKET_TIMEOUT_MS = 1_000;
  /** A server is considered dead if it has not broadcast for 30 seconds. */
  private static final int SERVER_EXPIRY_MS = 30_000;

  /**
   * Listens for UDP broadcasts for {@value #LISTEN_DURATION_MS} ms
   * and returns the list of still-active servers (message received within
   * the last {@value #SERVER_EXPIRY_MS} ms).
   *
   * @return List of strings in the format {@code name:ip:port}.
   */
  public static List<String> discoverServers() {
    // key = "name:ip:port", value = timestamp of the last received message
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
              System.out.println("Server found: " + parts[0]
                  + " at " + parts[1] + ":" + parts[2]);
            }
          }

        } catch (SocketTimeoutException e) {
          // No packet received during the time window — continue normally
        }

        // Remove expired servers (no message received for more than 30 seconds)
        long now = System.currentTimeMillis();
        serverTimestamps.entrySet().removeIf(entry -> {
          boolean expired = (now - entry.getValue()) > SERVER_EXPIRY_MS;
          if (expired) {
            System.out.println("Server expired: " + entry.getKey());
          }
          return expired;
        });
      }

    } catch (java.net.BindException e) {
      System.err.println("Port " + DISCOVERY_PORT
          + " already in use (another discovery running?).");
    } catch (Exception e) {
      System.err.println("Error during server discovery: " + e.getMessage());
    }

    return new ArrayList<>(serverTimestamps.keySet());
  }

  /**
   * Displays the list of available servers in a formatted way.
   */
  public static void displayAvailableServers() {
    List<String> servers = discoverServers();

    if (servers.isEmpty()) {
      System.out.println("No game servers available.");
    } else {
      System.out.println("\n=== Available Game Servers ===");
      for (String s : servers) {
        String[] parts = s.split(":");
        if (parts.length == 3) {
          System.out.println("- " + parts[0] + " | Address: " + parts[1] + ":" + parts[2]);
        }
      }
      System.out.println("==============================\n");
    }
  }
}