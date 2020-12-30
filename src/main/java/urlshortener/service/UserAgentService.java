package urlshortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import urlshortener.domain.UserAgent;
import urlshortener.repository.UserAgentRepository;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserAgentService {
    private final UserAgentRepository userAgentRepository;

    private String userAgentInfo;

    @Autowired
    public UserAgentService(UserAgentRepository userAgentRepository) {
        this.userAgentRepository = userAgentRepository;
    }

    //Save the User Agent info
    public void extractUserAgent(HttpServletRequest request, String hash) {
        Map<String,String> headersInfo = getHeadersInfo(request);
        UserAgent userAgent = new UserAgent(null, hash, headersInfo.get("user-agent"));
        userAgentRepository.save(userAgent);
        userAgentInfo += userAgent.getUserAgent() + "\n";
    }

    //Return a Map with all the info in the header of the request
    private Map<String, String> getHeadersInfo(HttpServletRequest request) {

        Map<String, String> map = new HashMap<>();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }
    

    public String getUserAgentInfo() {
        return userAgentInfo;
    }

}
