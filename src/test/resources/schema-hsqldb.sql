-- Clean database

DROP TABLE SHORTURL IF EXISTS CASCADE;
DROP TABLE QRTABLE IF EXISTS CASCADE;
DROP TABLE CLICK IF EXISTS CASCADE;
-- ShortURL

CREATE TABLE SHORTURL
(
    HASH        VARCHAR(30) PRIMARY KEY, -- Key
    TARGET      VARCHAR(1024),           -- Original URL
    SPONSOR     VARCHAR(1024),           -- Sponsor URL
    CREATED     TIMESTAMP,               -- Creation date
    OWNER       VARCHAR(255),            -- User id
    MODE        INTEGER,                 -- Redirect mode
    SAFE        BOOLEAN,                 -- Safe target
    IP          VARCHAR(20),             -- IP
    COUNTRY     VARCHAR(50),             -- Country
    ACCESSIBLE  BOOLEAN,                 -- Accesible URL
);

-- QRTABLE

CREATE TABLE QRTABLE
(
    HASH    VARCHAR(10) NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH), -- Key
    QRBYTEARRAY varbinary(1024)
);

-- Click

CREATE TABLE CLICK
(
    ID       BIGINT IDENTITY,                                             -- KEY
    HASH     VARCHAR(10) NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH), -- Foreing key
    CREATED  TIMESTAMP,                                                   -- Creation date
    REFERRER VARCHAR(1024),                                               -- Traffic origin
    BROWSER  VARCHAR(50),                                                 -- Browser
    PLATFORM VARCHAR(50),                                                 -- Platform
    IP       VARCHAR(20),                                                 -- IP
    COUNTRY  VARCHAR(50)                                                  -- Country
)