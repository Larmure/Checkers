package fr.ubordeaux.pdp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Broadcasts the presence of a game server over UDP.
 *
 * <p>This service periodically sends a discovery message containing the server
 * name, local IP address, and TCP port.
 */
public class DiscoveryService implements Runnable {
  /** The name of the game server. */
  private final String serverName;

  /** The TCP port for the game server. */
  private final int tcpPort;

  /** The UDP port for discovery messages. */
  private static final int DISCOVERY_PORT = 12346;

  /** The interval between broadcast messages. */
  private static final int BROADCAST_INTERVAL = 10000;

  /** Controls the broadcast loop lifecycle. */
  private volatile boolean running = true;

  /**
   * Constructs a DiscoveryService with the specified server name and TCP port.
   *
   * @param serverName the name of the game server to include in discovery messages
   * @param tcpPort the TCP port on which the game server is listening for client connections
   */
  public DiscoveryService(String serverName, int tcpPort) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
  }

  /**
   * Stops the discovery loop.
   */
  public void stop() {
    running = false;
  }

  /**
   * Runs the discovery service, broadcasting the server's presence at regular intervals.
   */
  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setBroadcast(true);
      InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

      String localIp = InetAddress.getLocalHost().getHostAddress();
      String message = serverName + ":" + localIp + ":" + tcpPort;
      byte[] buffer = message.getBytes();

      System.out.println("Discovery service started, broadcasting every 10 seconds...");

      while (running) {
        DatagramPacket packet =
            new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);
        socket.send(packet);
        Thread.sleep(BROADCAST_INTERVAL);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}