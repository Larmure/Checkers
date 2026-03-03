package fr.ubordeaux.pdp.model.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerTest {

    // --- Classe concrète interne pour tester la classe abstraite ---
    // On crée cette classe juste pour pouvoir instancier Player
    static class ConcretePlayer extends Player {
        public ConcretePlayer(String name) {
            super(name);
        }
        // Pas besoin de surcharger d'autres méthodes pour ces tests
    }
    // -------------------------------------------------------------

    private Player player;

    @BeforeEach
    void setUp() {
        // Initialisation avant chaque test avec notre classe concrète
        player = new ConcretePlayer("Test Name");
    }

    /**
     * Teste le Constructeur et getName().
     * Objectif : Vérifier que le nom passé au constructeur est bien stocké.
     */
    @Test
    void testConstructorAndGetName() {
        // Le nom a été défini dans le setUp()
        assertNotNull(player.getName(), "Le nom ne doit pas être null");
        assertEquals("Test Name", player.getName(), "Le constructeur doit assigner le bon nom");
    }

    /**
     * Teste setName().
     * Objectif : Vérifier qu'on peut changer le nom après l'initialisation.
     */
    @Test
    void testSetName() {
        String newName = "Nouveau Nom";
        player.setName(newName);
        
        assertEquals(newName, player.getName(), "Le setter doit mettre à jour le nom");
    }

    /**
     * Teste getPlayTime() et setPlayTime().
     * Objectif : Vérifier les accesseurs du temps de jeu (utile pour le mode Blitz).
     */
    @Test
    void testPlayTimeAccessors() {
        // 1. Vérification de la valeur par défaut (int est initialisé à 0)
        assertEquals(0, player.getPlayTime(), "Le temps de jeu initial doit être 0");

        // 2. Modification de la valeur
        int newTime = 300; // exemple : 300 secondes
        player.setPlayTime(newTime);

        // 3. Vérification de la mise à jour
        assertEquals(newTime, player.getPlayTime(), "Le setter doit mettre à jour le temps de jeu");
    }

    /**
     * Teste toString().
     * Objectif : Vérifier que toString retourne bien le nom du joueur.
     */
    @Test
    void testToString() {
        // Selon le code fourni : return this.name;
        String expected = "Test Name";
        assertEquals(expected, player.toString(), "La méthode toString() doit retourner le nom du joueur");
    }
}