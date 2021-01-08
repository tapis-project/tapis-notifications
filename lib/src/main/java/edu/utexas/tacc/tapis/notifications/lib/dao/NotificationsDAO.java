package edu.utexas.tacc.tapis.notifications.lib.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanism;
import edu.utexas.tacc.tapis.notifications.lib.models.NotificationMechanismEnum;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.json.JSONObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationsDAO {

    private static final Logger log = LoggerFactory.getLogger(NotificationsDAO.class);
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final TypeReference<Map<String, Object>> JsonTypeRef = new TypeReference<>() {
    };


    private class TopicRowProcessor extends BasicRowProcessor {
        @Override
        public Topic toBean(ResultSet rs, Class type) throws SQLException {
            Topic topic = new Topic();
            topic.setId(rs.getInt("id"));
            topic.setCreated(rs.getTimestamp("created").toInstant());
            topic.setDescription(rs.getString("description"));
            topic.setName(rs.getString("name"));
            topic.setOwner(rs.getString("owner"));
            topic.setUuid((UUID) rs.getObject("uuid"));
            topic.setTenantId(rs.getString("tenant_id"));

            try {
                topic.setSchema(mapper.readValue(rs.getString("schema"), JsonTypeRef));
            } catch (JsonProcessingException ex) {
                throw new SQLException("Invalid JSON?", ex);
            }
            return topic;
        }
    }

    private class SubscriptionRowProcessor extends BasicRowProcessor {
        @Override
        public Subscription toBean(ResultSet rs, Class type) throws SQLException {
            Subscription subscription = new Subscription();
            subscription.setId(rs.getInt("id"));
            subscription.setCreated(rs.getTimestamp("created").toInstant());
            subscription.setUuid((UUID) rs.getObject("uuid"));
            subscription.setTenantId(rs.getString("tenant_id"));
            subscription.setTopicId(rs.getInt("topic_id"));
            try {
                subscription.setFilters(mapper.readValue(rs.getString("schema"), JsonTypeRef));
            } catch (JsonProcessingException ex) {
                throw new SQLException("Invalid JSON?", ex);
            }
            return subscription;
        }

    }

    private class NotificationMechanismRowProcessor extends BasicRowProcessor {
        @Override
        public NotificationMechanism toBean(ResultSet rs, Class type) throws SQLException {
            NotificationMechanismEnum mechanism = NotificationMechanismEnum.valueOf(rs.getString("mechanism"));
            String target = rs.getString("target");
            NotificationMechanism nm = new NotificationMechanism(mechanism, target);
            nm.setCreated(rs.getTimestamp("created").toInstant());
            nm.setSubscriptionId(rs.getInt("subscription_id"));
            nm.setTenantId(rs.getString("tenant_id"));
            nm.setUuid((UUID) rs.getObject("uuid"));
            return nm;
        }

        @Override
        public List<NotificationMechanism> toBeanList(ResultSet rs, Class type) throws SQLException {
            List<NotificationMechanism> list = new ArrayList<>();
            while (rs.next()) {
                list.add(toBean(rs, type));
            }
            return list;
        }

    }


    public Topic getTopicByUUID(UUID topicUUID) {
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

    public Subscription getSubscriptionByUUID(UUID uuid) throws DAOException {

        try (
            Connection connection = HikariConnectionPool.getConnection();
        ) {
            QueryRunner runner = new QueryRunner();
            SubscriptionRowProcessor rowProcessor = new SubscriptionRowProcessor();
            BeanHandler<Subscription> handler = new BeanHandler<>(Subscription.class, rowProcessor);
            String stmt = DAOStatements.GET_SUBSCRIPTION_BY_UUID;

            Subscription subscription = runner.query(connection, stmt, handler,
                uuid
            );

            RowProcessor mechsRowProcessor = new NotificationMechanismRowProcessor();
            ResultSetHandler<List<NotificationMechanism>> mechsHandler = new BeanListHandler<>(NotificationMechanism.class, mechsRowProcessor);
            String getMechsStmt = DAOStatements.GET_MECHANISMS_FOR_SUBSCRIPTION;
            List<NotificationMechanism> mechs = runner.query(connection, getMechsStmt, mechsHandler,
                subscription.getId()
            );

            subscription.setMechanisms(mechs);
            return subscription;


        } catch (SQLException ex) {
            throw new DAOException("Could not create subscription", ex);
        }
    }


    public Subscription createSubscription(Topic topic, Subscription subscription) throws DAOException {
        try (
            Connection connection = HikariConnectionPool.getConnection();
            PreparedStatement stmt = connection.prepareStatement(DAOStatements.CREATE_SUBSCRIPTION);
            PreparedStatement insertMechanismStmt = connection.prepareStatement(DAOStatements.CREATE_NOTIFICATION_MECHANISM)
        ) {
            stmt.setString(1, subscription.getTenantId());
            stmt.setInt(2, topic.getId());
            stmt.setString(3, mapper.writeValueAsString(subscription.getFilters()));
            ResultSet rs = stmt.executeQuery();
            int subId = rs.getInt("id");
            UUID subUUID = (UUID) rs.getObject("uuid");
            // Save the subscription object

            connection.setAutoCommit(false);
            // Save the notification mechanisms
            for (NotificationMechanism mechanism : subscription.getMechanisms()) {
                insertMechanismStmt.setString(1, subscription.getTenantId());
                insertMechanismStmt.setInt(2, subId);
                insertMechanismStmt.setString(3, mechanism.getMechanism().name());
                insertMechanismStmt.setString(4, mechanism.getTarget());
                insertMechanismStmt.addBatch();
            }
            insertMechanismStmt.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

            // return full subscription with mechanisms
            return getSubscriptionByUUID(subUUID);


        } catch (SQLException ex) {
            throw new DAOException("Could not create subscription", ex);
        } catch (JsonProcessingException ex) {
            throw new DAOException("Could not create subscription, invalid filter JSON", ex);
        }
    }

    public List<Subscription> getSubscriptionsForTopic(UUID topicUUID) throws DAOException {
        try (
            Connection connection = HikariConnectionPool.getConnection();
            PreparedStatement stmt = connection.prepareStatement(DAOStatements.GET_SUBSCRIPTIONS_BY_TOPIC_UUID)
        ) {
            ResultSet rs;
            stmt.setObject(1, topicUUID);
            rs = stmt.executeQuery();
            Map<UUID, Subscription> subMap = new HashMap<>();
            while (rs.next()) {
                UUID subUUID = (UUID) rs.getObject("uuid");
                Subscription sub = subMap.get(subUUID);
                if (sub == null) {
                    sub = new Subscription();
                    sub.setCreated(rs.getTimestamp("created").toInstant());
                    sub.setId(rs.getInt("id"));
                    sub.setTenantId(rs.getString("tenant_id"));
                    sub.setUuid((UUID) rs.getObject("uuid"));
                    sub.setFilters((Map<String, Object>) rs.getObject("filters"));
                    subMap.put(sub.getUuid(), sub);
                }
                NotificationMechanism mechanism = new NotificationMechanism(
                    NotificationMechanismEnum.valueOf(rs.getString("mechanism")),
                    rs.getString("target")
                );
                sub.addMechanism(mechanism);

            }

            return new ArrayList<>(subMap.values());
        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage(), ex);
        }
    }


    public List<NotificationMechanism> getMechanismsByTopicUUID(UUID topicUUID) {
        return null;
    }


}
