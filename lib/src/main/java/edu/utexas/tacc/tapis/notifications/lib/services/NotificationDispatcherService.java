package edu.utexas.tacc.tapis.notifications.lib.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanismEnum;
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
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.AcknowledgableDelivery;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * This service is to be instantiated as a singleton. The dispatcher listens to a
 * stream of incoming messages from the NotificationsService, matches the
 * notification to a subscription for that topic, then ultimately
 * dispatches the notification via the NotificationMechanisms in the subscription.
 */
 @Service @Named @Singleton
public class NotificationDispatcherService {

    private final NotificationsService notificationsService;
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
    private int MAX_THREADS = 50;


    @Inject
    public NotificationDispatcherService(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    private Mono<Notification> deserializeNotification(AcknowledgableDelivery message) {
        try {
            Notification notification = mapper.readValue(message.getBody(), Notification.class);
            message.ack();
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
    public Flux<Notification> processMessages() throws ServiceException {

        Flux<AcknowledgableDelivery> msgStream = notificationsService.streamNotificationMessages();
        return msgStream.groupBy( (m)-> {
            try {
                m.ack();
                return mapper.readValue(m.getBody(), Notification.class).getTenantId();
            } catch (IOException ex) {
                log.error("invalid message", ex);
                return Mono.empty();
            }
        }).flatMap((group) -> {
            Scheduler tenantScheduler = Schedulers.newBoundedElastic(10, 50, group.key().toString());
            return group
                .flatMap(this::deserializeNotification)
                .flatMap(this::matchSubscriptionsByTopic)
                .flatMap(this::dispatchNotification)
                .flatMap(pair -> Mono.fromCallable(() -> this.handleNotification(pair)).subscribeOn(tenantScheduler))
                .flatMap(this::cleanup);
        });


    }

    public Flux<Pair<Notification, Subscription>>   matchSubscriptionsByTopic(@NotNull final Notification notification) {

        // Pull out topic from cache
        // get list of subscriptions for topic in cache
        // Filter the list of subscriptions based on the filters in the subscription
        List<Subscription> subscriptions;
        try {
            subscriptions = notificationsService.getSubscriptionsForTopic(notification.getTenantId(), notification.getTopicName());
        } catch (ServiceException ex) {
            return Flux.empty();
        }
        List<Pair<Notification, Subscription>> out = new ArrayList<>();
        subscriptions.forEach((subscription -> {
            out.add(Pair.of(notification, subscription));
        }));
        log.debug(out.toString());
        return Flux.fromStream(out.stream());
    }

    public Flux<Pair<Notification, NotificationMechanism>> dispatchNotification(@NotNull  Pair<Notification, Subscription> pair) {
        log.info(pair.toString());
        Notification notification = pair.getLeft();
        Subscription subscription = pair.getRight();

        return Flux.fromStream(subscription.getMechanisms().stream().map( mech -> Pair.of(notification, mech)));
    }

    private Notification handleNotification(Pair<Notification, NotificationMechanism> pair) {
        Notification  notification = pair.getLeft();
        NotificationMechanism mech = pair.getRight();

        if (mech.getMechanism().equals(NotificationMechanismEnum.EMAIL)) {
            handleEmail(mech.getTarget());
        } else if (mech.getMechanism().equals(NotificationMechanismEnum.WEBHOOK)) {
            handleWebHook(mech.getTarget());
        } else if (mech.getMechanism().equals(NotificationMechanismEnum.QUEUE)) {
            handleQueue(notification, mech.getTarget());
        } else {
            log.error("Invalid notification mechanism??? {}", mech);
            log.error(notification.toString());
        }
        return notification;
    }


    private void handleWebHook(String url) {
        log.info("sending webhook to {}", url);
        int duration = sleeper(10, 100);
        try {
            Thread.sleep(duration);
            log.info("Slept for {}", duration);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private int sleeper(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min ) + min;
    }

    private void handleEmail(String emailAddress) {
        log.info("sending email to {}", emailAddress);
        int duration = sleeper(10, 100);
        try {
            Thread.sleep(duration);
            log.info("Slept for {}", duration);
        } catch (InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void handleQueue(Notification notification, String queueName) {
        //TODO: Make sure all messages are TTL at 7 days or whatever is decided as TTL
        log.info("dispatching to queue {}", queueName);
        try {
            notificationsService.enqueueNotification(notification, queueName);
        } catch (ServiceException ex) {
            log.error("Could not enqueue notification", ex);
        }

    }


    public Mono<Notification> cleanup(Notification notification) {
        log.debug(notification.toString());
        return Mono.just(notification);
    }


    public void sendEmail() {};

    public void sendWebhook(URI webhookURL) {};

    public void sendToQueue(Queue internalQueue) {};

    public void sendToAbaco() {};






}
