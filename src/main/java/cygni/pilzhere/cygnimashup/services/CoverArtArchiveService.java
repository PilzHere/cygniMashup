package cygni.pilzhere.cygnimashup.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.model.Album;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pilzhere
 * @created 05/05/2022 - 3:32 PM
 * @project cygniMashup
 */

@Service
public class CoverArtArchiveService {

    private final Map<String, String> albumRedirects = new HashMap<>();

    /**
     * Gets links to artist's album covers.
     *
     * @param artistAlbums
     * @return
     */
    public List<Album> getArtistAlbumCoverLinks (List<Album> artistAlbums) {
        albumRedirects.clear();
        artistAlbums = getArtistAlbumCoverRedirectionLinks(artistAlbums);
        artistAlbums = getArtistAlbumCoverLinks2(artistAlbums);

        return artistAlbums;
    }

    public List<Album> getArtistAlbumCoverRedirectionLinks (List<Album> artistAlbums) {
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger nok = new AtomicInteger(0);

        for (Album album : artistAlbums) {
            final String coverArtArchiveURL = "http://coverartarchive.org/release-group/" + album.getId();
            final URL url;
            try {
                url = new URL(coverArtArchiveURL);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // JSON response is redirected: we need a webclient!
            final WebClient webClient = WebClient.create();

            long timeOut = 2L;
            webClient.get()
                    .uri(url.toString())
                    .accept(MediaType.TEXT_PLAIN)
                    .exchange() // deprecated: can cause memory leaks!
                    .timeout(Duration.ofSeconds(timeOut))
                    .doOnError(exception -> {
                        nok.incrementAndGet();
                    })
                    .doOnSuccess(clientResponse -> {
                        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                        String redirectLocation = headers.getFirst("Location");

                        //System.out.println("1: " + redirectLocation);

                        albumRedirects.put(album.getId(), redirectLocation);

                        ok.incrementAndGet();
                    })
                    .subscribe();
        }

        while ((ok.get() + nok.get()) != artistAlbums.size()) {
            // wait
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return getArtistAlbumCoverLinks2(artistAlbums);
    }

    public List<Album> getArtistAlbumCoverLinks2 (List<Album> artistAlbums) {
        /* CYGNI: Det här är delen som tar tid att få ner. Detta var lite svårare då JSON-svaret fås från redirection
        vilket ObjectMapper inte tycker om!
        Jag har googlat och googlat efter en enklare lösning där man kan få alla album covers på samma gång
        med WebClient men inte hittat något svar som jag förstår...
        Detta funkar, har gjort några fulingar... men koden är snabbare den jag hade innan (~30 sek --> ~3-12 sek) men
        inte perfekt.
        Googlade fram något om Mono och Flux... men det är nytt för mig och jag förstår inte det riktigt tyvärr...
         */

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger nok = new AtomicInteger(0);

        for (Album album : artistAlbums) {
            final String redirectLink = albumRedirects.get(album.getId());
            final WebClient webClient = WebClient.create();
            final long timeOut = 5L;

            if (redirectLink != null) {
                if (!redirectLink.isBlank()) {
                    final URL url;
                    try {
                        url = new URL(redirectLink);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }

                    webClient.get()
                            .uri(url.toString())
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange() // deprecated: can cause memory leaks!
                            .timeout(Duration.ofSeconds(timeOut))
                            .doOnError(exception -> {
                                album.setImageLink("Could not be found.");
                                nok.incrementAndGet();
                            })
                            .doOnSuccess(clientResponse -> {
                                HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                                String redirectLocation = headers.getFirst("Location");

                                if (redirectLocation == null) {
                                    nok.getAndIncrement();
                                    album.setImageLink("Could not be found.");
                                } else {
                                    URL jsonURL;
                                    try {
                                        jsonURL = new URL(redirectLocation);
                                    } catch (MalformedURLException e) {
                                        nok.getAndIncrement();
                                        throw new RuntimeException(e);
                                    }

                                    ObjectMapper objectMapper = new ObjectMapper();

                                    final JsonNode jsonRootNode;
                                    try {
                                        jsonRootNode = objectMapper.readTree(jsonURL);
                                    } catch (IOException e) {
                                        nok.getAndIncrement();
                                        throw new RuntimeException(e);
                                    }

                                    final JsonNode jsonImagesArrayNode = jsonRootNode.path("images");

                                    if (jsonImagesArrayNode.isArray()) {
                                        for (final JsonNode jsonNode : jsonImagesArrayNode) {
                                            final String currentAlbumCoverLink = jsonNode.get("image").asText();

                                            //System.out.println("Album: " + album.getTitle());
                                            //System.out.println(redirectLocation);
                                            //System.out.println("2: " + currentAlbumCoverLink);

                                            album.setImageLink(currentAlbumCoverLink);
                                            ok.incrementAndGet();
                                            break; // Get first found in array.
                                        }
                                    }
                                }
                            })
                            .subscribe();
                }
            } else {
                album.setImageLink("Could not be found.");
                nok.incrementAndGet();
            }
        }

        while ((ok.get() + nok.get()) != artistAlbums.size()) {
            // wait
            try {
                Thread.sleep(25L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return artistAlbums;
    }

    /**
     * This is NOT USED. Single-threaded version.
     *
     * @param artistAlbums
     * @return
     */
    public List<Album> getArtistAlbumCoverLinksOriginal (List<Album> artistAlbums) {

        for (Album album : artistAlbums) {
            final String coverArtArchiveURL = "http://coverartarchive.org/release-group/" + album.getId();
            final URL url;
            try {
                url = new URL(coverArtArchiveURL);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // JSON response is redirected: we need a webclient!
            final WebClient webClient = WebClient.create();

            ResponseEntity<String> response1 = webClient.get()
                    .uri(url.toString())
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            if (response1.getStatusCode() == HttpStatus.NOT_FOUND) {
                album.setImageLink("Could not be found.");

                continue;
            }

            HttpHeaders headers = response1.getHeaders();
            final String redirectLocation1 = headers.getFirst("Location");

            //System.out.println(redirectLocation1);

            if (redirectLocation1 == null) { // Sometimes there is none.
                album.setImageLink("Could not be found.");

                continue;
            }

            URL url1 = null;
            try {
                url1 = new URL(redirectLocation1);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            ResponseEntity<String> response2 = webClient.get()
                    .uri(url1.toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class)
                    .block(); // <-- FIXME: ERRORS här!

            HttpHeaders headers2 = response2.getHeaders();
            final String redirectLocation2 = headers2.getFirst("Location");

            //System.out.println(redirectLocation2);

            if (redirectLocation2 == null) { // Sometimes there is none.
                album.setImageLink("Could not be found.");

                continue;
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
        }

        return artistAlbums;
    }
}
