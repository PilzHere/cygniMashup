package cygni.pilzhere.cygnimashup;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author pilzhere
 * @created 23/04/2022 - 4:27 PM
 * @project cygniMashup
 */
public class Album {
    @JsonProperty ("id")
    private String id;

    @JsonProperty ("title")
    private String title;

    @JsonProperty ("image")
    private String imageLink;

    public Album () {
    }

    public Album (String id, String title, String imageLink) {
        this.id = id;
        this.title = title;
        this.imageLink = imageLink;
    }

    public String getId () {
        return id;
    }

    public String getTitle () {
        return title;
    }

    public String getImageLink () {
        return imageLink;
    }

    public void setImageLink (String imageLink) {
        this.imageLink = imageLink;
    }
}
