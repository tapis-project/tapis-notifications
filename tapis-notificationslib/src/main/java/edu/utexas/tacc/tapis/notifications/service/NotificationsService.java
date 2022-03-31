package edu.utexas.tacc.tapis.notifications.service;


import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import edu.utexas.tacc.tapis.notifications.model.Notification;
import org.jvnet.hk2.annotations.Contract;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;

/*
 * Interface for Subscriptions Service
 * Annotate as an hk2 Contract in case we have multiple implementations
 */
@Contract
public interface NotificationsService
{
  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
  String createSubscription(ResourceRequestUser rUser, Subscription subscription, String scrubbedText)
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

  int updateSubscriptionTTL(ResourceRequestUser rUser, String subscriptionId, String newTTL)
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

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------
  void postEvent(ResourceRequestUser rUser, Event event) throws IOException;
//
//  Event readEvent(ResourceRequestUser rUser) throws TapisException;


  // -----------------------------------------------------------------------
  // ------------------------- Test Sequence -------------------------------
  // -----------------------------------------------------------------------
  String beginTestSequence(ResourceRequestUser rUser, String baseServiceUrl, String subscriptionTTL)
          throws TapisException, IOException, URISyntaxException, IllegalStateException, IllegalArgumentException;

  TestSequence getTestSequence(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException;

  int deleteTestSequence(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException;

  void recordTestNotification(String tenant, String user, String subscriptionId, Notification notification)
          throws TapisException, IllegalStateException;
}
