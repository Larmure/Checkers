package fr.ubordeaux.pdp.model.core;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.player.HumanPlayer;
import fr.ubordeaux.pdp.model.player.Player;
import fr.ubordeaux.pdp.view.GameView;

/**
 * Tests for the GameCheckers class.
 *
 * Each method annotated with @Test checks one specific behavior of the game.
 * A test passes when nothing crashes AND all assertEquals/assertTrue calls are true.
 */
class GameCheckersTest {

    // The game we are testing. It is recreated fresh before each test.
    private GameCheckers game;

    /**
     * This method is called automatically BEFORE each test.
     * It guarantees that every test starts with a brand new game.
     */
    @BeforeEach
    void createNewGame() {
        game = new GameCheckers();
    }

    // =========================================================================
    // Who plays first?
    // =========================================================================
    @Test
    void whitePlayerGoesFirst() {
        // At the start of a game, it should be White's turn.
        Player currentPlayer = game.getCurrentPlayer();
        assertSame(game.getWhitePlayer(), currentPlayer,
                "At the start, the White Player should play first.");
    }

    // =========================================================================
    // Does the turn switch correctly between players?
    // =========================================================================
    @Test
    void turnSwitchesToBlackAfterWhite() throws Exception {
        // We cheat a little: we directly access the private field "isWhiteTurn"
        // and force it to false, as if White had just played.
        Field turnField = GameCheckers.class.getDeclaredField("isWhiteTurn");
        turnField.setAccessible(true); // Ask Java for permission to access it
        turnField.set(game, false); // Force: "it is no longer White's turn"

        // Now, the current player should be Black.
        Player currentPlayer = game.getCurrentPlayer();
        assertSame(game.getBlackPlayer(), currentPlayer,
                "When isWhiteTurn is false, the current player should be Black.");
    }

    // =========================================================================
    //  A player cannot play during the opponent's turn
    // =========================================================================
    @Test
    void aPlayerCannotPlayDuringTheOpponentsTurn() throws Exception {
        // Situation: it is White's turn (default state).
        // We create a fake Black player to simulate their attempt to play.
        Player fakeBlack = new HumanPlayer("Black Player");

        // Black should NOT be able to play during White's turn.
        assertFalse(game.isValidMove(null, fakeBlack),
                "Black should not be able to play during White's turn.");

        // Now force it to be Black's turn.
        Field turnField = GameCheckers.class.getDeclaredField("isWhiteTurn");
        turnField.setAccessible(true);
        turnField.set(game, false);

        // We create a fake White player to test the reverse situation.
        // (After setting isWhiteTurn=false, getCurrentPlayer() returns Black,
        //  so we build a separate fake White player for this check.)
        Player fakeWhite = new HumanPlayer("White Player");

        assertFalse(game.isValidMove(null, fakeWhite),
                "White should not be able to play during Black's turn.");
    }

    // =========================================================================
    //  Plays invalid moves
    // =========================================================================

    @Test
    void applyMoveWithInvalidMoveDoesNothing() {
        // Try to move from a non-existent or illegal square.
        // This should hit the "move == null" branch in applyMove.
        String currentPlayer = game.getCurrentPlayer().toString();

        game.applyMove("H12", "E1", false); // Clearly invalid squares

        // The turn should NOT have switched since the move was rejected.
        assertEquals(currentPlayer, game.getCurrentPlayer().toString(),
                "An invalid move should not switch the turn.");
    }

    // =========================================================================
    //  Observers (views) are properly notified
    // =========================================================================
    @Test
    void theViewIsNotifiedWhenRequested() {
        // We create a spy view that records whether it was called.
        boolean[] wasNotified = { false }; // Single-element array so we can modify it inside the anonymous class

        GameView spyView = new GameView() {
            @Override
            public void update(GameCheckers g) {
                wasNotified[0] = true; // Mark that the view received the notification
            }

            @Override
            public void start() {
            }

            @Override
            public void display(GameCheckers g) {
            }

            @Override
            public void showHint(String from, String to) {
            }
        };

        // Add our spy view to the game, then notify all observers.
        game.addObserver(spyView);
        game.notifyObservers();

        // The spy view must have been called.
        assertTrue(wasNotified[0], "The view should receive a notification after notifyObservers().");
    }

    // =========================================================================
    //  The game should not be over at the very beginning
    // =========================================================================
    @Test
    void gameIsNotFinishedAtStart() {
        // At the start, both sides have pieces and available moves.
        // So checkGameOver() should return the current state, not FINISHED.
        State returnedState = game.checkGameOver();

        assertNotEquals(State.FINISHED, returnedState,
                "The game should not be finished on the very first turn.");

        assertSame(game.getState(), returnedState,
                "checkGameOver() should return the current state as long as the game continues.");
    }

    // =========================================================================
    //  The game state can be changed
    // =========================================================================
    @Test
    void gameStateCanBeChanged() {
        // Check that the initial state exists.
        assertNotNull(game.getState(), "The initial state should not be null.");

        // Force the game state to FINISHED.
        State finishedState = State.FINISHED;
        game.setState(finishedState);

        // The game state should now be the one we just set.
        assertSame(finishedState, game.getState(),
                "setState() should correctly replace the current state.");
    }

    // =========================================================================
    //  Playing a move passes the turn to the other player
    // =========================================================================
    @Test
    void playingAMoveSwitchesTurnToOtherPlayer() {
        // Get the list of available moves for the current player (White).
        List<Move> availableMoves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(availableMoves.isEmpty(),
                "There should be available moves at the start of the game.");

        // Play the first available move.
        Move firstMove = availableMoves.get(0);
        String fromSquare = game.getBoard().indexToSquare(firstMove.getFrom());
        String toSquare = game.getBoard().indexToSquare(firstMove.getTo());

        game.applyMove(fromSquare, toSquare, false);

        // After White plays, it should no longer be White's turn.
        assertSame(game.getBlackPlayer(), game.getCurrentPlayer(),
                "After White plays, it should be Black's turn.");
    }

    // =========================================================================
    //  The game ends when a side has no more moves
    // =========================================================================
    @Test
    void gameEndsWhenASideHasNoMoreMoves() throws Exception {
        // Create a fake board that always says "no moves available".
        Board fakeBoard = new Board(12) {
            @Override
            public List<Move> getWhiteValidMoves() {
                return List.of();
            } // Empty list

            @Override
            public List<Move> getBlackValidMoves() {
                return List.of();
            } // Empty list
        };

        // Inject this fake board into the game using Java reflection.
        Field boardField = GameCheckers.class.getDeclaredField("board");
        boardField.setAccessible(true);
        boardField.set(game, fakeBoard);

        // Now checkGameOver() should detect that no moves are possible
        // and return FINISHED.
        State finalState = game.checkGameOver();

        assertEquals(State.FINISHED, finalState,
                "If no moves are possible, the game should transition to FINISHED.");
    }

    // =========================================================================
    //  Simulate an opening sequence (White plays, Black plays, White plays again)
    // =========================================================================
    @Test
    void simulateTwoFullTurns() {
        // Add an empty observer just to avoid errors if the code tries to notify views.
        game.addObserver(new GameView() {
            @Override
            public void update(GameCheckers g) {
            }

            @Override
            public void start() {
            }

            @Override
            public void display(GameCheckers g) {
            }

            @Override
            public void showHint(String from, String to) {
            }
        });

        // --- Turn 1: White plays ---
        List<Move> whiteMoves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(whiteMoves.isEmpty(), "White should have moves available at the start.");

        Move whiteMove = whiteMoves.get(0);
        game.applyMove(
                game.getBoard().indexToSquare(whiteMove.getFrom()),
                game.getBoard().indexToSquare(whiteMove.getTo()),
                false);

        // After White's move, it should be Black's turn.
        assertSame(game.getBlackPlayer(), game.getCurrentPlayer(),
                "After White plays, it should be Black's turn.");

        // --- Turn 1: Black plays ---
        List<Move> blackMoves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(blackMoves.isEmpty(), "Black should have moves available on their turn.");

        Move blackMove = blackMoves.get(0);
        game.applyMove(
                game.getBoard().indexToSquare(blackMove.getFrom()),
                game.getBoard().indexToSquare(blackMove.getTo()),
                false);

        // After Black's move, it should be White's turn again.
        assertSame(game.getWhitePlayer(), game.getCurrentPlayer(),
                "After Black plays, it should be White's turn again.");
    }

    @Test
    void testApplyMoveInvalidShouldPrintPossibleMoves() {
        // We use a move that is clearly invalid (from a square to the same square).
        // This will cause board.validateMove to return null.
        String sameSquare = "A1";

        // This call will enter the 'if (move == null)' block.
        // It will then execute the 'for (Move m : possibleMoves)' loop 
        // to print all valid options to the console.
        game.applyMove(sameSquare, sameSquare, false);

        // Verification: The turn should NOT have switched to the other player.
        // isWhiteTurn remains true because of the early 'return' in your model.
        assertTrue(game.getIsWhiteTurn(), "The turn should not change after an invalid move.");

        // Ensure the game board still exists
        assertNotNull(game.getBoard(), "The board should still be accessible.");
    }

    // =========================================================================
    //  Timer Player tests
    // =========================================================================
    @Test
    void testTimerPlayerDecrementsCurrentPlayerTime() {
        // Setup initial times for both players
        game.getWhitePlayer().setPlayTime(100);
        game.getBlackPlayer().setPlayTime(100);

        // At start, it is White's turn
        assertTrue(game.getIsWhiteTurn(), "It should be White's turn initially.");

        // Call timerPlayer (simulating 1 second passing)
        game.timerPlayer();

        assertEquals(99, game.getWhitePlayer().getPlayTime(), "White player's time should be decremented by 1.");
        assertEquals(100, game.getBlackPlayer().getPlayTime(), "Black player's time should remain unchanged.");

        // Switch to Black's turn manually for testing
        game.setWhiteTurn(false);
        game.timerPlayer();

        assertEquals(99, game.getWhitePlayer().getPlayTime(), "White player's time should remain unchanged.");
        assertEquals(99, game.getBlackPlayer().getPlayTime(), "Black player's time should be decremented by 1.");
    }

    // =========================================================================
    //  Undo / Redo Manage tests
    // =========================================================================
    @Test
    void testUndoAndRedoManage() {
        // Register a spy observer to verify that notifyObservers() is called
        boolean[] wasNotified = { false };
        game.addObserver(new GameView() {
            @Override
            public void update(GameCheckers g) {
                wasNotified[0] = true;
            }

            @Override
            public void start() {
            }

            @Override
            public void display(GameCheckers g) {
            }

            @Override
            public void showHint(String from, String to) {
            }
        });

        // 1. Play a valid move first so we have something in the history
        List<Move> whiteMoves = game.getPossibleMoves(game.getCurrentPlayer());
        Move firstMove = whiteMoves.get(0);
        String fromSquare = game.getBoard().indexToSquare(firstMove.getFrom());
        String toSquare = game.getBoard().indexToSquare(firstMove.getTo());

        game.applyMove(fromSquare, toSquare, false);

        // After move, turn switches to Black
        assertFalse(game.getIsWhiteTurn(), "Turn should switch to Black after White plays.");

        // Reset observer flag before testing undo
        wasNotified[0] = false;

        // 2. Test Undo
        game.undoManage();

        assertTrue(game.getIsWhiteTurn(), "Turn should revert to White after undo.");
        assertTrue(wasNotified[0], "Observers should be notified after a successful undo.");

        // Reset observer flag before testing redo
        wasNotified[0] = false;

        // 3. Test Redo
        game.redoManage();

        assertFalse(game.getIsWhiteTurn(), "Turn should switch back to Black after redo.");
        assertTrue(wasNotified[0], "Observers should be notified after a successful redo.");
    }

    @Test
    void testUndoManageFailsOnEmptyHistory() {
        // At the very beginning of the game, history is empty.
        assertTrue(game.getIsWhiteTurn(), "Game starts with White's turn.");

        // Try to undo when there are no moves
        game.undoManage();

        // The undo should fail gracefully, meaning the turn does NOT change
        assertTrue(game.getIsWhiteTurn(), "Turn should not change if undo fails (empty history).");
    }

    // =========================================================================
    //  Manoury Notation Tests
    // =========================================================================

    @Test
    void applyMoveWithInvalidManouryFormatCatchesException() {
        String currentPlayer = game.getCurrentPlayer().toString();

        game.applyMove("XYZ", "ABC", true);

        assertEquals(currentPlayer, game.getCurrentPlayer().toString(),
                "Turn shouldn't change after an invalide Manoury move.");
        assertTrue(game.getIsWhiteTurn());
    }

    @Test
    void applyMoveWithInvalidManouryMovePrintsSuggestions() {
        String currentPlayer = game.getCurrentPlayer().toString();

        game.applyMove("1", "1", true);

        assertEquals(currentPlayer, game.getCurrentPlayer().toString(),
                "Turn shouldn't change after an illegal Manoury move.");
        assertTrue(game.getIsWhiteTurn());
    }

    // =========================================================================
    //  Draw Rules (GameOver conditions)
    // =========================================================================

    @Test
    void checkGameOver_DrawBy50HalfTurnsWithoutProgress() throws Exception {
        // Force the noProgressCount to 50 (25 full turns rule)
        Field field = GameCheckers.class.getDeclaredField("noProgressCount");
        field.setAccessible(true);
        field.set(game, 50);

        State finalState = game.checkGameOver();

        assertEquals(State.FINISHED, finalState,
                "Game should transition to FINISHED after 50 half-turns without progress.");
    }

    @Test
    void checkGameOver_DrawBy32HalfTurnsInEndgame() throws Exception {
        // Force the endGameCount to 32 (16 full turns rule)
        Field field = GameCheckers.class.getDeclaredField("endGameCount");
        field.setAccessible(true);
        field.set(game, 32);

        State finalState = game.checkGameOver();

        assertEquals(State.FINISHED, finalState,
                "Game should transition to FINISHED after 32 half-turns in an endgame scenario.");
    }

    @Test
    void checkGameOver_DrawByThreeFoldRepetition() throws Exception {
        // Get the current board's unique string signature
        String currentSignature = game.getBoard().boardString();

        // Create a fake history where this exact position appears 3 times
        List<String> fakeHistory = new java.util.ArrayList<>();
        fakeHistory.add(currentSignature);
        fakeHistory.add(currentSignature);
        fakeHistory.add(currentSignature);

        // Inject the fake history into the game via Reflection
        Field field = GameCheckers.class.getDeclaredField("positionHistory");
        field.setAccessible(true);
        field.set(game, fakeHistory);

        State finalState = game.checkGameOver();

        assertEquals(State.FINISHED, finalState,
                "Game should transition to FINISHED when the same position occurs 3 times.");
    }
}