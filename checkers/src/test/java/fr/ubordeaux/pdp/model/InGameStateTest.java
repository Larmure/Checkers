package fr.ubordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InGameStateTest {

    private GameCheckers game;
    private InGameState state;

    @BeforeEach
    void setUp() {
        // On initialise le jeu et l'état avant chaque test
        game = new GameCheckers();
        state = new InGameState(game);
    }

    /**
     * Teste le constructeur indirectement.
     * Objectif : Vérifier que l'instance est bien créée.
     */
    @Test
    void testInitialization() {
        assertNotNull(state, "L'état InGameState ne doit pas être null après initialisation");
    }

    /**
     * Teste la méthode isGameOver().
     * Objectif : Vérifier que l'état "En Cours" retourne toujours false.
     */
    @Test
    void testIsGameOver() {
        // On s'attend à ce que le jeu ne soit PAS terminé
        assertFalse(state.isGameOver(), "InGameState doit retourner false pour isGameOver()");
    }

    /**
     * Teste la méthode handle().
     * Objectif : Vérifier que la méthode utilise bien l'objet GameCheckers pour afficher le joueur.
     */
    @Test
    void testHandle() {
        // Cette méthode appelle game.getCurrentPlayer().
        // Comme on passe un vrai GameCheckers, cela va fonctionner.
        // On vérifie simplement que l'appel se fait sans erreur.
        
        assertDoesNotThrow(() -> state.handle(), 
            "La méthode handle() doit s'exécuter sans erreur en utilisant l'instance de GameCheckers");
    }
}