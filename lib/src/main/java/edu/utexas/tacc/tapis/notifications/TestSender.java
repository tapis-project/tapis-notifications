package edu.utexas.tacc.tapis.notifications;

import edu.utexas.tacc.tapis.notifications.cache.TopicsCache;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.service.NotificationDispatcherService;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

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
// TODO
//        for (var i=0; i<NUM_TOPICS; i++) {
//            Topic topic = new Topic();
//            String topicDescription = String.format("test-topic-%s", UUID.randomUUID().toString());
//            topic.setDescription(topicDescription);
//            topic.setName(topicDescription);
//            topic.setOwner("testuser");
//            topic.setTenantId("testtenant");
//            topic = notificationsService.createTopic(topic);
//
//            for (var k=0; k<10; k++) {
//                Map<String, Object> filters = new HashMap<>();
//                filters.put("source", "tapis.files.ops");
//                filters.put("subject", "test-system");
//                filters.put("type", "tapis.files.object.deleted");
//                Subscription subscription = new Subscription();
//                subscription.setTenantId(topic.getTenantId());
//                subscription.setTopicId(topic.getId());
//                subscription.setFilters(filters);
//                List<NotificationMechanism> mechs = new ArrayList<>();
//                NotificationMechanism mech = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "tests@test.com");
//                NotificationMechanism mech2 = new NotificationMechanism(NotificationMechanismEnum.WEBHOOK, "https://test.com/path/");
//                mechs.add(mech);
//                mechs.add(mech2);
//                subscription.setMechanisms(mechs);
//                subscription = notificationsService.createSubscription(topic, subscription);
//
//            }
//
//            for (var j=0; j<10; j++) {
//                Notification notification = new Notification.Builder()
//                    .setTopicName(topic.getName())
//                    .setId(UUID.randomUUID().toString())
//                    .setTenantId(topic.getTenantId())
//                    .setSource("tapis.files.ops")
//                    .setSubject("test-system")
//                    .setType("tapis.files.object.deleted")
//                    .setData("{}")
//                    .build();
//                notificationsService.sendNotification(notification);
//            }
//        }
    }

}
