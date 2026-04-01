// package fr.ubordeaux.pdp.view.gui.dialogs;

// import static org.mockito.Mockito.*;
// import static org.testfx.api.FxAssert.verifyThat;
// import static org.testfx.matcher.control.LabeledMatchers.hasText;

// import fr.ubordeaux.pdp.ConfigManager;
// import javafx.stage.Stage;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.testfx.api.FxRobot;
// import org.testfx.framework.junit5.ApplicationExtension;
// import org.testfx.framework.junit5.Start;

// @ExtendWith(ApplicationExtension.class)
// class ShortcutDialogTest {

//   private ConfigManager mockConfig;
//   private ShortcutManager shortcutManager;
//   private ShortcutDialog dialog;

//   @Start
//   void start(Stage stage) {
//     mockConfig = mock(ConfigManager.class);

//     // Set up default shortcuts so the dialog can render its labels.
//     when(mockConfig.getShortcut("new-game")).thenReturn("Ctrl+N");
//     when(mockConfig.getShortcut("load-game")).thenReturn("Ctrl+L");
//     when(mockConfig.getShortcut("save-game")).thenReturn("Ctrl+S");
//     when(mockConfig.getShortcut("configuration")).thenReturn("Ctrl+Comma");
//     when(mockConfig.getShortcut("info")).thenReturn("Ctrl+I");
//     when(mockConfig.getShortcut("quit")).thenReturn("Ctrl+Q");
//     when(mockConfig.getShortcut("undo")).thenReturn("Ctrl+U");
//     when(mockConfig.getShortcut("redo")).thenReturn("Ctrl+R");
//     when(mockConfig.getShortcut("pause")).thenReturn("Ctrl+P");
//     when(mockConfig.getShortcut("hint")).thenReturn("Ctrl+H");

//     shortcutManager = new ShortcutManager(mockConfig);
//     dialog = new ShortcutDialog(shortcutManager);
//     dialog.show();
//   }

//   // Label rendering tests

//   @Test
//   void dialog_showsFormattedActionLabels(FxRobot robot) {
//     // ShortcutDialogUtils.formatAction() is called for each row —
//     // verify that the rendered labels match the expected display text.
//     verifyThat("New Game", hasText("New Game"));
//     verifyThat("Load Game", hasText("Load Game"));
//     verifyThat("Save Game", hasText("Save Game"));
//     verifyThat("Quit", hasText("Quit"));
//     verifyThat("Undo", hasText("Undo"));
//   }

//   @Test
//   void dialog_showsCurrentShortcutForNewGame(FxRobot robot) {
//     // The label next to "New Game" must display the current shortcut text.
//     verifyThat("Ctrl+N", hasText("Ctrl+N"));
//   }

//   @Test
//   void dialog_showsCurrentShortcutForQuit(FxRobot robot) {
//     verifyThat("Ctrl+Q", hasText("Ctrl+Q"));
//   }

//   // Reset button test

//   @Test
//   void resetButton_callsResetToDefaultsOnManager(FxRobot robot) {
//     // Click the "Reset to defaults" button and verify the manager is notified.
//     robot.clickOn("Reset to defaults");

//     verify(mockConfig, atLeastOnce()).setShortcut(eq("new-game"), anyString());
//     verify(mockConfig, atLeastOnce()).setShortcut(eq("quit"), anyString());
//   }

//   // OK button: save() must be called

//   @Test
//   void okButton_callsSaveOnManager(FxRobot robot) {
//     robot.clickOn("OK");

//     verify(mockConfig).saveShortcuts();
//   }

//   // Cancel button: save() must NOT be called

//   @Test
//   void cancelButton_doesNotCallSave(FxRobot robot) {
//     robot.clickOn("Cancel");

//     verify(mockConfig, never()).saveShortcuts();
//   }
// }