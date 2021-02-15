package edu.utexas.tacc.tapis.notifications.lib.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import edu.utexas.tacc.tapis.notifications.lib.RabbitMQConnection;
import edu.utexas.tacc.tapis.notifications.lib.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public List<Subscription> getSubscriptionsForTopic(UUID topicUUID) throws ServiceException {
        try {
           return notificationsDAO.getSubscriptionsForTopic(topicUUID);
        } catch (DAOException ex) {
            String msg = String.format("Could not get subscriptions for topic %s", topicUUID.toString());
            throw new ServiceException(msg, ex);
        }
    }

    public Topic createTopic(Topic topic) throws ServiceException {
        try {
            return notificationsDAO.createTopic(topic);
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

    public Subscription subscribeToTopic(Subscription subscription) {
        return null;
    }

    public void sendNotification(@Valid Notification notification) throws ServiceException {
        try {
            String m = mapper.writeValueAsString(notification);
            OutboundMessage message = new OutboundMessage("", NOTIFICATIONS_SERVICE_QUEUE_NAME, m.getBytes());
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
        Flux<AcknowledgableDelivery> childMessageStream = receiver.consumeManualAck(NOTIFICATIONS_SERVICE_QUEUE_NAME, options);
        return childMessageStream
            .delaySubscription(sender.declareQueue(QueueSpecification.queue(NOTIFICATIONS_SERVICE_QUEUE_NAME)));
    }




}
