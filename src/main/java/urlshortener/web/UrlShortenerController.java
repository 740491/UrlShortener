package urlshortener.web;


import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.apache.commons.validator.routines.UrlValidator;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.safebrowsing.Safebrowsing;
import com.google.api.services.safebrowsing.model.*;
import org.apache.logging.log4j.LogManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;

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

//  @RequestMapping(value = "/link", method = RequestMethod.POST)
//  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
//                                            @RequestParam(value = "sponsor", required = false)
//                                                String sponsor,
//                                            HttpServletRequest request) {
//    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
//        "https"});
//    if (urlValidator.isValid(url) && urlAccessible(url)) {
//      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
//      HttpHeaders h = new HttpHeaders();
//      h.setLocation(su.getUri());
//      Map<String,String> headersInfo = getHeadersInfo(request);
//      su.setRequestInfo(headersInfo.get("user-agent"));
//      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
//    } else {
//      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//    }
//  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                               @RequestParam(value = "sponsor", required = false)
                                                    String sponsor,
                                               HttpServletRequest request) throws IOException, WriterException {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
            "https"});
    if (urlValidator.isValid(url) && urlAccessible(url)) {

      // waiting to know how to return both shorturl and object byte[] to later display it
      //Qr qrResponse = new Qr();
      //byte[] imageByte= qrResponse.getQRCodeImage(String.valueOf(su.getUri()), 500, 500);
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), checkThreat(url));
      HttpHeaders h = new HttpHeaders();

      h.setLocation(su.getUri());
      Map<String,String> headersInfo = getHeadersInfo(request);
      su.setRequestInfo(headersInfo.get("user-agent"));
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }


  @RequestMapping(value = "/csvFile", method = RequestMethod.POST, produces = "application/csv")
  public void csvFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "sponsor", required = false)
                                                    String sponsor, HttpServletRequest request) throws IOException {
      //System.out.println("RemoteAddress:");
      //0:0:0:0:0:0:0:1
      //System.out.println(request.getRemoteAddr());
      StringWriter sw = new StringWriter();
      UrlValidator urlValidator = new UrlValidator(new String[] {"http",
            "https"});
      Reader reader = new InputStreamReader(file.getInputStream());
      CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(0).build();

      ShortURL su;
      //Escalabilidad 10 puntos (XHR Streaming):
      AsyncContext ctx = request.startAsync();
      HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType(String.valueOf(MediaType.TEXT_PLAIN));
      
      if(!file.isEmpty()){
        String[] rows = null;
        while((rows = csvReader.readNext()) != null) {
          sw.write(rows[0] + ", ");
          response.getOutputStream().print((rows[0] + ", "));

          if (urlValidator.isValid(rows[0]) && urlAccessible(rows[0])) {
            su = shortUrlService.save(rows[0], sponsor, request.getRemoteAddr(), checkThreat(rows[0]));
            sw.write(su.getUri().toString() + "\n");
            response.getOutputStream().print((su.getUri().toString() + "\n"));
          }else{
            sw.write("Invalid URL" + "\n");
            response.getOutputStream().print(("Invalid URL" + "\n"));
          }
          response.setContentLength(sw.toString().getBytes().length);
          response.flushBuffer();
        }
        ctx.complete();
        csvReader.close();

      }
  }


  @MessageMapping("/csvfile")
  @SendTo("/csvmessages/messages")
  public String csvFileSend(String message, @RequestParam(value = "sponsor", required = false)
                              String sponsor) throws Exception {
      message = message.replace("\"","");
      if(message.endsWith(",\\r")){
        message = message.substring(0, message.length() - 2);
      }
      System.out.println("New message received:");
      System.out.println(message);
      StringWriter sw = new StringWriter();
      UrlValidator urlValidator = new UrlValidator(new String[] {"http",
              "https"});

      ShortURL su;
      sw.write(message + " ");
      //Escalabilidad 15 puntos (WebSockets SockJs)
      if (urlValidator.isValid(message.split(",")[0]) && urlAccessible(message.split(",")[0])) {
        su = shortUrlService.save(message.split(",")[0], sponsor, "WebSocket", checkThreat(message.split(",")[0]));
        sw.write("http://localhost:8080" + su.getUri().toString() + "\n");
      }else{
        sw.write("Invalid URL" + "\n");
      }
      System.out.println("Returns: "+ sw.toString());
      return sw.toString();
  }

  //Returns true if the url request gives code 200 in the header, otherwise returns false
  private boolean urlAccessible(String url) {
    try {
      URL urlForGet = new URL(url);
      HttpURLConnection connection;
      connection = (HttpURLConnection) urlForGet.openConnection();
      connection.setRequestMethod("GET");
      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        System.out.println("CODE 200");
        return true;
      } else {
        System.out.print("Error Code:");
        System.out.println(connection.getResponseCode());
        return false;
      }
    } catch (IOException e) {
      System.out.println("URL not accesible");
      return false;
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
