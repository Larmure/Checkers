package fr.ubordeaux.pdp.view.gui.dialogs;

import java.lang.reflect.Field;

import fr.ubordeaux.pdp.ConfigManager;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Spinner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import fr.ubordeaux.pdp.model.core.Configuration;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

@ExtendWith(ApplicationExtension.class)
class ConfigDialogTest {

  // On crée un runnable vide pour le callback
  private final Runnable mockRunnable = () -> {
  };

  private final ConfigManager mockConfig = mock(ConfigManager.class);
  private final ShortcutManager mockShortcutManager = new ShortcutManager(mockConfig);

  /**
   * Nécessaire pour initialiser le Toolkit JavaFX en mode "headless" (sans fenêtre visible)
   * Très utile si vous faites tourner les tests sur un serveur d'intégration (CI/CD).
   */
  @BeforeAll
  static void setupSpec() {
    if (Boolean.getBoolean("headless")) {
      System.setProperty("testfx.robot", "glass");
      System.setProperty("testfx.headless", "true");
      System.setProperty("prism.order", "sw");
      System.setProperty("prism.text", "t2k");
    }
    // Force l'initialisation du toolkit
    Platform.startup(() -> {
    });
  }

  @Test
  @DisplayName("Le dialogue s'initialise avec les valeurs par défaut")
  void testDefaultInitialization() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<ConfigDialog> dialogRef = new AtomicReference<>();

    // Les créations de noeuds JavaFX doivent se faire sur le thread JavaFX
    Platform.runLater(() -> {
      dialogRef.set(new ConfigDialog(mockShortcutManager, mockRunnable, null));
      latch.countDown();
    });
    latch.await();

    ConfigDialog dialog = dialogRef.get();
    DialogPane pane = dialog.getDialogPane();

    // Vérifie que les boutons sont présents
    Button startButton = (Button) pane.lookupButton(
        pane.getButtonTypes().stream()
            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
            .findFirst().get());
    assertNotNull(startButton);

    // On peut vérifier l'état interne si on a rendu les champs 'package-private' 
    // ou via la recherche CSS (lookup) de TestFX.
  }

  @Test
  @DisplayName("Cocher Blitz active le spinner de temps")
  void testBlitzTogglesTimeSpinner(FxRobot robot) throws InterruptedException, Exception {
    AtomicReference<ConfigDialog> dialogRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(() -> {
      dialogRef.set(new ConfigDialog(mockShortcutManager, mockRunnable, null));
      dialogRef.get().show();
      latch.countDown();
    });
    latch.await();

    ConfigDialog dialog = dialogRef.get();

    // Récupération des champs via réflexion
    Field blitzCheckField = ConfigDialog.class.getDeclaredField("blitzCheck");
    blitzCheckField.setAccessible(true);
    CheckBox blitzCheck = (CheckBox) blitzCheckField.get(dialog);

    Field timeSpinnerField = ConfigDialog.class.getDeclaredField("timeSpinner");
    timeSpinnerField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Spinner<Integer> timeSpinner = (Spinner<Integer>) timeSpinnerField.get(dialog);

    // 1. On utilise interact() pour forcer le décochage de manière 100% fiable
    robot.interact(() -> blitzCheck.setSelected(false));
    assertTrue(timeSpinner.isDisabled());

    // 2. On utilise interact() pour forcer le cochage
    robot.interact(() -> blitzCheck.setSelected(true));

    // 3. Le spinner doit maintenant être activé !
    assertFalse(timeSpinner.isDisabled());
  }

  @Test
  @DisplayName("L'état des contrôles AI se met à jour correctement")
  void testAiControlsStateUpdate(FxRobot robot) throws Exception {
    AtomicReference<ConfigDialog> dialogRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(() -> {
      dialogRef.set(new ConfigDialog(mockShortcutManager, mockRunnable, null));
      dialogRef.get().show();
      latch.countDown();
    });
    latch.await();

    ConfigDialog dialog = dialogRef.get();

    // Récupération des champs via réflexion
    Field whiteAiCheckField = ConfigDialog.class.getDeclaredField("whiteAiCheck");
    whiteAiCheckField.setAccessible(true);
    CheckBox whiteAiCheck = (CheckBox) whiteAiCheckField.get(dialog);

    Field blackAiCheckField = ConfigDialog.class.getDeclaredField("blackAiCheck");
    blackAiCheckField.setAccessible(true);
    CheckBox blackAiCheck = (CheckBox) blackAiCheckField.get(dialog);

    Field whiteAiModeComboField = ConfigDialog.class.getDeclaredField("whiteAiModeCombo");
    whiteAiModeComboField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ComboBox<String> whiteAiModeCombo = (ComboBox<String>) whiteAiModeComboField.get(dialog);

    Field blackAiModeComboField = ConfigDialog.class.getDeclaredField("blackAiModeCombo");
    blackAiModeComboField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ComboBox<String> blackAiModeCombo = (ComboBox<String>) blackAiModeComboField.get(dialog);

    // 1. On désactive les deux IA
    robot.interact(() -> {
      whiteAiCheck.setSelected(false);
      blackAiCheck.setSelected(false);
    });
    assertTrue(whiteAiModeCombo.isDisabled());
    assertTrue(blackAiModeCombo.isDisabled());

    // 2. On active au moins une IA
    robot.interact(() -> whiteAiCheck.setSelected(true));
    assertFalse(whiteAiModeCombo.isDisabled());
    assertTrue(blackAiModeCombo.isDisabled());
  }

  @Test
  @DisplayName("buildConfiguration lit l'UI et construit correctement l'objet Configuration")
  void testBuildConfiguration(FxRobot robot) throws Exception {
    AtomicReference<ConfigDialog> dialogRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(() -> {
      dialogRef.set(new ConfigDialog(mockShortcutManager, mockRunnable, null));
      latch.countDown();
    });
    latch.await();

    ConfigDialog dialog = dialogRef.get();

    // 1. Récupération directe des composants via réflexion (plus robuste que lookup)
    Field blitzCheckField = ConfigDialog.class.getDeclaredField("blitzCheck");
    blitzCheckField.setAccessible(true);
    CheckBox blitzCheck = (CheckBox) blitzCheckField.get(dialog);

    Field timeSpinnerField = ConfigDialog.class.getDeclaredField("timeSpinner");
    timeSpinnerField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Spinner<Integer> timeSpinner = (Spinner<Integer>) timeSpinnerField.get(dialog);

    Field whiteAiModeComboField = ConfigDialog.class.getDeclaredField("whiteAiModeCombo");
    whiteAiModeComboField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ComboBox<String> whiteAiModeCombo = (ComboBox<String>) whiteAiModeComboField.get(dialog);

    Field blackAiModeComboField = ConfigDialog.class.getDeclaredField("blackAiModeCombo");
    blackAiModeComboField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ComboBox<String> blackAiModeCombo = (ComboBox<String>) blackAiModeComboField.get(dialog);

    Field aiTimeSpinnerField = ConfigDialog.class.getDeclaredField("aiTimeSpinner");
    aiTimeSpinnerField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Spinner<Integer> aiTimeSpinner = (Spinner<Integer>) aiTimeSpinnerField.get(dialog);

    Field minimaxScoringComboField = ConfigDialog.class.getDeclaredField("minimaxScoringCombo");
    minimaxScoringComboField.setAccessible(true);
    @SuppressWarnings("unchecked")
    ComboBox<String> minimaxScoringCombo = (ComboBox<String>) minimaxScoringComboField.get(dialog);

    Field whiteAiCheckField = ConfigDialog.class.getDeclaredField("whiteAiCheck");
    whiteAiCheckField.setAccessible(true);
    CheckBox whiteAiCheck = (CheckBox) whiteAiCheckField.get(dialog);

    Field blackAiCheckField = ConfigDialog.class.getDeclaredField("blackAiCheck");
    blackAiCheckField.setAccessible(true);
    CheckBox blackAiCheck = (CheckBox) blackAiCheckField.get(dialog);

    // 2. Modification des valeurs de l'interface (Simulation des actions de l'utilisateur)
    robot.interact(() -> {
      blitzCheck.setSelected(true);
      timeSpinner.getValueFactory().setValue(42); // 42 minutes
      whiteAiCheck.setSelected(true);
      blackAiCheck.setSelected(true);
      whiteAiModeCombo.setValue("MCTS");
      blackAiModeCombo.setValue("Alpha-Beta");
      minimaxScoringCombo.setValue("Advanced");
      aiTimeSpinner.getValueFactory().setValue(15); // 15 secondes
    });

    // 3. Exécution de buildConfiguration en simulant l'appel du ResultConverter
    Configuration config = null;
    for (ButtonType type : dialog.getDialogPane().getButtonTypes()) {
      if (type.getButtonData() == ButtonData.OK_DONE) {
        config = dialog.getResultConverter().call(type);
        break;
      }
    }

    // 4. Vérifications que les données de l'interface ont été correctement transformées
    assertNotNull(config);
    assertTrue(config.isBlitz());
    assertEquals(42, config.getTime()); // Le spinner de temps a été lu correctement
    assertTrue(config.iswhiteAi());
    assertTrue(config.isblackAi());
    assertEquals("mcts", config.getWhiteAiMode());
    assertEquals("alphabeta", config.getBlackAiMode());
    assertEquals("mcts", config.getAiMode());
    assertEquals("advanced", config.getMinimaxScoring());
    assertEquals(15000, config.getAiTime()); // L'IA time (15) a bien été multiplié par 1000 pour les millisecondes
  }
}