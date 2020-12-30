package urlshortener.web;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import net.minidev.json.JSONObject;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import urlshortener.domain.ShortURL;
import urlshortener.domain.UserAgent;
import urlshortener.service.*;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

@RestController
public class UrlShortenerController {


  private final ShortURLService shortUrlService;

  private final QrService qrService;

  private final ClickService clickService;

  @Autowired
  AccessibleURLService accessibleURLService;

  @Autowired
  ThreatChecker threadChecker;

  @Autowired
  UserAgentService userAgentService;

  public UrlShortenerController(ShortURLService shortUrlService, QrService qrService, ClickService clickService,
                                AccessibleURLService accessibleURLService, ThreatChecker threadChecker,
                                UserAgentService userAgentService) {
    this.shortUrlService = shortUrlService;
    this.qrService = qrService;
    this.clickService = clickService;
    this.accessibleURLService = accessibleURLService;
    this.threadChecker = threadChecker;
    this.userAgentService = userAgentService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      if (l.getAccessible() && l.getSafe()) {
        userAgentService.extractUserAgent(request, id);
        clickService.saveClick(id, extractIP(request));
        return createSuccessfulRedirectToResponse(l);
      } else {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }


  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<JSONObject> shortener(@RequestParam("url") String url,
                                              @RequestParam(value = "sponsor", required = false)
                                                    String sponsor,
                                              @RequestParam(value = "qrCheck", required = false) Boolean qrCheck,
                                              HttpServletRequest request) throws IOException, WriterException {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
            "https"});
    //if (urlValidator.isValid(url)) {

      // waiting to know how to return both shorturl and object byte[] to later display it
      //Qr qrResponse = new Qr();
      //byte[] imageByte= qrResponse.getQRCodeImage(String.valueOf(su.getUri()), 500, 500);
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), false);
      accessibleURLService.accessible(su.getHash(), su.getTarget());
      threadChecker.checkThreat(su.getHash(), su.getTarget());
      HttpHeaders h = new HttpHeaders();
      JSONObject response = new JSONObject();

      URI su_uri = su.getUri();
      h.setLocation(su_uri);


      response.put("su", su);
      response.put("uri", su_uri.toString());
      response.put("safe", su.getSafe());

      String qrURL;

      System.out.println("URL CONTROLLER qrCheck: " + qrCheck);

      if(qrCheck != null && qrCheck){
        qrURL = request.getScheme() + "://" + request.getServerName() + ":8080/qr/" + su.getHash();
        //qrService.getQRCodeImage(su_uri.toString(),500,500);
        response.put("qr", qrURL);
        System.out.println("URL CONTROLLER qrUrl: " + qrURL);

      }
      return new ResponseEntity<>(response, h, HttpStatus.CREATED);
    //} else {
    //  return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      //}
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

          if (urlValidator.isValid(rows[0])) {
            su = shortUrlService.save(rows[0], sponsor, request.getRemoteAddr(), false);
            sw.write(su.getUri().toString() + "\n");
            response.getOutputStream().print((su.getUri().toString() + "\n"));
            accessibleURLService.accessible(su.getHash(), su.getTarget());
            threadChecker.checkThreat(su.getHash(), su.getTarget());
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
      if (urlValidator.isValid(message.split(",")[0])) {
        su = shortUrlService.save(message.split(",")[0], sponsor, "WebSocket", false);
        sw.write("http://localhost:8080" + su.getUri().toString() + "\n");
        accessibleURLService.accessible(su.getHash(), su.getTarget());
        threadChecker.checkThreat(su.getHash(), su.getTarget());
      }else{
        sw.write("Invalid URL" + "\n");
      }
      System.out.println("Returns: "+ sw.toString());
      return sw.toString();
  }




    @RequestMapping(value = "/userAgents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> userAgents() {
        //Force new update
        userAgentService.updateUserAgentInfo();
        return new ResponseEntity<>(userAgentService.getUserAgentInfo(), HttpStatus.OK);
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
