// package fr.ubordeaux.pdp.view.gui.dialogs;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// import fr.ubordeaux.pdp.ConfigManager;
// import java.util.List;
// import javafx.scene.input.KeyCode;
// import javafx.scene.input.KeyCodeCombination;
// import javafx.scene.input.KeyCombination;
// import org.junit.jupiter.api.Test;

// class ShortcutDialogUtilsTest {

//   // Tests for formatAction()

//   @Test
//   void formatAction_singleWord_capitalizesFirstLetter() {
//     assertEquals("Quit", ShortcutDialogUtils.formatAction("quit"));
//   }

//   @Test
//   void formatAction_hyphenatedTwoWords_newGame() {
//     assertEquals("New Game", ShortcutDialogUtils.formatAction("new-game"));
//   }

//   @Test
//   void formatAction_hyphenatedTwoWords_loadGame() {
//     assertEquals("Load Game", ShortcutDialogUtils.formatAction("load-game"));
//   }

//   @Test
//   void formatAction_hyphenatedTwoWords_saveGame() {
//     assertEquals("Save Game", ShortcutDialogUtils.formatAction("save-game"));
//   }

//   @Test
//   void formatAction_singleWord_undo() {
//     assertEquals("Undo", ShortcutDialogUtils.formatAction("undo"));
//   }

//   @Test
//   void formatAction_singleWord_hint() {
//     assertEquals("Hint", ShortcutDialogUtils.formatAction("hint"));
//   }

//   // Tests for isModifierOnly()

//   @Test
//   void isModifierOnly_ctrl_returnsTrue() {
//     assertTrue(ShortcutDialogUtils.isModifierOnly(KeyCode.CONTROL));
//   }

//   @Test
//   void isModifierOnly_shift_returnsTrue() {
//     assertTrue(ShortcutDialogUtils.isModifierOnly(KeyCode.SHIFT));
//   }

//   @Test
//   void isModifierOnly_alt_returnsTrue() {
//     assertTrue(ShortcutDialogUtils.isModifierOnly(KeyCode.ALT));
//   }

//   @Test
//   void isModifierOnly_meta_returnsTrue() {
//     assertTrue(ShortcutDialogUtils.isModifierOnly(KeyCode.META));
//   }

//   @Test
//   void isModifierOnly_regularKeyN_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isModifierOnly(KeyCode.N));
//   }

//   @Test
//   void isModifierOnly_enter_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isModifierOnly(KeyCode.ENTER));
//   }

//   @Test
//   void isModifierOnly_space_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isModifierOnly(KeyCode.SPACE));
//   }

//   // Tests for isReserved()

//   @Test
//   void isReserved_keyA_returnsTrue() {
//     // Ctrl+A is reserved by the system (select all)
//     assertTrue(ShortcutDialogUtils.isReserved(KeyCode.A));
//   }

//   @Test
//   void isReserved_keyC_returnsTrue() {
//     // Ctrl+C is reserved by the system (copy)
//     assertTrue(ShortcutDialogUtils.isReserved(KeyCode.C));
//   }

//   @Test
//   void isReserved_keyZ_returnsTrue() {
//     // Ctrl+Z is reserved by the system (undo)
//     assertTrue(ShortcutDialogUtils.isReserved(KeyCode.Z));
//   }

//   @Test
//   void isReserved_keyN_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isReserved(KeyCode.N));
//   }

//   @Test
//   void isReserved_keyQ_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isReserved(KeyCode.Q));
//   }

//   @Test
//   void isReserved_keyS_returnsFalse() {
//     assertFalse(ShortcutDialogUtils.isReserved(KeyCode.S));
//   }

//   // Tests for isDuplicate()

//   @Test
//   void isDuplicate_combinationUsedByAnotherAction_returnsTrue() {
//     // Ctrl+N is assigned to "new-game" — assigning it to "undo" is a duplicate
//     ConfigManager mockConfig = mock(ConfigManager.class);
//     when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+N");
//     when(mockConfig.getShortcut("undo")).thenReturn("Ctrl+U");
//     ShortcutManager manager = new ShortcutManager(mockConfig);

//     KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
//     List<String> actions = List.of("new-game", "undo");

//     assertTrue(ShortcutDialogUtils.isDuplicate(ctrlN, "undo", actions, manager));
//   }

//   @Test
//   void isDuplicate_sameCombinationForSameAction_returnsFalse() {
//     // Reassigning Ctrl+N to "new-game" itself is not a duplicate
//     ConfigManager mockConfig = mock(ConfigManager.class);
//     when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+N");
//     ShortcutManager manager = new ShortcutManager(mockConfig);

//     KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
//     List<String> actions = List.of("new-game");

//     assertFalse(ShortcutDialogUtils.isDuplicate(ctrlN, "new-game", actions, manager));
//   }

//   @Test
//   void isDuplicate_uniqueCombination_returnsFalse() {
//     // Ctrl+F is not assigned to any action — not a duplicate
//     ConfigManager mockConfig = mock(ConfigManager.class);
//     when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+N");
//     when(mockConfig.getShortcut("undo")).thenReturn("Ctrl+U");
//     ShortcutManager manager = new ShortcutManager(mockConfig);

//     KeyCombination ctrlF = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
//     List<String> actions = List.of("new-game", "undo");

//     assertFalse(ShortcutDialogUtils.isDuplicate(ctrlF, "quit", actions, manager));
//   }

//   @Test
//   void isDuplicate_actionWithNullShortcut_doesNotThrow() {
//     // An action with no shortcut (null) must not cause a NullPointerException
//     ConfigManager mockConfig = mock(ConfigManager.class);
//     when(mockConfig.getShortcut("new-game")).thenReturn(null);
//     ShortcutManager manager = new ShortcutManager(mockConfig);

//     KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
//     List<String> actions = List.of("new-game");

//     assertDoesNotThrow(() -> ShortcutDialogUtils.isDuplicate(ctrlN, "undo", actions, manager));
//   }

//   @Test
//   void isDuplicate_emptyActionList_returnsFalse() {
//     // No actions to check against — can never be a duplicate
//     ConfigManager mockConfig = mock(ConfigManager.class);
//     ShortcutManager manager = new ShortcutManager(mockConfig);

//     KeyCombination ctrlN = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);

//     assertFalse(ShortcutDialogUtils.isDuplicate(ctrlN, "new-game", List.of(), manager));
//   }
// }