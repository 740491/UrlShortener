package urlshortener.repository.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import urlshortener.domain.Qr;
import urlshortener.domain.ShortURL;
import urlshortener.repository.QrRepository;

@Repository
public class QrRepositoryImpl implements QrRepository {
    private static final Logger log = LoggerFactory
            .getLogger(ShortURLRepositoryImpl.class);

    private static final RowMapper<Qr> rowMapper =
            (rs, rowNum) -> new Qr(rs.getString("hash"), rs.getBytes("qrByteArray"));

    private final JdbcTemplate jdbc;

    public QrRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Qr findByKey(String id) {
        try {
            return jdbc.queryForObject("SELECT * FROM qrTable WHERE hash=?",
                    rowMapper, id);
        } catch (Exception e) {
            log.debug("When select qr for key {}", id, e);
            return null;
        }
    }

    @Override
    public Qr save(Qr qr) {
        try {
            jdbc.update("INSERT INTO qrTable VALUES (?,?)",
                    qr.getHash(), qr.getQrByteArray());
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate When insert qr for key {}", qr.getHash(), e);
            e.printStackTrace();
            return qr;
        } catch (Exception e) {
            log.debug("When insert qr: ", e);
            e.printStackTrace();
            return null;
        }
        return qr;
    }
}
