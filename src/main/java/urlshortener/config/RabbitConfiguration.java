package urlshortener.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import urlshortener.repository.ShortURLRepository;
import urlshortener.service.RabbitMQSafeServerService;
import urlshortener.service.TaskQueueRabbitMQClientService;

@ComponentScan("urlshortener.service")
@ComponentScan("urlshortener.repository")
@Configuration
public class RabbitConfiguration {

/*
    @Bean
    public Queue safetasksRequest() {
        return new Queue("safetasksRequest");
    }
    @Bean
    public Queue safetasksReplies() {
        return new Queue("safetasksReplies");
    }

    @Autowired
    private final ShortURLRepository shortURLRepository;

    public RabbitConfiguration(ShortURLRepository shortURLRepository) {
        this.shortURLRepository = shortURLRepository;
    }


    @Bean
    public TaskQueueRabbitMQClientService taskQueueService(){return new TaskQueueRabbitMQClientService(shortURLRepository);}

    @Bean
    public RabbitMQSafeServerService server(){ return new RabbitMQSafeServerService();}

 */
}
