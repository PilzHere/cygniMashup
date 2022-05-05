package cygni.pilzhere.cygnimashup.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author pilzhere
 * @created 05/05/2022 - 3:30 PM
 * @project cygniMashup
 */

@Service
public class WikiDataService {

    /**
     * Gets the QID from the artist.
     *
     * @param node
     * @return the artist's QID.
     */
    public String getArtistWikiDataQID (JsonNode node) {
        final JsonNode jsonArray = node.get("relations");
        String qid = null;

        if (jsonArray.isArray()) {
            for (final JsonNode jsonNode : jsonArray) {
                if (Objects.equals(jsonNode.get("type").asText(), "wikidata")) {
                    final JsonNode urlNode = jsonNode.path("url");
                    final String urlWiki = urlNode.get("resource").asText();

                    final String resourceBeginning = "https://www.wikidata.org/wiki/";
                    qid = urlWiki.replace(resourceBeginning, "");

                    break;
                }
            }
        }

        if (qid == null)
            qid = "Could not be found.";

        return qid;
    }
}
