package fr.ubordeaux.pdp.model.core;

import fr.ubordeaux.pdp.model.player.PlayerColor;
import fr.ubordeaux.pdp.model.tools.Internationalization;
import fr.ubordeaux.pdp.model.tools.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a draughts (checkers) board encoded with bitboards.
 *
 * <p>Supports board sizes of 8, 10, and 12. Each playable (dark) square is mapped
 * to a bit
 * index. Two 64-bit longs are used per piece type to handle boards larger than
 * 64 playable
 * squares (e.g. the 12×12 board has 72 playable squares).
 *
 * <p>Coordinate system: rows are labelled {@code A} (bottom) to {@code L} (top);
 * columns are
 * numbered {@code 1} (left) to {@code size} (right). Only dark squares (where
 * {@code (row+col)}
 * is even) store pieces.
 */
public class Board {

  // ---- Edge masks (one bit per left/right-edge playable square) ----
  /** Mask for the left edge of the board. */
  private long leftMask1;
  /** Mask for the left edge of the board (high bits for boards > 64 squares). */
  private long leftMask2;
  /** Mask for the right edge of the board. */
  private long rightMask1;
  /** Mask for the right edge of the board (high bits for boards > 64 squares). */
  private long rightMask2;

  /** Diagonal offsets (index deltas) for even rows. */
  private Map<String, Integer> diagsPair = new HashMap<>();

  /** Diagonal offsets (index deltas) for odd rows. */
  private Map<String, Integer> diagsUnpair = new HashMap<>();

  /** Side length of the board (8, 10, or 12). */
  private int sizeBoard;

  /** Total number of playable squares ({@code sizeBoard * sizeBoard / 2}). */
  private int indexMax;

  // ---- Bitboards (low 64 bits then high bits for boards > 64 squares) ----
  /** Bitboard for white pawns (low 64 bits). */
  private long whitePawns1;
  /** Bitboard for white checkers (low 64 bits). */
  private long whiteCheckers1;
  /** Bitboard for black pawns (low 64 bits). */
  private long blackPawns1;
  /** Bitboard for black checkers (low 64 bits). */
  private long blackCheckers1;

  /** Bitboard for white pawns (high 64 bits). */
  private long whitePawns2;
  /** Bitboard for white checkers (high 64 bits). */
  private long whiteCheckers2;
  /** Bitboard for black pawns (high 64 bits). */
  private long blackPawns2;
  /** Bitboard for black checkers (high 64 bits). */
  private long blackCheckers2;

  /**
   * Constructs a new board of the given size and places pieces in their starting
   * positions.
   *
   * @param size the side length of the board; must be 8, 10, or 12
   * @throws IllegalArgumentException if {@code size} is not one of the valid
   *                                  values
   */
  public Board(int size) {
    if (!Utils.VALID_SIZES.contains(size)) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.invalid_size", size));
    }
    this.sizeBoard = size;
    this.indexMax = (this.sizeBoard * this.sizeBoard) / 2;
    initPosition();
    initEdgeMasks();
    initDiag();
  }

  // ---------------------------------------------------------------------------
  // Initialisation
  // ---------------------------------------------------------------------------

  /**
   * Places all pawns in their standard starting positions using bitboard
   * arithmetic.
   *
   *
   * <p>White occupies the lowest-indexed rows; black occupies the highest-indexed
   * rows. On a
   * 12×12 board the black bitboard overflows 64 bits and the surplus is stored in
   * {@code blackPawns2}.
   */
  private void initPosition() {
    this.whitePawns1 = 0L;
    this.blackPawns1 = 0L;
    this.whiteCheckers1 = 0L;
    this.blackCheckers1 = 0L;

    this.whitePawns2 = 0L;
    this.blackPawns2 = 0L;
    this.whiteCheckers2 = 0L;
    this.blackCheckers2 = 0L;

    switch (this.sizeBoard) {
      case 8:
        // Set the 12 lowest bits (indices 0-11) for white; shift them by 20 for black.
        this.whitePawns1 = (1L << 12) - 1;
        this.blackPawns1 = ((1L << 12) - 1) << 20;
        break;
      case 10:
        // Set the 20 lowest bits (indices 0-19) for white; shift them by 30 for black.
        this.whitePawns1 = (1L << 20) - 1;
        this.blackPawns1 = ((1L << 20) - 1) << 30;
        break;
      case 12:
        // Set the 30 lowest bits (indices 0-29) for white; shift them by 42 for black.
        // Black overflows: the top 8 bits spill into blackPawns2.
        this.whitePawns1 = (1L << 30) - 1;
        this.blackPawns1 = ((1L << 30) - 1) << 42;
        this.blackPawns2 = (1L << 8) - 1;
        break;
      default:
        break;
    }
  }

  /**
   * Initialises the diagonal offset maps for even and odd rows.
   *
   *
   * <p>Each direction key ({@code "NW"}, {@code "NE"}, {@code "SW"}, {@code "SE"})
   * maps to the
   * signed index delta that moves one step in that direction.
   */
  private void initDiag() {
    int half = this.sizeBoard / 2;
    this.diagsPair.put("NW", half - 1);
    this.diagsPair.put("NE", half);
    this.diagsPair.put("SW", -(half + 1));
    this.diagsPair.put("SE", -half);

    this.diagsUnpair.put("NW", half);
    this.diagsUnpair.put("NE", half + 1);
    this.diagsUnpair.put("SW", -half);
    this.diagsUnpair.put("SE", -(half - 1));
  }

  /**
   * Builds bitmasks that identify squares on the left and right edges of the
   * board.
   *
   *
   * <p>These masks are used to prevent diagonal moves from "wrapping" around board
   * edges.
   */
  private void initEdgeMasks() {
    leftMask1 = leftMask2 = 0L;
    rightMask1 = rightMask2 = 0L;

    for (int row = 0; row < sizeBoard; row++) {
      int leftIndex = boardToBitIndex(row, 0);
      int rightIndex = boardToBitIndex(row, sizeBoard - 1);

      if (leftIndex >= 0) {
        if (leftIndex < 64) {
          leftMask1 |= (1L << leftIndex);
        } else {
          leftMask2 |= (1L << (leftIndex - 64));
        }
      }

      if (rightIndex >= 0) {
        if (rightIndex < 64) {
          rightMask1 |= (1L << rightIndex);
        } else {
          rightMask2 |= (1L << (rightIndex - 64));
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Edge detection
  // ---------------------------------------------------------------------------

  /**
   * Returns {@code true} if the playable square at {@code index} is on the left
   * edge.
   *
   * @param index bit index of the square
   * @return {@code true} when the square is on the leftmost column
   */
  private boolean onLeftEdge(int index) {
    if (index < 64) {
      return (leftMask1 & (1L << index)) != 0;
    }
    return (leftMask2 & (1L << (index - 64))) != 0;
  }

  /**
   * Returns {@code true} if the playable square at {@code index} is on the right
   * edge.
   *
   * @param index bit index of the square
   * @return {@code true} when the square is on the rightmost column
   */
  private boolean onRightEdge(int index) {
    if (index < 64) {
      return (rightMask1 & (1L << index)) != 0;
    }
    return (rightMask2 & (1L << (index - 64))) != 0;
  }

  // ---------------------------------------------------------------------------
  // Bit-level presence queries
  // ---------------------------------------------------------------------------

  /**
   * Tests whether a given bit is set in the supplied pair of bitboards.
   *
   * @param index     bit index of the square to test
   * @param bitboard1 low 64-bit word
   * @param bitboard2 high 64-bit word (for indices ≥ 64)
   * @return {@code true} if the bit at {@code index} is set
   */
  private boolean hasPawn(int index, long bitboard1, long bitboard2) {
    if (index < 64) {
      return ((bitboard1 >>> index) & 1L) == 1L;
    }
    return ((bitboard2 >>> (index - 64)) & 1L) == 1L;
  }

  /**
   * Returns {@code true} if a white pawn occupies the square identified by its
   * algebraic name.
   *
   * @param square algebraic square name (e.g. {@code "A1"})
   * @return {@code true} if a white pawn is present
   */
  public boolean isWhitePawn(String square) {
    return isBitWhitePawn(squareToIndex(square));
  }

  /**
   * Returns {@code true} if a black pawn occupies the square identified by its
   * algebraic name.
   *
   * @param square algebraic square name (e.g. {@code "H8"})
   * @return {@code true} if a black pawn is present
   */
  public boolean isBlackPawn(String square) {
    return isBitBlackPawn(squareToIndex(square));
  }

  /**
   * Returns {@code true} if a white checker (king) occupies the named square.
   *
   * @param square algebraic square name
   * @return {@code true} if a white checker is present
   */
  public boolean isWhiteChecker(String square) {
    return isBitWhiteChecker(squareToIndex(square));
  }

  /**
   * Returns {@code true} if a black checker (king) occupies the named square.
   *
   * @param square algebraic square name
   * @return {@code true} if a black checker is present
   */
  public boolean isBlackChecker(String square) {
    return isBitBlackChecker(squareToIndex(square));
  }

  /**
   * Returns {@code true} if the square at {@code index} is occupied by a white pawn.
   *
   * @param index bit index of the square
   * @return {@code true} if a white pawn is present at that index
   */
  public boolean isBitWhitePawn(int index) {
    return hasPawn(index, whitePawns1, whitePawns2);
  }

  /**
   * Returns {@code true} if the square at {@code index} is occupied by a black pawn.
   *
   * @param index bit index of the square
   * @return {@code true} if a black pawn is present at that index
   */
  public boolean isBitBlackPawn(int index) {
    return hasPawn(index, blackPawns1, blackPawns2);
  }

  /**
   * Returns {@code true} if the square at {@code index} is occupied by a white checker.
   *
   * @param index bit index of the square
   * @return {@code true} if a white checker is present at that index
   */
  public boolean isBitWhiteChecker(int index) {
    return hasPawn(index, whiteCheckers1, whiteCheckers2);
  }

  /**
   * Returns {@code true} if the square at {@code index} is occupied by a black checker.
   *
   * @param index bit index of the square
   * @return {@code true} if a black checker is present at that index
   */
  public boolean isBitBlackChecker(int index) {
    return hasPawn(index, blackCheckers1, blackCheckers2);
  }

  /**
   * Returns {@code true} if the named square is occupied by any piece.
   *
   * @param square algebraic square name
   * @return {@code true} when the square is not empty
   */
  public boolean occupied(String square) {
    return isOccupied(squareToIndex(square));
  }

  /**
   * Returns {@code true} if the square at {@code index} is occupied by any piece.
   *
   * @param index bit index of the square
   * @return {@code true} when the square is not empty
   */
  private boolean isOccupied(int index) {
    return isBitWhitePawn(index)
        || isBitBlackPawn(index)
        || isBitWhiteChecker(index)
        || isBitBlackChecker(index);
  }

  // ---------------------------------------------------------------------------
  // Game-state queries
  // ---------------------------------------------------------------------------

  /**
   * Returns {@code true} if the given side has no pieces remaining on the board.
   *
   * @param p the player color to check
   * @return {@code true} when all bitboards for that colour are zero
   */
  public boolean noPiecesLeft(PlayerColor p) {
    if (p == PlayerColor.WHITE) {
      return whitePawns1 == 0L
          && whitePawns2 == 0L
          && whiteCheckers1 == 0L
          && whiteCheckers2 == 0L;
    }
    return blackPawns1 == 0L
        && blackPawns2 == 0L
        && blackCheckers1 == 0L
        && blackCheckers2 == 0L;
  }

  /**
   * Returns the total number of playable squares on the board, which is the upper
   * bound for valid square indices.
   *
   * @return the maximum index of a playable square
   */
  public int getIndexMax() {
    return this.indexMax;
  }

  // ---------------------------------------------------------------------------
  // Bitboard mutation helpers
  // ---------------------------------------------------------------------------

  /**
   * Sets the bit at {@code index} in the appropriate bitboard to add a white pawn.
   *
   * @param index bit index of the square where the white pawn should be added
   */
  private void addWhitePawn(int index) {
    if (index < 64) {
      whitePawns1 |= (1L << index);
    } else {
      whitePawns2 |= (1L << (index - 64));
    }
  }

  /**
   * Clears the bit at {@code index} in the appropriate bitboard to remove a white
   * pawn.
   *
   * @param index bit index of the square where the white pawn should be removed
   */
  private void removeWhitePawn(int index) {
    if (index < 64) {
      whitePawns1 &= ~(1L << index);
    } else {
      whitePawns2 &= ~(1L << (index - 64));
    }
  }

  /**
   * Sets the bit at {@code index} in the appropriate bitboard to add a black pawn.
   *
   * @param index bit index of the square where the black pawn should be added
   */
  private void addBlackPawn(int index) {
    if (index < 64) {
      blackPawns1 |= (1L << index);
    } else {
      blackPawns2 |= (1L << (index - 64));
    }
  }

  /**
   * Clears the bit at {@code index} in the appropriate bitboard to remove a black
   * pawn.
   *
   * @param index bit index of the square where the black pawn should be removed
   */
  private void removeBlackPawn(int index) {
    if (index < 64) {
      blackPawns1 &= ~(1L << index);
    } else {
      blackPawns2 &= ~(1L << (index - 64));
    }
  }

  /**
   * Sets the bit at {@code index} in the appropriate bitboard to add a white checker.
   *
   * @param index bit index of the square where the white checker should be added
   */
  private void addWhiteChecker(int index) {
    if (index < 64) {
      whiteCheckers1 |= (1L << index);
    } else {
      whiteCheckers2 |= (1L << (index - 64));
    }
  }

  /**
   * Clears the bit at {@code index} in the appropriate bitboard to remove a white*
   * checker.
   *
   * @param index bit index of the square where the white checker should be removed
   */
  private void removeWhiteChecker(int index) {
    if (index < 64) {
      whiteCheckers1 &= ~(1L << index);
    } else {
      whiteCheckers2 &= ~(1L << (index - 64));
    }
  }

  /**
   * Sets the bit at {@code index} in the appropriate bitboard to add a black checker.
   *
   * @param index bit index of the square where the black checker should be added
   */
  private void addBlackChecker(int index) {
    if (index < 64) {
      blackCheckers1 |= (1L << index);
    } else {
      blackCheckers2 |= (1L << (index - 64));
    }
  }

  /**
   * Clears the bit at {@code index} in the appropriate bitboard to remove a black
   * checker.
   *
   * @param index bit index of the square where the black checker should be removed
   */
  private void removeBlackChecker(int index) {
    if (index < 64) {
      blackCheckers1 &= ~(1L << index);
    } else {
      blackCheckers2 &= ~(1L << (index - 64));
    }
  }

  // ---------------------------------------------------------------------------
  // Promotion
  // ---------------------------------------------------------------------------

  /**
   * Promotes the pawn at bit {@code index} to a checker in place.
   *
   * @param index bit index of the pawn
   * @throws IllegalArgumentException if the index is out of bounds or no pawn is
   *                                  present
   */
  private void promoteBit(int index) {
    if (index < 0 || index >= indexMax) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.index_out_of_bounds", index));
    }
    if (isBitWhitePawn(index)) {
      removeWhitePawn(index);
      addWhiteChecker(index);
      return;
    }
    if (isBitBlackPawn(index)) {
      removeBlackPawn(index);
      addBlackChecker(index);
      return;
    }
    throw new IllegalArgumentException(
        Internationalization.get("board.error.no_piece_at_index", index));
  }

  // ---------------------------------------------------------------------------
  // Move application
  // ---------------------------------------------------------------------------

  /**
   * Applies a {@link Move} to the board: relocates the moving piece, removes any
   * captured
   * pieces, and auto-promotes a pawn that has reached the opposite back rank.
   *
   * @param move the move to apply; must refer to valid squares
   */
  public void applyMove(Move move) {
    int from = move.getFrom();
    int to = move.getTo();

    // Relocate the moving piece.
    if (isBitWhitePawn(from)) {
      removeWhitePawn(from);
      addWhitePawn(to);
    } else if (isBitBlackPawn(from)) {
      removeBlackPawn(from);
      addBlackPawn(to);
    } else if (isBitWhiteChecker(from)) {
      removeWhiteChecker(from);
      addWhiteChecker(to);
    } else if (isBitBlackChecker(from)) {
      removeBlackChecker(from);
      addBlackChecker(to);
    }

    // Remove all captured pieces.
    for (int captured : move.getCaptured()) {
      if (isBitWhitePawn(captured)) {
        move.getCapturedColors().add(Piece.WHITE_PAWN);
        removeWhitePawn(captured);
      }
      if (isBitBlackPawn(captured)) {
        move.getCapturedColors().add(Piece.BLACK_PAWN);
        removeBlackPawn(captured);
      }
      if (isBitWhiteChecker(captured)) {
        move.getCapturedColors().add(Piece.WHITE_CHECKER);
        removeWhiteChecker(captured);
      }
      if (isBitBlackChecker(captured)) {
        move.getCapturedColors().add(Piece.BLACK_CHECKER);
        removeBlackChecker(captured);
      }
    }

    // Auto-promotion when a pawn reaches the back rank.
    boolean promoted = false;
    int row = to / (sizeBoard / 2);

    if (isBitWhitePawn(to) && row == sizeBoard - 1) {
      promoteBit(to);
      promoted = true;
    } else if (isBitBlackPawn(to) && row == 0) {
      promoteBit(to);
      promoted = true;
    }

    if (promoted) {
      move.setPromotion(true);
    }
  }

  // ---------------------------------------------------------------------------
  // Diagonal-map selection
  // ---------------------------------------------------------------------------

  /**
   * Returns the diagonal offset map appropriate for the row that contains
   * {@code index}.
   *
   *
   * <p>Even rows use {@link #diagsPair}; odd rows use {@link #diagsUnpair}.
   *
   * @param index bit index of the square whose row determines the map
   * @return the diagonal offset map for that row parity
   */
  private Map<String, Integer> diagsForIndex(int index) {
    int row = index / (sizeBoard / 2);
    return (row % 2 == 0) ? diagsPair : diagsUnpair;
  }

  // ---------------------------------------------------------------------------
  // Simple-move target generation
  // ---------------------------------------------------------------------------

  /**
   * Returns the list of bit indices reachable by a single (non-capturing) pawn
   * step from
   * {@code from}.
   *
   * @param from bit index of the pawn
   * @param isWhite {@code true} if the pawn is white (moves up)
   * @return mutable list of destination indices; may be empty
   */
  private List<Integer> pawnSimpleTargets(int from, boolean isWhite) {
    List<Integer> targets = new ArrayList<>();
    Map<String, Integer> diags = diagsForIndex(from);

    for (Map.Entry<String, Integer> entry : diags.entrySet()) {
      String dir = entry.getKey();

      if (isWhite && (dir.equals("SW") || dir.equals("SE"))) {
        continue;
      }
      if (!isWhite && (dir.equals("NW") || dir.equals("NE"))) {
        continue;
      }
      if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(from)) {
        continue;
      }
      if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(from)) {
        continue;
      }

      int delta = entry.getValue();
      int to = from + delta;
      if (to >= 0 && to < indexMax && !isOccupied(to)) {
        targets.add(to);
      }
    }
    return targets;
  }

  /**
   * Returns all squares reachable by a single (non-capturing) checker (king)
   * slide from
   * {@code from} in any diagonal direction.
   *
   * @param from bit index of the checker
   * @return mutable list of destination indices; may be empty
   */
  private List<Integer> checkerSimpleTargets(int from) {
    List<Integer> targets = new ArrayList<>();

    for (String dir : diagsPair.keySet()) {
      int current = from;

      while (true) {
        Map<String, Integer> diags = diagsForIndex(current);
        int delta = diags.get(dir);

        if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(current)) {
          break;
        }
        if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(current)) {
          break;
        }

        int next = current + delta;
        if (next < 0 || next >= indexMax) {
          break;
        }
        if (isOccupied(next)) {
          break;
        }

        targets.add(next);
        current = next;
      }
    }
    return targets;
  }

  // ---------------------------------------------------------------------------
  // Public move-list API
  // ---------------------------------------------------------------------------

  /**
   * Returns the list of legal moves for white in the current position.
   *
   *
   * <p>If any capture is available, only capturing moves are returned
   * (mandatory-capture rule).
   *
   * @return non-null, possibly empty list of legal {@link Move} objects
   */
  public List<Move> getWhiteValidMoves() {
    return getValidMoves(true);
  }

  /**
   * Returns the list of legal moves for black in the current position.
   *
   *
   * <p>If any capture is available, only capturing moves are returned
   * (mandatory-capture rule).
   *
   * @return non-null, possibly empty list of legal {@link Move} objects
   */
  public List<Move> getBlackValidMoves() {
    return getValidMoves(false);
  }

  /**
   * Core move-generation routine shared by both colours.
   *
   *
   * <p>Captures are collected first. If any exist, simple moves are skipped and only
   * the
   * maximum-length captures are returned.
   *
   * @param isWhite {@code true} to generate moves for white; {@code false} for
   *                black
   * @return list of legal {@link Move} objects
   */
  private List<Move> getValidMoves(boolean isWhite) {
    List<Move> captures = new ArrayList<>();
    List<Move> simples = new ArrayList<>();

    for (int i = 0; i < indexMax; i++) {
      if (isWhite) {
        if (isBitWhitePawn(i)) {
          captures.addAll(getBestPawnCaptures(i, true));
        }
        if (isBitWhiteChecker(i)) {
          captures.addAll(getBestCheckerCaptures(i, true));
        }
      } else {
        if (isBitBlackPawn(i)) {
          captures.addAll(getBestPawnCaptures(i, false));
        }
        if (isBitBlackChecker(i)) {
          captures.addAll(getBestCheckerCaptures(i, false));
        }
      }
    }

    if (!captures.isEmpty()) {
      return captures;
    }

    // No captures available: collect simple moves.
    for (int i = 0; i < indexMax; i++) {
      if (isWhite) {
        if (isBitWhitePawn(i)) {
          for (int to : pawnSimpleTargets(i, true)) {
            simples.add(new Move(List.of(i, to), List.of()));
          }
        }
        if (isBitWhiteChecker(i)) {
          for (int to : checkerSimpleTargets(i)) {
            simples.add(new Move(List.of(i, to), List.of()));
          }
        }
      } else {
        if (isBitBlackPawn(i)) {
          for (int to : pawnSimpleTargets(i, false)) {
            simples.add(new Move(List.of(i, to), List.of()));
          }
        }
        if (isBitBlackChecker(i)) {
          for (int to : checkerSimpleTargets(i)) {
            simples.add(new Move(List.of(i, to), List.of()));
          }
        }
      }
    }

    return simples;
  }

  // ---------------------------------------------------------------------------
  // Pawn capture generation
  // ---------------------------------------------------------------------------

  /**
   * Returns the longest pawn capture sequences available from {@code from}.
   *
   * @param from    bit index of the pawn
   * @param isWhite {@code true} if the pawn belongs to white
   * @return list of {@link Move} objects with the maximum capture count; empty if
   *         none
   */
  private List<Move> getBestPawnCaptures(int from, boolean isWhite) {
    List<CapturePath> all = pawnMultiCaptures(from, isWhite);
    if (all.isEmpty()) {
      return List.of();
    }

    int max = all.stream().mapToInt(c -> c.captures).max().orElse(0);
    List<Move> best = new ArrayList<>();

    for (CapturePath c : all) {
      if (c.captures == max) {
        best.add(new Move(c.path, new ArrayList<>(c.captured)));
      }
    }
    return best;
  }

  /**
   * Enumerates all pawn multi-capture sequences from {@code from} via depth-first
   * search.
   *
   * @param from    bit index of the pawn
   * @param isWhite {@code true} if the pawn belongs to white
   * @return list of every possible {@link CapturePath}
   */
  private List<CapturePath> pawnMultiCaptures(int from, boolean isWhite) {
    List<CapturePath> results = new ArrayList<>();
    dfsPawn(from, isWhite, new ArrayList<>(List.of(from)), new HashSet<>(), results, 0);
    return results;
  }

  /**
   * Recursive DFS that explores all pawn capture continuations from
   * {@code current}.
   *
   * @param current      bit index of the pawn's current position
   * @param isWhite      {@code true} if the pawn belongs to white
   * @param path         ordered list of squares visited so far (including start)
   * @param captured     set of enemy bit indices already captured in this path
   * @param results      accumulator for completed capture paths
   * @param captureCount number of captures performed so far in this path
   */
  private void dfsPawn(
      int current,
      boolean isWhite,
      List<Integer> path,
      Set<Integer> captured,
      List<CapturePath> results,
      int captureCount) {

    boolean foundNext = false;
    Map<String, Integer> diags = diagsForIndex(current);

    for (Map.Entry<String, Integer> entry : diags.entrySet()) {
      String dir = entry.getKey();
      int delta = entry.getValue();

      if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(current)) {
        continue;
      }
      if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(current)) {
        continue;
      }

      int mid = current + delta;
      if (mid < 0 || mid >= indexMax) {
        continue;
      }
      if (captured.contains(mid)) {
        continue;
      }

      boolean enemy = isWhite
          ? (isBitBlackPawn(mid) || isBitBlackChecker(mid))
          : (isBitWhitePawn(mid) || isBitWhiteChecker(mid));
      if (!enemy) {
        continue;
      }

      // Compute landing square.
      Map<String, Integer> nextDiags = diagsForIndex(mid);
      int to = mid + nextDiags.get(dir);

      if (to < 0 || to >= indexMax) {
        continue;
      }
      if (isOccupied(to)) {
        continue;
      }
      if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(mid)) {
        continue;
      }
      if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(mid)) {
        continue;
      }

      // Valid jump: recurse then backtrack.
      foundNext = true;
      captured.add(mid);
      path.add(to);

      dfsPawn(to, isWhite, path, captured, results, captureCount + 1);

      path.remove(path.size() - 1);
      captured.remove(mid);
    }

    if (!foundNext && captureCount > 0) {
      results.add(new CapturePath(path, new ArrayList<>(captured)));
    }
  }

  // ---------------------------------------------------------------------------
  // Checker (king) capture generation
  // ---------------------------------------------------------------------------

  /**
   * Returns the longest checker capture sequences available from {@code from}.
   *
   * @param from    bit index of the checker
   * @param isWhite {@code true} if the checker belongs to white
   * @return list of {@link Move} objects with the maximum capture count; empty if
   *         none
   */
  private List<Move> getBestCheckerCaptures(int from, boolean isWhite) {
    List<CapturePath> all = checkerMultiCaptures(from, isWhite);
    if (all.isEmpty()) {
      return List.of();
    }

    int max = all.stream().mapToInt(c -> c.captures).max().orElse(0);
    List<Move> best = new ArrayList<>();

    for (CapturePath c : all) {
      if (c.captures == max) {
        best.add(new Move(c.path, c.captured));
      }
    }
    return best;
  }

  /**
   * Enumerates all checker multi-capture sequences from {@code from} via
   * depth-first search.
   *
   * @param from    bit index of the checker
   * @param isWhite {@code true} if the checker belongs to white
   * @return list of every possible {@link CapturePath}
   */
  private List<CapturePath> checkerMultiCaptures(int from, boolean isWhite) {
    List<CapturePath> results = new ArrayList<>();
    dfsChecker(from, isWhite, new ArrayList<>(List.of(from)), new HashSet<>(), results, 0);
    return results;
  }

  /**
   * Recursive DFS that explores all checker capture continuations from
   * {@code current}.
   *
   *
   * <p>Unlike a pawn, a checker can slide multiple squares before and after a jump.
   *
   * @param current      bit index of the checker's current position
   * @param isWhite      {@code true} if the checker belongs to white
   * @param path         ordered list of squares visited so far (including start)
   * @param captured     set of enemy bit indices already captured in this path
   * @param results      accumulator for completed capture paths
   * @param captureCount number of captures performed so far in this path
   */
  private void dfsChecker(
      int current,
      boolean isWhite,
      List<Integer> path,
      Set<Integer> captured,
      List<CapturePath> results,
      int captureCount) {

    boolean foundNext = false;

    for (String dir : diagsPair.keySet()) {
      int pos = current;
      boolean enemyFound = false;
      int enemyIndex = -1;

      while (true) {
        Map<String, Integer> diags = diagsForIndex(pos);
        int delta = diags.get(dir);

        if ((dir.equals("NW") || dir.equals("SW")) && onLeftEdge(pos)) {
          break;
        }
        if ((dir.equals("NE") || dir.equals("SE")) && onRightEdge(pos)) {
          break;
        }

        int next = pos + delta;
        if (next < 0 || next >= indexMax) {
          break;
        }

        if (!enemyFound) {
          if (isOccupied(next)) {
            boolean enemy = isWhite
                ? (isBitBlackPawn(next) || isBitBlackChecker(next))
                : (isBitWhitePawn(next) || isBitWhiteChecker(next));
            if (!enemy || captured.contains(next)) {
              break;
            }
            enemyFound = true;
            enemyIndex = next;
            pos = next;
          } else {
            pos = next;
          }
        } else {
          // Slide past the captured enemy and record all valid landing squares.
          if (isOccupied(next)) {
            break;
          }

          foundNext = true;
          captured.add(enemyIndex);
          path.add(next);

          dfsChecker(next, isWhite, path, captured, results, captureCount + 1);

          path.remove(path.size() - 1);
          captured.remove(enemyIndex);

          pos = next;
        }
      }
    }

    if (!foundNext && captureCount > 0) {
      results.add(new CapturePath(path, new ArrayList<>(captured)));
    }
  }

  // ---------------------------------------------------------------------------
  // Internal value type
  // ---------------------------------------------------------------------------

  /**
   * Immutable value object that records a single complete capture sequence.
   *
   *
   * <p>Used internally by the DFS capture-generation methods before being converted
   * into
   * {@link Move} objects.
   */
  private static class CapturePath {

    /** Sequence of bit indices from the origin to the final landing square. */
    final List<Integer> path;

    /** Bit indices of every enemy piece captured along this path. */
    final List<Integer> captured;

    /** Number of captures in this path (equals {@code captured.size()}). */
    final int captures;

    /**
     * Constructs a {@code CapturePath} with defensive copies of both lists.
     *
     * @param path     sequence of bit indices (origin → intermediate squares →
     *                 destination)
     * @param captured set of captured enemy bit indices
     */
    CapturePath(List<Integer> path, List<Integer> captured) {
      this.path = new ArrayList<>(path);
      this.captured = new ArrayList<>(captured);
      this.captures = captured.size();
    }
  }

  // ---------------------------------------------------------------------------
  // Coordinate conversion utilities
  // ---------------------------------------------------------------------------

  /**
   * Converts a bit index to its algebraic square name (e.g. {@code 0} →
   * {@code "A1"}).
   *
   * @param index bit index in the range {@code [0, indexMax)}
   * @return algebraic name such as {@code "A1"} or {@code "H8"}
   */
  public String indexToSquare(int index) {
    int row = index / (sizeBoard / 2);
    int col = (index % (sizeBoard / 2)) * 2 + (row % 2 == 0 ? 0 : 1);
    char rowChar = (char) ('A' + row);
    return "" + rowChar + (col + 1);
  }

  /**
   * Converts an algebraic square name to its bit index.
   *
   * @param square algebraic name such as {@code "A1"}; row is a letter, column is
   *               a number
   * @return bit index in the range {@code [0, indexMax)}
   * @throws IllegalArgumentException if the square is outside the board or is a
   *                                  light square
   */
  public int squareToIndex(String square) {
    if (square == null) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.null_square"));
    }
    char rowChar = Character.toUpperCase(square.charAt(0));
    int row = rowChar - 'A';
    int col = Integer.parseInt(square.substring(1)) - 1;

    if (row < 0 || row >= this.sizeBoard || col < 0 || col >= this.sizeBoard) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.square_out_of_bounds", square));
    }
    if ((row + col) % 2 != 0) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.invalid_light_square", square));
    }
    return (row * sizeBoard + col) / 2;
  }

  /**
   * Maps a {@code (row, col)} board position to its bit index, or {@code -1} for
   * light squares.
   *
   * @param row zero-based row index (0 = bottom row 'A')
   * @param col zero-based column index (0 = leftmost column)
   * @return bit index, or {@code -1} when the square is not playable
   */
  private int boardToBitIndex(int row, int col) {
    if ((row + col) % 2 != 0) {
      return -1; // light square, not stored
    }
    int blackBeforeRow = row * (sizeBoard / 2);
    int blackInRow = col / 2;
    return blackBeforeRow + blackInRow;
  }

  // ---------------------------------------------------------------------------
  // Display
  // ---------------------------------------------------------------------------

  /**
   * Returns a human-readable ASCII representation of the board, with row letters
   * on the left
   * and column numbers along the bottom.
   *
   *
   * <p>Piece symbols: {@code w} = white pawn, {@code W} = white checker, {@code b} =
   * black
   * pawn, {@code B} = black checker, {@code _} = empty or light square.
   *
   * @return multi-line string depicting the current board state
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");

    for (int row = sizeBoard - 1; row >= 0; row--) {
      char rowChar = (char) ('A' + row);
      sb.append(rowChar).append("  ");

      for (int col = 0; col < sizeBoard; col++) {
        int bitIndex = boardToBitIndex(row, col);
        sb.append(cellString(bitIndex));

      }
      sb.append("\n");
    }

    sb.append("  ");
    for (int col = 1; col <= sizeBoard; col++) {
      sb.append(String.format("%2d ", col));
    }
    sb.append("\n");

    return sb.toString();
  }

  /**
   * Helper method to convert a bit index to its corresponding cell string for display.
   *
   * @param bitIndex bit index of the square, or -1 for light squares
   * @return string representation of the cell's contents
   */
  private String cellString(int bitIndex) {
    String res = "";
    if (bitIndex == -1) {
      res = "_  ";
    } else if (isBitWhitePawn(bitIndex)) {
      res = "o  ";
    } else if (isBitWhiteChecker(bitIndex)) {
      res = "O  ";
    } else if (isBitBlackPawn(bitIndex)) {
      res = "x  ";
    } else if (isBitBlackChecker(bitIndex)) {
      res = "X  ";
    } else {
      res = "_  ";
    }
    return res;
  }

  /**
   * Returns a simplified board string without row/column labels, intended for testing
   * purposes.
   *
   * @return multi-line string with only piece symbols and underscores for empty squares
   */
  public String boardString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");

    for (int row = sizeBoard - 1; row >= 0; row--) {
      for (int col = 0; col < sizeBoard; col++) {
        int bitIndex = boardToBitIndex(row, col);
        sb.append(cellString(bitIndex));
      }
      sb.append("\n");
    }

    sb.append("  ");
    sb.append("\n");

    return sb.toString();
  }

  /**
  * Restores a piece on the board at the given index, based on the type string.
  *
  * @param index bit index of the square to restore
  * @param type string representing the piece type:
  *     "WP" for white pawn,
  *     "BP" for black pawn,
  *     "WC" for white checker,
  *     "BC" for black checker
  */
  public void restorePiece(int index, Piece type) {
    if (index < 0 || index >= indexMax) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.index_out_of_bounds", index));
    }
    switch (type) {
      case WHITE_PAWN:
        addWhitePawn(index);
        break;
      case BLACK_PAWN:
        addBlackPawn(index);
        break;
      case WHITE_CHECKER:
        addWhiteChecker(index);
        break;
      case BLACK_CHECKER:
        addBlackChecker(index);
        break;
      default:
        throw new IllegalArgumentException(
            Internationalization.get("board.error.invalid_piece_type", type));
    }
  }

  /**
   * Demotes a checker back to a pawn at the given index, if a checker is present.
   *
   * @param index bit index of the square to demote
   */
  public void demoteBit(int index) {
    if (isBitWhiteChecker(index)) {
      removeWhiteChecker(index);
      addWhitePawn(index);
    } else if (isBitBlackChecker(index)) {
      removeBlackChecker(index);
      addBlackPawn(index);
    }
  }

  /**
   * Promotes the pawn on the named square to a checker (king).
   *
   * @param square algebraic square name of the pawn to promote
   * @throws IllegalArgumentException if the square is out of bounds or contains
   *                                  no pawn
   */
  public void promote(String square) {
    promoteBit(squareToIndex(square));
  }

  /**
   * Applies a simple move from one square to another.
   *
   *
   * <p>This helper method converts board coordinates (e.g. "C3", "D4")
   * into internal indices and applies the move.
   *
   *
   * <p>Mainly intended for testing purposes. No move legality is checked.
   *
   * @param from source square (e.g. "C3")
   * @param to   destination square (e.g. "D4")
   */
  public void move(String from, String to) {
    int f = squareToIndex(from);
    int t = squareToIndex(to);
    Move m = new Move(f, t);
    applyMove(m);
  }

  /**
   * Removes any piece (white/black, pawn/checker) from the given square.
   * Intended for testing setup.
   *
   * @param from algebraic square name (e.g. "C3") of the piece to remove
   */
  public void remove(String from) {
    int f = squareToIndex(from);
    removeBlackChecker(f);
    removeBlackPawn(f);
    removeWhiteChecker(f);
    removeWhitePawn(f);
  }

  /**
   * Adds a white pawn on the given square.
   * Testing helper.
   *
   * @param square algebraic square name (e.g. "C3") where the white pawn should be placed
   */
  public void addWhite(String square) {
    int f = squareToIndex(square);
    addWhitePawn(f);
  }

  /**
   * Adds a black pawn on the given square.
   * Testing helper.
   *
   * @param square algebraic square name (e.g. "C3") where the black pawn should be placed
   */
  public void addBlack(String square) {
    int f = squareToIndex(square);
    addBlackPawn(f);
  }

  /**
   * Clears the board of all pieces. Testing helper.
   */
  public void clearBoard() {
    whitePawns1 = whitePawns2 = 0L;
    blackPawns1 = blackPawns2 = 0L;
    whiteCheckers1 = whiteCheckers2 = 0L;
    blackCheckers1 = blackCheckers2 = 0L;
  }

  /**
   * Returns the list of simple target squares for a checker on a given square.
   *
   *
   * <p>This method converts the board coordinate (e.g., "C3") to an internal index
   * and delegates to {@link #checkerSimpleTargets(int)} to compute the targets.
   *
   * @param square the source square in standard notation (e.g., "C3")
   * @return a list of target indices where the checker can move without capturing
   */
  public List<Integer> checkerSimpleTarg(String square) {
    return checkerSimpleTargets(squareToIndex(square));
  }

  /**
   * Returns the size of the board (e.g., 8 for an 8x8 board).
   *
   * @return the dimension of the board
   */
  public int getSizeBoard() {
    return sizeBoard;
  }

  /**
   * Creates a deep copy of this board.
   *
   * <p>The returned board has the same size and identical piece bitboards, but is an
   * independent instance that can be safely mutated for AI search.
   *
   * @return a deep copy of this board state
   */
  public Board copy() {
    Board clone = new Board(this.sizeBoard);
    clone.whitePawns1 = this.whitePawns1;
    clone.whitePawns2 = this.whitePawns2;
    clone.blackPawns1 = this.blackPawns1;
    clone.blackPawns2 = this.blackPawns2;
    clone.whiteCheckers1 = this.whiteCheckers1;
    clone.whiteCheckers2 = this.whiteCheckers2;
    clone.blackCheckers1 = this.blackCheckers1;
    clone.blackCheckers2 = this.blackCheckers2;
    return clone;
  }

  // ---------------------------------------------------------------------------
  // Manoury Conversion Utilities
  // ---------------------------------------------------------------------------

  /**
   * Converts a Manoury notation square number (e.g., 1 to 50 for a 10x10 board) 
   * to its internal bit index.
   *
   * @param manoury the square number in Manoury notation
   * @return bit index in the range {@code [0, indexMax)}
   * @throws IllegalArgumentException if the number is outside the valid range
   */
  public int manouryToIndex(int manoury) {
    if (manoury < 1 || manoury > this.indexMax) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.manoury_out_of_bounds", manoury));
    }

    int zeroBasedManoury = manoury - 1;
    int squaresPerRow = sizeBoard / 2;

    // Calculate the row counting from the top (0 = top row, 9 = bottom row for 10x10)
    int rowFromTop = zeroBasedManoury / squaresPerRow;
    // Calculate position within that row (0 to 4 for 10x10)
    int colInRow = zeroBasedManoury % squaresPerRow;

    // Invert the row to match the internal bottom-up representation
    int internalRow = (sizeBoard - 1) - rowFromTop;

    return (internalRow * squaresPerRow) + colInRow;
  }

  /**
   * Converts an internal bit index to its corresponding Manoury notation 
   * square number (e.g., 1 to 50).
   *
   * @param index bit index in the range {@code [0, indexMax)}
   * @return the square number in Manoury notation
   * @throws IllegalArgumentException if the index is outside the board limits
   */
  public int indexToManoury(int index) {
    if (index < 0 || index >= this.indexMax) {
      throw new IllegalArgumentException(
          Internationalization.get("board.error.internal_index_out_of_bounds", index));
    }

    int squaresPerRow = sizeBoard / 2;

    // Calculate internal row and column
    int internalRow = index / squaresPerRow;
    int colInRow = index % squaresPerRow;

    // Invert the row to match Manoury's top-down representation
    int rowFromTop = (sizeBoard - 1) - internalRow;

    return (rowFromTop * squaresPerRow) + colInRow + 1; // +1 because Manoury starts at 1
  }

  /**
   * Returns the total count of white pawns currently on the board by summing the
   * bit counts of both white pawn bitboards.
   *
   * @return the number of white pawns on the board
   */
  public int whitePawnsCount() {
    return Long.bitCount(whitePawns1) + Long.bitCount(whitePawns2);
  }

  /**
   * Returns the total count of black pawns currently on the board by summing the
   * bit counts of both black pawn bitboards.
   *
   * @return the number of black pawns on the board
   */
  public int blackPawnsCount() {
    return Long.bitCount(blackPawns1) + Long.bitCount(blackPawns2);
  }

  /**
   * Returns the total count of white checkers currently on the board by summing the
   * bit counts of both white checker bitboards.
   *
   * @return the number of white checkers on the board
   */
  public int whiteCheckersCount() {
    return Long.bitCount(whiteCheckers1) + Long.bitCount(whiteCheckers2);
  }

  /**
   * Returns the total count of black checkers currently on the board by summing the
   * bit counts of both black checker bitboards.
   *
   * @return the number of black checkers on the board
   */
  public int blackCheckersCount() {
    return Long.bitCount(blackCheckers1) + Long.bitCount(blackCheckers2);
  }

}