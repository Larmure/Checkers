package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.model.core.GameCheckers;
import fr.ubordeaux.pdp.view.GameView;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the graphical user interface.
 *
 * <p>This class bridges the MVC observer pattern and the JavaFX threading model:
 * <ul>
 *   <li>It extends {@link GameView}, so {@link fr.ubordeaux.pdp.model.core.GameCheckers}
 *       can notify it via {@link #update(GameCheckers)} without knowing any
 *       JavaFX details.</li>
 *   <li>All UI mutations are dispatched to the JavaFX Application Thread via
 *       {@link Platform#runLater}, which is required by JavaFX.</li>
 * </ul>
 *
 * <h2>Startup sequence</h2>
 * <ol>
 *   <li>{@link #start()} is called by {@link fr.ubordeaux.pdp.App} when the
 *       {@code --gui} flag is present.</li>
 *   <li>{@link Platform#startup} bootstraps the JavaFX runtime and runs the
 *       provided {@link Runnable} on the JavaFX Application Thread.</li>
 *   <li>The {@link Stage}, {@link Scene}, and {@link MainView} are created,
 *       sized to 90 % of the primary screen, and shown.</li>
 *   <li>{@link MainView#bindToScene(Scene)} is called so the board scales with
 *       the window.</li>
 *   <li>{@link MainView#passStageToMenu(Stage)} is called so {@link MenuView}
 *       can open modal dialogs.</li>
 * </ol>
 *
 * <h2>Update flow</h2>
 */
public class GraphicalUserInterface extends GameView {

  /** The application's primary window. Created in {@link #start()}. */
  private Stage stage;

  /**
   * Root layout node. Created in {@link #start()} and mutated only on the
   * JavaFX Application Thread.
   */
  private MainView mainView;

  /**
   * Bootstraps the JavaFX runtime, creates the primary window, and shows it.
   *
   * <p>The window is sized to 90 % of the primary screen's visual bounds and
   * centred on screen. Minimum dimensions are clamped to 720 × 540 px.
   *
   * <p>Called from {@link fr.ubordeaux.pdp.App#main} on the main thread.
   * {@link Platform#startup} is used (instead of {@code Application.launch})
   * because the controller is instantiated before this method is called.
   */
  @Override
  public void start() {
    Platform.startup(() -> {
      stage = new Stage();
      mainView = new MainView(controller);

      Rectangle2D screen = Screen.getPrimary().getVisualBounds();
      double initW = screen.getWidth() * 0.90;
      double initH = screen.getHeight() * 0.90;

      Scene scene = new Scene(mainView, initW, initH);

      // Load the CSS stylesheet — src/main/resources/
      scene.getStylesheets().add(
          getClass().getResource("/style.css").toExternalForm()
      );

      stage.setScene(scene);
      stage.setTitle("Checkers — Universite de Bordeaux");
      stage.setMinWidth(720);
      stage.setMinHeight(540);

      // Centre the window on screen.
      stage.setX(screen.getMinX() + (screen.getWidth() - initW) / 2.0);
      stage.setY(screen.getMinY() + (screen.getHeight() - initH) / 2.0);

      // Wire responsive board sizing before showing the window.
      mainView.bindToScene(scene);

      stage.show();

      // Pass the stage to MenuView *after* show() so dialogs have a visible owner.
      mainView.passStageToMenu(stage);
      mainView.passGuiToMenu(this);
 
      // Intercept the window close button (X) — same logic as the Quit menu item.
      stage.setOnCloseRequest(e -> {
        e.consume(); // prevent immediate close
        requestQuit();
      });
    });
  }

  /**
   * Schedules a view refresh on the JavaFX Application Thread.
   *
   * <p>This method is called by
   * {@link fr.ubordeaux.pdp.model.core.GameCheckers#notifyObservers()} on
   * whatever thread triggered the model change (typically the controller
   * thread). {@link Platform#runLater} ensures the actual UI update happens
   * safely on the JavaFX thread.
   *
   * @param game the current game state; must not be {@code null}
   */
  @Override
  public void update(GameCheckers game) {
    Platform.runLater(() -> {
      if (mainView != null) {
        mainView.update(game);
      }
    });
  }

  /**
   * Delegates to {@link #update(GameCheckers)}.
   *
   * <p>Both methods exist because {@link GameView} declares {@code display()}
   * for initial rendering and {@code update()} for incremental changes; in the
   * GUI they are equivalent.
   *
   * @param game the current game state; must not be {@code null}
   */
  @Override
  public void display(GameCheckers game) {
    update(game);
  }

  /**
   * Handles a quit request from the menu or the window close button.
   *
   * <p>If the current game has unsaved changes, an {@link Alert} asks the user
   * to confirm before exiting. If the user confirms (or there are no unsaved
   * changes), the JavaFX platform is shut down cleanly before calling
   * {@code System.exit(0)}.
   *
   * <p>Must be called on the JavaFX Application Thread.
   */
  public void requestQuit() {
    if (controller.getGame() != null && controller.hasUnsavedChanges()) {
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.initOwner(stage);
      confirm.setTitle("Quit");
      confirm.setHeaderText("Current game has unsaved changes.");
      confirm.setContentText("Save before quitting?");
      confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
 
      confirm.showAndWait().ifPresent(response -> {
        if (response == ButtonType.YES) {
          mainView.openSaveDialog(); // delegate to MenuView's save dialog
          doQuit();
        } else if (response == ButtonType.NO) {
          doQuit();
        }
        // CANCEL — do nothing, user stays in the game.
      });
    } else {
      doQuit();
    }
  }
 
  /**
   * Performs the actual shutdown: closes the JavaFX platform cleanly then
   * exits the JVM.
   */
  private void doQuit() {
    Platform.exit();
    System.exit(0);
  }
}