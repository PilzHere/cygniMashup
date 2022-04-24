package cygni.pilzhere.cygnimashup.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author pilzhere
 * @created 17/04/2022 - 9:10 PM
 * @project cygniMashup
 */

@Getter
@Setter
@NoArgsConstructor
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
    private List<Album> albums;

    public Artist (String mbid, String qid, String name, String wikipediaURL, String description, List<Album> albums) {
        this.mbid = mbid;
        this.qid = qid;
        this.name = name;
        this.wikipediaURL = wikipediaURL;
        this.description = description;
        this.albums = albums;
    }
}
