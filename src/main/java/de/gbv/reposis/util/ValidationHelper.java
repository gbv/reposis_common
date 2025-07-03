package de.gbv.reposis.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class ValidationHelper {

    public static boolean validateUrl(String url) {
        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }
}
