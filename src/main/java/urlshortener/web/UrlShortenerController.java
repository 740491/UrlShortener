package urlshortener.web;


import com.google.zxing.WriterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.minidev.json.JSONObject;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import urlshortener.domain.ShortURL;
import urlshortener.service.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Objects;

@RestController
public class UrlShortenerController {


  private final ShortURLService shortUrlService;

  private final QrService qrService;

  private final ClickService clickService;

  @Autowired
  AccessibleURLService accessibleURLService;

  @Autowired
  ThreatChecker threadChecker;

  //@Autowired
  //TaskQueueRabbitMQClientService taskQueueService;

  @Autowired
  UserAgentService userAgentService;

  public UrlShortenerController(ShortURLService shortUrlService, QrService qrService, ClickService clickService,
                                AccessibleURLService accessibleURLService, UserAgentService userAgentService) {

    this.shortUrlService = shortUrlService;
    this.qrService = qrService;
    this.clickService = clickService;
    this.accessibleURLService = accessibleURLService;

    this.threadChecker = threadChecker;
    this.userAgentService = userAgentService;
  }

    @Operation(summary = "method redirectTo given a request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "url.getMode", description = "Creates succesful redirection to hashed url"
            ),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST"
                    ),
            @ApiResponse(responseCode = "404", description = "Not Found"
            )
    })
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
          JSONObject response = new JSONObject();
          response.put("error code", HttpStatus.BAD_REQUEST);
        if(!l.getAccessible()){
            response.put("error", "This URL is not accessible");
        }else{
            response.put("error", "This URL is not safe");
        }
          return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
      }
    } else {
        JSONObject response = new JSONObject();
        response.put("error", "This shorted url does not exist");
        response.put("error code", HttpStatus.NOT_FOUND);
      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
  }

    @Operation(summary = "Creates a shortened url and returns a JSON")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "short url created",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(type = "object", example = "{su: ShortURL, uri: string , safe: Boolean, qr: string}")) })
    }

    )
  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<JSONObject> shortener(@RequestParam("url") String url,
                                              @RequestParam(value = "sponsor", required = false)
                                                    String sponsor,
                                              @RequestParam(value = "qrCheck", required = false) Boolean qrCheck,
                                              HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
            "https"});

      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), false);
      accessibleURLService.accessible(su.getHash(), su.getTarget());
      threadChecker.checkThreat(su.getHash(), su.getTarget());

      //taskQueueService.send(su.getHash(), su.getTarget());

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
        response.put("qr", qrURL);
        System.out.println("URL CONTROLLER qrUrl: " + qrURL);

      }
      return new ResponseEntity<>(response, h, HttpStatus.CREATED);

    //} else {
    //  return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      //}
  }


  @Operation(summary = "Post method to submit a csv file")
  @RequestBody(description = "file = .csv")
  @MessageMapping("/csvfile")
  @SendTo({"/csvmessages/messages"})
  public String csvFileSend(String message, @RequestParam(value = "sponsor", required = false)
          String sponsor) {
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
      su = shortUrlService.save(message.split(",")[0], sponsor, "Websockets", false);
      System.out.println(su);
      sw.write("http://localhost:8080" + su.getUri().toString() + "\n");
      accessibleURLService.accessible(su.getHash(), su.getTarget());
      threadChecker.checkThreat(su.getHash(), su.getTarget());
      //taskQueueService.send(su.getHash(), su.getTarget());

      System.out.println("Returns: "+ sw.toString());
      return sw.toString();
  }



    @Operation(summary = "Get userAgents info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "userAgents info obtained",
                    content = { @Content(mediaType = "string",
                            schema = @Schema(type = "string", example = "[{\"id\":0,\"hash\":\"cb5e0090\",\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36\"},{\"id\":1,\"hash\":\"587fe5c7\",\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36\"}]")) })
    }

    )
    @RequestMapping(value = "/userAgents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> userAgents() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        //Force new update
        userAgentService.updateUserAgentInfo();
        return new ResponseEntity<>(userAgentService.getUserAgentInfo(), h, HttpStatus.OK);
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
