package fr.ubordeaux.pdp.view.gui.dialogs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fr.ubordeaux.pdp.ConfigManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShortcutManagerTest {

  // Fake ConfigManager created by Mockito
  private ConfigManager mockConfig;

  // The class under test
  private ShortcutManager shortcutManager;

  @BeforeEach
  void setUp() {
    // Before each test: create a fresh fake ConfigManager and ShortcutManager
    mockConfig = mock(ConfigManager.class);
    shortcutManager = new ShortcutManager(mockConfig);
  }

  @Test
  void get_CtrlN_returnsCorrectKeyCode() {
    when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+N");

    KeyCombination kc = shortcutManager.get("new-game");

    assertNotNull(kc);
    assertTrue(kc instanceof KeyCodeCombination);
    KeyCodeCombination kcc = (KeyCodeCombination) kc;
    assertEquals(KeyCode.N, kcc.getCode());
    assertEquals(KeyCombination.ModifierValue.DOWN, kcc.getControl());
  }

  @Test
  void get_nullValue_returnsNull() {
    // parse() must return null when the config returns null
    when(mockConfig.getShortcut("new-game")).thenReturn(null);

    assertNull(shortcutManager.get("new-game"));
  }

  @Test
  void get_invalidString_returnsNull() {
    // "blabla" cannot be parsed into a KeyCode — parse() catches the exception and returns null
    when(mockConfig.getShortcut("new-game")).thenReturn("blabla");

    assertNull(shortcutManager.get("new-game"));
  }

  @Test
  void get_modifierOnlyNoKeyCode_returnsNull() {
    // "Ctrl+" has no key code after the modifier — code stays null, parse() returns null (line 102)
    when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+");

    assertNull(shortcutManager.get("new-game"));
  }

  @Test
  void get_CtrlShiftS_returnsBothModifiers() {
    // Covers the Shift branch inside parse()
    when(mockConfig.getShortcut("save-game")).thenReturn("Ctrl+Shift+S");

    KeyCombination kc = shortcutManager.get("save-game");

    assertNotNull(kc);
    KeyCodeCombination kcc = (KeyCodeCombination) kc;
    assertEquals(KeyCode.S, kcc.getCode());
    assertEquals(KeyCombination.ModifierValue.DOWN, kcc.getControl());
    assertEquals(KeyCombination.ModifierValue.DOWN, kcc.getShift());
  }

  @Test
  void get_AltF_returnsAltModifier() {
    // Covers the Alt branch inside parse() and mods.add(ALT_DOWN)
    when(mockConfig.getShortcut("hint")).thenReturn("Alt+F");

    KeyCombination kc = shortcutManager.get("hint");

    assertNotNull(kc);
    KeyCodeCombination kcc = (KeyCodeCombination) kc;
    assertEquals(KeyCode.F, kcc.getCode());
    assertEquals(KeyCombination.ModifierValue.DOWN, kcc.getAlt());
  }

  @Test
  void set_CtrlN_serializesAsCtrlPlusN() {
    // Covers the Ctrl branch inside serialize()
    KeyCombination kc = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);

    shortcutManager.set("new-game", kc);

    verify(mockConfig).setShortcut("new-game", "Ctrl+N");
  }

  @Test
  void set_ShiftS_serializesAsShiftPlusS() {
    // Covers the Shift branch inside serialize()
    KeyCombination kc = new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN);

    shortcutManager.set("save-game", kc);

    verify(mockConfig).setShortcut("save-game", "Shift+S");
  }

  @Test
  void set_AltF_serializesAsAltPlusF() {
    // Covers the Alt branch inside serialize()
    KeyCombination kc = new KeyCodeCombination(KeyCode.F, KeyCombination.ALT_DOWN);

    shortcutManager.set("hint", kc);

    verify(mockConfig).setShortcut("hint", "Alt+F");
  }

  @Test
  void save_callsSaveShortcutsOnConfigManager() {
    shortcutManager.save();

    verify(mockConfig).saveShortcuts();
  }

  @Test
  void resetToDefaults_setsCtrlNForNewGame() {
    shortcutManager.resetToDefaults();

    verify(mockConfig).setShortcut("new-game", "Ctrl+N");
  }

  @Test
  void resetToDefaults_setsAllTenShortcuts() {
    // Verifies that all 10 default shortcuts are defined
    shortcutManager.resetToDefaults();

    verify(mockConfig).setShortcut("new-game", "Ctrl+N");
    verify(mockConfig).setShortcut("load-game", "Ctrl+L");
    verify(mockConfig).setShortcut("save-game", "Ctrl+S");
    verify(mockConfig).setShortcut("configuration", "Ctrl+Comma");
    verify(mockConfig).setShortcut("info", "Ctrl+I");
    verify(mockConfig).setShortcut("quit", "Ctrl+Q");
    verify(mockConfig).setShortcut("undo", "Ctrl+U");
    verify(mockConfig).setShortcut("redo", "Ctrl+R");
    verify(mockConfig).setShortcut("pause", "Ctrl+P");
    verify(mockConfig).setShortcut("hint", "Ctrl+H");
  }

  @Test
  void setAndGet_roundTrip_returnsSameCombination() {
    // set() serializes the combination; get() parses it back — the display text must match
    when(mockConfig.getShortcut("undo")).thenReturn("Ctrl+U");

    KeyCombination original = new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN);
    shortcutManager.set("undo", original);

    KeyCombination retrieved = shortcutManager.get("undo");

    assertNotNull(retrieved);
    assertEquals(original.getDisplayText(), retrieved.getDisplayText());
  }
}