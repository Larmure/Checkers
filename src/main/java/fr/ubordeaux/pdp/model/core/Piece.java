package fr.ubordeaux.pdp.model.core;

/**
 * Represents the different types of pieces in a game of checkers.
 *
 * <p>A piece belongs to one of the two players (white or black) and
 * can be either a pawn or a checker (king).</p>
 *
 * <ul>
 *   <li>{@link #WHITE_PAWN} : white pawn</li>
 *   <li>{@link #BLACK_PAWN} : black pawn</li>
 *   <li>{@link #WHITE_CHECKER} : white checker (king)</li>
 *   <li>{@link #BLACK_CHECKER} : black checker (king)</li>
 * </ul>
 */
public enum Piece {

  /** Pawn belonging to the white player. */
  WHITE_PAWN,

  /** Pawn belonging to the black player. */
  BLACK_PAWN,

  /** Checker (king) belonging to the white player. */
  WHITE_CHECKER,

  /** Checker (king) belonging to the black player. */
  BLACK_CHECKER
}