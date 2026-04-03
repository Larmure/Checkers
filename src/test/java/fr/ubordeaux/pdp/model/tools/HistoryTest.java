package fr.ubordeaux.pdp.model.tools;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.player.PlayerColor;

class HistoryTest {

    private History history;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Move simpleMove() {
        return new Move(21, 17);
    }

    private Move singleCapture() {
        return new Move(List.of(21, 14), List.of(17));
    }

    private Move multiCapture() {
        return new Move(List.of(21, 14, 7), List.of(17, 10));
    }

    private Move promotionMove() {
        Move m = new Move(4, 1);
        m.setPromotion(true);
        return m;
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        history = new History();
    }

    // ─── addMove / getLastMove ───────────────────────────────────────────────

    @Test
    void testAddMoveAndGetLastMove() {
        Move move = simpleMove();
        history.addMove(PlayerColor.WHITE, move);
        assertEquals(move, history.getLastMove());
    }

    @Test
    void testGetLastMoveReturnsLatestAdded() {
        history.addMove(PlayerColor.WHITE, simpleMove());
        Move last = singleCapture();
        history.addMove(PlayerColor.BLACK, last);
        assertEquals(last, history.getLastMove());
    }

    @Test
    void testGetLastMoveOnEmptyHistoryThrows() {
        assertThrows(IllegalArgumentException.class, () -> history.getLastMove());
    }

    // ─── reMove ──────────────────────────────────────────────────────────────

    @Test
    void testReMoveRemovesLastEntry() {
        Move first = simpleMove();
        history.addMove(PlayerColor.WHITE, first);
        history.addMove(PlayerColor.BLACK, singleCapture());
        history.reMove();
        assertEquals(first, history.getLastMove());
    }

    @Test
    void testReMoveOnEmptyHistoryThrows() {
        assertThrows(IllegalArgumentException.class, () -> history.reMove());
    }

    @Test
    void testReMoveUntilEmptyThenThrows() {
        history.addMove(PlayerColor.WHITE, simpleMove());
        history.reMove();
        assertThrows(IllegalArgumentException.class, () -> history.getLastMove());
    }

    @Test
    void testHistoryStringWhitePrefix() {
        history.addMove(PlayerColor.WHITE, simpleMove());
        assertTrue(history.historyString().contains("W 21-17"));
    }

    @Test
    void testHistoryStringBlackPrefix() {
        history.addMove(PlayerColor.BLACK, simpleMove());
        assertTrue(history.historyString().contains("B 21-17"));
    }

    @Test
    void testHistoryStringSimpleCapture() {
        history.addMove(PlayerColor.WHITE, singleCapture());
        String s = history.historyString();
        assertTrue(s.contains("{" + Internationalization.get("history.capture.single") + "}"));
    }

    @Test
    void testHistoryStringMultipleCapture() {
        history.addMove(PlayerColor.WHITE, multiCapture());
        assertTrue(history.historyString()
                .contains("{" + Internationalization.get("history.capture.multiple") + "}"));
    }

    @Test
    void testHistoryStringPromotion() {
        history.addMove(PlayerColor.WHITE, promotionMove());
        assertTrue(history.historyString().contains("{" + Internationalization.get("history.promotion") + "}"));
    }

    @Test
    void testHistoryStringMultipleMoves() {
        history.addMove(PlayerColor.WHITE, simpleMove());
        history.addMove(PlayerColor.BLACK, singleCapture());
        String s = history.historyString();
        assertTrue(s.contains("W 21-17"));
        assertTrue(s.contains("B 21x14"));
        assertTrue(s.contains("{" + Internationalization.get("history.capture.single") + "}"));
    }
}