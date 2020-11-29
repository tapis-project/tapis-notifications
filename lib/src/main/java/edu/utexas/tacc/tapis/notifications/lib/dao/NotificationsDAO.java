package edu.utexas.tacc.tapis.notifications.lib.dao;


import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationsDAO {

    private static final Logger log = LoggerFactory.getLogger(NotificationsDAO.class);



    private class SubscriptionRowProcessor extends BasicRowProcessor {

    }


    public Topic getTopicByUUID(UUID topicUUID){
        return null;
    }

    public List<Subscription> getSubscriptionsForTopic(UUID topicUUID) {
        return null;
    }

    public List<NotificationMechanism> getMechanismsByTopicUUID(UUID topicUUID){
        return null;
    }



}
