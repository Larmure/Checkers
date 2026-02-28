package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.GameController;
import fr.ubordeaux.pdp.controller.Helpable;

import java.util.Timer;

import fr.ubordeaux.pdp.model.Internationalization;


public class PauseCommand implements Command, Helpable {
    private final Timer blitzTimer;

    public PauseCommand(Timer blitzTimer) {
        this.blitzTimer = blitzTimer;
        Internationalization.init();
    }

    @Override
    public void execute() {
        blitzTimer.cancel();
        System.out.println(Internationalization.get("game.pause"));
    }

    /**
     * Returns the help string for the pause command.
     *
     * @return A brief description of the pause functionality.
     */
    @Override
    public String getHelp() {
        return "pause : Stop the time in blitz.";
    }

}
