package fr.ubordeaux.pdp.controller.commands;

import fr.ubordeaux.pdp.controller.Command;
import fr.ubordeaux.pdp.controller.Helpable;
import fr.ubordeaux.pdp.model.core.*;
import fr.ubordeaux.pdp.model.tools.*;

public class LoadCommand implements Command, Helpable {

    @Override
    public void execute() {
        System.out.println(Internationalization.get("load.execute"));
    }

    /**
     * Returns the help string for the load command.
     *
     * @return A brief description of the load functionality.
     */
    @Override
    public String getHelp() {
        return Internationalization.get("load.help");
    }

}
