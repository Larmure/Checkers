package fr.ubordeaux.pdp.model.tools;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * A custom JLine completer that implements Bash-style tab completion behavior.
 *
 * <p>The completion logic follows these rules:
 * <ul>
 * <li><b>No match:</b> Triggers a visual/audible alert.</li>
 * <li><b>Unique match:</b> Automatically completes the word.</li>
 * <li><b>Ambiguous match (1st Tab):</b> Triggers an alert and remembers the prefix.</li>
 * <li><b>Ambiguous match (2nd Tab):</b> Displays the list of matching candidates.</li>
 * </ul>
 */
public class BashStyleCompleter implements Completer {

  /** A sentinel value to indicate no match was found. */
  private static final String NO_MATCH_SENTINEL = "\0";
  /** The collection of valid command strings for completion. */
  private final Collection<String> commands;
  /** The last ambiguous word that triggered a completion alert. */
  private String lastAmbiguousWord = null;

  /**
   * Constructs a new completer with a predefined set of available commands.
   *
   * @param commands the collection of valid command strings.
   */
  public BashStyleCompleter(Collection<String> commands) {
    this.commands = commands;
  }

  /**
   * Processes the completion request based on the current user input.
   *
   * @param reader the current {@link LineReader} instance.
   * @param line the parsed input line containing the word to complete.
   * @param candidates the list to be populated with matching {@link Candidate} objects.
   */
  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String word = line.word();

    List<String> matches = commands.stream()
        .filter(cmd -> cmd.startsWith(word))
        .sorted()
        .collect(Collectors.toList());

    if (matches.isEmpty()) {
      handleNoMatch(reader);
      return;
    }

    if (matches.size() == 1) {
      candidates.add(new Candidate(matches.get(0)));
      lastAmbiguousWord = null;
      return;
    }

    handleAmbiguousMatch(word, matches, candidates, reader);
  }

  /**
   * Handles cases where no commands match the current prefix.
   *
   * @param reader the active line reader.
   */
  private void handleNoMatch(LineReader reader) {
    if (NO_MATCH_SENTINEL.equals(lastAmbiguousWord)) {
      lastAmbiguousWord = null;
    } else {
      notifyUser(reader);
      lastAmbiguousWord = NO_MATCH_SENTINEL;
    }
  }

  /**
   * Manages the "Double-Tab" logic for multiple possible matches.
   *
   * @param word the current input prefix that triggered the completion.
   * @param matches the list of matching command strings.
   * @param candidates the list to be populated with matching {@link Candidate} objects if the
   *     user confirms the ambiguous completion.
   * @param reader the active line reader.
   */
  private void handleAmbiguousMatch(
      String word, List<String> matches, List<Candidate> candidates, LineReader reader) {
    if (word.equals(lastAmbiguousWord)) {
      matches.forEach(cmd -> candidates.add(new Candidate(cmd)));
      lastAmbiguousWord = null;
    } else {
      notifyUser(reader);
      lastAmbiguousWord = word;
    }
  }

  /**
   * Provides visual feedback to the user when a completion cannot be performed.
   * It displays a temporary error message and then refreshes the line.
   *
   * @param reader the active line reader.
   */
  private void notifyUser(LineReader reader) {
    new Thread(
        () -> {
          try {
            String errorMessage = Internationalization.get("cli.command_not_found");
            reader.printAbove("\033[31m  [!] " + errorMessage + "\033[0m");
            Thread.sleep(800);
            // ANSI escape codes to clear the temporary message line
            reader.printAbove("\033[A\033[2K");
            reader.callWidget(LineReader.REDRAW_LINE);
            reader.callWidget(LineReader.REDISPLAY);
          } catch (Exception ignored) {
            // Thread interruption or JLine internal issues are ignored for feedback
          }
        })
        .start();
  }
}