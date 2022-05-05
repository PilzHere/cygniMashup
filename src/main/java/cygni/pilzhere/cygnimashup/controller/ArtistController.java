package cygni.pilzhere.cygnimashup.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.model.Album;
import cygni.pilzhere.cygnimashup.model.Artist;
import cygni.pilzhere.cygnimashup.services.CoverArtArchiveService;
import cygni.pilzhere.cygnimashup.services.MuzicBrainzService;
import cygni.pilzhere.cygnimashup.services.WikiDataService;
import cygni.pilzhere.cygnimashup.services.WikipediaService;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final MuzicBrainzService muzicBrainzService;
    private final WikiDataService wikiDataService;
    private final WikipediaService wikipediaService;
    private final CoverArtArchiveService coverArtArchiveService;

    final boolean printDebugInfo = false;

    public ArtistController (MuzicBrainzService muzicBrainzService, WikiDataService wikiDataService, WikipediaService wikipediaService, CoverArtArchiveService coverArtArchiveService) {
        this.muzicBrainzService = muzicBrainzService;
        this.wikiDataService = wikiDataService;
        this.wikipediaService = wikipediaService;
        this.coverArtArchiveService = coverArtArchiveService;
    }

    /**
     * Gets artist data and outputs JSON data about the artist.
     *
     * @param mbid
     * @return JSON data from artist and artist's albums.
     */
    @GetMapping ("/mbid")
    public Artist getArtistWithInfo (@RequestParam String mbid) {
        System.out.println("Fetching artist information using MBID: " + mbid + " ...");
        Instant timeStart = Instant.now();

        final JsonNode artistRootJSON = muzicBrainzService.findArtistData(mbid);

        Artist artist = new Artist();
        artist.setMbid(mbid);
        artist.setName(muzicBrainzService.getArtistName(artistRootJSON));
        artist.setQid(wikiDataService.getArtistWikiDataQID(artistRootJSON));
        artist.setAlbums(muzicBrainzService.getArtistAlbumIDs(artistRootJSON));
        artist.setAlbums(coverArtArchiveService.getArtistAlbumCoverLinks(artist.getAlbums()));
        artist.setWikipediaURL(wikipediaService.getArtistWikipediaURL(artist.getQid()));
        artist.setDescription(wikipediaService.getArtistWikipediaDescription(artist.getWikipediaURL()));

        if (printDebugInfo) {
            System.out.println("Artist name: " + artist.getName() + '\n'
                    + "QID: " + artist.getQid() + '\n'
                    + "URL: " + artist.getWikipediaURL() + '\n'
                    + "Description from Wikipedia: \n"
                    + artist.getDescription());
            System.out.println("Albums: ");
            for (Album album : artist.getAlbums()) {
                System.out.println(album.getId() + '\n' + album.getTitle() + '\n' + album.getImageLink());
            }
        }

        Instant timeEnd = Instant.now();
        final long elapsedTime = Duration.between(timeStart, timeEnd).toMillis();
        System.out.println("Fetching artist information done. (" + elapsedTime + " ms)");

        return artist;
    }
}
