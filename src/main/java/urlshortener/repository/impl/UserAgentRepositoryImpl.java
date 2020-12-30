package urlshortener.repository.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import urlshortener.domain.UserAgent;
import urlshortener.repository.UserAgentRepository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

@Repository
public class UserAgentRepositoryImpl implements UserAgentRepository {
    private static final Logger log = LoggerFactory
            .getLogger(ShortURLRepositoryImpl.class);

    private static final RowMapper<UserAgent> rowMapper =
            (rs, rowNum) -> new UserAgent(rs.getLong("id"),
                    rs.getString("hash"), rs.getString("userAgent"));

    private final JdbcTemplate jdbc;

    public UserAgentRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserAgent save(UserAgent userAgent) {
        try {
            KeyHolder holder = new GeneratedKeyHolder();
            jdbc.update(conn -> {
                PreparedStatement ps = conn
                        .prepareStatement(
                                "INSERT INTO USERAGENT VALUES (?, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS);
                ps.setNull(1, Types.BIGINT);
                ps.setString(2, userAgent.getHash());
                ps.setString(3, userAgent.getUserAgent());
                return ps;
            }, holder);
            if (holder.getKey() != null) {
                new DirectFieldAccessor(userAgent).setPropertyValue("id", holder.getKey()
                        .longValue());
            } else {
                log.debug("Key from database is null");
            }
        } catch (DuplicateKeyException e) {
            log.debug("When insert for click with id " + userAgent.getId(), e);
            return userAgent;
        } catch (Exception e) {
            log.debug("When insert a click", e);
            return null;
        }
        return userAgent;
    }

    @Override
    public List<UserAgent> listAll() {
        try {
            return jdbc.query("SELECT * FROM useragent",
                    rowMapper);
        } catch (Exception e) {
            log.debug("Exception when listAll:", e);
            return Collections.emptyList();
        }
    }
}
