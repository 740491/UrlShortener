package urlshortener.web;


import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
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
import urlshortener.service.AccessibleURLService;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;
import urlshortener.service.ThreatChecker;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.util.*;

@RestController
public class UrlShortenerController {


  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  @Autowired
  AccessibleURLService accessibleURLService;

  @Autowired
  ThreatChecker threadChecker;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      if (l.getAccessible() && l.getSafe()) {
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
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                               @RequestParam(value = "sponsor", required = false)
                                                    String sponsor,
                                               HttpServletRequest request) throws IOException, WriterException {
    ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), false);

    accessibleURLService.accessible(su.getHash(), su.getTarget());
    threadChecker.checkThreat(su.getHash(), su.getTarget());

    HttpHeaders h = new HttpHeaders();

    h.setLocation(su.getUri());
    Map<String,String> headersInfo = getHeadersInfo(request);
    su.setRequestInfo(headersInfo.get("user-agent"));
    return new ResponseEntity<>(su, h, HttpStatus.CREATED);
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



  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }


}
