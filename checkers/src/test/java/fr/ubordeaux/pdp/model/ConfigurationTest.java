package fr.ubordeaux.pdp.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

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
        Configuration config = new Configuration(true, 60, true, 10, true, false);
        
        assertTrue(config.isBlitz());
        assertEquals(60, config.getTime());
        assertTrue(config.isContest());
        assertEquals(10, config.getSize());
        assertTrue(config.isVerbose());
        assertFalse(config.isDebug());
    }

    @Test
    void testConstructorValidationBlitzAndTime() {
        // If blitz is false but time is not the default value,
        // the class must enforce default values.
        Configuration config = new Configuration(false, 999, false, 8, false, false);
        
        assertEquals(Utils.DEFAULT_BLITZ, config.isBlitz());
        assertEquals(Utils.DEFAULT_TIME, config.getTime());
    }

    @Test
    void testConstructorValidationInvalidSize() {
        // Tests an invalid board size (e.g., 7)
        // It must be replaced by DEFAULT_BOARD_SIZE.
        Configuration config = new Configuration(false, 0, false, 7, false, false);
        
        assertEquals(Utils.DEFAULT_BOARD_SIZE, config.getSize());
    }

    @Test
    void testCopyConstructor() {
        Configuration original = new Configuration(true, 30, true, 8, true, true);
        Configuration copy = new Configuration(original);
        
        assertEquals(original.isBlitz(), copy.isBlitz());
        assertEquals(original.getSize(), copy.getSize());
        assertEquals(original.isDebug(), copy.isDebug());
    }

    @Test
    void testModifiedCopyConstructor() {
        // Tests the constructor that allows changing verbose and debug while copying the rest
        Configuration original = new Configuration(true, 30, true, 8, false, false);
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
            false
        );
        
        // Build the expected string dynamically or with constants
        String expected = "blitz=" + Utils.DEFAULT_BLITZ + 
                          ", time=" + Utils.DEFAULT_TIME + 
                          ", contest=" + Utils.DEFAULT_CONTEST + 
                          ", size=" + Utils.DEFAULT_BOARD_SIZE + 
                          ", verbose=false, debug=false";
                          
        assertEquals(expected, config.toString(), "The toString method must reflect the object's actual state.");
    }   
}