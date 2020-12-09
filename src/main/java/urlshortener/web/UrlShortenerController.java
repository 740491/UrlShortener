package urlshortener.web;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.safebrowsing.Safebrowsing;
import com.google.api.services.safebrowsing.model.*;
import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import urlshortener.domain.ShortURL;
import urlshortener.service.AccessibleURLService;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.Executor;

@RestController
public class UrlShortenerController {
  public static final JacksonFactory GOOGLE_JSON_FACTORY = JacksonFactory.getDefaultInstance();
  public static final String GOOGLE_API_KEY = "AIzaSyBsK1WLBmoX-6AMs0-ady2UlkSiPFFv0zo"; // Google API key
  public static final String GOOGLE_CLIENT_ID = "UrlShortener-group-E"; // client id
  public static final String GOOGLE_CLIENT_VERSION = "0.0.1"; // client version
  public static final String GOOGLE_APPLICATION_NAME = "APP-UrlShortener-group-E"; // application name
  public static final List<String> GOOGLE_THREAT_TYPES = Arrays.asList(new String[]{"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"});
  public static final List<String> GOOGLE_PLATFORM_TYPES = Arrays.asList(new String[]{"ANY_PLATFORM"});
  public static final List<String> GOOGLE_THREAT_ENTRYTYPES = Arrays.asList(new String[]{"URL"});
  public static NetHttpTransport httpTransport;

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  @Autowired
  AccessibleURLService accessibleURLService;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                               @RequestParam(value = "sponsor", required = false)
                                                    String sponsor,
                                               HttpServletRequest request) throws IOException, WriterException {

    // waiting to know how to return both shorturl and object byte[] to later display it
    //Qr qrResponse = new Qr();
    //byte[] imageByte= qrResponse.getQRCodeImage(String.valueOf(su.getUri()), 500, 500);
    ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), checkThreat(url));

    accessibleURLService.accessible(su.getHash(),su.getTarget());

    HttpHeaders h = new HttpHeaders();

    h.setLocation(su.getUri());
    Map<String,String> headersInfo = getHeadersInfo(request);
    su.setRequestInfo(headersInfo.get("user-agent"));
    return new ResponseEntity<>(su, h, HttpStatus.CREATED);
  }


  @RequestMapping(value = "/csvFile", method = RequestMethod.POST, produces = "application/csv")
  public ResponseEntity<InputStreamResource> csvFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "sponsor", required = false)
                                                    String sponsor, HttpServletRequest request) throws IOException {

      String path = System.getProperty("user.dir") + "/src/main/resources/";

      String name =  file.getOriginalFilename();
      File f = new File(path + name);
      try (OutputStream os = new FileOutputStream(f)) {
        os.write(file.getBytes());
      }

      if(!file.isEmpty()){
        CSVReader csvReader = new CSVReader(new FileReader(f));
        String[] rows = null;
        List<String> l = new ArrayList<>(0);
        while((rows = csvReader.readNext()) != null) {
          l.add(rows[0]);
        }
        csvReader.close();

        UrlValidator urlValidator = new UrlValidator(new String[] {"http",
                "https"});

        List<String> shortened = new ArrayList<>(0);
        String s;
        shortened.add(l.get(0));
        for(int i=1; i<l.size(); i++){
          s = l.get(i);
          if (urlValidator.isValid(s)) {

            // waiting to know how to return both shorturl and object byte[] to later display it
            //Qr qrResponse = new Qr();
            //byte[] imageByte= qrResponse.getQRCodeImage(String.valueOf(su.getUri()), 500, 500);
            ShortURL su = shortUrlService.save(s, sponsor, request.getRemoteAddr(), checkThreat(s));
            shortened.add(su.getUri().toString().replace("\"",""));
          }else{
            shortened.add(("Invalid URL").replace("\"",""));
          }
        }

        CSVWriter writer = new CSVWriter(new FileWriter(f.getPath()));
        String[] output;
        for (int i=1; i<l.size(); i++) {
          output = new String[]{l.get(i), " " + shortened.get(i)};
          writer.writeNext(output);
        }

        writer.close();


        InputStreamResource resource = new InputStreamResource(new FileInputStream(f));
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        h.setContentLength(f.length());

        return new ResponseEntity<InputStreamResource>(resource, h, HttpStatus.OK);

      }else{
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
  }


  //Return a Map with all the info in the header of the request
  private Map<String, String> getHeadersInfo(HttpServletRequest request) {

    Map<String, String> map = new HashMap<>();

    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = (String) headerNames.nextElement();
      String value = request.getHeader(key);
      map.put(key, value);
    }

    return map;
  }

  /**
   * Check if 'url' is marked as a potencial threat
   * @param url is a string with the URL to check
   * @return true if and only if url is not registered as a threat
   */
  private static boolean checkThreat(String url) {
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
        return false;
      } else{ //The url is safe
        System.out.println("URL SECURE");
        return true;
      }
    } catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
      return false;
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

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }


}
