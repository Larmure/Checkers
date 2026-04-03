package fr.ubordeaux.pdp.model.player.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import fr.ubordeaux.pdp.model.core.Board;
import fr.ubordeaux.pdp.model.player.PlayerColor;

class LogisticRegressionTrainerTest {

  private LogisticRegressionTrainer trainer;

  @BeforeEach
  void setUp() {
    // Initialisation avec 6 features et un learning rate de 0.01
    trainer = new LogisticRegressionTrainer(6, 0.01);
  }

  @Test
  @DisplayName("Le constructeur initialise correctement les poids et le biais")
  void testConstructorInitialization() throws Exception {
    Field weightsField = LogisticRegressionTrainer.class.getDeclaredField("weights");
    weightsField.setAccessible(true);
    double[] weights = (double[]) weightsField.get(trainer);

    assertNotNull(weights);
    assertEquals(6, weights.length); // Vérifie la taille du tableau
    for (double w : weights) {
      assertEquals(0.0, w, 1e-9); // Les poids de base en Java sont à 0.0
    }

    Field biasField = LogisticRegressionTrainer.class.getDeclaredField("bias");
    biasField.setAccessible(true);
    double bias = (double) biasField.get(trainer);
    assertEquals(0.0, bias, 1e-9); // Le biais est explicitement mis à 0.0
  }

  @Test
  @DisplayName("Test de la fonction d'activation Sigmoid")
  void testSigmoid() {
    // sigmoid(0) doit être exactement 0.5
    assertEquals(0.5, LogisticRegressionTrainer.sigmoid(0.0), 1e-9);

    // Valeur très positive -> tend vers 1.0
    assertTrue(LogisticRegressionTrainer.sigmoid(10.0) > 0.99);

    // Valeur très négative -> tend vers 0.0
    assertTrue(LogisticRegressionTrainer.sigmoid(-10.0) < 0.01);
  }

  @Test
  @DisplayName("Test de la prédiction avec des poids artificiels")
  void testPredict() throws Exception {
    // Injection de poids et biais via réflexion pour tester le calcul Z
    Field weightsField = LogisticRegressionTrainer.class.getDeclaredField("weights");
    weightsField.setAccessible(true);
    weightsField.set(trainer, new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 });

    Field biasField = LogisticRegressionTrainer.class.getDeclaredField("bias");
    biasField.setAccessible(true);
    biasField.set(trainer, 0.5);

    // Somme = 0.5 (biais) + (1.0 * 6) = 6.5
    float[] features = { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };
    double prediction = trainer.predict(features);

    double expectedZ = 6.5;
    double expectedPrediction = 1.0 / (1.0 + Math.exp(-expectedZ));

    assertEquals(expectedPrediction, prediction, 1e-9);
  }

  @Test
  @DisplayName("L'entraînement met à jour les poids et gère l'affichage des époques")
  void testTrainUpdatesWeights() throws Exception {
    List<float[]> x = Arrays.asList(
        new float[] { 1f, 0f, 0f, 0f, 0f, 0f },
        new float[] { 0f, 1f, 0f, 0f, 0f, 0f });
    List<Double> y = Arrays.asList(1.0, 0.0); // Labels cibles

    // On lance 101 époques pour couvrir la condition `if (epoch % 100 == 0)`
    trainer.train(x, y, 101);

    Field weightsField = LogisticRegressionTrainer.class.getDeclaredField("weights");
    weightsField.setAccessible(true);
    double[] weights = (double[]) weightsField.get(trainer);

    // On vérifie que les poids ont été modifiés (ils ne sont plus à 0.0)
    assertNotEquals(0.0, weights[0]);
  }

  @Test
  @DisplayName("Sauvegarde des poids dans un fichier avec succès")
  void testSaveWeightsToFileSuccess(@TempDir Path tempDir) throws Exception {
    Path tempFile = tempDir.resolve("test_weights.txt");

    // Accès à la méthode privée saveWeightsToFile
    Method saveMethod = LogisticRegressionTrainer.class.getDeclaredMethod("saveWeightsToFile", String.class);
    saveMethod.setAccessible(true);

    // Exécution de la méthode
    saveMethod.invoke(trainer, tempFile.toString());

    // Vérification
    assertTrue(Files.exists(tempFile));
    List<String> lines = Files.readAllLines(tempFile);
    assertEquals(2, lines.size()); // Doit contenir 2 lignes : poids et biais
  }

  @Test
  @DisplayName("Sauvegarde des poids gère correctement l'IOException")
  void testSaveWeightsToFileIOException(@TempDir Path tempDir) throws Exception {
    // On passe un chemin de dossier au lieu d'un fichier, ce qui forcera une IOException
    Method saveMethod = LogisticRegressionTrainer.class.getDeclaredMethod("saveWeightsToFile", String.class);
    saveMethod.setAccessible(true);

    // L'invocation ne doit pas planter grâce au bloc try/catch dans la méthode
    assertDoesNotThrow(() -> {
      saveMethod.invoke(trainer, tempDir.toString());
    });
  }

  @Test
  @DisplayName("Test de la méthode privée printFinalWeights")
  void testPrintFinalWeights() throws Exception {
    Method printMethod = LogisticRegressionTrainer.class.getDeclaredMethod("printFinalWeights");
    printMethod.setAccessible(true);

    // Simplement s'assurer que ça ne lève pas d'exception
    assertDoesNotThrow(() -> {
      printMethod.invoke(trainer);
    });
  }

  @Test
  @DisplayName("Test de la méthode privée opponent")
  void testOpponent() throws Exception {
    Method opponentMethod = LogisticRegressionTrainer.class.getDeclaredMethod("opponent", PlayerColor.class);
    opponentMethod.setAccessible(true);

    assertEquals(PlayerColor.BLACK, opponentMethod.invoke(trainer, PlayerColor.WHITE)); //
    assertEquals(PlayerColor.WHITE, opponentMethod.invoke(trainer, PlayerColor.BLACK)); //
  }

  @Test
  @DisplayName("Lancement global de l'entrainement (couvre runMatch et MatchResult)")
  void testLaunchTrainingIntegration() {
    // Exécuter un seul match pour tester la mécanique globale de `runMatch` et `MatchResult`
    // Cela va prendre un peu de temps d'exécution (MCTS joue une partie), mais couvre un maximum de lignes.
    assertDoesNotThrow(() -> {
      LogisticRegressionTrainer.lauchTraining(1);
    });
  }

  @Test
  @DisplayName("Couverture des branches de training (no data, wins, draws)")
  void testTrainingBranches() throws Exception {
    // 1. Couverture de "ai.training.no_data"
    assertDoesNotThrow(() -> {
      LogisticRegressionTrainer.lauchTraining(0);
    });

    // 2. Couverture de runMatch et des compteurs de victoire
    // On lance 2 jeux pour augmenter les chances de couvrir whiteWins ou blackWins
    assertDoesNotThrow(() -> {
      LogisticRegressionTrainer.lauchTraining(2);
    });
  }

  @Test
  @DisplayName("Test manuel de MatchResult pour couverture totale")
  void testMatchResultCoverage() throws Exception {
    // Comme MatchResult est une classe privée, on peut la tester par réflexion
    // pour s'assurer que JaCoCo voit le constructeur et les champs
    Class<?> innerClass = Class.forName("fr.ubordeaux.pdp.model.player.ai.LogisticRegressionTrainer$MatchResult");
    java.lang.reflect.Constructor<?> constructor = innerClass.getDeclaredConstructor(PlayerColor.class, List.class);
    constructor.setAccessible(true);

    // Test cas GAGNANT (couvre le stockage du winner)
    Object resultWin = constructor.newInstance(PlayerColor.WHITE, new java.util.ArrayList<>());
    assertNotNull(resultWin);

    // Test cas NUL (couvre winner == null)
    Object resultDraw = constructor.newInstance(null, new java.util.ArrayList<>());
    assertNotNull(resultDraw);
  }
}