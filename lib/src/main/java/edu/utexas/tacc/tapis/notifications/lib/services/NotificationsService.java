package edu.utexas.tacc.tapis.notifications.lib.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.notifications.lib.RabbitMQConnection;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ConstraintViolationException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DuplicateEntityException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Queue;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.OutboundMessageResult;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

import javax.inject.Inject;
import javax.validation.Valid;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


/**
 * This class handles all of the interactions with the persistence of notifications/subscriptions/topics
 * and also the main entry point to push notifications onto the rabbitmq queue.
 */
@Service
public class NotificationsService {

    private final NotificationsDAO notificationsDAO;
    private final Sender sender;
    private final Receiver receiver;
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
    private static final String NOTIFICATIONS_SERVICE_QUEUE_NAME = "tapis.notifications";
//    private final TopicsCache topicsCache;


    @Inject
    public NotificationsService(@NotNull NotificationsDAO notificationsDAO){
        this.notificationsDAO = notificationsDAO;
//        this.topicsCache = topicsCache;
        ConnectionFactory connectionFactory = RabbitMQConnection.getInstance();
        ReceiverOptions receiverOptions = new ReceiverOptions()
            .connectionFactory(connectionFactory)
            .connectionSubscriptionScheduler(Schedulers.newBoundedElastic(8, 1000, "receiver"));
        SenderOptions senderOptions = new SenderOptions()
            .connectionFactory(connectionFactory)
            .connectionSubscriptionScheduler(Schedulers.newBoundedElastic(8, 1000, "sender"));
        receiver = RabbitFlux.createReceiver(receiverOptions);
        sender = RabbitFlux.createSender(senderOptions);
    }

    public Topic getTopic(String tenantId, String topicName) throws ServiceException {
        try {
            return notificationsDAO.getTopicByTenantAndName(tenantId, topicName);
        } catch (DAOException ex) {
            String msg = String.format("Could not get topic %s", topicName);
            throw new ServiceException(msg, ex);
        }
    }

    public List<Topic> getTopicsByTenantAndOwner(String tenantId, String owner) throws ServiceException {
        try {
            return notificationsDAO.getTopicsByTenantAndOwner(tenantId, owner);
        } catch (DAOException ex) {
            String msg = String.format("Could not get topics for user %s", owner);
            throw new ServiceException(msg, ex);
        }
    }

    public List<Subscription> getSubscriptionsForTopic(String tenantId, String topicName) throws ServiceException {
        try {
            return notificationsDAO.getSubscriptionsForTopic(tenantId, topicName);
        } catch (DAOException ex) {
            String msg = String.format("Could not get subscriptions for topic %s", topicName);
            throw new ServiceException(msg, ex);
        }
    }

    public Topic createTopic(Topic topic) throws DuplicateEntityException, ServiceException {
        try {
            return notificationsDAO.createTopic(topic);
        } catch (ConstraintViolationException ex) {
            //Topic name + tenant Id are unique, so this gets called
            // when an attempt to create a duplicate topic happens.
            throw new DuplicateEntityException("Topic with this name already exists", ex);
        } catch (DAOException ex) {
            throw new ServiceException("Could not create topic", ex);
        }
    }

    public void deleteTopic(UUID topicUUID) throws ServiceException {
        try {
            notificationsDAO.deleteTopic(topicUUID);
        } catch (DAOException ex) {
            throw new ServiceException("Could not delete topic", ex);
        }
    }

    public void deleteTopic(String tenantId, String topicName) throws ServiceException {
        try {
            notificationsDAO.deleteTopic(tenantId, topicName);
        } catch (DAOException ex) {
            throw new ServiceException("Could not delete topic", ex);
        }
    }

    public Subscription createSubscription(Topic topic, Subscription subscription) throws ServiceException, DuplicateEntityException {
        try {
            return notificationsDAO.createSubscription(topic, subscription);
        } catch (DAOException ex) {
            throw new ServiceException("Could not subscribe to topic", ex);
        }
    }

    public Subscription getSubscription(UUID subUUID) throws ServiceException {
        try {
            return notificationsDAO.getSubscriptionByUUID(subUUID);
        } catch (DAOException ex) {
            throw new ServiceException("Could not retrieve subscription.", ex);
        }
    }


    public void deleteSubscription(Subscription subscription) throws ServiceException {
        try {
            notificationsDAO.deleteSubscription(subscription.getUuid());
        } catch (DAOException ex) {
            throw new ServiceException("Could not delete topic.");
        }
    }


    /**
     * Send a notification, i.e. put it on the main queue. All notifications sent with this method
     * are set to have a TTL of 7 days.
     * @param notification
     * @throws ServiceException
     */
    public void sendNotification(@Valid Notification notification) throws ServiceException {
        try {
            log.info(notification.toString());
            String m = mapper.writeValueAsString(notification);

            long TTL = Duration.ofDays(7).toMillis();
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .expiration(String.valueOf(TTL))
                .build();
            OutboundMessage message = new OutboundMessage("", NOTIFICATIONS_SERVICE_QUEUE_NAME, properties, m.getBytes());
            Flux<OutboundMessageResult> confirms = sender.sendWithPublishConfirms(Mono.just(message));
            sender.declareQueue(QueueSpecification.queue(NOTIFICATIONS_SERVICE_QUEUE_NAME))
                .thenMany(confirms)
                .subscribe();
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    public Flux<AcknowledgableDelivery> streamNotificationMessages() throws ServiceException {
        ConsumeOptions options = new ConsumeOptions();
        options.qos(1000);
        Flux<AcknowledgableDelivery> messages = receiver.consumeManualAck(NOTIFICATIONS_SERVICE_QUEUE_NAME, options);
        return messages
            .delaySubscription(sender.declareQueue(QueueSpecification.queue(NOTIFICATIONS_SERVICE_QUEUE_NAME)));
    }

    private Mono<Notification> deserializeNotification(Delivery message) {
        try {
            Notification notification = mapper.readValue(message.getBody(), Notification.class);
            return Mono.just(notification);
        } catch (IOException ex) {
            log.error("Could not deserialize message!", ex);
            return Mono.empty();
        }
    }


    /**
     * Re
     * are set to have a TTL of 7 days.
     * @param notification
     * @throws ServiceException
     */
    public void enqueueNotification(@Valid Notification notification, String queueName) throws ServiceException {
        try {
            String internalQueueName = notification.getTenantId() + ":" + queueName;
            String m = mapper.writeValueAsString(notification);
            long TTL = Duration.ofDays(7).toMillis();
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .expiration(String.valueOf(TTL))
                .build();
            OutboundMessage message = new OutboundMessage("", internalQueueName, properties, m.getBytes());
            Flux<OutboundMessageResult> confirms = sender.sendWithPublishConfirms(Mono.just(message));
            sender.declareQueue(QueueSpecification.queue(internalQueueName))
                .thenMany(confirms)
                .subscribe();
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    public Flux<Notification> streamNotificationsOnQueue(String queueName) {
        return receiver.consumeAutoAck(queueName)
            .flatMap(this::deserializeNotification);
    }

    public Queue createQueue(Queue queueSpec) throws ServiceException, DuplicateEntityException {
        try {
            Queue queue = notificationsDAO.createQueue(queueSpec);
            sender.declareQueue(QueueSpecification.queue(queue.getUuid().toString())).subscribe();
            return queue;
        } catch (DAOException ex) {
            throw new ServiceException("Could not create queue.", ex);
        }
    }

}
