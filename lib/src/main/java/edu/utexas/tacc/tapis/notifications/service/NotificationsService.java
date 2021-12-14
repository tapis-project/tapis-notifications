package edu.utexas.tacc.tapis.notifications.service;


//import com.rabbitmq.client.AMQP;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Delivery;
import org.jvnet.hk2.annotations.Contract;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
//import reactor.rabbitmq.AcknowledgableDelivery;
//import reactor.rabbitmq.ConsumeOptions;
//import reactor.rabbitmq.OutboundMessage;
//import reactor.rabbitmq.OutboundMessageResult;
//import reactor.rabbitmq.QueueSpecification;
//import reactor.rabbitmq.RabbitFlux;
//import reactor.rabbitmq.Receiver;
//import reactor.rabbitmq.ReceiverOptions;
//import reactor.rabbitmq.Sender;
//import reactor.rabbitmq.SenderOptions;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
//import edu.utexas.tacc.tapis.notifications.model.Subscription.Permission;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Set;

/*
 * Interface for Subscriptions Service
 * Annotate as an hk2 Contract in case we have multiple implementations
 */
@Contract
public interface NotificationsService
{
  void createSubscription(ResourceRequestUser rUser, Subscription subscription, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException;

  void patchSubscription(ResourceRequestUser rUser, String subscriptionId, PatchSubscription patchSubscription, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  void putSubscription(ResourceRequestUser rUser, Subscription putSubscription, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int enableSubscription(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int disableSubscription(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int deleteSubscription(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int changeSubscriptionOwner(ResourceRequestUser rUser, String subscriptionId, String newOwnerName)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException, NotFoundException;

  boolean checkForSubscription(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  boolean isEnabled(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  Subscription getSubscription(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  int getSubscriptionsTotalCount(ResourceRequestUser rUser, List<String> searchList, List<OrderBy> orderByList,
                        String startAfter) throws TapisException, TapisClientException;

  List<Subscription> getSubscriptions(ResourceRequestUser rUser, List<String> searchList, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException;

  List<Subscription> getSubscriptionsUsingSqlSearchStr(ResourceRequestUser rUser, String searchStr, int limit,
                                     List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException;

  Set<String> getAllowedSubscriptionIDs(ResourceRequestUser rUser)
          throws TapisException;

  String getSubscriptionOwner(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;
}
///**
// * This class handles all of the interactions with the persistence of notifications/subscriptions/topics
// * and also the main entry point to push notifications onto the rabbitmq queue.
// */
//@Service
//public interface NotificationsService {
//
//    private final NotificationsDAO notificationsDAO;
//    private final Sender sender;
//    private final Receiver receiver;
//    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
//    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);
//    private static final String NOTIFICATIONS_SERVICE_QUEUE_NAME = "tapis.notifications";
////    private final TopicsCache topicsCache;
//
//
//    @Inject
//    public NotificationsService(@NotNull NotificationsDAO notificationsDAO){
//        this.notificationsDAO = notificationsDAO;
////        this.topicsCache = topicsCache;
//        ConnectionFactory connectionFactory = RabbitMQConnection.getInstance();
//        ReceiverOptions receiverOptions = new ReceiverOptions()
//            .connectionFactory(connectionFactory)
//            .connectionSubscriptionScheduler(Schedulers.newBoundedElastic(8, 1000, "receiver"));
//        SenderOptions senderOptions = new SenderOptions()
//            .connectionFactory(connectionFactory)
//            .connectionSubscriptionScheduler(Schedulers.newBoundedElastic(8, 1000, "sender"));
//        receiver = RabbitFlux.createReceiver(receiverOptions);
//        sender = RabbitFlux.createSender(senderOptions);
//    }
//
//    public Topic getTopic(String tenantId, String topicName) throws ServiceException {
//        try {
//            return notificationsDAO.getTopicByTenantAndName(tenantId, topicName);
//        } catch (DAOException ex) {
//            String msg = String.format("Could not get topic %s", topicName);
//            throw new ServiceException(msg, ex);
//        }
//    }
//
//    public List<Topic> getTopicsByTenantAndOwner(String tenantId, String owner) throws ServiceException {
//        try {
//            return notificationsDAO.getTopicsByTenantAndOwner(tenantId, owner);
//        } catch (DAOException ex) {
//            String msg = String.format("Could not get topics for user %s", owner);
//            throw new ServiceException(msg, ex);
//        }
//    }
//
//    public List<Subscription> getSubscriptionsForTopic(String tenantId, String topicName) throws ServiceException {
//        try {
//            return notificationsDAO.getSubscriptionsForTopic(tenantId, topicName);
//        } catch (DAOException ex) {
//            String msg = String.format("Could not get subscriptions for topic %s", topicName);
//            throw new ServiceException(msg, ex);
//        }
//    }
//
//    public Topic createTopic(Topic topic) throws DuplicateEntityException, ServiceException {
//        try {
//            return notificationsDAO.createTopic(topic);
//        } catch (ConstraintViolationException ex) {
//            //Topic name + tenant Id are unique, so this gets called
//            // when an attempt to create a duplicate topic happens.
//            throw new DuplicateEntityException("Topic with this name already exists", ex);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not create topic", ex);
//        }
//    }
//
//    public void deleteTopic(UUID topicUUID) throws ServiceException {
//        try {
//            notificationsDAO.deleteTopic(topicUUID);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not delete topic", ex);
//        }
//    }
//
//    public void deleteTopic(String tenantId, String topicName) throws ServiceException {
//        try {
//            notificationsDAO.deleteTopic(tenantId, topicName);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not delete topic", ex);
//        }
//    }
//
//    public Subscription createSubscription(Topic topic, Subscription subscription) throws ServiceException, DuplicateEntityException {
//        try {
//            return notificationsDAO.createSubscription(topic, subscription);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not subscribe to topic", ex);
//        }
//    }
//
//    public Subscription getSubscription(UUID subUUID) throws ServiceException {
//        try {
//            return notificationsDAO.getSubscriptionByUUID(subUUID);
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not retrieve subscription.", ex);
//        }
//    }
//
//
//    public void deleteSubscription(Subscription subscription) throws ServiceException {
//        try {
//            notificationsDAO.deleteSubscription(subscription.getUuid());
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not delete topic.");
//        }
//    }
//
//
//    /**
//     * Send a notification, i.e. put it on the main queue. All notifications sent with this method
//     * are set to have a TTL of 7 days.
//     * @param notification
//     * @throws ServiceException
//     */
//    public void sendNotification(@Valid Notification notification) throws ServiceException {
//        try {
//            log.info(notification.toString());
//            String m = mapper.writeValueAsString(notification);
//
//            long TTL = Duration.ofDays(7).toMillis();
//            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
//                .expiration(String.valueOf(TTL))
//                .build();
//            OutboundMessage message = new OutboundMessage("", NOTIFICATIONS_SERVICE_QUEUE_NAME, properties, m.getBytes());
//            Flux<OutboundMessageResult> confirms = sender.sendWithPublishConfirms(Mono.just(message));
//            sender.declareQueue(QueueSpecification.queue(NOTIFICATIONS_SERVICE_QUEUE_NAME))
//                .thenMany(confirms)
//                .subscribe();
//        } catch (Exception e) {
//            log.info(e.getMessage());
//            throw new ServiceException(e.getMessage());
//        }
//    }
//
//    public Flux<AcknowledgableDelivery> streamNotificationMessages() throws ServiceException {
//        ConsumeOptions options = new ConsumeOptions();
//        options.qos(1000);
//        Flux<AcknowledgableDelivery> messages = receiver.consumeManualAck(NOTIFICATIONS_SERVICE_QUEUE_NAME, options);
//        return messages
//            .delaySubscription(sender.declareQueue(QueueSpecification.queue(NOTIFICATIONS_SERVICE_QUEUE_NAME)));
//    }
//
//    private Mono<Notification> deserializeNotification(Delivery message) {
//        try {
//            Notification notification = mapper.readValue(message.getBody(), Notification.class);
//            return Mono.just(notification);
//        } catch (IOException ex) {
//            log.error("Could not deserialize message!", ex);
//            return Mono.empty();
//        }
//    }
//
//
//    /**
//     * Re
//     * are set to have a TTL of 7 days.
//     * @param notification
//     * @throws ServiceException
//     */
//    public void enqueueNotification(@Valid Notification notification, String queueName) throws ServiceException {
//        try {
//            String internalQueueName = notification.getTenantId() + ":" + queueName;
//            String m = mapper.writeValueAsString(notification);
//            long TTL = Duration.ofDays(7).toMillis();
//            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
//                .expiration(String.valueOf(TTL))
//                .build();
//            OutboundMessage message = new OutboundMessage("", internalQueueName, properties, m.getBytes());
//            Flux<OutboundMessageResult> confirms = sender.sendWithPublishConfirms(Mono.just(message));
//            sender.declareQueue(QueueSpecification.queue(internalQueueName))
//                .thenMany(confirms)
//                .subscribe();
//        } catch (Exception e) {
//            log.info(e.getMessage());
//            throw new ServiceException(e.getMessage());
//        }
//    }
//
//    public Flux<Notification> streamNotificationsOnQueue(String queueName) {
//        return receiver.consumeAutoAck(queueName)
//            .flatMap(this::deserializeNotification);
//    }
//
//    public Queue createQueue(Queue queueSpec) throws ServiceException, DuplicateEntityException {
//        try {
//            Queue queue = notificationsDAO.createQueue(queueSpec);
//            sender.declareQueue(QueueSpecification.queue(queue.getUuid().toString())).subscribe();
//            return queue;
//        } catch (DAOException ex) {
//            throw new ServiceException("Could not create queue.", ex);
//        }
//    }
//
//}
