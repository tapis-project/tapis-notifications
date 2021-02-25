package edu.utexas.tacc.tapis.notifications.api.resources;

import edu.utexas.tacc.tapis.notifications.api.models.CreateNotificationRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateSubscriptionRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.api.providers.TopicsAuthz;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanismEnum;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsPermissionsService;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Site;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTestNg;
import org.glassfish.jersey.test.TestProperties;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test(groups = {"integration"})
public class ITestTopicsResource extends JerseyTestNg.ContainerPerClassTest {
    private static final Logger log = LoggerFactory.getLogger(ITestTopicsResource.class);
    private static class TopicResponse extends TapisResponse<Topic> {}
    private static class StringResponse extends TapisResponse<String> {}
    private static class TopicErrorResponse extends TapisResponse<String> {}
    private static class SubscriptionResponse extends TapisResponse<Subscription>{}
    private SKClient skClient;
    private ServiceJWT serviceJWT;

    private TenantManager tenantManager;
    private String user1jwt;
    private String user2jwt;
    private Map<String, Tenant> tenantMap = new HashMap<>();

    private Tenant tenant;
    private Site site;

    @BeforeClass
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @BeforeMethod
    public void doFlywayMigrations() {
        Flyway flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
            .load();
        flyway.clean();
        flyway.migrate();

        tenant = new Tenant();
        tenant.setTenantId("testTenant");
        tenant.setBaseUrl("https://test.tapis.io");
        tenantMap.put(tenant.getTenantId(), tenant);
        site = new Site();
        site.setSiteId("dev");
    }

    @BeforeClass
    public void setUpUsers() throws Exception {
        user1jwt = IOUtils.resourceToString("/user1jwt", StandardCharsets.UTF_8);
        user2jwt = IOUtils.resourceToString("/user2jwt", StandardCharsets.UTF_8);
    }

    @Override
    protected ResourceConfig configure() {
        tenantManager = Mockito.mock(TenantManager.class);
        skClient = Mockito.mock(SKClient.class);
        serviceJWT = Mockito.mock(ServiceJWT.class);
        JWTValidateRequestFilter.setSiteId("test");
        JWTValidateRequestFilter.setService("files");
        ResourceConfig app = new BaseResourceConfig()
            .register(new JWTValidateRequestFilter(tenantManager))
            .register(TopicsAuthz.class)
            .register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(skClient).to(SKClient.class);
                    bind(tenantManager).to(TenantManager.class);
                    bind(serviceJWT).to(ServiceJWT.class);
                    bindAsContract(NotificationsDAO.class);
                    bindAsContract(NotificationsService.class);
                    bindAsContract(NotificationsPermissionsService.class);
                }
            });

        app.register(TopicsResource.class);
        return app;
    }


    @DataProvider(name = "topicNameProvider")
    public Object[] topicNameProvider() {
        return new String[]{"test.topic.1", "test"};
    }

    // Only a-Z . - _ 0-9 chars are eligible
    @DataProvider(name = "badTopicNameProvider")
    public Object[] badTopicNameProvider() {
        return new String[]{"~Bad", "/bad", "*bad", "#bad"};
    }



    @BeforeMethod
    public void initMocks() throws Exception {
        when(tenantManager.getTenants()).thenReturn(tenantMap);
        when(tenantManager.getTenant(any())).thenReturn(tenant);
        when(tenantManager.getSite(any())).thenReturn(site);
    }


    private void deleteTopic(String userJWT, String topicName) {
        StringResponse resp = target("/v3/notifications/topics/" + topicName)
            .request()
            .header("x-tapis-token", userJWT)
            .delete(StringResponse.class);
    }



    private Topic createTopic(String userJWT, String topicName) {
        CreateTopicRequest request = new CreateTopicRequest();
        request.setDescription("test topic description");
        request.setName(topicName);

        TopicResponse resp = target("/v3/notifications/topics")
            .request()
            .header("x-tapis-token", userJWT)
            .post(Entity.json(request), TopicResponse.class);
        return resp.getResult();
    }

    private Topic getTopic(String userJWT, String topicName) {
        TopicResponse resp = target("/v3/notifications/topics/" + topicName)
            .request()
            .header("x-tapis-token", userJWT)
            .get(TopicResponse.class);
        return resp.getResult();
    }

    private Subscription createSubscription(String userJWT, String topicName, CreateSubscriptionRequest req) {
        SubscriptionResponse resp =  target("/v3/notifications/topics/" + topicName + "/subscriptions")
            .request()
            .header("x-tapis-token", userJWT)
            .post(Entity.json(req), SubscriptionResponse.class);
        return resp.getResult();
    }

    private void deleteSubscription(String userJWT, String topicName, Subscription sub) {
        StringResponse resp = target("/v3/notifications/topics/" + topicName + "/subscriptions/" + sub.getUuid().toString())
            .request()
            .header("x-tapis-token", userJWT)
            .delete(StringResponse.class);
    }


    @Test
    public void createTopic() throws Exception{
        Topic topic = createTopic(user1jwt, "TEST-TOPIC");
        Assert.assertNotNull(topic.getCreated());
        Assert.assertNotNull(topic.getId());
        Assert.assertNotNull(topic.getDescription());
    }


    @Test(dataProvider = "topicNameProvider")
    public void testGetTopic(String topicName) throws Exception {
        Topic topic = createTopic(user1jwt, topicName);
        Topic returnedTopic = getTopic(user1jwt, topicName);
        Assert.assertEquals(topic.getUuid(), returnedTopic.getUuid());
    }

    @Test(dataProvider = "badTopicNameProvider")
    public void testCreateBad(String topicName) throws Exception {
        CreateTopicRequest request = new CreateTopicRequest();
        request.setDescription("test topic description");
        request.setName(topicName);

        Assert.assertThrows(BadRequestException.class, ()-> {
            TopicResponse resp = target("/v3/notifications/topics")
                .request()
                .header("x-tapis-token", user1jwt)
                .post(Entity.json(request), TopicResponse.class);
        });
    }

    @Test
    public void testCreateSubscription() throws Exception {
        Topic topic = createTopic(user1jwt, "test.topic");
        CreateSubscriptionRequest subReq = new CreateSubscriptionRequest();
        List<NotificationMechanism> mechs = new ArrayList<>();
        NotificationMechanism mech = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@good.com");;
        mechs.add(mech);
        subReq.setNotificationMechanisms(mechs);
        subReq.setFilter("{}");
        createSubscription(user1jwt, "test.topic", subReq);

    }

    @Test
    public void testDeleteTopic() {
        Topic topic = createTopic(user1jwt, "test.topic");
        deleteTopic(user1jwt, "test.topic");
    }

    @Test
    public void testDeleteSubscription() {
        Topic topic = createTopic(user1jwt, "test.topic");
        CreateSubscriptionRequest subReq = new CreateSubscriptionRequest();
        List<NotificationMechanism> mechs = new ArrayList<>();
        NotificationMechanism mech = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@good.com");;
        mechs.add(mech);
        subReq.setNotificationMechanisms(mechs);
        subReq.setFilter("{}");
        Subscription sub =  createSubscription(user1jwt, "test.topic", subReq);
        deleteSubscription(user1jwt, "test.topic", sub);
    }


    private void sendNotification(String userJWT, String topicName, CreateNotificationRequest request) {
        StringResponse resp = target("/v3/notifications/topics/" + topicName )
            .request()
            .header("x-tapis-token", userJWT)
            .post(Entity.json(request), StringResponse.class);
    }

    @Test
    public void testSendAndReceiveMessages() throws Exception {
        createTopic(user1jwt, "test.topic");
        Client client = ClientBuilder.newBuilder()
            .register(SseFeature.class).build();
        WebTarget target = client.target("/v3/notifications/test.topic/messages");
        EventSource eventSource = EventSource.target(target).build();
        EventListener listener = new EventListener() {
            @Override
            public void onEvent(InboundEvent inboundEvent) {
                log.info(inboundEvent.toString());
            }
        };
        eventSource.register(listener, "message-to-client");
        eventSource.open();

        for (var i=0;i<10;i++){

            CreateNotificationRequest request = new CreateNotificationRequest();
            request.setId(String.valueOf(i));
            request.setData("{}");
            request.setSource("test.source");
            request.setType("test.type");
            request.setSubject("test.subject");
            sendNotification(user1jwt, "test.topic", request);
        }
        eventSource.close();

    }


}
