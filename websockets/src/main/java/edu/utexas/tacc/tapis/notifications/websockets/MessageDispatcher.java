package edu.utexas.tacc.tapis.notifications.websockets;


import edu.utexas.tacc.tapis.shared.notifications.Notification;
import edu.utexas.tacc.tapis.shared.notifications.TapisNotificationsClient;
import org.jvnet.hk2.annotations.Service;
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

    private TapisNotificationsClient notificationsClient;

    @Inject
    public MessageDispatcher (TapisNotificationsClient notificationsClient) {
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
     *
     * @throws IOException
     */
    public Mono<Void> sendToUser(Notification notification) {
        String routingKey = String.format("%s.%s", notification.getTenant(), notification.getRecipient());
        return notificationsClient.sendUserNotificationAsync(notification);
    }






}
