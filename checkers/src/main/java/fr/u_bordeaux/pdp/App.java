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
     * Entry point of the application.
     * Delegates logic to run() and handles exit codes.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int status = run(args);

        // Status handling
        if (status == 1) {
            System.exit(0); // Exit cleanly (Help or Version)
        } else if (status == 2) {
            System.exit(1); // Exit with error (Invalid arguments)
        }
        
        // Status 0 means continue execution
    }

    /**
     * Parses arguments and sets global flags.
     * This method is separated for unit testing purposes to avoid System.exit().
     * @param args command line arguments: -h/--help, -V/--version, -v/--verbose, -d/--debug.
     * @return 0 to continue, 1 to stop (info displayed), 2 for error
     */
    public static int run(String[] args) {
        // Options definition
        Options options = new Options();
        options.addOption("h", "help", false, "display help");
        options.addOption("V", "version", false, "display version");
        options.addOption("v", "verbose", false, "increase verbosity");
        options.addOption("d", "debug", false, "display debug messages");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("checkers", options);
                return 1; // stop cleanly
            }

            if (cmd.hasOption("V")) {
                System.out.println("checkers version 1.0");
                return 1; // stop cleanly
            }

            if (cmd.hasOption("v")) {
                verbose = true;
                System.out.println("Verbose mode enabled.");
            }

            if (cmd.hasOption("d")) {
                debug = true;
                System.out.println("Debug mode enabled.");
            }

            System.out.println("Welcome to Checkers!");
            return 0; // continue

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            return 2; // error
        }
    }

    //Getters
    public static boolean isVerbose() {
        return verbose;
    }

    public static boolean isDebug() {
        return debug;
    }

    /**
     * Resets the global state. 
     * Essential for isolated unit tests.
     */
    public static void reset() {
        verbose = false;
        debug = false;
    }
}