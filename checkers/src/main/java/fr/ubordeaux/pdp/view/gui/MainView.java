package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.controller.GameController;
import javafx.scene.layout.BorderPane;

public class MainView extends BorderPane {
    public MainView(GameController controller) {

        this.setTop(new MenuView(controller));
        
    }
}