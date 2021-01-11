package urlshortener.repository;

import urlshortener.domain.Qr;
import urlshortener.domain.ShortURL;
import urlshortener.domain.UserAgent;

import java.util.List;

public interface UserAgentRepository {

    UserAgent save(UserAgent userAgent);

    List<UserAgent> listAll();
}
