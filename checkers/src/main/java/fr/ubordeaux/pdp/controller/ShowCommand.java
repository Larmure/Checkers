package fr.ubordeaux.pdp.controller;

public class ShowCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHelp() {
        return "show board|history|time : Display the board, moves history or the remaining time of each player.";
    }

}
