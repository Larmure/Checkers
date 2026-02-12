package fr.ubordeaux.pdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for App command line arguments (F1). Uses App.run() to test logic without triggering
 * System.exit().
 */
public class AppTest {

  @BeforeEach
  public void setUp() {
    App.reset();
  }

  @Test
  public void testHelpOption() {
    String[] args = {"-h"};
    // We expect status 1 (Stop cleanly)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Help option should return status 1");
  }

  @Test
  public void testVersionOption() {
    String[] args = {"-V"};
    // We expect status 1 (Stop cleanly)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Version option should return status 1");
  }

  @Test
  public void testVerboseOption() {
    String[] args = {"-v"};
    // We expect status 0 (Continue)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status, "Verbose option should return status 0");
    assertTrue(App.isVerbose(), "Verbose flag should be true");
  }

  @Test
  public void testDebugOption() {
    String[] args = {"-d"};
    // We expect status 0 (Continue)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isDebug(), "Debug flag should be true");
  }

  @Test
  public void testInvalidOption() {
    String[] args = {"-z"}; // Non-existent option
    // We expect status 2 (Error)
    int status = App.run(args);
    assertEquals(App.EXIT_ERROR, status, "Invalid option should return status 2");
  }

  @Test
  public void testNoOptions() {
    String[] args = {};
    // We expect status 0 (Continue)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertFalse(App.isVerbose());
    assertFalse(App.isDebug());
  }
}