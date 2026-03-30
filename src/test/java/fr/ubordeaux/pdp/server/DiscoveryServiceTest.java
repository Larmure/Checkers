package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.junit.jupiter.api.Test;

class DiscoveryServiceTest {

  @Test
  void shouldBroadcastServerPresence() throws Exception {
    DatagramSocket listener = new DatagramSocket(12346);
    listener.setSoTimeout(2000);

    DiscoveryService service = new DiscoveryService("GameServer", 12345);
    Thread thread = new Thread(service);
    thread.start();

    byte[] buffer = new byte[1024];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    listener.receive(packet);

    String message = new String(packet.getData(), 0, packet.getLength());

    service.stop();
    thread.join(1000);
    listener.close();

    assertTrue(message.contains("GameServer"));
    assertTrue(message.contains("12345"));
  }
}