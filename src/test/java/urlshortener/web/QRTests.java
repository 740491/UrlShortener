package urlshortener.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import urlshortener.domain.Qr;
import urlshortener.service.QrService;
import urlshortener.service.ShortURLService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static urlshortener.fixtures.ShortURLFixture.someUrl;

public class QRTests {

    private MockMvc mockMvc;

    @Mock
    QrService qrService;

    @Mock
    private ShortURLService shortUrlService;

    @InjectMocks
    private QrController qrController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(qrController).build();
    }


    @Test
    public void thatQRreturnsOkifURLisOK() throws Exception {
        when(qrService.findByKey("someKey")).thenReturn(new Qr("someKey",null));

        mockMvc.perform(get("/qr/{id}", "someKey"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    //The QR will always return an image, because it converts "http://localhost:8080/{id}" to a QR image.
    //If the {id} doesn't exits, you won't be able to redirect it, but the image will be created and stored, so if in a
    //future you create that {id}, the QR will be usable
}
