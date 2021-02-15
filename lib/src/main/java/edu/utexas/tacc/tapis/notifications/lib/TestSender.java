package edu.utexas.tacc.tapis.notifications.lib;

import edu.utexas.tacc.tapis.notifications.lib.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationDispatcherService;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;
import java.util.UUID;

public class TestSender {

    public static void main(String[] args) throws Exception {

        ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        ServiceLocatorUtilities.bind(new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(TopicsCache.class).in(Singleton.class);
                bindAsContract(NotificationsDAO.class);
                bindAsContract(NotificationsService.class).in(Singleton.class);
                bindAsContract(NotificationDispatcherService.class).in(Singleton.class);
            }
        });

        NotificationsService notificationsService = locator.getService(NotificationsService.class);

        int NUM_TOPICS = 10;

        for (var i=0; i<NUM_TOPICS; i++) {
            Topic topic = new Topic();
            String topicDescription = String.format("test-topic-%s", i);
            topic.setDescription(topicDescription);
            topic.setName(topicDescription);
            topic.setOwner("testuser");
            topic.setTenantId("testtenant");
            notificationsService.createTopic(topic);

            for (var j=0; j<100; j++) {
                Notification notification = new Notification.Builder()
                    .setId(UUID.randomUUID().toString())
                    .setTenantId(topic.getTenantId())
                    .setSource("tapis.files.ops")
                    .setSubject("test-system")
                    .setType("tapis.files.object.deleted")
                    .setTopicName(topic.getName())
                    .build();
                notificationsService.sendNotification(notification);
            }
        }





    }

}
