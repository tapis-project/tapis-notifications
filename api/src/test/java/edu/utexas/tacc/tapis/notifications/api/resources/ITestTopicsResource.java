package edu.utexas.tacc.tapis.notifications.api.resources;

import edu.utexas.tacc.tapis.notifications.api.models.CreateSubscriptionRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanismEnum;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Site;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTestNg;
import org.glassfish.jersey.test.TestProperties;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.inject.Singleton;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Test(groups = {"integration"})
public class ITestTopicsResource extends JerseyTestNg.ContainerPerClassTest {
    private static final Logger log = LoggerFactory.getLogger(ITestTopicsResource.class);
    private static class TopicResponse extends TapisResponse<Topic> {}
    private static class TopicErrorResponse extends TapisResponse<String> {}
    private static class SubscriptionResponse extends TapisResponse<Subscription>{};
    private SKClient skClient;
    private ServiceJWT serviceJWT;

    protected TenantManager tenantManager;
    protected String user1jwt;
    protected String user2jwt;
    protected Map<String, Tenant> tenantMap = new HashMap<>();

    protected Tenant tenant;
    protected Site site;

    public ITestTopicsResource() {
        tenant = new Tenant();
        tenant.setTenantId("testTenant");
        tenant.setBaseUrl("https://test.tapis.io");
        tenantMap.put(tenant.getTenantId(), tenant);
        site = new Site();
        site.setSiteId("dev");
    }

    @BeforeTest
    public void doFlywayMigrations() {
        Flyway flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
            .load();
        flyway.clean();
        flyway.migrate();
    }

    @BeforeClass
    public void setUpUsers() throws Exception {
        user1jwt = IOUtils.resourceToString("/user1jwt", StandardCharsets.UTF_8);
        user2jwt = IOUtils.resourceToString("/user2jwt", StandardCharsets.UTF_8);
    }

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        forceSet(TestProperties.CONTAINER_PORT, "0");
        tenantManager = Mockito.mock(TenantManager.class);
        skClient = Mockito.mock(SKClient.class);
        serviceJWT = Mockito.mock(ServiceJWT.class);
        JWTValidateRequestFilter.setSiteId("test");
        JWTValidateRequestFilter.setService("files");
        ResourceConfig app = new BaseResourceConfig()
            .register(new JWTValidateRequestFilter(tenantManager))
            .register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(skClient).to(SKClient.class);
                    bind(tenantManager).to(TenantManager.class);
                    bind(serviceJWT).to(ServiceJWT.class);
                    bindAsContract(NotificationsDAO.class);
                    bindAsContract(NotificationsService.class);
                }
            });

        app.register(TopicsResource.class);
        return app;
    }


    @DataProvider(name = "topicNameProvider")
    public Object[] topicNameProvider() {
        return new String[]{"test.topic.1", "test"};
    }


    @BeforeMethod
    public void beforeTest() throws Exception {
        when(tenantManager.getTenants()).thenReturn(tenantMap);
        when(tenantManager.getTenant(any())).thenReturn(tenant);
        when(tenantManager.getSite(any())).thenReturn(site);
    }



    private Topic createTopic(String topicName) {
        CreateTopicRequest request = new CreateTopicRequest();
        request.setDescription("test topic description");
        request.setName(topicName);

        TopicResponse resp = target("/v3/notifications/topics")
            .request()
            .header("x-tapis-token", user1jwt)
            .post(Entity.json(request), TopicResponse.class);
        return resp.getResult();
    }

    @Test
    public void createTopic() throws Exception{

        Topic topic = createTopic("TEST-TOPIC");
        Assert.assertNotNull(topic.getCreated());
        Assert.assertNotNull(topic.getId());
        Assert.assertNotNull(topic.getDescription());
    }



    @Test(dataProvider = "topicNameProvider")
    public void testGetTopic(String topicName) throws Exception {
        Topic topic = createTopic(topicName);
        TopicResponse resp = target("/v3/notifications/topics/" + topicName)
            .request()
            .header("x-tapis-token", user1jwt)
            .get(TopicResponse.class);
        Assert.assertEquals(resp.getResult().getName(), topicName);
    }

    @Test
    public void testCreateSubscription() throws Exception {
        Topic topic = createTopic("test.topic");
        CreateSubscriptionRequest subReq = new CreateSubscriptionRequest();
        List<NotificationMechanism> mechs = new ArrayList<>();
        NotificationMechanism mech = new NotificationMechanism(NotificationMechanismEnum.EMAIL, "test@good.com");;
        mechs.add(mech);
        subReq.setNotificationMechanisms(mechs);
        subReq.setFilter("{}");
        SubscriptionResponse resp = target("/v3/notifications/topics/test.topic/subscriptions")
            .request()
            .header("x-tapis-token", user1jwt)
            .post(Entity.json(subReq), SubscriptionResponse.class);

    }

    @Test
    public void testDeleteTopic() {

    }

    @Test
    public void testDeleteSubscription() {

    }


    @Test
    public void testSendAndReceiveMessages() {

    }


}
