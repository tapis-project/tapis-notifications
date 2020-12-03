package edu.utexas.tacc.tapis.notifications.lib.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationsDAO {

    private static final Logger log = LoggerFactory.getLogger(NotificationsDAO.class);
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final TypeReference<Map<String, Object>> JsonTypeRef = new TypeReference<>() {};


    private class TopicRowProcessor extends BasicRowProcessor {
        @Override
        public Topic toBean(ResultSet rs, Class type) throws SQLException {
            Topic topic = new Topic();
            topic.setId(rs.getInt("id"));
            topic.setCreated(rs.getTimestamp("created").toInstant());
            topic.setDescription(rs.getString("description"));
            topic.setName(rs.getString("name"));
            topic.setOwner(rs.getString("owner"));
            topic.setUuid( (UUID) rs.getObject("uuid"));
            topic.setTenantId(rs.getString("tenant_id"));

            try {
                topic.setSchema(mapper.readValue(rs.getString("schema"), JsonTypeRef));
            } catch (JsonProcessingException ex) {
                throw new SQLException("Invalid JSON?", ex);
            }
            return topic;
        }
    }


    public Topic getTopicByUUID(UUID topicUUID){
        return null;
    }

    public Topic createTopic(Topic topic) throws DAOException {
        RowProcessor rowProcessor = new TopicRowProcessor();
        try (Connection connection = HikariConnectionPool.getConnection()) {
            BeanHandler<Topic> handler = new BeanHandler<>(Topic.class, rowProcessor);
            String stmt = DAOStatements.CREATE_TOPIC;
            QueryRunner runner = new QueryRunner();
            Topic insertedTopic = runner.query(connection, stmt, handler,
                topic.getTenantId(),
                topic.getName(),
                mapper.writeValueAsString(topic.getSchema()),
                topic.getDescription(),
                topic.getOwner()
            );
            return insertedTopic;
        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage(), ex);
        } catch (JsonProcessingException ex) {
            throw new DAOException("Invalid schema!", ex);
        }

    }

    public List<Subscription> getSubscriptionsForTopic(UUID topicUUID) {
        return null;
    }

    public List<NotificationMechanism> getMechanismsByTopicUUID(UUID topicUUID){
        return null;
    }



}
