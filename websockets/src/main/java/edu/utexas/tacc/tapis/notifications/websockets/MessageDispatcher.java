package edu.utexas.tacc.tapis.notifications.websockets;


import edu.utexas.tacc.tapis.shared.notifications.Notification;
import edu.utexas.tacc.tapis.shared.notifications.NotificationMechanism;
import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.io.IOException;


/**
 * Takes *all* messages from the tapis.notifications
 * and creates topics for end users to pull from in the
 * websocket endpoints
 */
@Service
public class MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

    private final TapisNotificationsClient notificationsClient;
    private final UserNotificationService userNotificationService;

    @Inject
    public MessageDispatcher (TapisNotificationsClient notificationsClient, UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
        this.notificationsClient = notificationsClient;
    }


    /**
     * Subscribes to a Flux<Notification> and sends them on a new new routing key for each user:
     * in the format {tenant}.{username}
     * @throws IOException
     */
    public Flux<Void> dispatchMessages() {
        return notificationsClient.streamNotifications("#")
            .flatMap(this::sendToUser);
    }

    /**
     * Puts a message in the users notifications message queue, from which they will get it via websockets
     * @param notification
     * @return
     */
    public Mono<Void> sendToUser(Notification notification) {
        if (notification.getNotificationMechanism() != null) {
            sendNotificationViaMechanism(notification);
        }
        return userNotificationService.sendUserNotificationAsync(notification);
    }


    private void sendNotificationViaMechanism(Notification notification) {
        NotificationMechanism mechanism = notification.getNotificationMechanism();
        if (notification.getNotificationMechanism() != null && mechanism.getMechanism().equals("email")) {
            sendEmail(mechanism.getEmailAddress());
        }

        if (mechanism.getMechanism().equals("webhook")) {
            sendWebhook(mechanism.getWebhookURL());
        }

    }

    private void sendEmail(String emailAddress) {

    }

    private void sendWebhook(String webhookURL) {

    }




}
