package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.view.GameView;
import fr.ubordeaux.pdp.model.core.GameCheckers;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GraphicalUserInterface extends GameView {
    private Stage stage;

    @Override
    public void start() {

        Platform.startup(() -> {
            this.stage = new Stage();
            MainView mainView = new MainView(controller);
            Scene scene = new Scene(mainView, 1000, 700);
            
            stage.setScene(scene);
            stage.setTitle("Checkers");
            stage.show();
        });
    }

    @Override
    public void display(GameCheckers game) {

    }

    @Override
    public void update(GameCheckers game) {
        Platform.runLater(() -> display(game));
    }
}