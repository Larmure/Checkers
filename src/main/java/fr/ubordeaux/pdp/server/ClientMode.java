package fr.ubordeaux.pdp.server;

/**
 * Represents the current operating mode of the game client.
 *
 * The mode determines which commands are available at any given time,
 * preventing logical conflicts (e.g. starting a server while connected
 * as a client, or joining a server while already hosting one).
 *
 * <pre>
 * LOCAL    → all commands available (default state)
 * SERVER   → server is running locally; client commands are blocked
 * CONNECTED → connected to a remote server; server-start is blocked
 * </pre>
 */
public enum ClientMode {

  /** Default state. No server running, not connected to any server. */
  LOCAL,

  /**
   * A game server has been started locally via {@code server start}.
   * Only server management commands are allowed:
   * {@code server stop}, {@code server status}, {@code players}, {@code scoreboard}.
   * Client commands ({@code join}, {@code ping}) are blocked.
   */
  SERVER,

  /**
   * Connected to a remote game server via {@code join}.
   * Game commands and client commands are allowed.
   * {@code server start} is blocked.
   */
  CONNECTED
}