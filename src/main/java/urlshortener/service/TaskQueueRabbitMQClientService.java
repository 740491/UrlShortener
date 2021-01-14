package urlshortener.service;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import urlshortener.repository.ShortURLRepository;

@Service
public class TaskQueueRabbitMQClientService {
    /*
    @Autowired
    private AmqpTemplate template;

    @Autowired
    private Queue safetasksRequest;

    private final ShortURLRepository shortURLRepository;


    public TaskQueueRabbitMQClientService(ShortURLRepository shortURLRepository) {
        this.shortURLRepository = shortURLRepository;
    }

    public void send(String hash, String target) {

        this.template.convertAndSend(safetasksRequest.getName(), new String[]{hash, target});
        System.out.println();
        System.out.println(" [x] Sent "+ safetasksRequest.getName() + "'" + hash+ " " + target + "'");
    }

    @RabbitListener(queues = "safetasksReplies")
    public void getReply(String[] message){
        String hash = message[0];
        Boolean value = Boolean.valueOf(message[1]);
        System.out.println("RECIBIDO EN EL CLIENTE: " + hash + " y " + value);
        shortURLRepository.mark(shortURLRepository.findByKey(hash), value);

    }
     */
}
