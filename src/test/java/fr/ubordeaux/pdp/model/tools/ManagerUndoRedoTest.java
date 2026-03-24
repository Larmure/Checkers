package fr.ubordeaux.pdp.model.tools;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.player.PlayerColor;

import java.util.ArrayList;
import java.util.List;

import fr.ubordeaux.pdp.model.core.Piece;

class ManagerUndoRedoTest {

    private ManagerUndoRedo manager;
    private SpyBoard spyBoard;

    @BeforeEach
    void setUp() {
        spyBoard = new SpyBoard(Utils.DEFAULT_BOARD_SIZE);
        manager = new ManagerUndoRedo(spyBoard);
    }

    @Test
    void testRegisterMoveClearsRedo() {
        Move move1 = new Move(spyBoard.squareToIndex("A1"), spyBoard.squareToIndex("B2"));
        Move move2 = new Move(spyBoard.squareToIndex("B2"), spyBoard.squareToIndex("C3"));

        manager.registerMove(PlayerColor.WHITE, move1);
        manager.undo(false);

        manager.registerMove(PlayerColor.WHITE, move2);

        boolean redoResult = manager.redo(false);
        assertFalse(redoResult, "The redo should fail because the Redo stack was cleared by the new move.");
    }

    @Test
    void testUndoSimpleMove() {
        Move move = new Move(spyBoard.squareToIndex("C3"), spyBoard.squareToIndex("D4"));
        manager.registerMove(PlayerColor.WHITE, move);

        boolean result = manager.undo(false);

        assertTrue(result, "The undo operation should succeed.");
        assertNotNull(spyBoard.lastAppliedMove, "A reverse move should be applied to the board.");

        // FIX: Compare the integer destination to the integer index of the origin
        int expectedIndex = spyBoard.squareToIndex("C3");
        assertEquals(expectedIndex, spyBoard.lastAppliedMove.getTo(),
                "The destination of the reverse move should be the origin of the initial move.");
    }

    @Test
    void testUndoWithPromotionAndCapture() {
        Move move = new Move(spyBoard.squareToIndex("G7"), spyBoard.squareToIndex("H8"));

        move.setPromotion(true);
        move.getCaptured().add(45);
        move.getCapturedColors().add(Piece.BLACK_PAWN);

        manager.registerMove(PlayerColor.WHITE, move);

        manager.undo(false);

        // This will now pass because SpyBoard overrides the correct method signature
        assertTrue(spyBoard.demoteBitCalled,
                "The demoteBit method of the board should be called if the move was a promotion.");
        assertEquals(1, spyBoard.restoredPieces.size(), "A captured piece should have been restored.");
        assertEquals(45, spyBoard.restoredPieces.get(0),
                "The correct position of the captured piece should be restored.");
    }

    @Test
    void testRedoMove() {
        Move move = new Move(spyBoard.squareToIndex("E5"), spyBoard.squareToIndex("F6"));
        move.getCapturedColors().add(Piece.WHITE_PAWN);

        manager.registerMove(PlayerColor.BLACK, move);
        manager.undo(true);

        spyBoard.lastAppliedMove = null;

        boolean result = manager.redo(true);

        assertTrue(result, "The Redo should succeed.");
        assertNotNull(spyBoard.lastAppliedMove, "The move should be reapplied to the board.");

        // FIX: Compare the integer destination to the integer index
        int expectedIndex = spyBoard.squareToIndex("F6");
        assertEquals(expectedIndex, spyBoard.lastAppliedMove.getTo(),
                "The destination should match the original move.");
        assertTrue(move.getCapturedColors().isEmpty(),
                "Captured colors should be cleared to avoid duplicates during Redo.");
    }

    @Test
    void testUndoEmptyHistoryReturnsFalse() {
        boolean result = manager.undo(true);
        assertFalse(result, "Doing an undo with an empty history should return false.");
    }

    // --- Inner class to spy on the board (Manual Spy / Mock) ---
    private static class SpyBoard extends Board {
        Move lastAppliedMove;
        boolean demoteBitCalled = false;
        List<Integer> restoredPieces = new ArrayList<>();

        public SpyBoard(int size) {
            super(size);
        }

        @Override
        public void applyMove(Move move) {
            this.lastAppliedMove = move;
        }

        // FIX: Replaced 'Object' with 'int' to properly override the Board method. 
        // Added @Override to ensure it complains at compile time if it doesn't match perfectly.
        @Override
        public void demoteBit(int position) {
            this.demoteBitCalled = true;
        }

        @Override
        public void restorePiece(int pos, Piece type) {
            this.restoredPieces.add(pos);
        }
    }
}