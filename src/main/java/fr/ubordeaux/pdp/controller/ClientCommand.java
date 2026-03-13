package fr.ubordeaux.pdp.controller;

/**
 * Semantic marker for commands specific to the network client.
 * Command hierarchy:
 * <pre>
 * Command (base interface)
 * ├── [Game]           NewCommand, UndoCommand, RedoCommand, SetCommand...
 * │                   → act on GameController / GameCheckers
 * ├── [Server network] ServerListCommand, ServerStartCommand, ServerStopCommand
 * │                   → act on GameServer / ServerListService
 * └── [Client]        JoinCommand, PingCommand, QuitClientCommand, HelpClientCommand
 *                     → act on ClientSession (client connection state)
 * </pre>
 * Client commands receive a ClientSession to manipulate
 * the connection state without direct coupling to the {@code client} class.
 */
public interface ClientCommand extends Command {
    // Marker interface — all client commands implement this in addition to Command,
    // allowing them to be easily identified and filtered.
}