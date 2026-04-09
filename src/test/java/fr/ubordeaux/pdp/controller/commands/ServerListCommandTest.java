package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ServerListCommand}.
 */
class ServerListCommandTest {

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void execute_whenDiscoveryPortIsBusy_printsNoServersFound() throws Exception {
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));

    try (DatagramSocket occupied = new DatagramSocket(12346)) {
      new ServerListCommand().execute();
    } catch (java.net.BindException ignored) {
      // The port was already occupied before this test acquired it.
      new ServerListCommand().execute();
    }

    String out = outContent.toString(StandardCharsets.UTF_8);
    String err = errContent.toString(StandardCharsets.UTF_8);

    assertTrue(err.contains("Port 12346 already in use."));
    assertTrue(out.contains("Your IP address :"));
  }

  @Test
  void getHelp_describesTheCommand() {
    String help = new ServerListCommand().getHelp();

    assertTrue(help.contains("server list"));
    assertTrue(help.contains("30s scan"));
  }

  @Test
  void execute_whenServerBroadcastIsReceived_printsServerTable() {
    // Skip this integration test when another process already occupies discovery port.
    Assumptions.assumeTrue(isDiscoveryPortAvailable());

    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));

    assertTimeoutPreemptively(Duration.ofSeconds(50), () -> {
      Thread sender = new Thread(() -> {
        try {
          // Give the listener time to bind UDP 12346.
          Thread.sleep(300);
          try (DatagramSocket socket = new DatagramSocket()) {
            String msg = "Alpha:127.0.0.1:12345";
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), 12346);
            // Keep broadcasting during the whole 30s scan so the server entry does not expire.
            long deadline = System.currentTimeMillis() + 33_000;
            while (System.currentTimeMillis() < deadline) {
              socket.send(packet);
              Thread.sleep(1_000);
            }
          }
        } catch (Exception ignored) {
          // If sender fails, assertions below will fail because expected output is missing.
        }
      }, "server-list-test-sender");

      sender.setDaemon(true);
      sender.start();

      new ServerListCommand().execute();
    });

    String out = outContent.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("Available Game Servers"));
    assertTrue(out.contains("Alpha"));
    assertTrue(out.contains("127.0.0.1:12345"));
    assertTrue(out.contains("Use: join <ip>:<port> to connect."));
  }

  private static boolean isDiscoveryPortAvailable() {
    try (DatagramSocket ignored = new DatagramSocket(12346)) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
