package edu.utexas.tacc.tapis.notifications.lib.services;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.utexas.tacc.tapis.notifications.lib.dao.NotificationsDAO;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionCache {

//    private static final Duration CACHE_TIMEOUT = Duration.ofMinutes(5);
//    private final NotificationsDAO notificationsDAO;
//    private final LoadingCache<UUID, List<Subscription>> cache;


//    @Inject
//    public SubscriptionCache(NotificationsDAO notificationsDAO) {
//        this.notificationsDAO = notificationsDAO;
//        this.cache = CacheBuilder.newBuilder()
//            .recordStats()
//            .expireAfterWrite(CACHE_TIMEOUT)
//            .build(new CacheLoader<>() {
//                @Override
//                public List<Subscription> load(@NotNull String tenantId, String username) throws Exception {
//                    return notificationsDAO.getSubscriptionsForTopic(tenantId, username);
//                }
//            });
//    }
//
//    public List<Subscription> getSubscriptionsByTopicUUID(UUID topicUUID) throws Exception {
//        return cache.get(topicUUID);
//    }



}
