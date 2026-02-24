package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;

public class HintCommand implements Command, Helpable {

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the help string for the hint command.
     *
     * @return A brief description of the hint functionality.
     */
    @Override
    public String getHelp() {
        return "hint : Display a recommanded move.";
    }

}
