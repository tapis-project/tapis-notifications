package edu.utexas.tacc.tapis.notifications.lib.cache;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.DAOException;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


@Service
public class TopicsCache {

    private static final Logger log = LoggerFactory.getLogger(TopicsCache.class);
    private final LoadingCache<CacheKey, Topic> cache;
    private NotificationsDAO dao;

    @Inject
    public TopicsCache(NotificationsDAO dao) {
        this.dao = dao;
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(new TopicLoader());
    }


    private class TopicLoader extends CacheLoader<CacheKey, Topic> {

        @Override
        public Topic load(CacheKey cacheKey) throws NotFoundException, DAOException {
            Topic topic = dao.getTopicByTenantAndName(cacheKey.getTenantId(), cacheKey.getTopicName());
            if (topic == null) {
                throw new NotFoundException("Topic not found");
            }
            List<Subscription> subscriptions = dao.getSubscriptionsForTopic(topic.getTenantId(), topic.getName());
            topic.setSubscriptions(subscriptions);
            return topic;
        }
    }

    public Topic getTopic(@NotNull String tenantId, @NotNull String topicName) throws ServiceException {
        try {
            CacheKey key = new CacheKey(tenantId, topicName);
            return cache.get(key);
        } catch (NotFoundException ex) {
           return null;
        } catch (ExecutionException ex) {
            throw new ServiceException("Could not retrieve topic", ex);
        }
    }

    private static class CacheKey {
        String tenantId;
        String topicName;

        CacheKey(String tenantId, String topicName){
            this.tenantId = tenantId;
            this.topicName = topicName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (!Objects.equals(tenantId, cacheKey.tenantId)) return false;
            return Objects.equals(topicName, cacheKey.topicName);
        }

        @Override
        public int hashCode() {
            int result = tenantId != null ? tenantId.hashCode() : 0;
            result = 31 * result + (topicName != null ? topicName.hashCode() : 0);
            return result;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTopicName() {
            return topicName;
        }

        public void setTopicName(String topicName) {
            this.topicName = topicName;
        }
    }


}

