package edu.utexas.tacc.tapis.notifications.dao;

import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationsDao
{
  // ====================
  // General
  // ====================

  Exception checkDB();

  void migrateDB() throws TapisException;

  int getNextSeriesSeqCount(ResourceRequestUser rUser, String seriesId) throws TapisException;

  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
  boolean createSubscription(ResourceRequestUser rUser, Subscription sub, Instant expiry)
          throws TapisException, IllegalStateException;

  void patchSubscription(ResourceRequestUser rUser, String owner, String name, Subscription patchedSubscription)
          throws TapisException, IllegalStateException;

  void updateEnabled(String tenant, String owner, String name, boolean enabled)
          throws TapisException;

  void updateSubscriptionTTL(String tenant, String owner, String name, int newTTL, Instant newExpiry)
          throws TapisException;

  int deleteSubscriptionByName(String tenant, String owner, String name) throws TapisException;

  int deleteSubscriptionByUuid(String tenant, UUID uuid) throws TapisException;

  int deleteSubscriptionsBySubject(String tenant, String owner, String subject, boolean anyOwner) throws TapisException;

  boolean checkForSubscription(String tenant, String owner, String name) throws TapisException;

  boolean isEnabled(String tenant, String owner, String name) throws TapisException;

  Subscription getSubscriptionByName(String tenant, String owner, String name) throws TapisException;

  Subscription getSubscriptionByUuid(String tenant, UUID uuid) throws TapisException;

  int getSubscriptionsCount(String tenant, String owner, List<String> searchList, ASTNode searchAST,
                            Set<String> setOfIDs, List<OrderBy> orderByList, String startAfter)
          throws TapisException;

  List<Subscription> getSubscriptions(String tenant, String owner, List<String> searchList, ASTNode searchAST,
                                      Set<String> names, int limit, List<OrderBy> orderByList, int skip,
                                      String startAfter, boolean anyOwner)
          throws TapisException;

  List<Subscription> getSubscriptionsForEvent(Event event) throws TapisException;

  Set<String> getSubscriptionNamesByOwner(String tenant, String owner) throws TapisException;

  Set<String> getSubscriptionIDs(String tenant) throws TapisException;

  List<Subscription> getExpiredSubscriptions() throws TapisException;

  // -----------------------------------------------------------------------
  // -------------------- Notifications ------------------------------------
  // -----------------------------------------------------------------------

  boolean persistNotificationsAndUpdateLastEvent(String tenant, Event event, int bucketNum, List<Notification> notifications)
          throws TapisException;

  List<Notification> getNotificationsForEvent(String tenant, Event event, int bucketNum) throws TapisException;

  List<Notification> getNotifications(int bucketNum) throws TapisException;

  Notification getNotification(String tenant, UUID uuid) throws TapisException;

  void deleteNotificationAndAddToRecovery(String tenant, Notification notification) throws TapisException;

  void deleteNotificationsByDeliveryTarget(String tenant, Notification notification) throws TapisException;

  UUID getLastEventUUID(int bucketNum) throws TapisException;

  boolean checkForLastEvent(UUID eventUuid, int bucketNum) throws TapisException;

  List<Notification> getNotificationsInRecovery(int bucketNum) throws TapisException;

  void deleteNotificationFromRecovery(Notification notification) throws TapisException;

  int getNotificationRecoveryAttemptCount(Notification notification) throws TapisException;

  void setNotificationRecoveryAttemptCount(Notification notification, int attemptCount) throws TapisException;

  // -----------------------------------------------------------------------
  // --------------------- Test Sequences ----------------------------------
  // -----------------------------------------------------------------------
  boolean createTestSequence(ResourceRequestUser rUser, String name)
          throws TapisException, IllegalStateException;

  TestSequence getTestSequence(String tenant, String owner, String name) throws TapisException;

  void addTestSequenceNotification(String tenant, String user, String subscrId, Notification notification)
          throws TapisException, IllegalStateException;

  boolean checkForTestSequence(String tenant, String owner, String name) throws TapisException;
}
