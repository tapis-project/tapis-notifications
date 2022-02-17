package edu.utexas.tacc.tapis.notifications.dao;

import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription.SubscriptionOperation;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface NotificationsDao
{
  // ====================
  // General
  // ====================

  Exception checkDB();

  void migrateDB() throws TapisException;

  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
  boolean createSubscription(ResourceRequestUser rUser, Subscription sub, Instant expiry, String createJsonStr,
                             String scrubbedText)
          throws TapisException, IllegalStateException;

  void patchSubscription(ResourceRequestUser rUser, String subId, Subscription patchedSubscription,
                String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void putSubscription(ResourceRequestUser rUser, Subscription putSubscription, String updateJsonStr, String scrubbedText)
          throws TapisException, IllegalStateException;

  void updateEnabled(ResourceRequestUser rUser, String tenantId, String id, boolean enabled) throws TapisException;

  void updateSubscriptionOwner(ResourceRequestUser rUser, String tenantId, String id, String newOwnerName) throws TapisException;

  void updateSubscriptionTTL(ResourceRequestUser rUser, String tenantId, String id, int newTTL, Instant newExpiry)
          throws TapisException;

  void addUpdateRecord(ResourceRequestUser rUser, String tenant, String id, SubscriptionOperation op,
                       String upd_json, String upd_text) throws TapisException;

  int deleteSubscription(String tenant, String id) throws TapisException;

  boolean checkForSubscription(String tenant, String id) throws TapisException;

  boolean isEnabled(String tenant, String id) throws TapisException;

  Subscription getSubscription(String tenant, String id) throws TapisException;

  int getSubscriptionsCount(String tenant, List<String> searchList, ASTNode searchAST, Set<String> setOfIDs,
                   List<OrderBy> orderByList, String startAfter) throws TapisException;

  List<Subscription> getSubscriptions(String tenant, List<String> searchList, ASTNode searchAST, Set<String> subIDs, int limit,
                    List<OrderBy> orderByList, int skip, String startAfter) throws TapisException;

  Set<String> getSubscriptionIDsByOwner(String tenant, String owner) throws TapisException;

  Set<String> getSubscriptionIDs(String tenant) throws TapisException;

  String getSubscriptionOwner(String tenant, String id) throws TapisException;

  // -----------------------------------------------------------------------
  // -------------------- Notifications ------------------------------------
  // -----------------------------------------------------------------------

//  boolean createNotification(Notification notification) throws TapisException;

//  int deleteNotification(Notification notification) throws TapisException;

}
