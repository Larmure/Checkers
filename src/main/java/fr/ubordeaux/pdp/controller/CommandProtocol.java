package fr.ubordeaux.pdp.controller;

/**
 * Defines the protocol for communication between the game server and clients.
 * This enum represents the various commands that can be sent from the server to
 * the clients, such as pinging for connectivity checks, quitting the game, and
 * handling errors.
 */
public enum CommandProtocol {
    /** A ping command to check connectivity. */
    PING,
    /** A pong command as a response to a ping. */
    PONG,
    /** A quit command to leave the game. */
    QUIT,
    /** A bye command to indicate the end of a session. */
    BYE,
    /** An error command to signal an error condition. */
    ERROR;
}
