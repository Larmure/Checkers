package fr.ubordeaux.pdp.controller;

public class HintCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "hint : Display a recommanded move.";
    }

}
