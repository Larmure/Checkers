package fr.ubordeaux.pdp.controller;

public class QuitCommand implements Command {

    @Override
    public void execute() {
        System.out.println("Exiting the game.");
        System.exit(0);
    }

}
