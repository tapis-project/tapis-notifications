package edu.utexas.tacc.tapis.notifications.lib.services;


import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Queue;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

@Service
public class NotificationDispatcherService {

    private final NotificationsDAO notificationsDAO;

    @Inject
    public NotificationDispatcherService(NotificationsDAO notificationsDAO) {
        this.notificationsDAO = notificationsDAO;
    }


    public List<Subscription> matchSubscriptions(Notification notification, List<Subscription> subscriptions) {
        return null;
    }

    public List<Subscription> matchSubscriptionsByTopic(@NotNull Notification notification, @NotNull Topic topic) {
        // get list of subscriptions for topic in cache


        return null;
    }


    public void sendEmail() {};

    public void sendWebhook(URI webhookURL) {};

    public void sendToQueue(Queue internalQueue) {};

    public void sendToAbaco() {};






}
