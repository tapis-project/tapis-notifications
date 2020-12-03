package edu.utexas.tacc.tapis.notifications.lib.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Test(groups = {"integration"})
public class ITestNotificationsDAO {

    @BeforeMethod
    public void doFlywayMigrations() {
        Flyway flyway = Flyway.configure()
            .dataSource("jdbc:postgresql://localhost:5432/test", "test", "test")
            .load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    public void testCreateTopic() throws Exception {
        ObjectMapper mapper = TapisObjectMapper.getMapper();
        String schema = IOUtils.toString(this.getClass().getResource("/schema1.json"),"UTF-8");
        Map<String, Object> schemaObject = mapper.readValue(schema, new TypeReference<>() {});
        NotificationsDAO dao = new NotificationsDAO();
        Topic topic = new Topic();
        topic.setTenantId("testTenant");
        topic.setName("test-topic");
        topic.setSchema(schemaObject);
        topic.setDescription("test description");
        topic.setOwner("testUser1");

        Topic newTopic = dao.createTopic(topic);
        Assert.assertNotNull(newTopic.getUuid());
        Assert.assertEquals(newTopic.getName(), "test-topic");
        Assert.assertNotNull(newTopic.getSchema());

    }

}
