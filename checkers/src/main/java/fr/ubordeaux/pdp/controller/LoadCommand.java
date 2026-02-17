package fr.ubordeaux.pdp.controller;

public class LoadCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the help string for the load command.
     *
     * @return A brief description of the load functionality.
     */
    @Override
    public String getHelp() {
        return "load FILE : Load a game from a file.";
    }

}
