package urlshortener.web;

import com.google.zxing.WriterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
public class QrController {
    private final ShortURLService shortUrlService;

    @Autowired
    QrService qrService;

    public QrController(ShortURLService shortUrlService, QrService qrService) {
        this.shortUrlService = shortUrlService;
        this.qrService = qrService;
    }

    @Operation(summary = "Get a QR by its hash")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR obtained",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(type = "object", example = "{qr: byte[]}")) })
                    }

    )
    @GetMapping("/qr/{hash}")
    public ResponseEntity<JSONObject> qr(@PathVariable("hash") String hash,
                                         HttpServletRequest request) throws IOException, WriterException, ExecutionException, InterruptedException {

        JSONObject response = new JSONObject();
        HttpHeaders h = new HttpHeaders();

        URI uri = URI.create(request.getScheme() + "://" + request.getServerName() + ":8080/" + hash);
        h.setLocation(uri);

        Qr qr = qrService.findByKey(hash);
        Future<byte[]> qrByteArray;
        System.out.println("QR CONTROLLER qr object: " + qr);
        byte[] qrByteArrayLocal;
        if(qr != null) {
            System.out.println("Ya existia qr con hash: " + hash);

            qrByteArrayLocal = qr.getQrByteArray();
            response.put("qr", qrByteArrayLocal);
        }
        else {
            qrByteArray = qrService.getQRCodeImageAndStore(uri.toString(),500,500);
            qrByteArrayLocal = qrByteArray.get();
            System.out.println("RESPUESTA QRSERVICE: " + qrService.save(hash,qrByteArrayLocal));
            System.out.println("NO existia qr con hash: " + hash);

            System.out.println("qr out controller: " + qrByteArray);

            response.put("qr", qrByteArrayLocal);
        }





        return new ResponseEntity<>(response, h, HttpStatus.OK);

    }
}
