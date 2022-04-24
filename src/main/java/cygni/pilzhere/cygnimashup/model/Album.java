package cygni.pilzhere.cygnimashup.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author pilzhere
 * @created 23/04/2022 - 4:27 PM
 * @project cygniMashup
 */

@Getter
@Setter
@NoArgsConstructor
public class Album {
    @JsonProperty ("id")
    private String id;

    @JsonProperty ("title")
    private String title;

    @JsonProperty ("image")
    private String imageLink;

    public Album (String id, String title, String imageLink) {
        this.id = id;
        this.title = title;
        this.imageLink = imageLink;
    }
}
