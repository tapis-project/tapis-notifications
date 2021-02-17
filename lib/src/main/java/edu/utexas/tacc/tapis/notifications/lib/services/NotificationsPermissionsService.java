package edu.utexas.tacc.tapis.notifications.lib.services;


import edu.utexas.tacc.tapis.notifications.lib.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class NotificationsPermissionsService {

    private TopicsCache topicsCache;

    @Inject
    public NotificationsPermissionsService(TopicsCache topicsCache) {
        this.topicsCache = topicsCache;
    }

    // Format is notifications:{tenantId}:{r,rw}:{topic
    private static final String PERMSSPEC = "notifications:%s:%s:%s";

    public boolean isPermitted(String tenantId, String topicName, String username, String accountType) throws ServiceException {
        if (accountType.equals("service")) return true;
        Topic topic = topicsCache.getTopic(tenantId, topicName);
        return topic.getOwner().equals(username);
    }


}
