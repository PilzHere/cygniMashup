package cygni.pilzhere.cygnimashup.exception;

/**
 * @author pilzhere
 * @created 05/05/2022 - 10:58 PM
 * @project cygniMashup
 */
public class WikipediaJsonNotFoundException extends RuntimeException {
        final private String url;

        public WikipediaJsonNotFoundException (String url) {
            this.url = url;
        }

        @Override
        public String getMessage () {
            return "Wikipedia JSON was not found. JSON URL: ".concat(url);
        }

}
