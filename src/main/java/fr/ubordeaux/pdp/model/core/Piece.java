package fr.ubordeaux.pdp.model.core;

/**
 * Représente les différents types de pièces dans une partie de dames.
 *
 * <p>Une pièce peut appartenir à l'un des deux joueurs (blanc ou noir) et
 * être soit un pion, soit une dame (checker).</p>
 *
 * <ul>
 *   <li>{@link #WHITE_PAWN} : pion blanc</li>
 *   <li>{@link #BLACK_PAWN} : pion noir</li>
 *   <li>{@link #WHITE_CHECKER} : dame blanche</li>
 *   <li>{@link #BLACK_CHECKER} : dame noire</li>
 * </ul>
 */
public enum Piece {

  /** Pion appartenant au joueur blanc. */
  WHITE_PAWN,

  /** Pion appartenant au joueur noir. */
  BLACK_PAWN,

  /** Dame appartenant au joueur blanc. */
  WHITE_CHECKER,

  /** Dame appartenant au joueur noir. */
  BLACK_CHECKER
}