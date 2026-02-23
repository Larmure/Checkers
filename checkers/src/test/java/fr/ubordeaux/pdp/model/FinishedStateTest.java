package fr.ubordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class FinishedStateTest {

    /**
     * Teste la méthode isGameOver().
     * Objectif : Vérifier que l'état "Fini" retourne toujours true.
     */
    @Test
    void testIsGameOver() {
        FinishedState state = new FinishedState();
        
        // On s'attend à ce que le jeu soit considéré comme terminé
        assertTrue(state.isGameOver(), "FinishedState doit retourner true pour isGameOver()");
    }

    /**
     * Teste la méthode handle().
     * Objectif : Vérifier que la méthode s'exécute sans erreur.
     * Note : Comme elle fait juste un System.out.println, on vérifie juste qu'elle ne plante pas.
     */
    @Test
    void testHandle() {
        FinishedState state = new FinishedState();
        
        // On exécute la méthode. Si elle lance une exception, le test échouera.
        assertDoesNotThrow(() -> state.handle(), "La méthode handle() ne doit pas lever d'exception");
    }
}