package fr.ubordeaux.pdp.view.gui;

import fr.ubordeaux.pdp.controller.GameController;
import javafx.scene.control.*;

public class MenuView extends MenuBar {
    private final GameController controller;

    public MenuView(GameController controller) {
        this.controller = controller;
        initMenus();
    }

    private void initMenus() {

        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New Game");
        newItem.setOnAction(e -> controller.executeCommand("new", new String[0]));

        MenuItem loadItem = new MenuItem("Load Game");
        loadItem.setOnAction(e -> controller.executeCommand("load", new String[0]));

        MenuItem saveItem = new MenuItem("Save Game");
        saveItem.setOnAction(e -> controller.executeCommand("save", new String[0]));

        MenuItem configItem = new MenuItem("Configuration");

        MenuItem infoItem = new MenuItem("Info");

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> controller.executeCommand("quit", new String[0]));

        fileMenu.getItems().addAll(newItem, loadItem, saveItem, new SeparatorMenuItem(), configItem, infoItem, quitItem);

        Menu gameMenu = new Menu("Game");
        MenuItem undoItem = new MenuItem("Undo");
        undoItem.setOnAction(e -> controller.executeCommand("undo", new String[]{"1"}));

        MenuItem redoItem = new MenuItem("Redo");
        redoItem.setOnAction(e -> controller.executeCommand("redo", new String[]{"1"}));

        MenuItem pauseItem = new MenuItem("Pause");
        pauseItem.setOnAction(e -> controller.executeCommand("pause", new String[0]));

        MenuItem hintItem = new MenuItem("Hint");
        hintItem.setOnAction(e -> controller.executeCommand("hint", new String[0]));

        gameMenu.getItems().addAll(undoItem, redoItem, pauseItem, hintItem);

        this.getMenus().addAll(fileMenu, gameMenu);
    }

}