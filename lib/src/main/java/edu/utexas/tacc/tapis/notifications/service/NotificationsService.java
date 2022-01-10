package edu.utexas.tacc.tapis.notifications.service;


//import com.rabbitmq.client.AMQP;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Delivery;
import edu.utexas.tacc.tapis.notifications.model.Event;
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
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
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
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException;

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

//  Set<String> getAllowedSubscriptionIDs(ResourceRequestUser rUser)
//          throws TapisException, TapisClientException;
//
  String getSubscriptionOwner(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------
  void postEvent(ResourceRequestUser rUser, Event event) throws TapisException;
}
