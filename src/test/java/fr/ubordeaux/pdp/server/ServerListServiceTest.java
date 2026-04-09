package fr.ubordeaux.pdp.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.model.tools.Internationalization;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ServerListService}.
 *
 * <p>These tests do not try to simulate real UDP broadcasts. They simply verify
 * that discovery starts correctly, prints user-facing information, and returns
 * a non-null result.
 */
class ServerListServiceTest {

  @Test
  void discoverServers_returnsNonNullList() {
    List<String> servers = ServerListService.discoverServers();
    assertNotNull(servers, "discoverServers() doit toujours renvoyer une liste non nulle.");
  }

  @Test
  void discoverServers_printsLocalAddressOrFallbackAndScanMessage() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));

    try {
      List<String> servers = ServerListService.discoverServers();

      assertNotNull(servers, "La liste retournée ne doit pas être nulle.");

      String stdout = out.toString();
      String stderr = err.toString();

      boolean printedAddress = stdout.contains("Your IP address")
          || stdout.contains(Internationalization.get("server.list.local_ip_unknown"));

      boolean printedScanMessage = stdout.contains(
          Internationalization.get("server.list.scanning"))
          || stderr.contains(Internationalization.get("server.list.port_in_use", 12346))
          || stderr.contains("Discovery");

      assertTrue(
          printedAddress,
          "Le service doit afficher l'adresse IP locale ou un message de repli.");
      assertTrue(
          printedScanMessage,
          "Le service doit afficher soit le message de scan, soit un message d'erreur réseau.");
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }
}