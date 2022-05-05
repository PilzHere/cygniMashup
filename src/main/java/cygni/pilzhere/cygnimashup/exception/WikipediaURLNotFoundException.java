package cygni.pilzhere.cygnimashup.exception;

import java.io.IOException;

/**
 * @author pilzhere
 * @created 05/05/2022 - 10:51 PM
 * @project cygniMashup
 */
public class WikipediaURLNotFoundException extends RuntimeException {
    final private String url;

    public WikipediaURLNotFoundException (String url) {
        this.url = url;
    }

    @Override
    public String getMessage () {
        return "WikiData URL was not found. URL: ".concat(url);
    }
}
