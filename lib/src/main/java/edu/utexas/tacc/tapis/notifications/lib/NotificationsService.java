package edu.utexas.tacc.tapis.notifications.lib;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.files.lib.json.TapisObjectMapper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import javax.inject.Inject;
import java.io.IOException;

@Service
public class NotificationsService implements INotificationsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
    private final Receiver receiver;
    private final Sender sender;
    private static final String EXCHANGE_NAME = "notifications";

    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();

    @Inject
    public NotificationsService() {
        ConnectionFactory connectionFactory = RabbitMQConnection.getInstance();
        ReceiverOptions receiverOptions = new ReceiverOptions()
            .connectionFactory(connectionFactory)
            .connectionSubscriptionScheduler(Schedulers.newElastic("receiver"));
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(connectionFactory)
            .resourceManagementScheduler(Schedulers.newElastic("sender"));
        receiver = RabbitFlux.createReceiver(receiverOptions);
        sender = RabbitFlux.createSender(senderOptions);
        sender.declareExchange(ExchangeSpecification.exchange(EXCHANGE_NAME));
    }

    @Override
    public void sendNotification(String tenantId, String recipientUser, String creator, String body, String level) throws ServiceException {
        try {
            Notification note = new Notification(tenantId, recipientUser, creator, body, level);
            String m = mapper.writeValueAsString(note);
            OutboundMessage outboundMessage = new OutboundMessage(EXCHANGE_NAME, );
            sender.send(Mono.just(outboundMessage)).subscribe();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Flux<Notification> streamNotifications() {
        ConsumeOptions options = new ConsumeOptions();
        options.qos(1000);
        Flux<Delivery> notificationStream = receiver.consumeAutoAck(QUEUE_NAME, options);
        return notificationStream
            .delaySubscription(sender.declareQueue(QueueSpecification.queue(QUEUE_NAME)))
            .flatMap(this::deserializeNotification);

    }

    private Mono<Notification> deserializeNotification(Delivery message) {
        try {
            Notification note = mapper.readValue(message.getBody(), Notification.class);
            return Mono.just(note);
        } catch (IOException ex) {
            log.error("ERROR: Could new deserialize message");
            return Mono.empty();
        }
    }



}
