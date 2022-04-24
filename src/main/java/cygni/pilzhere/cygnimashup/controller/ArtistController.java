package cygni.pilzhere.cygnimashup.controller;

import cygni.pilzhere.cygnimashup.Artist;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author pilzhere
 * @created 17/04/2022 - 9:11 PM
 * @project cygniMashup
 */

@RestController
public class GreetingController {

    private static final String template = "You want info about artist id: %s!";
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/artist")
    Artist greeting(@RequestParam String mbid) {
        return new Artist(counter.incrementAndGet(), String.format(template, mbid));
    }

    //@GetMapping("/country")
    public Object getArtistOrBandInformation(@RequestParam String artistOrBandName) {
        final String modifiedArtistOrBandName = artistOrBandName + "%20band"; // For better result.
        final String url = "https://en.wikipedia.org/w/api.php?action=query&format=json&list=search&srsearch=" + modifiedArtistOrBandName;
        RestTemplate restTemplate = new RestTemplate();

        final Object objCountry = restTemplate.getForObject(url, Object.class);
        return objCountry;
    }
}
