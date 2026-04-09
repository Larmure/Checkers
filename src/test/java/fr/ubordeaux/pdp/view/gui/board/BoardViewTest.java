package fr.ubordeaux.pdp.view.gui.board;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.model.core.Move;
import fr.ubordeaux.pdp.model.player.AiPlayer;
import fr.ubordeaux.pdp.model.player.HumanPlayer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link BoardView#handleClick(int, int, int, int)}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Early return conditions (AI turn, null board)</li>
 *   <li>First click behavior (selected piece with valid destinations)</li>
 *   <li>Second click behavior (move execution)</li>
 *   <li>State management (selection, hints, valid moves)</li>
 *   <li>Visual updates (redraw on selection)</li>
 * </ul>
 */
class BoardViewTest {

  @BeforeAll
  static void initJavaFxToolkit() {
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException ignored) {
      // JavaFX toolkit already initialized by another test class.
    }
  }

  @Nested
  @DisplayName("handleClick early returns")
  class HandleClickEarlyReturns {

    private BoardView boardView;
    private GameController controller;
    private Board board;
    private GameCheckers game;

    @BeforeEach
    void setUp() {
      controller = mock(GameController.class);
      board = mock(Board.class);
      game = mock(GameCheckers.class);

      when(controller.getGame()).thenReturn(game);
      when(game.getBoard()).thenReturn(board);
      when(board.getSizeBoard()).thenReturn(8);

      boardView = new BoardView(controller);
    }

    @Test
    @DisplayName("handleClick should return early when it's AI turn")
    void testHandleClickReturnsEarlyOnAiTurn() throws Exception {
      // Set board with AI player as current player
      AiPlayer aiPlayer = mock(AiPlayer.class);
      when(game.getCurrentPlayer()).thenReturn(aiPlayer);

      boardView.refresh(game);

      int gridRow = 0;
      int gridCol = 0;
      int modelRow = 7;
      int modelCol = 0;

      // Should not throw
      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      // Verify no move was executed
      verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("handleClick should return early when board is null")
    void testHandleClickReturnsEarlyWhenBoardNull() throws Exception {
      boardView.drawEmpty(8);

      int gridRow = 0;
      int gridCol = 0;
      int modelRow = 7;
      int modelCol = 0;

      // Should not throw
      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      // Verify no move was executed
      verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
    }
  }

  @Nested
  @DisplayName("handleClick first click behavior")
  class FirstClickBehavior {

    private BoardView boardView;
    private GameController controller;
    private Board board;
    private GameCheckers game;
    private HumanPlayer humanPlayer;

    @BeforeEach
    void setUp() {
      controller = mock(GameController.class);
      board = mock(Board.class);
      game = mock(GameCheckers.class);
      humanPlayer = mock(HumanPlayer.class);

      when(board.getSizeBoard()).thenReturn(8);
      when(controller.getGame()).thenReturn(game);
      when(game.getBoard()).thenReturn(board);
      when(game.getCurrentPlayer()).thenReturn(humanPlayer);
      when(board.indexToSquare(anyInt())).thenAnswer(
          invocation -> {
            int index = invocation.getArgument(0);
            int row = index / 4;
            int col = (index % 4) * 2;
            if (row % 2 == 1)
              col += 1;
            return "" + (char) ('A' + row) + (col + 1);
          });

      boardView = new BoardView(controller);
    }

    @Test
    @DisplayName("handleClick should not select empty square")
    void testFirstClickOnEmptySquareDoesNotSelect() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      boardView.refresh(game);

      int gridRow = 2;
      int gridCol = 3;
      int modelRow = 5;
      int modelCol = 3;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      // Selection state should remain unselected
      int selRow = getSelRow(boardView);
      int selCol = getSelCol(boardView);
      assertEquals(-1, selRow);
      assertEquals(-1, selCol);

      verify(controller, never()).executeMove(anyString(), anyString(), anyBoolean());
    }

    @Test
    @DisplayName("handleClick should select square with white piece")
    void testFirstClickOnWhitePieceSelectsSquare() throws Exception {
      // Pawn at index 10 -> modelRow = 2, modelCol = 4
      when(board.isBitWhiteChecker(10)).thenReturn(false);
      when(board.isBitWhitePawn(10)).thenReturn(true);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      int gridRow = 5;
      int gridCol = 4;
      int modelRow = 2;
      int modelCol = 4;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      // Selection state should be updated
      int selRow = getSelRow(boardView);
      int selCol = getSelCol(boardView);
      assertEquals(gridRow, selRow);
      assertEquals(gridCol, selCol);
    }

    @Test
    @DisplayName("handleClick should select square with black piece")
    void testFirstClickOnBlackPieceSelectsSquare() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(8)).thenReturn(true);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      // modelRow=2, modelCol=0: dark square, index=8, gridRow=5, gridCol=0
      int gridRow = 5;
      int gridCol = 0;
      int modelRow = 2;
      int modelCol = 0;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      int selRow = getSelRow(boardView);
      int selCol = getSelCol(boardView);
      assertEquals(gridRow, selRow);
      assertEquals(gridCol, selCol);
    }

    @Test
    @DisplayName("handleClick should compute valid destination squares")
    void testFirstClickComputesValidDestinations() throws Exception {
      // White pawn at B2 (modelRow=1, modelCol=1, index=4)
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(4)).thenReturn(true);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      // Create possible move for white pawn from index 4 to index 6
      Move move = mock(Move.class);
      when(move.getFrom()).thenReturn(4); // Index of selected piece
      when(move.getTo()).thenReturn(6); // Destination index
      when(move.isCapture()).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(List.of(move));
      when(board.indexToSquare(6)).thenReturn("D3");

      boardView.refresh(game);

      // modelRow=1, modelCol=1: gridRow=6, gridCol=1
      int gridRow = 6;
      int gridCol = 1;
      int modelRow = 1;
      int modelCol = 1;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      Set<String> validSquares = getValidDestinationSquares(boardView);
      assertTrue(validSquares.contains("D3"));
    }

    @Test
    @DisplayName("handleClick should include capture path in valid destinations")
    void testFirstClickIncludesCapturePathInValidDestinations() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(0)).thenReturn(true); // Piece at (0,0), index=0
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      // Move with capture and path
      Move move = mock(Move.class);
      when(move.getFrom()).thenReturn(0);
      when(move.getTo()).thenReturn(4);
      when(move.isCapture()).thenReturn(true);
      when(move.getPath()).thenReturn(List.of(1, 4)); // Path through squares

      when(game.getPossibleMoves(humanPlayer)).thenReturn(List.of(move));
      when(board.indexToSquare(1)).thenReturn("B1");
      when(board.indexToSquare(4)).thenReturn("C2");

      boardView.refresh(game);

      // modelRow=0, modelCol=0: gridRow=7, gridCol=0
      int gridRow = 7;
      int gridCol = 0;
      int modelRow = 0;
      int modelCol = 0;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      Set<String> validSquares = getValidDestinationSquares(boardView);
      assertTrue(validSquares.contains("B1"));
      assertTrue(validSquares.contains("C2"));
    }

    @Test
    @DisplayName("handleClick should clear hint state on first click")
    void testFirstClickClearsHintState() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(3)).thenReturn(true);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);
      boardView.showHint("A1", "B2");

      int gridRow = 4;
      int gridCol = 3;
      int modelRow = 3;
      int modelCol = 3;

      invokeHandleClick(boardView, gridRow, gridCol, modelRow, modelCol);

      String hintFrom = getHintFrom(boardView);
      String hintTo = getHintTo(boardView);
      assertNull(hintFrom);
      assertNull(hintTo);
    }
  }

  @Nested
  @DisplayName("handleClick second click (move execution)")
  class SecondClickBehavior {

    private BoardView boardView;
    private GameController controller;
    private Board board;
    private GameCheckers game;
    private HumanPlayer humanPlayer;

    @BeforeEach
    void setUp() {
      controller = mock(GameController.class);
      board = mock(Board.class);
      game = mock(GameCheckers.class);
      humanPlayer = mock(HumanPlayer.class);

      when(board.getSizeBoard()).thenReturn(8);
      when(controller.getGame()).thenReturn(game);
      when(game.getBoard()).thenReturn(board);
      when(game.getCurrentPlayer()).thenReturn(humanPlayer);

      boardView = new BoardView(controller);
    }

    @Test
    @DisplayName("handleClick should execute move on second click")
    void testSecondClickExecutesMove() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(4)).thenReturn(true); // Pawn at index 4

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      // First click: select a piece at B2 (modelRow=1, modelCol=1)
      invokeHandleClick(boardView, 6, 1, 1, 1);

      // Verify piece is selected
      assertEquals(6, getSelRow(boardView));
      assertEquals(1, getSelCol(boardView));

      // Second click: move to destination at C3 (modelRow=2, modelCol=2)
      invokeHandleClick(boardView, 5, 2, 2, 2);

      // Move should be executed: B2 -> C3
      verify(controller).executeMove("B2", "C3", false);
    }

    @Test
    @DisplayName("handleClick should clear selection after move execution")
    void testSecondClickClearsSelection() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(0)).thenReturn(true);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      // First click
      invokeHandleClick(boardView, 7, 0, 0, 0);
      // Second click
      invokeHandleClick(boardView, 6, 1, 1, 1);

      // Selection should be cleared
      assertEquals(-1, getSelRow(boardView));
      assertEquals(-1, getSelCol(boardView));
    }

    @Test
    @DisplayName("handleClick should clear valid destination squares after move")
    void testSecondClickClearsValidDestinations() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(2)).thenReturn(true); // Index 2: (0,4)
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      // Create possible move from index 2 to index 3
      Move move = mock(Move.class);
      when(move.getFrom()).thenReturn(2);
      when(move.getTo()).thenReturn(3);
      when(move.isCapture()).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(List.of(move));
      when(board.indexToSquare(3)).thenReturn("G1");

      boardView.refresh(game);

      // First click: select piece at (0,4): gridRow=7, gridCol=4
      invokeHandleClick(boardView, 7, 4, 0, 4);
      Set<String> validBefore = getValidDestinationSquares(boardView);
      assertTrue(validBefore.contains("G1"));

      // Second click: move to (0,6): gridRow=7, gridCol=6
      invokeHandleClick(boardView, 7, 6, 0, 6);
      Set<String> validAfter = getValidDestinationSquares(boardView);
      assertTrue(validAfter.isEmpty());
    }

    @Test
    @DisplayName("handleClick should compute correct algebraic notation for move")
    void testSecondClickComputesCorrectAlgebraicNotation() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(10)).thenReturn(true); // Index 10: (2,4) = C5
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      // Select piece at (2,4) = C5: gridRow=5, gridCol=4
      invokeHandleClick(boardView, 5, 4, 2, 4);

      // Move to (2,6) = C7: gridRow=5, gridCol=6
      invokeHandleClick(boardView, 5, 6, 2, 6);

      verify(controller).executeMove("C5", "C7", false);
    }
  }

  @Nested
  @DisplayName("handleClick state and hint management")
  class StateAndHintManagement {

    private BoardView boardView;
    private GameController controller;
    private Board board;
    private GameCheckers game;
    private HumanPlayer humanPlayer;

    @BeforeEach
    void setUp() {
      controller = mock(GameController.class);
      board = mock(Board.class);
      game = mock(GameCheckers.class);
      humanPlayer = mock(HumanPlayer.class);

      when(board.getSizeBoard()).thenReturn(8);
      when(controller.getGame()).thenReturn(game);
      when(game.getBoard()).thenReturn(board);
      when(game.getCurrentPlayer()).thenReturn(humanPlayer);

      boardView = new BoardView(controller);
    }

    @Test
    @DisplayName("handleClick should clear selection after any second click (move attempt)")
    void testSecondFirstClickClearsSelection() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(0)).thenReturn(true); // Index 0: (0,0)
      when(board.isBitBlackChecker(5)).thenReturn(true); // Index 5: (1,3)
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);

      // Select first piece at (0,0): gridRow=7, gridCol=0
      invokeHandleClick(boardView, 7, 0, 0, 0);
      assertEquals(7, getSelRow(boardView));
      assertEquals(0, getSelCol(boardView));

      // Click on different piece at (1,3): gridRow=6, gridCol=3
      // This should be treated as a move destination and clear selection
      invokeHandleClick(boardView, 6, 3, 1, 3);
      assertEquals(-1, getSelRow(boardView));
      assertEquals(-1, getSelCol(boardView));

      // Verify moveexecution was attempted (from (0,0) to (1,3))
      verify(controller).executeMove("A1", "B4", false);
    }

    @Test
    @DisplayName("handleClick should clear hints when clicking any square")
    void testHandleClickAlwaysClearsHints() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(anyInt())).thenReturn(false);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      boardView.refresh(game);
      boardView.showHint("A1", "B2");

      invokeHandleClick(boardView, 5, 5, 2, 2);

      assertNull(getHintFrom(boardView));
      assertNull(getHintTo(boardView));
    }

    @Test
    @DisplayName("handleClick should clear hints before checking selection state")
    void testHandleClickClearsHintsBeforeProcessing() throws Exception {
      when(board.isBitWhiteChecker(anyInt())).thenReturn(false);
      when(board.isBitWhitePawn(1)).thenReturn(true);
      when(board.isBitBlackChecker(anyInt())).thenReturn(false);
      when(board.isBitBlackPawn(anyInt())).thenReturn(false);

      when(game.getPossibleMoves(humanPlayer)).thenReturn(new ArrayList<>());

      boardView.refresh(game);
      boardView.showHint("X1", "Y2");

      invokeHandleClick(boardView, 3, 0, 0, 0);

      // After click, hints should be null even though piece was selected
      String hintFrom = getHintFrom(boardView);
      String hintTo = getHintTo(boardView);
      assertNull(hintFrom);
      assertNull(hintTo);
    }
  }

  @Nested
  @DisplayName("getPieceChar mapping")
  class PieceCharMapping {

    private BoardView boardView;
    private Board board;
    private GameCheckers game;

    @BeforeEach
    void setUp() {
      GameController controller = mock(GameController.class);
      board = mock(Board.class);
      game = mock(GameCheckers.class);

      when(game.getBoard()).thenReturn(board);
      when(board.getSizeBoard()).thenReturn(8);

      boardView = new BoardView(controller);
      boardView.refresh(game);
    }

    @Test
    @DisplayName("getPieceChar should return O for white checker")
    void testGetPieceCharReturnsWhiteChecker() throws Exception {
      when(board.isBitWhiteChecker(0)).thenReturn(true);

      char piece = invokeGetPieceChar(boardView, 0, 0);

      assertEquals('O', piece);
    }

    @Test
    @DisplayName("getPieceChar should return x for black pawn")
    void testGetPieceCharReturnsBlackPawn() throws Exception {
      when(board.isBitWhiteChecker(0)).thenReturn(false);
      when(board.isBitWhitePawn(0)).thenReturn(false);
      when(board.isBitBlackChecker(0)).thenReturn(false);
      when(board.isBitBlackPawn(0)).thenReturn(true);

      char piece = invokeGetPieceChar(boardView, 0, 0);

      assertEquals('x', piece);
    }
  }

  // ========== Helper Methods ==========

  /**
   * Invokes the private {@code handleClick} method on the given BoardView
   * using reflection.
   */
  private static void invokeHandleClick(
      BoardView boardView, int gridRow, int gridCol, int modelRow, int modelCol)
      throws Exception {
    var method = BoardView.class.getDeclaredMethod(
        "handleClick", int.class, int.class, int.class, int.class);
    method.setAccessible(true);
    method.invoke(boardView, gridRow, gridCol, modelRow, modelCol);
  }

  /** Invokes the private {@code getPieceChar} method using reflection. */
  private static char invokeGetPieceChar(BoardView boardView, int modelRow, int modelCol)
      throws Exception {
    var method = BoardView.class.getDeclaredMethod("getPieceChar", int.class, int.class);
    method.setAccessible(true);
    return (char) method.invoke(boardView, modelRow, modelCol);
  }

  /** Retrieves the current selection row using reflection. */
  private static int getSelRow(BoardView boardView) throws Exception {
    Field field = BoardView.class.getDeclaredField("selRow");
    field.setAccessible(true);
    return (int) field.get(boardView);
  }

  /** Retrieves the current selection column using reflection. */
  private static int getSelCol(BoardView boardView) throws Exception {
    Field field = BoardView.class.getDeclaredField("selCol");
    field.setAccessible(true);
    return (int) field.get(boardView);
  }

  /** Retrieves the valid destination squares using reflection. */
  @SuppressWarnings("unchecked")
  private static Set<String> getValidDestinationSquares(BoardView boardView)
      throws Exception {
    Field field = BoardView.class.getDeclaredField("validDestinationSquares");
    field.setAccessible(true);
    return (Set<String>) field.get(boardView);
  }

  /** Retrieves the hint "from" value using reflection. */
  private static String getHintFrom(BoardView boardView) throws Exception {
    Field field = BoardView.class.getDeclaredField("hintFrom");
    field.setAccessible(true);
    return (String) field.get(boardView);
  }

  /** Retrieves the hint "to" value using reflection. */
  private static String getHintTo(BoardView boardView) throws Exception {
    Field field = BoardView.class.getDeclaredField("hintTo");
    field.setAccessible(true);
    return (String) field.get(boardView);
  }
}
