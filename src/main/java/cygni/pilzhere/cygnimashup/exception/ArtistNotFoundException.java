package cygni.pilzhere.cygnimashup.exception;

/**
 * @author pilzhere
 * @created 05/05/2022 - 5:52 PM
 * @project cygniMashup
 */
public class ArtistNotFoundException extends RuntimeException {
    final private String mbid;

    public ArtistNotFoundException (String mbid) {
        this.mbid = mbid;
    }

    @Override
    public String getMessage () {
        return "Artist with MBID: ".concat(mbid).concat(" was not found.");
    }
}
