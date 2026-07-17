package de.gbv.reposis.editor;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                return false;
            }
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            if (uri.getHost() == null) {
                return false;
            }
            uri.toURL();
            return true;
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }
}
