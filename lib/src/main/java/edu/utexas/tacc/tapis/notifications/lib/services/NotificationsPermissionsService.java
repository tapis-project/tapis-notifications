package edu.utexas.tacc.tapis.notifications.lib.services;


import edu.utexas.tacc.tapis.notifications.lib.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class NotificationsPermissionsService {

    private NotificationsDAO notificationsDAO;

    @Inject
    public NotificationsPermissionsService(NotificationsDAO notificationsDAO) {
        this.notificationsDAO = notificationsDAO;
    }

    // Format is notifications:{tenantId}:{r,rw}:{topic
    private static final String PERMSSPEC = "notifications:%s:%s:%s";

    public boolean isPermitted(String tenantId, String topicName, String username, String accountType) throws ServiceException {
      // TODO
//        if (accountType.equals("service")) return true;
//        try {
//            Topic topic = notificationsDAO.getTopicByTenantAndName(tenantId, topicName);
//            if (topic == null) return false;
//            return topic.getOwner().equals(username);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not retrieve topic", ex);
//        }
      return false;
    }

    public boolean isPermitted(Topic topic, String tenantId, String username, String accountType) {
        if (accountType.equals("service")) return true;
        return  (topic.getOwner().equals(username)) && (topic.getTenantId().equals(tenantId));
    }

}
