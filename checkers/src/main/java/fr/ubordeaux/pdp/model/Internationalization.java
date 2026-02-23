package fr.ubordeaux.pdp.model;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Handles application internationalization (i18n).
 * This class manages language detection based on system environment variables 
 * (LANG or LC_ALL) and provides localized strings using Resource Bundles.
 * @version 1.0
 */
public class Internationalization {
  /** The name of the resource bundle files. */
  private static final String BUNDLE_NAME = "messages";

  /** The active resource bundle for the selected locale. */
  private static ResourceBundle bundle;

  /**
   * Initializes the internationalization system.
   * Detects the system locale and verifies if it is supported (English or French). 
   * If the locale is unsupported, it defaults to English and prints a warning 
   * message to the standard error output as per specifications.
   */
  public static void init() {
    Locale sysLocale = Locale.getDefault();

    // Check for supported languages: English (default) and French
    if (!sysLocale.getLanguage().equals("fr") && !sysLocale.getLanguage().equals("en")) {
      System.err.println(MessageFormat.format(
          "Warning: Language {0} not supported. Defaulting to English.",
          sysLocale.getLanguage()));
      Locale.setDefault(Locale.ENGLISH);
    }

    bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
  }

  /**
   * Retrieves a localized string for a given key.
   *
   * @param key The identifier for the localized message.
   * @return The localized string.
   */
  public static String get(String key) {
    return bundle.getString(key);
  }
}