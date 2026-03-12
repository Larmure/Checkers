package fr.ubordeaux.pdp.controller;

/**
 * Defines the protocol for communication between the game server and clients.
 * This enum represents the various commands that can be sent from the server to
 * the clients, such as pinging for connectivity checks, quitting the game, and
 * handling errors.
 */
public enum CommandProtocol {
    PING,
    PONG,
    QUIT,
    BYE,
    ERROR;
}
