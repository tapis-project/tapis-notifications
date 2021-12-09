package edu.utexas.tacc.tapis.notifications.lib.dao;

import org.testng.annotations.Test;

@Test(groups = {"integration"})
public class ITestNotificationsDAO {
//
//    @BeforeMethod
//    public void doFlywayMigrations() {
//        Flyway flyway = Flyway.configure()
//            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
//            .load();
//        flyway.clean();
//        flyway.migrate();
//    }
//
//
//    private Topic createTopic() throws Exception{
//        ObjectMapper mapper = TapisObjectMapper.getMapper();
//        String schema = IOUtils.toString(this.getClass().getResource("/schema1.json"),"UTF-8");
//        Map<String, Object> schemaObject = mapper.readValue(schema, new TypeReference<>() {});
//        NotificationsDAO dao = new NotificationsDAO();
//        Topic topic = new Topic();
//        topic.setTenantId("testTenant");
//        topic.setName("test-topic");
//        topic.setDescription("test description");
//        topic.setOwner("testUser1");
//
//        Topic newTopic = dao.createTopic(topic);
//        return newTopic;
//
//    }
//
//    @Test
//    public void testCreateTopic() throws Exception {
//        Topic newTopic = createTopic();
//        Assert.assertNotNull(newTopic.getUuid());
//        Assert.assertEquals(newTopic.getName(), "test-topic");
//    }
//
//
//    @Test
//    public void testCreateSubscription() throws Exception {
//        Topic topic = createTopic();
//        Subscription subscription = new Subscription();
//        NotificationMechanism mech1 = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@test.com");
//        List<NotificationMechanism> mechanisms = new ArrayList<>();
//        mechanisms.add(mech1);
//        subscription.setMechanisms(mechanisms);
//        subscription.setTenantId(topic.getTenantId());
//        NotificationsDAO dao = new NotificationsDAO();
//        Subscription sub = dao.createSubscription(topic, subscription);
//        Assert.assertNotNull(sub.getUuid());
//        Assert.assertNotNull(sub.getCreated());
//        Assert.assertNotNull(sub.getMechanisms());
//        Assert.assertEquals(sub.getTopicId(), topic.getId());
//
//    }
//
//    @Test
//    public void testMultipleMechanisms() throws Exception {
//        Topic topic = createTopic();
//        Subscription subscription = new Subscription();
//        NotificationMechanism mech1 = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@test.com");
//        NotificationMechanism mech2 = new NotificationMechanism(NotificationMechanismEnum.QUEUE, "213dsfs34");
//        List<NotificationMechanism> mechanisms = new ArrayList<>();
//        mechanisms.add(mech1);
//        mechanisms.add(mech2);
//        subscription.setMechanisms(mechanisms);
//        subscription.setTenantId(topic.getTenantId());
//        NotificationsDAO dao = new NotificationsDAO();
//        Subscription sub = dao.createSubscription(topic, subscription);
//        Assert.assertNotNull(sub.getUuid());
//        Assert.assertNotNull(sub.getCreated());
//        Assert.assertNotNull(sub.getMechanisms());
//        Assert.assertEquals(sub.getTopicId(), topic.getId());
//        Assert.assertEquals(sub.getMechanisms().size(), 2);
//    }
//
//    @Test
//    public void testGetAllSubscriptionsForTopic() throws Exception {
//        NotificationsDAO dao = new NotificationsDAO();
//        Topic topic = createTopic();
//        Subscription subscription = new Subscription();
//        List<NotificationMechanism> mechanisms = new ArrayList<>();
//        for (var i=0; i<5; i++) {
//            NotificationMechanism mech1 = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@test.com");
//            mechanisms.add(mech1);
//            subscription.setMechanisms(mechanisms);
//            subscription.setTenantId(topic.getTenantId());
//            Subscription sub = dao.createSubscription(topic, subscription);
//        }
//        List<Subscription> subs = dao.getSubscriptionsForTopic(topic.getTenantId(), topic.getName());
//        Assert.assertEquals(subs.size(), 5);
//
//
//    }
}
