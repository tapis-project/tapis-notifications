package edu.utexas.tacc.tapis.notifications.lib;

import reactor.core.publisher.Flux;

public interface INotificationsService {

    void sendNotification(String tenantId, String recipientUser, String creator, String body, String level) throws ServiceException;
    Flux<Notification> streamNotifications();
}
