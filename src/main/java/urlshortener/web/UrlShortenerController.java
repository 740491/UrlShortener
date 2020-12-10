package urlshortener.web;


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
import urlshortener.service.ThreatChecker;

import javax.servlet.http.HttpServletRequest;
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
            ShortURL su = shortUrlService.save(s, sponsor, request.getRemoteAddr(), false);
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



  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }


}
