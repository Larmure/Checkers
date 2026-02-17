package fr.ubordeaux.pdp.controller;

public class SaveCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "save FILE : Save the current game.";
    }

}
