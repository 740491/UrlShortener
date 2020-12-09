package urlshortener.web;

import com.google.zxing.WriterException;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import urlshortener.domain.Qr;
import urlshortener.service.ClickService;
import urlshortener.service.QrService;
import urlshortener.service.ShortURLService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;

@RestController
public class QrController {
    private final ShortURLService shortUrlService;

    @Autowired
    QrService qrService;

    public QrController(ShortURLService shortUrlService, QrService qrService) {
        this.shortUrlService = shortUrlService;
        this.qrService = qrService;
    }

    @GetMapping("/qr/{hash}")
    public ResponseEntity<JSONObject> qr(@PathVariable("hash") String hash,
                                         HttpServletRequest request) throws IOException, WriterException {

        JSONObject response = new JSONObject();
        HttpHeaders h = new HttpHeaders();

        URI uri = URI.create(request.getScheme() + "://" + request.getServerName() + ":8080/" + hash);
        h.setLocation(uri);

        Qr qr = qrService.findByKey(hash);
        byte[] qrByteArray;
        System.out.println("QR CONTROLLER qr object: " + qr);
        if(qr != null) {
            System.out.println("Ya existia qr con hash: " + hash);
            qrByteArray = qr.getQrByteArray();
        }
        else {
            qrByteArray = qrService.getQRCodeImageAndStore(uri.toString(),500,500);
            System.out.println("RESPUESTA QRSERVICE: " + qrService.save(hash,qrByteArray));
            System.out.println("NO existia qr con hash: " + hash);

            System.out.println("qr out controller: " + qrByteArray);
        }
        response.put("qr", qrByteArray);




        return new ResponseEntity<>(response, h, HttpStatus.OK);

    }
}
