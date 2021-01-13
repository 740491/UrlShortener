package urlshortener.web;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;
import urlshortener.domain.ShortURL;
import urlshortener.service.*;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static urlshortener.fixtures.ShortURLFixture.*;


public class UrlShortenerTests {

  private MockMvc mockMvc;

  @Mock
  private ClickService clickService;

  @Mock
  private ShortURLService shortUrlService;

  @Mock
  private AccessibleURLService accessibleURLService;

  @Mock
  private UserAgentService userAgentService;

  @Mock
  private ThreatChecker threadChecker;

  @Mock
  private TaskQueueRabbitMQClientService taskQueueRabbitMQClientService;

  @InjectMocks
  private UrlShortenerController urlShortener;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(urlShortener).build();
  }

  @Test
  public void thatRedirectToReturnsTemporaryRedirectIfKeyExists()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(someUrl());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isTemporaryRedirect())
        .andExpect(redirectedUrl("http://example.com/"));
  }

  @Test
  public void thatRedirecToReturnsNotFoundIdIfKeyDoesNotExist()
          throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(null);

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void thatRedirecFailsIfURLisNotSafe()
          throws Exception {
    when(shortUrlService.findByKey("notSafe")).thenReturn(urlNotSafe());

    mockMvc.perform(get("/{id}", "notSafe")).andDo(print())
            .andExpect(status().isBadRequest());
  }

  @Test
  public void thatRedirecFailsIfURLisNotAccessible()
          throws Exception {
    when(shortUrlService.findByKey("notAccessible")).thenReturn(urlNotAccessible());

    mockMvc.perform(get("/{id}", "notAccessible")).andDo(print())
            .andExpect(status().isBadRequest());
  }

  @Ignore("Dont know how it works")
  @Test
    public void thatShortenerCreatesARedirectIfTheURLisOK() throws Exception {
    configureSave(null);

    mockMvc.perform(post("/link").param("url", "http://example.com/"))
        .andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.su.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.su.target", is("http://example.com/")))
        .andExpect(jsonPath("$.su.sponsor", is(nullValue())));
  }

  @Ignore("Dont know if we should do tests with sponsors as we dont implement it")
  @Test
  public void thatShortenerCreatesARedirectWithSponsor() throws Exception {
    configureSave("http://sponsor.com/");

    mockMvc.perform(
        post("/link").param("url", "http://example.com/").param(
            "sponsor", "http://sponsor.com/").param(
                "qrCheck", "false")).andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.su.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.su.target", is("http://example.com/")))
        .andExpect(jsonPath("$.su.sponsor", is("http://sponsor.com/")));
  }

  //TODO: crea un enlace que no es seguro

  //TODO: crea un enlace que no es accesible

  //TODO: pruebas con el CSV



  @Test
  public void thatUserAgentsIsOk()
          throws Exception {

    mockMvc.perform(get("/userAgents"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @Test(expected = NestedServletException.class)
  public void thatShortenerFailsIfTheRepositoryReturnsNull() throws Exception {
    when(shortUrlService.save(any(String.class), any(String.class), any(String.class), any(Boolean.class)))
        .thenReturn(null);

    mockMvc.perform(post("/link").param("url", "someKey")).andDo(print())
        .andExpect(status().isBadRequest());
  }

  private void configureSave(String sponsor) {
    when(shortUrlService.save(any(), any(), any(), any(Boolean.class)))
        .then((Answer<ShortURL>) invocation -> new ShortURL(
            "f684a3c4",
            "http://example.com/",
            URI.create("http://localhost/f684a3c4"),
            sponsor,
            null,
            null,
            0,
            false,
            null,
            null,
            false));
  }
}
