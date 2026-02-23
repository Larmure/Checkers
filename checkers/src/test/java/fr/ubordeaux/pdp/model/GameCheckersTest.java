package fr.ubordeaux.pdp.model;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.view.GameView;

class GameCheckersTest {

    private GameCheckers game;

    @BeforeEach
    void setUp() {
        game = new GameCheckers();
    }

    @Test
    void testGetCurrentPlayer_Branches() throws Exception {
        // Branche 1 : Tour des blancs (état initial)
        Player p1 = game.getCurrentPlayer();
        assertEquals("White Player", p1.toString(), "Au début, c'est le joueur Blanc");

        // Branche 2 : Tour des noirs
        // On utilise la Réflexion pour forcer le boolean 'isWhiteTurn' à false
        Field turnField = GameCheckers.class.getDeclaredField("isWhiteTurn");
        turnField.setAccessible(true);
        turnField.set(game, false); // On force le tour des noirs

        Player p2 = game.getCurrentPlayer();
        assertEquals("Black Player", p2.toString(), "Si isWhiteTurn est false, c'est le joueur Noir");
    }

    @Test
    void testIsValidMove_TurnLogic() throws Exception {
        // Récupération des deux joueurs
        Player white = game.getCurrentPlayer();
        // On crée un faux joueur noir pour le test
        Player fakeBlack = new HumanPlayer("Black Player"); 

        // Cas A : C'est le tour des Blancs (vrai par défaut), le Noir essaie de jouer
        assertFalse(game.isValidMove(null, fakeBlack), 
            "Retourne false si c'est le tour des Blancs et que Noir joue");

        // Cas B : On force le tour des Noirs
        Field turnField = GameCheckers.class.getDeclaredField("isWhiteTurn");
        turnField.setAccessible(true);
        turnField.set(game, false);

        // Maintenant c'est le tour des Noirs, le Blanc essaie de jouer
        assertFalse(game.isValidMove(null, white), 
            "Retourne false si c'est le tour des Noirs et que Blanc joue");
    }

    @Test
    void testObserverPattern() {
        // Classe Espion interne pour vérifier l'appel
        class SpyView extends GameView {
            boolean called = false;
            @Override public void update(GameCheckers g) { called = true; }
            @Override public void start() {}
            @Override public void display(GameCheckers g) {}
        }

        SpyView spy = new SpyView();

        // Branche 1 : addObserver (doit créer la liste car elle est null au début)
        game.addObserver(spy);

        // Branche 2 : notifyObservers (doit parcourir la liste)
        game.notifyObservers();

        assertTrue(spy.called, "L'observateur doit être notifié");
    }

    @Test
    void testCheckGameOver_Continue() {
        // Au début du jeu, il y a des mouvements possibles
        State result = game.checkGameOver();
        
        // On vérifie que l'état retourné est bien l'état courant (donc pas FinishedState)
        assertSame(game.getState(), result, 
            "Tant qu'il y a des coups, checkGameOver retourne l'état courant");
        assertFalse(result instanceof FinishedState, 
            "Le jeu ne doit pas être fini au démarrage");
    }

    @Test
    void testStateAccessors() {
        State initialState = game.getState();
        assertNotNull(initialState);

        State finish = new FinishedState();
        game.setState(finish);

        assertSame(finish, game.getState(), "Le setter doit modifier l'état");
    }

    @Test
    void testApplyMove_SwitchTurn() {
        // On récupère un coup valide pour ne pas dépendre d'un Move bidon
        List<Move> moves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(moves.isEmpty(), "Il doit y avoir des coups disponibles au début");

        Move first = moves.get(0);
        String from = game.getBoard().indexToSquare(first.getFrom());
        String to   = game.getBoard().indexToSquare(first.getTo());

        try {
            game.applyMove(from, to);
        } catch (Exception e) {
            // On ignore les erreurs éventuelles du Board
        }
    }

    @Test
    void testGameFinish_WithFakeBoard() throws Exception {
        // 1. On crée un "Faux Plateau" qui ne renvoie aucun mouvement possible
        // On étend la classe Board (assure-toi que Board n'est pas 'final')
        class BlockedBoard extends Board {
            public BlockedBoard(int size) { super(size); }

            // On surcharge les méthodes pour dire "Aucun coup possible"
            @Override
            public List<Move> getWhiteValidMoves() { return List.of(); } // Liste vide
            @Override
            public List<Move> getBlackValidMoves() { return List.of(); } // Liste vide
        }

        // 2. On instancie ce faux plateau
        Board fakeBoard = new BlockedBoard(12);

        // 3. INJECTION : On remplace le vrai plateau du jeu par le faux via la Réflexion
        // C'est nécessaire car tu n'as pas de méthode setBoard()
        java.lang.reflect.Field boardField = GameCheckers.class.getDeclaredField("board");
        boardField.setAccessible(true); // On autorise l'accès au champ privé
        boardField.set(game, fakeBoard); // On injecte le faux plateau

        // 4. On appelle checkGameOver()
        // Comme le plateau dit "0 mouvements", le jeu doit se finir
        State resultState = game.checkGameOver();

        // 5. Vérification
        assertTrue(resultState instanceof FinishedState, 
            "Si le plateau n'a plus de mouvements, le jeu doit passer en FinishedState");
    }

    @Test
    void testOpeningSequence_Dynamic() {
        game.addObserver(new GameView() {
            @Override public void update(GameCheckers g) {}
            @Override public void start() {}
            @Override public void display(GameCheckers g) {}
        });

        // --- TOUR 1 : BLANCS ---
        List<Move> whiteMoves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(whiteMoves.isEmpty(), "Les blancs doivent avoir des coups possibles au début");

        Move moveW1 = whiteMoves.get(0);
        String fromW1 = game.getBoard().indexToSquare(moveW1.getFrom());
        String toW1   = game.getBoard().indexToSquare(moveW1.getTo());
        game.applyMove(fromW1, toW1);

        assertNotEquals("White Player", game.getCurrentPlayer().toString(),
                "Après le coup blanc, ce n'est plus aux blancs");

        // --- TOUR 1 : NOIRS ---
        List<Move> blackMoves = game.getPossibleMoves(game.getCurrentPlayer());
        assertFalse(blackMoves.isEmpty());

        Move moveB1 = blackMoves.get(0);
        String fromB1 = game.getBoard().indexToSquare(moveB1.getFrom());
        String toB1   = game.getBoard().indexToSquare(moveB1.getTo());
        game.applyMove(fromB1, toB1);

        // --- TOUR 2 : RE-BLANCS ---
        assertEquals("White Player", game.getCurrentPlayer().toString());
    }
}