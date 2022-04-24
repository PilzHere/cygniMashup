package cygni.pilzhere.cygnimashup.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.model.Album;
import cygni.pilzhere.cygnimashup.model.Artist;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pilzhere
 * @created 17/04/2022 - 9:11 PM
 * @project cygniMashup
 */

@RestController
@RequestMapping ("/artists")
public class ArtistController {

    final boolean printDebugInfo = false;

    /**
     * Gets artist data and outputs JSON data about the artist.
     *
     * @param mbid
     * @return JSON data from artist and artist's albums.
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping ("/mbid")
    Artist getArtistWithInfo (@RequestParam String mbid) throws IOException, InterruptedException {
        System.out.println("Fetching artist information using MBID: " + mbid + " ...");
        Instant timeStart = Instant.now();

        final JsonNode artistRootJSON = getArtistJSON(mbid);
        final String artistName = getArtistName(artistRootJSON);
        final String artistQID = getArtistWikiDataQID(artistRootJSON);
        List<Album> artistAlbums = getArtistAlbumIDs(artistRootJSON);
        artistAlbums = getArtistAlbumCoverLinks(artistAlbums);
        final String wikipediaURL = getArtistWikipediaURL(artistQID);
        final String artistDescription = getArtistWikipediaDescription(wikipediaURL);

        if (printDebugInfo) {
            System.out.println("Artist name: " + artistName + '\n'
                    + "QID: " + artistQID + '\n'
                    + "URL: " + wikipediaURL + '\n'
                    + "Description from Wikipedia: \n"
                    + artistDescription);
            System.out.println("Albums: ");
            for (Album album : artistAlbums) {
                System.out.println(album.getId() + '\n' + album.getTitle() + '\n' + album.getImageLink());
            }
        }

        Instant timeEnd = Instant.now();
        final long elapsedTime = Duration.between(timeStart, timeEnd).toMillis();
        System.out.println("Fetching artist information done. (" + elapsedTime + " ms)");

        return new Artist(mbid, artistQID, artistName, wikipediaURL, artistDescription, artistAlbums);
    }

    /**
     * Gets a JSON node from the artist.
     *
     * @param mbid
     * @return JSON node containing data from artist.
     * @throws IOException
     */
    public JsonNode getArtistJSON (String mbid) throws IOException {
        final String musicBrainzURL = "https://musicbrainz.org/ws/2/artist/"
                + mbid
                + "?inc=url-rels+release-groups&fmt=json";
        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonRootNode = objectMapper.readTree(new URL(musicBrainzURL));

        return jsonRootNode;
    }

    /**
     * Gets the name of the artist.
     *
     * @param rootJSON
     * @return the name of the artist.
     */
    public String getArtistName (JsonNode rootJSON) {
        final String artistName = rootJSON.get("name").asText();

        return artistName;
    }

    /**
     * Gets the QID from the artist.
     *
     * @param rootJSON
     * @return the artist's QID.
     */
    public String getArtistWikiDataQID (JsonNode rootJSON) {
        final JsonNode jsonArray = rootJSON.get("relations");
        String qid = null;

        if (jsonArray.isArray()) {
            for (final JsonNode jsonNode : jsonArray) {
                if (Objects.equals(jsonNode.get("type").asText(), "wikidata")) {
                    final JsonNode urlNode = jsonNode.path("url");
                    final String urlWiki = urlNode.get("resource").asText();

                    final String resourceBeginning = "https://www.wikidata.org/wiki/";
                    qid = urlWiki.replace(resourceBeginning, "");
                }
            }
        }

        if (qid == null) qid = "Could not be found.";

        return qid;
    }

    /**
     * Gets the id (and title) for every album from artist.
     *
     * @param rootJSON
     * @return
     */
    public List<Album> getArtistAlbumIDs (JsonNode rootJSON) {
        final JsonNode jsonArtistAlbums = rootJSON.get("release-groups");
        final List<Album> artistAlbums = new ArrayList<>();

        if (jsonArtistAlbums.isArray()) {
            for (final JsonNode jsonNode : jsonArtistAlbums) {
                artistAlbums.add(new Album(jsonNode.get("id").asText(), jsonNode.get("title").asText(), ""));
            }
        }

        return artistAlbums;
    }

    /**
     * Gets links to artist's album covers.
     *
     * @param artistAlbums
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Album> getArtistAlbumCoverLinks (List<Album> artistAlbums) throws IOException, InterruptedException {
        /* CYGNI: Det här är delen som tar tid att få ner. Detta var lite svårare då JSON-svaret fås från redirection
        vilket ObjectMapper inte tycker om!
        Jag har googlat och googlat efter en enklare lösning där man kan få alla album covers på samma gång
        med WebClient men inte hittat något svar som jag förstår...
        Detta funkar, har gjort några fulingar... men koden är snabbare den jag hade innan (~30 sek --> ~5-12 sek) men
        inte perfekt.
        Googlade framm något om Mono och Flux men det är nytt för mig och jag förstår inte det riktigt tyvär...
         */

        final int indexesWanted = artistAlbums.size();
        AtomicInteger index = new AtomicInteger(0);
        final long sleepTime = 20L;

        for (Album album : artistAlbums) {
            final String coverArtArchiveURL = "http://coverartarchive.org/release-group/" + album.getId();
            final URL url = new URL(coverArtArchiveURL);

            // JSON response is redirected: we need a webclient!
            final WebClient webClient = WebClient.create();
            AtomicReference<String> redirectLocation1 = new AtomicReference<>();
            AtomicBoolean redirectLocation1Retrieved = new AtomicBoolean(false);

            webClient.get()
                    .uri(url.toString())
                    .accept(MediaType.TEXT_PLAIN)
                    .exchange() // deprecated: can cause memory leaks!
                    .subscribe(clientResponse -> {
                        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                        redirectLocation1.set(headers.getFirst("Location"));
                        redirectLocation1Retrieved.set(true);
                    });

            while (!redirectLocation1Retrieved.get()) Thread.sleep(sleepTime); // waiting

            webClient.get()
                    .uri(redirectLocation1.get())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange() // deprecated: can cause memory leaks!
                    .subscribe(clientResponse -> {
                        HttpHeaders headers2 = clientResponse.headers().asHttpHeaders();
                        final String redirectLocation2 = headers2.getFirst("Location");
                        if (redirectLocation2 == null) { // Sometimes there is none.
                            album.setImageLink("Could not be found.");
                            index.getAndIncrement();
                            return;
                        }

                        URL jsonURL;
                        try {
                            jsonURL = new URL(redirectLocation2);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }

                        ObjectMapper objectMapper = new ObjectMapper();

                        final JsonNode jsonRootNode;
                        try {
                            jsonRootNode = objectMapper.readTree(jsonURL);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        final JsonNode jsonImagesArrayNode = jsonRootNode.path("images");

                        if (jsonImagesArrayNode.isArray()) {
                            for (final JsonNode jsonNode : jsonImagesArrayNode) {
                                final String currentAlbumCoverLink = jsonNode.get("image").asText();
                                album.setImageLink(currentAlbumCoverLink);
                            }
                        }

                        index.getAndIncrement();
                    });
        }

        while (index.get() < indexesWanted) Thread.sleep(sleepTime); // waiting

        return artistAlbums;
    }

    /**
     * Gets the english Wikipedia link for the artist.
     *
     * @param qid
     * @return Artist's link on english Wikipedia.
     * @throws IOException
     */
    public String getArtistWikipediaURL (String qid) throws IOException {
        final String wikidataURL = "https://www.wikidata.org/w/api.php?format=json&action=wbgetentities&props=sitelinks/urls&ids="
                + qid
                + "&sitefilter=enwiki";
        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode JsonRootNode = objectMapper.readTree(new URL(wikidataURL));
        final JsonNode jsonSiteLinks = JsonRootNode.path("entities").path(qid).path("sitelinks").path("enwiki");
        final String wikipediaUrl = jsonSiteLinks.get("url").asText();

        return wikipediaUrl;
    }

    /**
     * Gets the short description from the artist's english Wikipedia link.
     *
     * @param wikipediaArtistURL
     * @return The description of the artist from english Wikipedia.
     * @throws IOException
     */
    public String getArtistWikipediaDescription (String wikipediaArtistURL) throws IOException {
        final String urlStart = "https://en.wikipedia.org/wiki/";
        final String wikipediaAPISearchTitle = wikipediaArtistURL.replace(urlStart, "");
        final String url = new URL("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exchars=1200&explaintext=&titles="
                + wikipediaAPISearchTitle
                + "&format=json").toString();

        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonRootNode = objectMapper.readTree(new URL(url));
        final JsonNode jsonSiteLinks = jsonRootNode.path("query").path("pages").elements().next(); // Node inside "pages" has random(?) name.
        final String wikipediaDescription = jsonSiteLinks.get("extract").asText();

        return wikipediaDescription;
    }
}
