package fr.u_bordeaux.pdp;

import org.apache.commons.cli.*;

/**
 * Main class for the Checkers game.
 * Handles command line arguments and initializes the game modes.
 * @author Lalatiana
 * @version 1.0
 */
public class App {
    // Global variables for program status
    private static boolean verbose = false;
    private static boolean debug = false;

    /**
     * Starts the checkers application.
     * Parses command line arguments to set the game mode.
     * @param args command line arguments: -h/--help, -V/--version, -v/--verbose, -d/--debug.
     * Exit codes: 0 Success, 1 Error
     */
    public static void main(String[] args) {
        // Options definition
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        options.addOption("V", "version", false, "display version");
        options.addOption("v", "verbose", false, "increase verbosity");
        options.addOption("d", "debug", false, "display debug messages");

        CommandLineParser parser = new DefaultParser();
        try {
            // Arguments parsing
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("checkers", options);
                System.exit(0);
            }

            if (cmd.hasOption("V")) {
                System.out.println("checkers version 1.0");
                System.exit(0);
            }

            if (cmd.hasOption("v")) {
                verbose = true;
                System.out.println("Verbose mode enabled.");
            }

            if (cmd.hasOption("d")) {
                debug = true;
                System.out.println("Debug mode enabled.");
            }
            // Default behavior if no stopping option is provided
            System.out.println("Welcome to Checkers!");

        } catch (ParseException e) {
            // Error handling
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}