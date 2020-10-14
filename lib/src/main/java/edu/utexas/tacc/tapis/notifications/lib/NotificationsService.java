package edu.utexas.tacc.tapis.notifications.lib;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.notifications.lib.TapisObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.pojo.Notification;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import java.io.IOException;
import java.util.UUID;

@Service
public class NotificationsService implements INotificationsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
    private final Receiver receiver;
    private final Sender sender;
    private static final String EXCHANGE_NAME = Constants.EXCHANGE_NAME;

    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();

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
        ExchangeSpecification spec = new ExchangeSpecification();
        spec.durable(true);
        spec.type("topic");
        spec.name(EXCHANGE_NAME);
        sender.declareExchange(spec).subscribe();
    }

    @Override
    public void sendNotification(String routingKey, Notification note) throws ServiceException {
        try {
            String m = mapper.writeValueAsString(note);
            OutboundMessage outboundMessage = new OutboundMessage(EXCHANGE_NAME, routingKey, m.getBytes());
            sender.send(Mono.just(outboundMessage)).subscribe();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Flux<Notification> streamNotifications(String bindingKey) {
        QueueSpecification qspec = new QueueSpecification();
        qspec.durable(true);
        qspec.name("tapis.notifications." + UUID.randomUUID().toString());

        // Binding the queue to the exchange
        BindingSpecification bindSpec = new BindingSpecification();
        bindSpec.exchange(EXCHANGE_NAME);
        bindSpec.queue(qspec.getName());
        bindSpec.routingKey(bindingKey);

        //This sets up the call to declare and bind the queue to the exchange. Note, this
        //is not executed now, but in the delaySubscription() call below.
        Mono<AMQP.Queue.BindOk> binding = sender.declareQueue(qspec)
            .then(sender.bindQueue(bindSpec));

        return receiver.consumeAutoAck(qspec.getName())
            .delaySubscription(binding)
            .flatMap(this::deserializeNotification);

    }

    private Mono<Notification> deserializeNotification(Delivery message) {
        try {
            Notification note = mapper.readValue(message.getBody(), Notification.class);
            return Mono.just(note);
        } catch (IOException ex) {
            log.error("ERROR: Could new deserialize message {}", message.getBody());
            return Mono.empty();
        }
    }



}
