package cygni.pilzhere.cygnimashup;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

/**
 * @author pilzhere
 * @created 17/04/2022 - 9:10 PM
 * @project cygniMashup
 */
public class Artist {
    @JsonProperty ("mbid")
    private String mbid;

    @JsonProperty ("qid")
    private String qid;

    @JsonProperty ("name")
    private String name;

    @JsonProperty ("wikipediaURL")
    private String wikipediaURL;

    @JsonProperty ("description")
    private String description;

    @JsonProperty ("albums")
    private List<Album> albums; // name, url

    public Artist () {

    }

    public Artist (String mbid, String qid, String name, String wikipediaURL, String description, List<Album> albums) {
        this.mbid = mbid;
        this.qid = qid;
        this.name = name;
        this.wikipediaURL = wikipediaURL;
        this.description = description;
        this.albums = albums;
    }

    public String getMbid () {
        return mbid;
    }

    public String getQid () {
        return qid;
    }

    public String getName () {
        return name;
    }

    public String getWikipediaURL () {
        return wikipediaURL;
    }

    public String getDescription () {
        return description;
    }

    public List<Album> getAlbums () {
        return albums;
    }
}
