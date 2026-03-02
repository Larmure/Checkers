package fr.ubordeaux.pdp.model;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Handles application internationalization (i18n).
 * * <p>This class manages language detection based on system environment variables 
 * (LANG or LC_ALL) and provides localized strings using Resource Bundles.
 * * @version 1.0
 */
public class Internationalization {
  /** The name of the resource bundle files. */
  private static final String BUNDLE_NAME = "messages";

  /** The active resource bundle for the selected locale. */
  private static ResourceBundle bundle;

  /**
   * Initializes the internationalization system.
   * * <p>Detects the system locale and verifies if it is supported (English or French). 
   * If the locale is unsupported, it defaults to English and prints a warning 
   * message to the standard error output as per specifications.
   */
  public static void init() {
    Locale sysLocale = Locale.getDefault();

    // Check for supported languages: English (default) and French [cite: 93]
    if (!sysLocale.getLanguage().equals("fr") && !sysLocale.getLanguage().equals("en")) {
      System.err.println(MessageFormat.format(
          "Warning: Language {0} not supported. Defaulting to English.",
          sysLocale.getLanguage()));
      Locale.setDefault(Locale.ENGLISH);
    }

    bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
  }

  /**
   * Retrieves a localized string from the resource bundle and formats it using the 
   * provided arguments. This method automatically escapes single quotes to ensure 
   * compatibility with MessageFormat.
   *
   * @param key the identifier for the localized message
   * @param args the arguments to be substituted into the message placeholders (e.g., {0}, {1})
   * @return the formatted localized string
   */
  public static String get(String key, Object... args) {
    String pattern = bundle.getString(key);
    return java.text.MessageFormat.format(pattern.replace("'", "''"), args);
  }
}