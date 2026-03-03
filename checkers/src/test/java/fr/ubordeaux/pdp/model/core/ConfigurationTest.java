package fr.ubordeaux.pdp.model.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import fr.ubordeaux.pdp.model.tools.Utils;

class ConfigurationTest {

    @Test
    void testDefaultConfiguration() {
        // Verifies that the default configuration retrieves the correct constants from Utils
        Configuration config = Configuration.getDefaultConfiguration();
        assertNotNull(config);
        assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
        assertEquals(Utils.DEFAULT_VERBOSE, config.isVerbose());
    }

    @Test
    void testFullConstructorAndGetters() {
        // Tests the main constructor with valid values
        Configuration config = new Configuration(true, 60, true, 10, true, false, true, true);
        
        assertTrue(config.isBlitz());
        assertEquals(60, config.getTime());
        assertTrue(config.isContest());
        assertEquals(10, config.getSize());
        assertTrue(config.isVerbose());
        assertFalse(config.isDebug());
        assertTrue(config.isWhiteIsAI());
        assertTrue(config.isBlackIsAI());
    }

    @Test
    void testConstructorValidationBlitzAndTime() {
        // If blitz is false but time is not the default value,
        // the class must enforce default values.
        Configuration config = new Configuration(false, 999, false, 8, false, false, true, true);
        
        assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
        assertEquals(Utils.DEFAULT_TIME, config.getTime());
    }

    @Test
    void testConstructorValidationInvalidSize() {
        // Tests an invalid board size (e.g., 7)
        // It must be replaced by DEFAULT_BOARD_SIZE.
        Configuration config = new Configuration(false, 0, false, 7, false, false, true, false);
        
        assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    }

    @Test
    void testCopyConstructor() {
        Configuration original = new Configuration(true, 30, true, 8, true, true, false, false);
        Configuration copy = new Configuration(original);
        
        assertEquals(original.isBlitz(), copy.isBlitz());
        assertEquals(original.getSize(), copy.getSize());
        assertEquals(original.isDebug(), copy.isDebug());
    }

    @Test
    void testModifiedCopyConstructor() {
        // Tests the constructor that allows changing verbose and debug while copying the rest
        Configuration original = new Configuration(true, 30, true, 8, false, false, false ,false);
        Configuration modified = new Configuration(original, true, true);
        
        assertEquals(original.isBlitz(), modified.isBlitz());
        assertTrue(modified.isVerbose());
        assertTrue(modified.isDebug());
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
            Utils.DEFAULT_BLACK_AI
        );
        
        // Build the expected string dynamically or with constants
        String expected = "blitz=" + Utils.DEFAULT_BLITZ + 
                          ", time=" + Utils.DEFAULT_TIME + 
                          ", contest=" + Utils.DEFAULT_CONTEST + 
                          ", size=" + Utils.DEFAULT_BOARD_SIZE + 
                          ", verbose=false, debug=false, whiteAI=false, blackAI=false";
                          
        assertEquals(expected, config.toString(), "The toString method must reflect the object's actual state.");
    }   
}