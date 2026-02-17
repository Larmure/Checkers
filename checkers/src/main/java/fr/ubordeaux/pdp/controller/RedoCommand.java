package fr.ubordeaux.pdp.controller;

public class RedoCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "redo [N] : Replay the last canceled move. (or the N-last)";
    }

}
