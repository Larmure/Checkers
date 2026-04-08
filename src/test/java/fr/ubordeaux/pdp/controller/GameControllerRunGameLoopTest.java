package fr.ubordeaux.pdp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.ubordeaux.pdp.model.core.Configuration;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.tools.Utils;
import fr.ubordeaux.pdp.view.CommandLineInterface;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameControllerRunGameLoopTest {

  @Test
  void testRunGameLoop_stopsWhenCliReturnsNull() throws Exception {
    FakeCliView cli = new FakeCliView((String) null);
    GameController controller = new GameController(cli);
    cli.setController(controller);
    controller.startNewGame(Configuration.getDefaultConfiguration());

    controller.start();
    controller.joinGameLoop();

    assertTrue(cli.startCalled, "CLI start() must be called by controller.start().");
    assertEquals(controller, cli.boundController,
        "The controller must be bound to the CLI view.");
    assertTrue(cli.readInputCalls > 0, "runGameLoop must read user input in non-AI turns.");
    assertTrue(cli.handledInputs.isEmpty(),
        "No input should be handled when readInput() returns null.");
    assertFalse(isGameLoopRunning(controller),
        "runGameLoop must stop when readInput() returns null.");
  }

  @Test
  void testRunGameLoop_dispatchesInputToCliHandleInput() throws Exception {
    FakeCliView cli = new FakeCliView("show board", null);
    GameController controller = new GameController(cli);
    cli.setController(controller);
    controller.startNewGame(Configuration.getDefaultConfiguration());

    controller.start();
    controller.joinGameLoop();

    assertEquals(List.of("show board"), cli.handledInputs,
        "runGameLoop must forward non-null input to cli.handleInput().");
    assertFalse(isGameLoopRunning(controller));
  }

  @Test
  void testRunGameLoop_playsAiTurnThenStopsOnNullInput() throws Exception {
    FakeCliView cli = new FakeCliView((String) null);
    GameController controller = new GameController(cli);
    cli.setController(controller);
    controller.startNewGame(buildWhiteAiConfig());

    GameCheckers game = controller.getGame();
    int historyBefore = game.getHistory().getSize();

    controller.start();
    controller.joinGameLoop();

    int historyAfter = game.getHistory().getSize();
    assertTrue(historyAfter > historyBefore,
        "runGameLoop must execute an AI move when current player is AI.");
    assertFalse(isGameLoopRunning(controller));
  }

  private boolean isGameLoopRunning(GameController controller) throws Exception {
    Field runningField = GameController.class.getDeclaredField("gameLoopRunning");
    runningField.setAccessible(true);
    return (boolean) runningField.get(controller);
  }

  private Configuration buildWhiteAiConfig() {
    return new Configuration(false, 0, false, 8, false, false, true, false, 1,
        Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);
  }

  private static final class FakeCliView extends CommandLineInterface {
    private final List<String> inputs;
    private int inputIndex = 0;
    private final List<String> handledInputs = new ArrayList<>();
    private boolean startCalled = false;
    private int readInputCalls = 0;
    private GameController boundController;

    private FakeCliView(String... scriptedInputs) {
      super(false, false);
      this.inputs = scriptedInputs == null ? List.of() : new ArrayList<>(Arrays.asList(scriptedInputs));
    }

    @Override
    public void start() {
      startCalled = true;
    }

    @Override
    public void setController(GameController controller) {
      this.boundController = controller;
      super.setController(controller);
    }

    @Override
    public String readInput() {
      readInputCalls++;
      if (inputIndex >= inputs.size()) {
        return null;
      }
      return inputs.get(inputIndex++);
    }

    @Override
    public void handleInput(String input) {
      handledInputs.add(input);
    }
  }
}
