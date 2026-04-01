// package fr.ubordeaux.pdp.view.gui.dialogs;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
// import static org.testfx.api.FxAssert.verifyThat;
// import static org.testfx.matcher.control.LabeledMatchers.hasText;

// import fr.ubordeaux.pdp.ConfigManager;
// import fr.ubordeaux.pdp.model.core.Configuration;
// import fr.ubordeaux.pdp.model.tools.Internationalization;
// import java.util.Optional;
// import java.util.Locale;
// import java.util.concurrent.atomic.AtomicReference;
// import javafx.application.Platform;
// import javafx.stage.Stage;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.testfx.api.FxRobot;
// import org.testfx.framework.junit5.ApplicationExtension;
// import org.testfx.framework.junit5.Start;
// import org.testfx.util.WaitForAsyncUtils;

// @ExtendWith(ApplicationExtension.class)
// class ConfigDialogTest {

//   private ConfigManager mockConfig;
//   private ShortcutManager shortcutManager;
//   private ConfigDialog dialog;

//   @Start
//   void start(Stage stage) {
//     Locale.setDefault(Locale.ENGLISH);
//     Internationalization.init();
//     mockConfig = mock(ConfigManager.class);

//     // Shortcuts needed by ShortcutManager inside ConfigDialog.
//     when(mockConfig.getShortcut(anyString())).thenReturn(null);

//     shortcutManager = new ShortcutManager(mockConfig);

//     // onShortcutsChanged callback — just a no-op for tests.
//     dialog = new ConfigDialog(shortcutManager, () -> {
//     });
//     dialog.show();
//   }

//   // Default values

//   @Test
//   void dialog_showsSectionLabels(FxRobot robot) {
//     // All four section titles must be visible.
//     verifyThat("Board", hasText("Board"));
//     verifyThat("Blitz", hasText("Blitz"));
//     verifyThat("Players", hasText("Players"));
//     verifyThat("Advanced", hasText("Advanced"));
//   }

//   @Test
//   void dialog_showsBoardSizeLabel(FxRobot robot) {
//     verifyThat("Board size:", hasText("Board size:"));
//   }

//   @Test
//   void dialog_showsStartGameButton(FxRobot robot) {
//     verifyThat(Internationalization.get("dialog.start.game"), hasText(Internationalization.get("dialog.start.game")));
//   }

//   @Test
//   void dialog_showsCancelButton(FxRobot robot) {
//     verifyThat(Internationalization.get("dialog.cancel"), hasText(Internationalization.get("dialog.cancel")));
//   }

//   @Test
//   void dialog_showsKeyboardShortcutsButton(FxRobot robot) {
//     verifyThat(Internationalization.get("dialog.keyboard.shortcuts"), hasText(Internationalization.get("dialog.keyboard.shortcuts")));
//   }

//   @Test
//   void dialog_blitzCheckbox_isUncheckedByDefault(FxRobot robot) {
//     // Default configuration has blitz disabled.
//     assertFalse(robot.lookup("Blitz mode").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   @Test
//   void dialog_whiteAiCheckbox_isUncheckedByDefault(FxRobot robot) {
//     assertFalse(robot.lookup("White player (AI)").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   @Test
//   void dialog_blackAiCheckbox_isUncheckedByDefault(FxRobot robot) {
//     assertFalse(robot.lookup("Black player (AI)").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   @Test
//   void dialog_contestCheckbox_isUncheckedByDefault(FxRobot robot) {
//     assertFalse(robot.lookup("Contest mode").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   @Test
//   void dialog_verboseCheckbox_isUncheckedByDefault(FxRobot robot) {
//     assertFalse(robot.lookup("Verbose").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   @Test
//   void dialog_debugCheckbox_isUncheckedByDefault(FxRobot robot) {
//     assertFalse(robot.lookup("Debug").queryAs(
//         javafx.scene.control.CheckBox.class).isSelected());
//   }

//   // Blitz checkbox enables/disables spinner

//   @Test
//   void blitzCheckbox_whenChecked_enablesTimeSpinner(FxRobot robot) {
//     // The spinner is disabled by default — checking blitz must enable it.
//     javafx.scene.control.Spinner<?> spinner = robot.lookup(".spinner")
//         .queryAs(javafx.scene.control.Spinner.class);
//     assertTrue(spinner.isDisabled(), "Spinner should be disabled before checking blitz");

//     robot.clickOn("Blitz mode");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertFalse(spinner.isDisabled(), "Spinner should be enabled after checking blitz");
//   }

//   @Test
//   void blitzCheckbox_whenUnchecked_disablesTimeSpinner(FxRobot robot) {
//     // Check then uncheck — spinner must go back to disabled.
//     robot.clickOn("Blitz mode");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Blitz mode");
//     WaitForAsyncUtils.waitForFxEvents();

//     javafx.scene.control.Spinner<?> spinner = robot.lookup(".spinner")
//         .queryAs(javafx.scene.control.Spinner.class);
//     assertTrue(spinner.isDisabled());
//   }

//   // --- Cancel: result converter returns null (covers lambda$new$0) ---

//   @Test
//   void cancelButton_dialogResultIsEmpty(FxRobot robot) {
//     // Capture the Optional result before clicking Cancel.
//     AtomicReference<Optional<Configuration>> optResult = new AtomicReference<>();
//     Platform.runLater(() -> optResult.set(Optional.ofNullable(dialog.getResult())));

//     robot.clickOn("Cancel");
//     WaitForAsyncUtils.waitForFxEvents();

//     // After cancel the dialog result must be null (Optional.empty()).
//     assertNull(dialog.getResult());
//   }

//   // --- Start Game: result converter returns a Configuration
//   //     (covers lambda$new$0 and buildConfiguration()) ---

//   @Test
//   void startGameButton_returnsNonNullConfiguration(FxRobot robot) {
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertNotNull(dialog.getResult());
//   }

//   @Test
//   void startGameButton_defaultSize_isEight(FxRobot robot) {
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     Configuration cfg = dialog.getResult();
//     assertNotNull(cfg);
//     assertEquals(8, cfg.getSize());
//   }

//   @Test
//   void startGameButton_defaultBlitz_isFalse(FxRobot robot) {
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertFalse(dialog.getResult().isBlitz());
//   }

//   @Test
//   void startGameButton_afterCheckingBlitz_blitzIsTrue(FxRobot robot) {
//     // Check blitz then confirm — the returned Configuration must have blitz=true.
//     robot.clickOn("Blitz mode");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().isBlitz());
//   }

//   @Test
//   void startGameButton_afterCheckingWhiteAi_whiteAiIsTrue(FxRobot robot) {
//     robot.clickOn("White player (AI)");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().iswhiteAi());
//   }

//   @Test
//   void startGameButton_afterCheckingBlackAi_blackAiIsTrue(FxRobot robot) {
//     robot.clickOn("Black player (AI)");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().isblackAi());
//   }

//   @Test
//   void startGameButton_afterCheckingContest_contestIsTrue(FxRobot robot) {
//     robot.clickOn("Contest mode");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().isContest());
//   }

//   @Test
//   void startGameButton_afterCheckingDebug_debugIsTrue(FxRobot robot) {
//     robot.clickOn("Debug");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().isDebug());
//   }

//   @Test
//   void startGameButton_afterCheckingVerbose_verboseIsTrue(FxRobot robot) {
//     robot.clickOn("Verbose");
//     WaitForAsyncUtils.waitForFxEvents();
//     robot.clickOn("Start Game");
//     WaitForAsyncUtils.waitForFxEvents();

//     assertTrue(dialog.getResult().isVerbose());
//   }
// }