package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.pojo.Notification;
import reactor.core.publisher.Flux;

public  interface INotificationsService {

    void sendNotification(String routingKey, Notification note) throws ServiceException;
    Flux<Notification> streamNotifications(String bindingKey);
}