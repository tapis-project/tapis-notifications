package edu.utexas.tacc.tapis.notifications.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.rabbitmq.client.AMQP;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//import reactor.rabbitmq.BindingSpecification;
//import reactor.rabbitmq.ExchangeSpecification;
//import reactor.rabbitmq.OutboundMessage;
//import reactor.rabbitmq.QueueSpecification;
//import reactor.rabbitmq.RabbitFlux;
//import reactor.rabbitmq.Receiver;
//import reactor.rabbitmq.ReceiverOptions;
//import reactor.rabbitmq.Sender;
//import reactor.rabbitmq.SenderOptions;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TapisNotificationsClient implements ITapisNotificationsClient {

//    private static final Logger log = LoggerFactory.getLogger(TapisNotificationsClient.class);
//    private final Receiver receiver;
//    private final Sender sender;
//    private static final String EXCHANGE_NAME = "tapis.notifications";
//    private static final String USER_EXCHANGE_NAME = "tapis.users.notifications";
//    private static final long EXPIRATION = Duration.ofDays(7).toMillis();
//
//    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
//
//    public TapisNotificationsClient() {
//        ConnectionFactory connectionFactory = RabbitMQConnection.getInstance();
//        ReceiverOptions receiverOptions = new ReceiverOptions()
//            .connectionFactory(connectionFactory)
//            .connectionSubscriptionScheduler(Schedulers.newElastic("receiver"));
//        SenderOptions senderOptions = new SenderOptions()
//            .connectionFactory(connectionFactory)
//            .resourceManagementScheduler(Schedulers.newElastic("sender"));
//        receiver = RabbitFlux.createReceiver(receiverOptions);
//        sender = RabbitFlux.createSender(senderOptions);
//        ExchangeSpecification spec = new ExchangeSpecification();
//        spec.durable(true);
//        spec.type("topic");
//        spec.name(EXCHANGE_NAME);
//        sender.declareExchange(spec).subscribe();
//    }
//
//
//    /**
//     * The routing key MUST be in the format of {service}.{tenant}.{eventType}.{OptionalUUID}
//     * @param routingKey
//     * @param note
//     * @return
//     */
//    @Override
//    public Mono<Void> sendNotificationAsync(String routingKey, Notification note) {
//        try {
//            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
//                .expiration(String.valueOf(EXPIRATION))
//                .build();
//            String m = mapper.writeValueAsString(note);
//            OutboundMessage outboundMessage = new OutboundMessage(EXCHANGE_NAME, routingKey, props, m.getBytes());
//            return sender.send(Mono.just(outboundMessage));
//        } catch (IOException ex) {
//            log.error("Could not serialize message, ignoring: {}", note.toString());
//            return Mono.empty();
//        }
//    }
//
//    @Override
//    public void sendNotification(String routingKey, Notification note) throws IOException {
//        sendNotificationAsync(routingKey, note).subscribe();
//    }
//
//    @Override
//    public Flux<Notification> streamNotifications(String bindingKey) {
//        QueueSpecification qspec = new QueueSpecification();
//        Map<String, Object> args = new HashMap<>();
//        args.put("x-expires", Duration.ofDays(7).toMillis());
//        qspec.durable(true);
//        qspec.arguments(args);
//        qspec.name("tapis.notifications." + UUID.randomUUID().toString());
//
//        // Binding the queue to the exchange
//        BindingSpecification bindSpec = new BindingSpecification();
//        bindSpec.exchange(EXCHANGE_NAME);
//        bindSpec.queue(qspec.getName());
//        bindSpec.routingKey(bindingKey);
//
//        //This sets up the call to declare and bind the queue to the exchange. Note, this
//        //is not executed now, but in the delaySubscription() call below.
//        Mono<AMQP.Queue.BindOk> binding = sender.declareQueue(qspec)
//            .then(sender.bindQueue(bindSpec));
//
//        return receiver.consumeAutoAck(qspec.getName())
//            .delaySubscription(binding)
//            .flatMap(this::deserializeNotification);
//
//    }
//
//    private Mono<Notification> deserializeNotification(Delivery message) {
//        try {
//            Notification note = mapper.readValue(message.getBody(), Notification.class);
//            return Mono.just(note);
//        } catch (IOException ex) {
//            log.error("ERROR: Could new deserialize message", ex);
//            return Mono.empty();
//        }
//    }
}