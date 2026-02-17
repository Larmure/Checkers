package fr.ubordeaux.pdp.controller;

public class PauseCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "pause : Stop the time in blitz.";
    }

}
