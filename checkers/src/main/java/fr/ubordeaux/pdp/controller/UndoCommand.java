package fr.ubordeaux.pdp.controller;

public class UndoCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "undo [N] : Cancel the last move played. (or the N-last)";
    }

}
