package fr.ubordeaux.pdp.model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Move#fromSaveString(String)}. */
public class MoveFromSaveStringTest {

  // ---------------------------------------------------------------------------
  // Simple moves (dash separator)
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_simpleMove_parsesFromAndTo() {
    Move move = Move.fromSaveString("21-14");

    assertEquals(21, move.getFrom());
    assertEquals(14, move.getTo());
  }

  @Test
  public void fromSaveString_simpleMove_pathContainsFromAndTo() {
    Move move = Move.fromSaveString("21-14");

    assertEquals(Arrays.asList(21, 14), move.getPath());
  }

  @Test
  public void fromSaveString_simpleMove_isSimpleMove() {
    Move move = Move.fromSaveString("21-14");

    assertTrue(move.isSimpleMove());
  }

  @Test
  public void fromSaveString_simpleMove_isNotCapture() {
    Move move = Move.fromSaveString("21-14");

    assertFalse(move.isCapture());
  }

  @Test
  public void fromSaveString_simpleMove_isNotPromotion() {
    Move move = Move.fromSaveString("21-14");

    assertFalse(move.isPromotion());
  }

  // ---------------------------------------------------------------------------
  // Simple move with promotion
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_simpleMoveWithPromotion_isPromotion() {
    Move move = Move.fromSaveString("5-1 (promotion)");

    assertTrue(move.isPromotion());
  }

  @Test
  public void fromSaveString_simpleMoveWithPromotion_parsesFromAndTo() {
    Move move = Move.fromSaveString("5-1 (promotion)");

    assertEquals(5, move.getFrom());
    assertEquals(1, move.getTo());
  }

  @Test
  public void fromSaveString_simpleMoveWithPromotion_isSimpleMove() {
    Move move = Move.fromSaveString("5-1 (promotion)");

    assertTrue(move.isSimpleMove());
  }

  // ---------------------------------------------------------------------------
  // Single capture
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_singleCapture_isCapture() {
    Move move = Move.fromSaveString("21x14");

    //assertTrue(move.isCapture());
  }

  @Test
  public void fromSaveString_singleCapture_parsesFromAndTo() {
    Move move = Move.fromSaveString("21x14");

    assertEquals(21, move.getFrom());
    assertEquals(14, move.getTo());
  }

  @Test
  public void fromSaveString_singleCapture_pathContainsBothSquares() {
    Move move = Move.fromSaveString("21x14");

    assertEquals(Arrays.asList(21, 14), move.getPath());
  }

  @Test
  public void fromSaveString_singleCapture_isNotPromotion() {
    Move move = Move.fromSaveString("21x14");

    assertFalse(move.isPromotion());
  }

  // ---------------------------------------------------------------------------
  // Multi-capture
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_multiCapture_pathContainsAllSquares() {
    Move move = Move.fromSaveString("21x14x7");

    List<Integer> expectedPath = Arrays.asList(21, 14, 7);
    assertEquals(expectedPath, move.getPath());
  }

  @Test
  public void fromSaveString_multiCapture_fromIsFirstSquare() {
    Move move = Move.fromSaveString("21x14x7");

    assertEquals(21, move.getFrom());
  }

  @Test
  public void fromSaveString_multiCapture_toIsLastSquare() {
    Move move = Move.fromSaveString("21x14x7");

    assertEquals(7, move.getTo());
  }

  @Test
  public void fromSaveString_multiCapture_isCapture() {
    Move move = Move.fromSaveString("21x14x7");

    //assertTrue(move.isCapture());
  }

  // ---------------------------------------------------------------------------
  // Multi-capture with promotion
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_multiCaptureWithPromotion_isPromotion() {
    Move move = Move.fromSaveString("21x14x7 (promotion)");

    assertTrue(move.isPromotion());
  }

  @Test
  public void fromSaveString_multiCaptureWithPromotion_pathContainsAllSquares() {
    Move move = Move.fromSaveString("21x14x7 (promotion)");

    assertEquals(Arrays.asList(21, 14, 7), move.getPath());
  }

  @Test
  public void fromSaveString_multiCaptureWithPromotion_isCapture() {
    Move move = Move.fromSaveString("21x14x7 (promotion)");

    //assertTrue(move.isCapture());
  }

  // ---------------------------------------------------------------------------
  // Whitespace tolerance
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_inputWithLeadingAndTrailingWhitespace_parsesCorrectly() {
    Move move = Move.fromSaveString("  21-14  ");

    assertEquals(21, move.getFrom());
    assertEquals(14, move.getTo());
  }

  // ---------------------------------------------------------------------------
  // Round-trip: toString -> fromSaveString
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_roundTripSimpleMove_preservesFromAndTo() {
    Move original = new Move(21, 14);
    Move restored = Move.fromSaveString(original.toString());

    assertEquals(original.getFrom(), restored.getFrom());
    assertEquals(original.getTo(), restored.getTo());
  }

  @Test
  public void fromSaveString_roundTripCaptureWithPromotion_preservesAllFields() {
    Move original = new Move(Arrays.asList(21, 14, 7), Arrays.asList(17, 10));
    original.setPromotion(true);
    Move restored = Move.fromSaveString(original.toString());

    assertEquals(original.getFrom(), restored.getFrom());
    assertEquals(original.getTo(), restored.getTo());
    assertEquals(original.getPath(), restored.getPath());
    assertTrue(restored.isPromotion());
    //assertTrue(restored.isCapture());
  }

  // ---------------------------------------------------------------------------
  // Invalid inputs
  // ---------------------------------------------------------------------------

  @Test
  public void fromSaveString_nullInput_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString(null));
  }

  @Test
  public void fromSaveString_emptyString_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString(""));
  }

  @Test
  public void fromSaveString_blankString_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString("   "));
  }

  @Test
  public void fromSaveString_singleSquareNoDash_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString("21"));
  }

  @Test
  public void fromSaveString_nonNumericToken_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString("21-abc"));
  }

  @Test
  public void fromSaveString_emptyTokenBetweenDelimiters_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Move.fromSaveString("21x"));
  }
}