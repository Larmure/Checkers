package fr.ubordeaux.pdp.controller;

/**
 * Semantic marker for commands that belong to the network client layer.
 *
 * <p>Command hierarchy:
 * <pre>
 * Command (base interface)
 * ├── [Game]           NewCommand, UndoCommand, RedoCommand, SetCommand...
 * │                    → act on GameController / GameCheckers
 * ├── [Network server] ServerListCommand, ServerStartCommand, ServerStopCommand
 * │                    → act on GameServer / ServerListService
 * └── [Client]         JoinCommand, PingCommand, QuitClientCommand, HelpClientCommand
 *                      → act on ClientSession (connection and mode state)
 * </pre>
 *
 * <p>Client commands receive a {@link fr.ubordeaux.pdp.server.ClientSession} to
 * manipulate connection state without coupling directly to the {@code client} shell.
 */
public interface ClientCommand extends Command {
  // Marker interface — allows client commands to be identified and filtered
  // independently of game or server commands.
}