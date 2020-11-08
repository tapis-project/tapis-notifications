package edu.utexas.tacc.tapis.notifications.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.shared.notifications.Notification;
import edu.utexas.tacc.tapis.shared.notifications.NotificationsConstants;
import edu.utexas.tacc.tapis.shared.notifications.RabbitMQConnection;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;


@Service
public class UserNotificationService {

    private final Receiver receiver;
    private final Sender sender;
    private static final String USER_EXCHANGE_NAME = NotificationsConstants.USER_NOTIFICATIONS_EXCHANGE;
    private static final long EXPIRATION = Duration.ofDays(7).toMillis();
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);

    public UserNotificationService() {
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
        spec.name(USER_EXCHANGE_NAME);
        sender.declareExchange(spec).subscribe();
    }

    public Mono<Void> sendUserNotificationAsync(Notification note) {
        String routingKey = String.format("%s.%s", note.getTenant(), note.getRecipient());
        UserNotification userNotification = new UserNotification.Builder()
            .setCreated(note.getCreated())
            .setLevel(note.getLevel())
            .setMessage(note.getBody())
            .build();

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .expiration(String.valueOf(EXPIRATION))
                .build();
            String m = mapper.writeValueAsString(userNotification);
            OutboundMessage outboundMessage = new OutboundMessage(USER_EXCHANGE_NAME, routingKey, props, m.getBytes());
            return sender.send(Mono.just(outboundMessage));
        } catch (IOException ex) {
            log.error("Could not serialize message, ignoring: {}", note.toString());
            return Mono.empty();
        }
    }

    public Flux<UserNotification> streamUserNotifications(AuthenticatedUser user) {
        QueueSpecification qspec = new QueueSpecification();
        // Queue name is same as binding key
        String bindingKey = MessageFormat.format("{0}.{1}", user.getTenantId(), user.getName());
        qspec.durable(true);
        qspec.name(bindingKey);

        // Binding the queue to the exchange
        BindingSpecification bindSpec = new BindingSpecification();
        bindSpec.exchange(USER_EXCHANGE_NAME);
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

    private Mono<UserNotification> deserializeNotification(Delivery message) {
        try {
            UserNotification note = mapper.readValue(message.getBody(), UserNotification.class);
            return Mono.just(note);
        } catch (IOException ex) {
            log.error("ERROR: Could new deserialize message {}", message.getBody());
            return Mono.empty();
        }
    }

    private void sendUserEmail(String emailAddress) {

    }

}
