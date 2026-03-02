package fr.ubordeaux.pdp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for App command line arguments (F1).
 * Uses App.run() to test logic without triggering System.exit().
 */
public class AppTest {

  @BeforeEach
  public void setUp() {
    App.reset();
  }

  @Test
  public void testHelpOption() {
    String[] args = { "-h" };
    // We expect status 1 (clean stop)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Help option should return status 1");
  }

  @Test
  public void testVersionOption() {
    String[] args = { "-V" };
    // We expect status 1 (clean stop)
    int status = App.run(args);
    assertEquals(App.EXIT_INFO, status, "Version option should return status 1");
  }

  @Test
  public void testVerboseOption() {
    String[] args = { "-v" };
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status, "Verbose option should return status 0");
    assertTrue(App.isVerbose(), "Verbose flag should be true");
  }

  @Test
  public void testDebugOption() {
    String[] args = { "-d" };
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isDebug(), "Debug flag should be true");
  }

  @Test
  public void testInvalidOption() {
    String[] args = { "-z" }; // Non-existent option
    // We expect status 2 (error)
    int status = App.run(args);
    assertEquals(App.EXIT_ERROR, status, "Invalid option should return status 2");
  }

  @Test
  public void testNoOptions() {
    String[] args = {};
    // We expect status 0 (continue execution)
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertFalse(App.isVerbose());
    assertFalse(App.isDebug());
  }

  @Test
  public void testBlitzAndTimeOptions() {
    String[] args = { "-b", "-t", "5" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    // Assuming you have static getters in App for these fields
    assertTrue(App.isBlitz(), "Blitz flag should be true");
    assertEquals(5, App.getTime(), "Time should be set to 5");
  }

  @Test
  public void testSizeAndContestOptions() {
    String[] args = { "-s", "10", "-c", "true" };
    int status = App.run(args);

    assertEquals(App.EXIT_SUCCESS, status);
    assertEquals(10, App.getSize(), "Size should be 10");
    assertTrue(App.isContest(), "Contest flag should be true");
  }

  @Test
  public void testUnrecognizedArguments() {
    // Tests the case where the user enters text that is not an option (e.g.,
    // "hello")
    String[] args = { "-v", "hello" };
    int status = App.run(args);

    assertEquals(App.EXIT_ERROR, status, "Extra arguments should trigger an error status");
  }

  @Test
  public void testRunExecutionFlow() {
    String[] args = { "-v" };
    int status = App.run(args);
    assertEquals(App.EXIT_SUCCESS, status);
    assertTrue(App.isVerbose(), "Verbose flag should remain true after run().");
  }
}