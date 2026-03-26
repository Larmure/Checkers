package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.GameCheckers;

/**
 * No-op implementation of {@link GameView} for headless (server-side) use.
 *
 * <p>Game controllers running inside {@link fr.ubordeaux.pdp.server.GameServer} have no
 * terminal to render to. This view silently discards all display and update calls so that
 * the server can run game logic without producing any console output.
 *
 * <p>{@link #setController(GameController)} is inherited from {@link GameView} and does not
 * need to be overridden.
 */

public class HeadlessView extends GameView {
 
  @Override
  public void display(GameCheckers game) {
    // Intentionally empty: no terminal available server-side.
  }
 
  @Override
  public void update(GameCheckers game) {
    // Intentionally empty: no terminal available server-side.
  }
 
  @Override
  public void start() {
    // Intentionally empty: no input loop needed server-side.
  }

  @Override
  public void showHint(String from, String to) {
    // Intentionally empty: no terminal available server-side.
  }

}