package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.evaluation.MaxEvaluator;
import fr.ubordeaux.pdp.model.player.ai.MinMaxAlphaBeta;
import fr.ubordeaux.pdp.model.tools.Internationalization;

/**
 * Executes the hint command by providing a hint to the player.
 * The actual hint generation logic is not implemented in this version and
 * simply prints a placeholder message to the console.
 *
 * @version 1.0
 */
public class HintCommand implements Command, Helpable {
  private final GameController controller;
  private final MinMaxAlphaBeta ai = new MinMaxAlphaBeta();

  public HintCommand(GameController controller) {
    this.controller = controller;
  }

  /**
   * Executes the hint command by providing a hint to the player.
   * The actual hint generation logic is not implemented in this version and
   * simply prints a placeholder message to the console.
   */
  @Override
  public void execute() {
    MaxEvaluator evaluator = new MaxEvaluator();
    Move hintMove = ai.getBestMove(controller.getGame().getManagerUndoRedo(), 
        controller.getGame().getBoard(), controller.getGame().getCurrentColor(), evaluator);

    if (hintMove != null) {
      String from = controller.getGame().getBoard().indexToSquare(hintMove.getFrom());
      String to = controller.getGame().getBoard().indexToSquare(hintMove.getTo());

      controller.displayHint(from, to);
    } else {
      System.out.println(Internationalization.get("hint.no_hint"));
    }
  }

  /**
   * Returns the help string for the hint command.
   *
   * @return A brief description of the hint functionality.
   */
  @Override
  public String getHelp() {
    return Internationalization.get("hint.help");
  }

}
