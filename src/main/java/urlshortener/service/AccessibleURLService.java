package urlshortener.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
@Configuration
@EnableAsync
public class AccessibleURLService {

    public AccessibleURLService() {}

    @Async
    public void accessible(String url) {
        try {
            Thread.sleep(5000);
            System.out.println("dentro del asincrono: " + url);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
