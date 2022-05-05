package cygni.pilzhere.cygnimashup.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.exception.ArtistNotFoundException;
import cygni.pilzhere.cygnimashup.model.Album;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pilzhere
 * @created 05/05/2022 - 3:27 PM
 * @project cygniMashup
 */

@Service
public class MuzicBrainzService {

    /**
     * Gets a JSON node from the artist.
     *
     * @param mbid
     * @return JSON node containing data from artist.
     */
    public JsonNode findArtistData (String mbid) {
        final String musicBrainzURL = "https://musicbrainz.org/ws/2/artist/"
                + mbid
                + "?inc=url-rels+release-groups&fmt=json";
        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonRootNode;
        try {
            jsonRootNode = objectMapper.readTree(new URL(musicBrainzURL));
        } catch (IOException e) {
            throw new ArtistNotFoundException(mbid);
        }

        return jsonRootNode;
    }

    /**
     * Gets the name of the artist.
     *
     * @param node
     * @return the name of the artist.
     */
    public String getArtistName (JsonNode node) {
        return node.get("name").asText();
    }

    /**
     * Gets the id (and title) for every album from artist.
     *
     * @param node
     * @return artist's album's IDs.
     */
    public List<Album> getArtistAlbumIDs (JsonNode node) {
        final JsonNode jsonArtistAlbums = node.get("release-groups");
        final List<Album> artistAlbums = new ArrayList<>();

        if (jsonArtistAlbums.isArray()) {
            for (final JsonNode jsonNode : jsonArtistAlbums) {
                artistAlbums.add(new Album(jsonNode.get("id").asText(), jsonNode.get("title").asText(), ""));
            }
        }

        return artistAlbums;
    }
}
