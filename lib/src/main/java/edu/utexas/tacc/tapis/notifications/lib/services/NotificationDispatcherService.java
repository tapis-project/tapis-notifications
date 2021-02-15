package edu.utexas.tacc.tapis.notifications.lib.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Queue;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.AcknowledgableDelivery;

import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.URI;
import java.util.List;


/**
 * This service is to be instantiated as a singleton. The dispatcher listens to a
 * stream of incoming messages from the NotificationsService, matches the
 * notification to a subscription for that topic, then ultimately
 * dispatches the notification via the NotificationMechanisms in the subscription.
 */
 @Service
public class NotificationDispatcherService {

    private final NotificationsService notificationsService;
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
    private int MAX_THREADS = 10;


    @Inject
    public NotificationDispatcherService(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    private Mono<Notification> deserializeNotification(AcknowledgableDelivery message) {
        try {
            Notification notification = mapper.readValue(message.getBody(), Notification.class);
            return Mono.just(notification);
        } catch (IOException ex) {
            message.nack(false);
            log.error("Could not deserialize message!", ex);
            return Mono.empty();
        }
    }


    /**
     * This is the main entrypoint for the class. This returns a Flux<?> events for each message that is processed. This
     * method must be subscribed to in order to start the listening. The main stream of messages are split into groups/rails
     * for each tenant, and they are executed in parallel in their own scheduler with a concurrency of MAX_THREADS
     * @return
     * @throws ServiceException
     */
    public Flux<Void> processMessages() throws ServiceException {

        Flux<AcknowledgableDelivery> msgStream = notificationsService.streamNotificationMessages();
        return msgStream.groupBy( (m)-> {
            try {
                return mapper.readValue(m.getBody(), Notification.class).getTenantId();
            } catch (IOException ex) {
                log.error("invalid message", ex);
                return Mono.empty();
            }
        }).flatMap((group) -> group
           .parallel()
           .runOn(Schedulers.newBoundedElastic(MAX_THREADS, 10, "Notifications-TenantPool:" + group.key()))
           .flatMap(this::deserializeNotification)
           .flatMap(this::matchSubscriptionsByTopic)
           .flatMap(this::dispatchNotification)
           .flatMap(this::cleanup));

    }

    public Flux<Notification> dispatchNotification(@NotNull  Pair<Notification, Subscription> pair) {
        log.info(pair.toString());
        Notification notification = pair.getLeft();
        Subscription subscription = pair.getRight();

        return Flux.empty();

    }

    public Flux<Pair<Notification, Subscription>>   matchSubscriptionsByTopic(@NotNull Notification notification) {

        // Pull out topic from cache
        // get list of subscriptions for topic in cache
        // Filter the list of subscriptions based on the filters in the subscription
        log.debug(notification.toString());
        return Flux.empty();
    }

    public Mono<Void> cleanup(Notification notification) {
        log.debug(notification.toString());
        return Mono.empty();
    }


    public void sendEmail() {};

    public void sendWebhook(URI webhookURL) {};

    public void sendToQueue(Queue internalQueue) {};

    public void sendToAbaco() {};






}
