package urlshortener.domain;


public class UserAgent {
    private Long id;
    private String hash;
    private String userAgent;

    public UserAgent(Long id, String hash, String userAgent) {
        this.id = id;
        this.hash = hash;
        this.userAgent = userAgent;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
}
