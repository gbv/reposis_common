package de.gbv.reposis.editor;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class providing validation methods for common data formats.
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * Checks whether the given string represents a syntactically valid URL.
     *
     * @param url the URL string to validate
     * @return {@code true} if the string is a valid URL, {@code false} otherwise
     */
    public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }
}
