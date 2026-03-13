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

  private final String serverName;
  private final int tcpPort;
  private static final int DISCOVERY_PORT = 12346; // port UDP pour la découverte
  private static final int BROADCAST_INTERVAL = 10000; // 10 secondes

  public DiscoveryService(String serverName, int tcpPort) {
    this.serverName = serverName;
    this.tcpPort = tcpPort;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setBroadcast(true);
      InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255"); // broadcast

      String localIp = InetAddress.getLocalHost().getHostAddress();
      String message = serverName + ":" + localIp + ":" + tcpPort;
      byte[] buffer = message.getBytes();

      System.out.println("Discovery service started, broadcasting every 10 seconds...");

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress,
              DISCOVERY_PORT);
        socket.send(packet);
        //System.out.println("Broadcast sent: " + message);
        Thread.sleep(BROADCAST_INTERVAL);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
