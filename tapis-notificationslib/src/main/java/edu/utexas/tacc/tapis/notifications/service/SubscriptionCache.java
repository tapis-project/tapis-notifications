package edu.utexas.tacc.tapis.notifications.service;


import org.jvnet.hk2.annotations.Service;

@Service
public class SubscriptionCache
{
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
