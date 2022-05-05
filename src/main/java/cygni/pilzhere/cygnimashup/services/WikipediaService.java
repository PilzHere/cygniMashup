package cygni.pilzhere.cygnimashup.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.exception.WikipediaJsonNotFoundException;
import cygni.pilzhere.cygnimashup.exception.WikipediaURLNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author pilzhere
 * @created 05/05/2022 - 3:31 PM
 * @project cygniMashup
 */

@Service
public class WikipediaService {

    /**
     * Gets the english Wikipedia link for the artist.
     *
     * @param qid
     * @return Artist's link on english Wikipedia.
     */
    public String getArtistWikipediaURL (String qid) {
        final String wikidataURL = "https://www.wikidata.org/w/api.php?format=json&action=wbgetentities&props=sitelinks/urls&ids="
                + qid
                + "&sitefilter=enwiki";
        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode JsonRootNode;
        try {
            JsonRootNode = objectMapper.readTree(new URL(wikidataURL));
        } catch (IOException e) {
            throw new WikipediaURLNotFoundException(wikidataURL);
        }

        final JsonNode jsonSiteLinks = JsonRootNode.path("entities").path(qid).path("sitelinks").path("enwiki");

        return jsonSiteLinks.get("url").asText();
    }

    /**
     * Gets the short description from the artist's english Wikipedia link.
     *
     * @param wikipediaArtistURL
     * @return The description of the artist from english Wikipedia.
     */
    public String getArtistWikipediaDescription (String wikipediaArtistURL) {
        final String urlStart = "https://en.wikipedia.org/wiki/";
        final String wikipediaAPISearchTitle = wikipediaArtistURL.replace(urlStart, "");
        final String url;
        try {
            url = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exchars=1200&explaintext=&titles="
                    + wikipediaAPISearchTitle
                    + "&format=json").toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonRootNode;
        try {
            jsonRootNode = objectMapper.readTree(new URL(url));
        } catch (IOException e) {
            throw new WikipediaJsonNotFoundException(url);
        }

        final JsonNode jsonSiteLinks = jsonRootNode.path("query").path("pages").elements().next(); // Node inside "pages" has random(?) name.

        return jsonSiteLinks.get("extract").asText();
    }
}
