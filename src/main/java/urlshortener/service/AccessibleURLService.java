package urlshortener.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@Configuration
@EnableAsync
public class AccessibleURLService {

    private final ShortURLRepository shortURLRepository;

    public AccessibleURLService(ShortURLRepository shortURLRepository) {
        this.shortURLRepository = shortURLRepository;
    }

    @Async
    public void accessible(String hash, String target) {
        Boolean accessible = urlAccessible(target);
        shortURLRepository.updateAccessible(hash, accessible);
    }

    /**
     *
     * @param url String with the url to check if its accessible
     * @return true if the url request gives code 200 in the header, otherwise returns false
     */
    private boolean urlAccessible(String url) {
        try {
            URL urlForGet = new URL(url);
            HttpURLConnection connection;
            connection = (HttpURLConnection) urlForGet.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000); //set timeout to 5 seconds
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println(url + " CODE 200");
                return true;
            } else {
                System.out.print("Error Code:");
                System.out.println(connection.getResponseCode());
                return false;
            }
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("TIMEOUUUUTT");
            return false;
        } catch (IOException e) {
            System.out.println("URL not accesible");
            return false;
        }
    }
}
