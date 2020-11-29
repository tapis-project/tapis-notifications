package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface ITapisNotificationsClient {

    Mono<Void> sendNotificationAsync(String routingKey, Notification note);
    void sendNotification(String routingKey, Notification note) throws IOException;
    Flux<Notification> streamNotifications(String bindingKey);
}
