package urlshortener.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.safebrowsing.Safebrowsing;
import com.google.api.services.safebrowsing.model.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class RabbitMQSafeServerService {
    /*
    @Autowired
    private AmqpTemplate template;

    @Autowired
    private Queue safetasksReplies;


    public static final JacksonFactory GOOGLE_JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public static final String GOOGLE_API_KEY = "AIzaSyBsK1WLBmoX-6AMs0-ady2UlkSiPFFv0zo"; // Google API key
    public static final String GOOGLE_CLIENT_ID = "UrlShortener-group-E"; // client id
    public static final String GOOGLE_CLIENT_VERSION = "0.0.1"; // client version
    public static final String GOOGLE_APPLICATION_NAME = "APP-UrlShortener-group-E"; // application name
    public static final List<String> GOOGLE_THREAT_TYPES = Arrays.asList(new String[]{"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"});
    public static final List<String> GOOGLE_PLATFORM_TYPES = Arrays.asList(new String[]{"ANY_PLATFORM"});
    public static final List<String> GOOGLE_THREAT_ENTRYTYPES = Arrays.asList(new String[]{"URL"});
    public static NetHttpTransport httpTransport;

    @RabbitListener(queues = "safetasksRequest")
    public void checkThreat(String[] message){
        String hash = message[0];
        String url = message[1];
        Boolean value = false;


        System.out.println("RECIBIDO EN EL SERVIDOR!: " + hash + " y " + url);

        try{
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            List<String> urls = Arrays.asList(new String[]{url});

            FindThreatMatchesRequest findThreatMatchesRequest = createFindThreatMatchesRequest(urls);

            Safebrowsing.Builder safebrowsingBuilder = new Safebrowsing.Builder(httpTransport, GOOGLE_JSON_FACTORY, null).setApplicationName(GOOGLE_APPLICATION_NAME);
            Safebrowsing safebrowsing = safebrowsingBuilder.build();
            FindThreatMatchesResponse findThreatMatchesResponse = safebrowsing.threatMatches().find(findThreatMatchesRequest).setKey(GOOGLE_API_KEY).execute();

            List<ThreatMatch> threatMatches = findThreatMatchesResponse.getMatches();

            //The url is safe
            if (threatMatches == null || threatMatches.size() == 0) {
                value = true;
            }

            this.template.convertAndSend(safetasksReplies.getName(), new String[]{hash, String.valueOf(value)});

        }catch (Exception e){
            e.printStackTrace();
            this.template.convertAndSend(safetasksReplies.getName(), new String[]{hash, String.valueOf(value)});
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

     */
}
