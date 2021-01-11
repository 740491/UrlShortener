package urlshortener.repository;

import urlshortener.domain.Qr;

public interface QrRepository {

    Qr findByKey(String id);

    Qr save(Qr qr);
}
