package fr.ubordeaux.pdp.model.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.player.ai.Ai;
import fr.ubordeaux.pdp.model.player.ai.Mcts;
import fr.ubordeaux.pdp.model.player.ai.SelectionMode;
import fr.ubordeaux.pdp.model.tools.Utils;

class ConfigurationTest {

        @Test
        void testDefaultConfiguration() {
                // Verifies that the default configuration retrieves the correct constants from
                // Utils
                Configuration config = Configuration.getDefaultConfiguration();
                assertNotNull(config);
                assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
                assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
        }

        @Test
        void testFullConstructorAndGetters() {
                // Tests the main constructor with valid values
                Configuration config = new Configuration(true, 60, true, 10, true, false, true, true, 5000,
                                Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);

                assertTrue(config.isBlitz());
                assertEquals(60, config.getTime());
                assertTrue(config.isContest());
                assertEquals(10, config.getSize());
                assertTrue(config.isVerbose());
                assertFalse(config.isDebug());
                assertTrue(config.iswhiteAi());
                assertTrue(config.isblackAi());
                assertEquals(5000, config.getAiTime());
                assertEquals(Utils.DEFAULT_AI_MODE, config.getAiMode());
                assertEquals(Ai.DEFAULT_DEPTH, config.getAiDepth());
        }

        @Test
        void testConstructorValidationBlitzAndTime() {
                // If blitz is false but time is not the default value,
                // the class must enforce default values.
                Configuration config = new Configuration(false, 999, false, 8, false, false, true, true, 100,
                                Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);

                assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
                assertEquals(Utils.DEFAULT_TIME, config.getTime());
        }

        @Test
        void testConstructorValidationInvalidSize() {
                // Tests an invalid board size (e.g., 7)
                // It must be replaced by DEFAULT_BOARD_SIZE.
                Configuration config = new Configuration(false, 0, false, 7, false, false, true, false, 100,
                                Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);

                assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
        }

        @Test
        void testCopyConstructor() {
                Configuration original = new Configuration(true, 30, true, 10, true, true, true, false, 2500,
                                "Minimax", 5, SelectionMode.ML); // Utilise des valeurs non-défaut pour être sûr

                Configuration copy = new Configuration(original);

                assertEquals(original.isBlitz(), copy.isBlitz());
                assertEquals(original.getTime(), copy.getTime());
                assertEquals(original.isContest(), copy.isContest());
                assertEquals(original.getSize(), copy.getSize());
                assertEquals(original.isVerbose(), copy.isVerbose());
                assertEquals(original.isDebug(), copy.isDebug());
                assertEquals(original.iswhiteAi(), copy.iswhiteAi());
                assertEquals(original.isblackAi(), copy.isblackAi());
                assertEquals(original.getAiTime(), copy.getAiTime());
                assertEquals(original.getAiMode(), copy.getAiMode());
                assertEquals(original.getAiDepth(), copy.getAiDepth());
                assertEquals(original.getSelectionMode(), copy.getSelectionMode());
        }

        @Test
        void testModifiedCopyConstructor() {
                Configuration original = new Configuration(true, 30, true, 10, false, false, true, false, 2500,
                                "Minimax", 5, SelectionMode.ML);

                // On copie mais on force verbose et debug à TRUE
                Configuration modified = new Configuration(original, true, true);

                // Vérification des champs modifiés
                assertTrue(modified.isVerbose());
                assertTrue(modified.isDebug());

                // Vérification que le reste n'a pas bougé
                assertEquals(original.isBlitz(), modified.isBlitz());
                assertEquals(original.getTime(), modified.getTime());
                assertEquals(original.getSize(), modified.getSize());
                assertEquals(original.getAiMode(), modified.getAiMode());
                assertEquals(original.getSelectionMode(), modified.getSelectionMode());
        }

        @Test
        void testToString() {
                // We use the exact default values from Utils to avoid
                // the constructor modifying the parameters.
                Configuration config = new Configuration(
                                Utils.DEFAULT_BLITZ,
                                Utils.DEFAULT_TIME,
                                Utils.DEFAULT_CONTEST,
                                Utils.DEFAULT_BOARD_SIZE,
                                false,
                                false,
                                Utils.DEFAULT_WHITE_AI,
                                Utils.DEFAULT_BLACK_AI,
                                Ai.DEFAULT_MAX_TIME_MS,
                                Utils.DEFAULT_AI_MODE,
                                Ai.DEFAULT_DEPTH,
                                Mcts.DEFAULT_SELECTION_MODE);

                // Build the expected string dynamically or with constants
                String expected = "blitz=" + Utils.DEFAULT_BLITZ +
                                ", time=" + Utils.DEFAULT_TIME +
                                ", contest=" + Utils.DEFAULT_CONTEST +
                                ", size=" + Utils.DEFAULT_BOARD_SIZE +
                                ", verbose=false, debug=false, whiteAi=false, blackAi=false"
                                + ", whiteAiMode=" + Utils.DEFAULT_AI_MODE
                                + ", blackAiMode=" + Utils.DEFAULT_AI_MODE
                                + ", aiTime=" + Ai.DEFAULT_MAX_TIME_MS
                                + ", aiMode=" + Utils.DEFAULT_AI_MODE + ", aiDepth=" + Ai.DEFAULT_DEPTH
                                + ", selectionMode="
                                + Mcts.DEFAULT_SELECTION_MODE;

                assertEquals(expected, config.toString(),
                                "The toString method must reflect the object's actual state.");
        }

        @Test
        void testConstructorValidationInvalidAiMode() {
                // Teste un mode IA non reconnu
                Configuration config = new Configuration(false, Utils.DEFAULT_TIME, false, 8,
                                false, false, true, true, 1000,
                                "MODE_INEXISTANT", Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);

                assertEquals(Utils.DEFAULT_AI_MODE, config.getAiMode(),
                                "Un mode IA invalide doit être remplacé par le mode par défaut.");
        }

        @Test
        void testConstructorValidationInvalidAiTime() {
                // Teste un temps IA trop bas (ex: 0)
                Configuration configLow = new Configuration(false, Utils.DEFAULT_TIME, false, 8,
                                false, false, true, true, 0,
                                Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);
                assertEquals(Ai.DEFAULT_MAX_TIME_MS, configLow.getAiTime(),
                                "Un temps trop bas doit être remplacé par le temps par défaut.");

                // Teste un temps IA trop haut
                Configuration configHigh = new Configuration(false, Utils.DEFAULT_TIME, false, 8,
                                false, false, true, true, Ai.MAX_TIME_MS + 1000,
                                Utils.DEFAULT_AI_MODE, Ai.DEFAULT_DEPTH, Mcts.DEFAULT_SELECTION_MODE);
                assertEquals(Ai.DEFAULT_MAX_TIME_MS, configHigh.getAiTime(),
                                "Un temps trop haut doit être remplacé par le temps par défaut.");
        }

        @Test
        void testConstructorValidationInvalidAiDepth() {
                // Teste une profondeur trop basse
                Configuration configLow = new Configuration(false, Utils.DEFAULT_TIME, false, 8,
                                false, false, true, true, 1000,
                                Utils.DEFAULT_AI_MODE, 0, Mcts.DEFAULT_SELECTION_MODE);
                assertEquals(Ai.DEFAULT_DEPTH, configLow.getAiDepth(),
                                "Une profondeur <= 0 doit être remplacée par celle par défaut.");

                // Teste une profondeur trop haute
                Configuration configHigh = new Configuration(false, Utils.DEFAULT_TIME, false, 8,
                                false, false, true, true, 1000,
                                Utils.DEFAULT_AI_MODE, Ai.MAX_SAFE_DEPTH + 1, Mcts.DEFAULT_SELECTION_MODE);
                assertEquals(Ai.DEFAULT_DEPTH, configHigh.getAiDepth(),
                                "Une profondeur dangereuse doit être remplacée par celle par défaut.");
        }
}