package edu.utexas.tacc.tapis.notifications.lib.services;


import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

@Service
public class NotificationsService {

    private final NotificationsDAO notificationsDAO;

    @Inject
    public NotificationsService(@NotNull NotificationsDAO notificationsDAO){
        this.notificationsDAO = notificationsDAO;
    }

    public Topic createTopic() {
        return null;
    }




}
