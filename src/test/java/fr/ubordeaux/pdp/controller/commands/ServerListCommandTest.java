package fr.ubordeaux.pdp.controller.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
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
    try (DatagramSocket occupied = new DatagramSocket(12346)) {
      System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
      System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));

      new ServerListCommand().execute();

      String out = outContent.toString(StandardCharsets.UTF_8);
      String err = errContent.toString(StandardCharsets.UTF_8);

      assertTrue(err.contains("Port 12346 already in use."));
      assertTrue(out.contains("No game servers found on the network."));
      assertTrue(out.contains("Your IP address :"));
    }
  }

  @Test
  void getHelp_describesTheCommand() {
    String help = new ServerListCommand().getHelp();

    assertTrue(help.contains("server list"));
    assertTrue(help.contains("30s scan"));
  }
}
