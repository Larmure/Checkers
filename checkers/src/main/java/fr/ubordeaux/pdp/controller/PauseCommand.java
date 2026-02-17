package fr.ubordeaux.pdp.controller;

public class PauseCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
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
