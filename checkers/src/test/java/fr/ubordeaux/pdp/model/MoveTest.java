package fr.ubordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class MoveTest {

  @Test
  void testSimpleMoveConstructor() {
    Move move = new Move(5, 9);

    assertEquals(5, move.getFrom());
    assertEquals(9, move.getTo());
    assertEquals(List.of(5, 9), move.getPath());
    assertTrue(move.getCaptured().isEmpty());

    assertTrue(move.isSimpleMove());
    assertFalse(move.isCapture());
    assertFalse(move.isPromotion());
  }

  @Test
  void testCaptureMoveConstructor() {
    List<Integer> path = List.of(10, 17, 26);
    List<Integer> captured = List.of(14, 21);

    Move move = new Move(path, captured);

    assertEquals(10, move.getFrom());
    assertEquals(26, move.getTo());
    assertEquals(path, move.getPath());
    assertEquals(captured, move.getCaptured());

    assertTrue(move.isCapture());
    assertFalse(move.isSimpleMove());
    assertFalse(move.isPromotion());
  }

  @Test
  void testPromotionFlag() {
    Move move = new Move(3, 7);

    assertFalse(move.isPromotion());
    move.setPromotion(true);
    assertTrue(move.isPromotion());
  }

  @Test
  void testIsSimpleMove() {
    Move simple = new Move(2, 6);
    assertTrue(simple.isSimpleMove());

    Move capture = new Move(List.of(2, 9), List.of(5));
    assertFalse(capture.isSimpleMove());
  }

  @Test
  void testIsCapture() {
    Move simple = new Move(4, 8);
    assertFalse(simple.isCapture());

    Move capture = new Move(List.of(4, 11), List.of(7));
    assertTrue(capture.isCapture());
  }

  @Test
  void testToStringSimple() {
    Move move = new Move(1, 5);
    assertEquals("1-5", move.toString());
  }

  @Test
  void testToStringCapture() {
    Move move = new Move(List.of(2, 9, 16), List.of(6, 13));
    assertEquals("2x9x16", move.toString());
  }

  @Test
  void testToStringPromotion() {
    Move move = new Move(3, 7);
    move.setPromotion(true);

    assertEquals("3-7 (promotion)", move.toString());
  }

  @Test
  void testToStringCaptureWithPromotion() {
    Move move = new Move(List.of(5, 14), List.of(9));
    move.setPromotion(true);

    assertEquals("5x14 (promotion)", move.toString());
  }

  @Test
  void testPathIsDefensiveCopy() {
    List<Integer> path = List.of(1, 4, 9);
    List<Integer> captured = List.of(3, 6);

    Move move = new Move(path, captured);

    assertNotSame(path, move.getPath());
    assertNotSame(captured, move.getCaptured());
  }

  @Test
  void testSingleElementPathConstructor() {
    List<Integer> path = List.of(12);
    List<Integer> captured = List.of();

    Move move = new Move(path, captured);

    assertEquals(12, move.getFrom());
    assertEquals(12, move.getTo());
    assertFalse(move.isSimpleMove());
  }
}