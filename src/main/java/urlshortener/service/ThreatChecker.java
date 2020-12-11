package urlshortener.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.safebrowsing.Safebrowsing;
import com.google.api.services.safebrowsing.model.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import urlshortener.repository.ShortURLRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Configuration
@EnableAsync
public class ThreatChecker {
    public static final JacksonFactory GOOGLE_JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public static final String GOOGLE_API_KEY = "AIzaSyBsK1WLBmoX-6AMs0-ady2UlkSiPFFv0zo"; // Google API key
    public static final String GOOGLE_CLIENT_ID = "UrlShortener-group-E"; // client id
    public static final String GOOGLE_CLIENT_VERSION = "0.0.1"; // client version
    public static final String GOOGLE_APPLICATION_NAME = "APP-UrlShortener-group-E"; // application name
    public static final List<String> GOOGLE_THREAT_TYPES = Arrays.asList(new String[]{"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"});
    public static final List<String> GOOGLE_PLATFORM_TYPES = Arrays.asList(new String[]{"ANY_PLATFORM"});
    public static final List<String> GOOGLE_THREAT_ENTRYTYPES = Arrays.asList(new String[]{"URL"});
    public static NetHttpTransport httpTransport;

    private final ShortURLRepository shortURLRepository;

    public ThreatChecker(ShortURLRepository shortURLRepository) {
        this.shortURLRepository = shortURLRepository;
    }

    /**
     * Check if 'url' is marked as a potencial threat in Google Safe Browsing
     * @param url is a string with the URL to check
     */
    @Async
    public void checkThreat(String hash, String url) {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            List<String> urls = Arrays.asList(new String[]{url});

            FindThreatMatchesRequest findThreatMatchesRequest = createFindThreatMatchesRequest(urls);

            Safebrowsing.Builder safebrowsingBuilder = new Safebrowsing.Builder(httpTransport, GOOGLE_JSON_FACTORY, null).setApplicationName(GOOGLE_APPLICATION_NAME);
            Safebrowsing safebrowsing = safebrowsingBuilder.build();
            FindThreatMatchesResponse findThreatMatchesResponse = safebrowsing.threatMatches().find(findThreatMatchesRequest).setKey(GOOGLE_API_KEY).execute();

            List<ThreatMatch> threatMatches = findThreatMatchesResponse.getMatches();

            //The url is not safe
            if (threatMatches != null && threatMatches.size() > 0) {
                for (ThreatMatch threatMatch : threatMatches) {
                    System.out.println(threatMatch.toPrettyString());
                }
                System.out.println("URL NOT SECURE");
                shortURLRepository.mark(shortURLRepository.findByKey(hash), false);
            } else{ //The url is safe
                System.out.println("URL SECURE");
                shortURLRepository.mark(shortURLRepository.findByKey(hash), true);
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            shortURLRepository.mark(shortURLRepository.findByKey(hash), false);
        }
    }

    //Send a request to Google Safe Browsing to check if 'urls' are threats
    private static FindThreatMatchesRequest createFindThreatMatchesRequest(List<String> urls) {
        FindThreatMatchesRequest findThreatMatchesRequest = new FindThreatMatchesRequest();

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setClientId(GOOGLE_CLIENT_ID);
        clientInfo.setClientVersion(GOOGLE_CLIENT_VERSION);
        findThreatMatchesRequest.setClient(clientInfo);

        ThreatInfo threatInfo = new ThreatInfo();
        threatInfo.setThreatTypes(GOOGLE_THREAT_TYPES);
        threatInfo.setPlatformTypes(GOOGLE_PLATFORM_TYPES);
        threatInfo.setThreatEntryTypes(GOOGLE_THREAT_ENTRYTYPES);

        List<ThreatEntry> threatEntries = new ArrayList<ThreatEntry>();

        for (String url : urls) {
            ThreatEntry threatEntry = new ThreatEntry();
            threatEntry.set("url", url);
            threatEntries.add(threatEntry);
        }
        threatInfo.setThreatEntries(threatEntries);
        findThreatMatchesRequest.setThreatInfo(threatInfo);

        return findThreatMatchesRequest;
    }
}
