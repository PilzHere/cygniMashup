package cygni.pilzhere.cygnimashup.exception;

/**
 * @author pilzhere
 * @created 05/05/2022 - 11:03 PM
 * @project cygniMashup
 */
public class CoverArtArchiveJsonNotFoundException extends RuntimeException {
    final private String url;

    public CoverArtArchiveJsonNotFoundException (String url) {
        this.url = url;
    }

    @Override
    public String getMessage () {
        return "CoverArtArchive JSON was not found. JSON URL: ".concat(url);
    }
}
