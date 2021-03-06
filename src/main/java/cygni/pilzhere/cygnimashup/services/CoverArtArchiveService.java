package cygni.pilzhere.cygnimashup.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cygni.pilzhere.cygnimashup.exception.CoverArtArchiveJsonNotFoundException;
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

            webClient.get()
                    .uri(url.toString())
                    .accept(MediaType.TEXT_PLAIN)
                    .exchange() // deprecated: can cause memory leaks!
                    .doOnError(exception -> nok.incrementAndGet())
                    .doOnSuccess(clientResponse -> {
                        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                        String redirectLocation = headers.getFirst("Location");
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
        /* CYGNI: Det h??r ??r delen som tar tid att f?? ner. Detta var lite sv??rare d?? JSON-svaret f??s fr??n redirection
        vilket ObjectMapper inte tycker om!
        Jag har googlat och googlat efter en enklare l??sning d??r man kan f?? alla album covers p?? samma g??ng
        med WebClient men inte hittat n??got svar som jag f??rst??r...
        Detta funkar, har gjort n??gra fulingar... men koden ??r snabbare den jag hade innan (~30 sek --> ~3-12 sek) men
        inte perfekt.
        Googlade fram n??got om Mono och Flux... men det ??r nytt f??r mig och jag f??rst??r inte det riktigt tyv??rr...
         */

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger nok = new AtomicInteger(0);

        for (Album album : artistAlbums) {
            final String redirectLink = albumRedirects.get(album.getId());
            final WebClient webClient = WebClient.create();

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
                                        throw new CoverArtArchiveJsonNotFoundException(jsonURL.toString());
                                    }

                                    final JsonNode jsonImagesArrayNode = jsonRootNode.path("images");

                                    if (jsonImagesArrayNode.isArray()) {
                                        for (final JsonNode jsonNode : jsonImagesArrayNode) {
                                            final String currentAlbumCoverLink = jsonNode.get("image").asText();

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
                    .block(); // <-- FIXME: ERRORS h??r!

            HttpHeaders headers2 = response2.getHeaders();
            final String redirectLocation2 = headers2.getFirst("Location");

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
