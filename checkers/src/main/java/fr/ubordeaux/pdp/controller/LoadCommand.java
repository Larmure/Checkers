package fr.ubordeaux.pdp.controller;

public class LoadCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "load FILE : Load a game from a file.";
    }

}
