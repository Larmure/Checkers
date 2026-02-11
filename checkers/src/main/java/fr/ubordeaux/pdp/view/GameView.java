package fr.ubordeaux.pdp.view;

import fr.ubordeaux.pdp.controller.GameController;

public abstract class GameView implements Observer {

    protected GameController controller;

    public void setController(GameController controller) {
        this.controller = controller;   
    }

    public abstract void start();

    public abstract void display();

}
