package fr.ubordeaux.pdp.model.player.ai;

/**
 * Enumeration for the selection mode used in MCTS. This allows switching between different 
 * selection strategies.
 */
public enum SelectionMode {
  /** Selection mode using UCT (Upper Confidence Bound for Trees). */
  UCT,
  /** Selection mode using Machine Learning. */
  ML
}
