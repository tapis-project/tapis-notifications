package edu.utexas.tacc.tapis.notifications.service;


import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import org.jvnet.hk2.annotations.Contract;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
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

  void patchSubscription(ResourceRequestUser rUser, String owner, String name, PatchSubscription patchSubscription, String scrubbedText)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int enableSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int disableSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalStateException, IllegalArgumentException, NotFoundException;

  int deleteSubscriptionByName(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException;

  int deleteSubscriptionByUuid(ResourceRequestUser rUser, String uuid)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException;

  int deleteSubscriptionsBySubject(ResourceRequestUser rUser, String owner, String subject, boolean anyOwner)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException;

  int updateSubscriptionTTL(ResourceRequestUser rUser, String owner, String name, String newTTL)
          throws TapisException, TapisClientException, NotAuthorizedException, IllegalArgumentException, NotFoundException;

  boolean isEnabled(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, TapisClientException, NotAuthorizedException;

  Subscription getSubscriptionByName(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, TapisClientException, NotAuthorizedException;

  Subscription getSubscriptionByUuid(ResourceRequestUser rUser, String uuid)
          throws TapisException, TapisClientException, NotAuthorizedException;

  int getSubscriptionsTotalCount(ResourceRequestUser rUser, String owner, List<String> searchList, List<OrderBy> orderByList,
                        String startAfter) throws TapisException, TapisClientException;

  List<Subscription> getSubscriptions(ResourceRequestUser rUser, String owner, List<String> searchList, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter, boolean anyOwner)
          throws TapisException, TapisClientException;

  List<Subscription> getSubscriptionsUsingSqlSearchStr(ResourceRequestUser rUser, String owner, String searchStr, int limit,
                                     List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException;

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------
  void publishEvent(ResourceRequestUser rUser, String source, String type, String subject, String data,
                    String seriesId, String timestamp, boolean deleteSubscriptionMatchingSubject,
                    boolean endSeries, String tenant)
          throws TapisException, IOException, IllegalArgumentException, NotAuthorizedException;

  int endEventSeries(ResourceRequestUser rUser, String source, String subject, String seriesId, String tenant)
          throws TapisException, IOException, IllegalArgumentException, NotAuthorizedException;

  // -----------------------------------------------------------------------
  // ------------------------- Test Sequence -------------------------------
  // -----------------------------------------------------------------------
  Subscription beginTestSequence(ResourceRequestUser rUser, String baseServiceUrl, String subscriptionTTL,
                                 Integer numberOfEvents, Boolean endSeries)
          throws TapisException, TapisClientException, IOException, IllegalStateException, IllegalArgumentException;

  TestSequence getTestSequence(ResourceRequestUser rUser, String name)
          throws TapisException, TapisClientException;

  int deleteTestSequence(ResourceRequestUser rUser, String name)
          throws TapisException, TapisClientException, NotAuthorizedException;

  void recordTestNotification(String tenant, String user, String name, Notification notification)
          throws TapisException, IllegalStateException;
}
