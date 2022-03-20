package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.search.parser.ASTParser;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.security.client.SKClient;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;


import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription.Permission;
import edu.utexas.tacc.tapis.notifications.model.Subscription.SubscriptionOperation;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;

import static edu.utexas.tacc.tapis.shared.TapisConstants.NOTIFICATIONS_SERVICE;

/*
 * Service level methods for Notifications.
 *   Uses Dao layer and other service library classes to perform all top level service operations.
 * Annotate as an hk2 Service so that default scope for Dependency Injection is singleton
 */
@Service
public class NotificationsServiceImpl implements NotificationsService
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Logging
  private static final Logger log = LoggerFactory.getLogger(NotificationsServiceImpl.class);

  // Default test subscription TTL is 60 minutes
  public static final int DEFAULT_TEST_TTL = 60;

  public static final String TEST_SUBSCR_TYPE_FILTER = "notifications.test.*";
  public static final String TEST_EVENT_TYPE = "notifications.test.begin";

  private static final Set<Permission> ALL_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  private static final Set<Permission> READMODIFY_PERMS = new HashSet<>(Set.of(Permission.READ, Permission.MODIFY));
  // Permspec format for systems is "system:<tenant>:<perm_list>:<system_id>"
  private static final String PERM_SPEC_PREFIX = "subscr";
  private static final String PERM_SPEC_TEMPLATE = "subscr:%s:%s:%s";

  private static final String SERVICE_NAME = TapisConstants.SERVICE_NAME_NOTIFICATIONS;
  // Message keys
  private static final String ERROR_ROLLBACK = "NTFLIB_ERROR_ROLLBACK";
  private static final String NOT_FOUND = "NTFLIB_SUBSCR_NOT_FOUND";

  // NotAuthorizedException requires a Challenge, although it serves no purpose here.
  private static final String NO_CHALLENGE = "NoChallenge";

  // Compiled regex for splitting around ":"
  private static final Pattern COLON_SPLIT = Pattern.compile(":");

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************

  // Use HK2 to inject singletons
  @Inject
  private NotificationsDao dao;

  @Inject
  private ServiceClients serviceClients;

  @Inject
  private ServiceContext serviceContext;

  // We must be running on a specific site and this will never change
  // These are initialized in method initService()
  private static String siteId;
  private static String siteAdminTenantId;
  public static String getSiteId() {return siteId;}
  public static String getServiceTenantId() {return siteAdminTenantId;}
  public static String getServiceUserId() {return SERVICE_NAME;}

  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  /**
   * Initialize the service:
   *   init service context
   *   migrate DB
   *   init message broker
   */
  public void initService(String siteAdminTenantId1, RuntimeParameters runParms)
          throws TapisException, TapisClientException
  {
    // Initialize service context and site info
    siteId = runParms.getSiteId();
    siteAdminTenantId = siteAdminTenantId1;
    serviceContext.initServiceJWT(siteId, NOTIFICATIONS_SERVICE, runParms.getServicePassword());
    // Make sure DB is present and updated to latest version using flyway
    dao.migrateDB();
    // Initialize the singleton instance of the message broker manager
    MessageBroker.init(runParms);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a new resource given a subscription and the text used to create the subscription.
   * Secrets in the text should be masked.
   * If id is empty then generate a uuid and use that as the id.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param sub - Pre-populated Subscription object
   * @param scrubbedText - Text used to create the Subscription object - secrets should be scrubbed. Saved in update record.
   * @return subscription Id
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - subscription exists OR subscription in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public String createSubscription(ResourceRequestUser rUser, Subscription sub, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotAuthorizedException
  {
    SubscriptionOperation op = SubscriptionOperation.create;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (sub == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    log.trace(LibUtils.getMsgAuth("NTFLIB_CREATE_TRACE", rUser, scrubbedText));
    String resourceTenantId = sub.getTenant();
    String resourceId = sub.getId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ARG", rUser, resourceId));
    }

    // sub1 is the subscription object that will be passed to the dao layer.
    Subscription sub1;
    // If no Id provided then fill in with a UUID.
    if (StringUtils.isBlank(resourceId))
    {
      resourceId = UUID.randomUUID().toString();
      sub1 = new Subscription(sub, resourceTenantId, resourceId);
    }
    else
    {
      sub1 = new Subscription(sub);
    }

    // Check if subscription already exists
    if (dao.checkForSubscription(resourceTenantId, resourceId))
    {
      throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_EXISTS", rUser, resourceId));
    }

    // Make sure owner, notes and tags are all set
    sub1.setDefaults();

    // ----------------- Resolve variables for any attributes that might contain them --------------------
    sub1.resolveVariables(rUser.getOboUserId());

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, resourceId, sub1.getOwner(), null, null);

    // ---------------- Check constraints on Subscription attributes ------------------------
    validateSubscription(rUser, sub1);

    // Construct Json string representing the Subscription about to be created
    Subscription scrubbedSubscription = new Subscription(sub1);
    String createJsonStr = TapisGsonUtils.getGson().toJson(scrubbedSubscription);

    // Compute the expiry time from now
    Instant expiry = Subscription.computeExpiryFromNow(sub1.getTtl());

    // ----------------- Create all artifacts --------------------
    // Creation of subscription and perms not in single DB transaction.
    // Use try/catch to rollback any writes in case of failure.
    boolean subCreated = false;
    String subsPermSpecALL = getPermSpecAllStr(resourceTenantId, resourceId);

    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient();
    try
    {
      // ------------------- Make Dao call to persist the subscription -----------------------------------
      subCreated = dao.createSubscription(rUser, sub1, expiry, createJsonStr, scrubbedText);

      // ------------------- Add permissions -----------------------------
      // Give owner full access to the subscription
//      skClient.grantUserPermission(resourceTenantId, sub1.getOwner(), subsPermSpecALL);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      // Log error
      String msg = LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ROLLBACK", rUser, resourceId, e0.getMessage());
      log.error(msg);

      // Rollback
      // Remove subscription from DB
      if (subCreated) try {dao.deleteSubscription(resourceTenantId, resourceId); }
      catch (Exception e) {
        log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, resourceId, "delete", e.getMessage()));}
      // Remove perms
//      try { skClient.revokeUserPermission(resourceTenantId, sub1.getOwner(), subsPermSpecALL); }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, resourceId, "revokePermOwner", e.getMessage()));}
      throw e0;
    }
    return resourceId;
  }

  /**
   * Update existing subscription given a PatchSubscription and the text used to create the PatchSubscription.
   * Secrets in the text should be masked.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param patchSubscription - Pre-populated PatchSubscription object
   * @param scrubbedText - Text used to create the PatchSubscription object - secrets should be scrubbed. Saved in update record.
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public void patchSubscription(ResourceRequestUser rUser, String subId, PatchSubscription patchSubscription,
                                String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException,
                 NotAuthorizedException, NotFoundException
  {
    SubscriptionOperation op = SubscriptionOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (patchSubscription == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();
    String resourceId = subId;

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId) || StringUtils.isBlank(resourceId) || StringUtils.isBlank(scrubbedText))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ARG", rUser, resourceId));
    }

    // Subscription must already exist
    if (!dao.checkForSubscription(resourceTenantId, resourceId))
    {
      throw new NotFoundException(LibUtils.getMsgAuth("NTFLIB_VER_NOT_FOUND", rUser, resourceId));
    }

    // Retrieve the subscription being patched and create fully populated Subscription with changes merged in
    Subscription origSubscription = dao.getSubscription(resourceTenantId, resourceId);
    Subscription patchedSubscription = createPatchedSubscription(origSubscription, patchSubscription);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, resourceId, origSubscription.getOwner(), null, null);

    // ---------------- Check constraints on Subscription attributes ------------------------
    validateSubscription(rUser, patchedSubscription);

    // Construct Json string representing the PatchSubscription about to be used to update the subscription
    String updateJsonStr = TapisGsonUtils.getGson().toJson(patchSubscription);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the subscription -----------------------------------
    dao.patchSubscription(rUser, subId, patchedSubscription, updateJsonStr, scrubbedText);
  }

  /**
   * Update all updatable attributes of a subscription given a subscription and the text used to create the subscription.
   * Incoming Subscription must contain the tenantId and subscriptionId
   * Secrets in the text should be masked.
   * Attributes that cannot be updated and so will be looked up and filled in:
   *   tenant, id, owner, enabled
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param putSub - Pre-populated Subscription object (including tenantId, subscriptionId)
   * @param scrubbedText - Text used to create the Subscription object - secrets should be scrubbed. Saved in update record.
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public void putSubscription(ResourceRequestUser rUser, Subscription putSub, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException,
                 NotAuthorizedException, NotFoundException
  {
    SubscriptionOperation op = SubscriptionOperation.modify;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (putSub == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    // Extract various names for convenience
    String resourceTenantId = putSub.getTenant();
    String resourceId = putSub.getId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId) || StringUtils.isBlank(resourceId) || StringUtils.isBlank(scrubbedText))
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ARG", rUser, resourceId));
    }

    // Subscription must already exist
    if (!dao.checkForSubscription(resourceTenantId, resourceId))
    {
      throw new NotFoundException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NOT_FOUND", rUser, resourceId));
    }

    // Retrieve the subscription being updated and create fully populated Subscription with changes merged in
    Subscription origSub = dao.getSubscription(resourceTenantId, resourceId);
    Subscription updatedSub = createUpdatedSubscription(origSub, putSub);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, resourceId, origSub.getOwner(), null, null);

    // ---------------- Check constraints on Subscription attributes ------------------------
    validateSubscription(rUser, updatedSub);

    // Construct Json string representing the Subscription about to be used to update
    String updateJsonStr = TapisGsonUtils.getGson().toJson(putSub);

    // ----------------- Create all artifacts --------------------
    // No distributed transactions so no distributed rollback needed
    // ------------------- Make Dao call to persist the subscription -----------------------------------
    dao.putSubscription(rUser, updatedSub, updateJsonStr, scrubbedText);
  }

  /**
   * Update enabled to true for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int enableSubscription(ResourceRequestUser rUser, String subId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    return updateEnabled(rUser, subId, SubscriptionOperation.enable);
  }

  /**
   * Update enabled to false for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int disableSubscription(ResourceRequestUser rUser, String subId)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    return updateEnabled(rUser, subId, SubscriptionOperation.disable);
  }

  /**
   * Delete a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int deleteSubscription(ResourceRequestUser rUser, String subId)
          throws TapisException, IllegalArgumentException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.delete;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    // If subscription does not exist then 0 changes
    if (!dao.checkForSubscription(rUser.getOboTenantId(), subId)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, null, null, null);

    // Delete the subscription
    return dao.deleteSubscription(rUser.getOboTenantId(), subId);
  }

  /**
   * Change owner of a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @param newOwnerName - User name of new owner
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int changeSubscriptionOwner(ResourceRequestUser rUser, String subId, String newOwnerName)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.changeOwner;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId) || StringUtils.isBlank(newOwnerName))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
 
    String resourceTenantId = rUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId))
         throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ARG", rUser, subId));

    // Subscription must already exist
    if (!dao.checkForSubscription(resourceTenantId, subId))
         throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, subId));

    // Retrieve the old owner
    String oldOwnerName = dao.getSubscriptionOwner(resourceTenantId, subId);

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, oldOwnerName, null, null);

    // If new owner same as old owner then this is a no-op
    if (newOwnerName.equals(oldOwnerName)) return 0;

    // ----------------- Make all updates --------------------
    // Changes not in single DB transaction.
    // Use try/catch to rollback any changes in case of failure.
    // Get SK client now. If we cannot get this rollback not needed.
    var skClient = getSKClient();
    String subsPermSpec = getPermSpecAllStr(resourceTenantId, subId);
    try {
      // ------------------- Make Dao call to update the subscription owner -----------------------------------
      dao.updateSubscriptionOwner(rUser, resourceTenantId, subId, newOwnerName);
//      // Add permissions for new owner
//      skClient.grantUserPermission(resourceTenantId, newOwnerName, subsPermSpec);
//      // Remove permissions from old owner
//      skClient.revokeUserPermission(resourceTenantId, oldOwnerName, subsPermSpec);
    }
    catch (Exception e0)
    {
      // Something went wrong. Attempt to undo all changes and then re-throw the exception
      try { dao.updateSubscriptionOwner(rUser, resourceTenantId, subId, oldOwnerName); } catch (Exception e) {
        log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, subId, "updateOwner", e.getMessage()));}
//      try { skClient.revokeUserPermission(resourceTenantId, newOwnerName, subsPermSpec); }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, subId, "revokePermNewOwner", e.getMessage()));}
//      try { skClient.grantUserPermission(resourceTenantId, oldOwnerName, subsPermSpec); }
//      catch (Exception e) {_log.warn(LibUtils.getMsgAuth(ERROR_ROLLBACK, rUser, subId, "grantPermOldOwner", e.getMessage()));}
      throw e0;
    }
    return 1;
  }

  /**
   * Update TTL for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @param newTTLStr - New value for TTL
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int updateSubscriptionTTL(ResourceRequestUser rUser, String subId, String newTTLStr)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.updateTTL;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId) || StringUtils.isBlank(newTTLStr))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    String resourceTenantId = rUser.getOboTenantId();

    // ---------------------------- Check inputs ------------------------------------
    if (StringUtils.isBlank(resourceTenantId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_CREATE_ERROR_ARG", rUser, subId));

    // Subscription must already exist
    if (!dao.checkForSubscription(resourceTenantId, subId))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, subId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, null, null, null);

    // If TTL provided is not an integer then throw an exception
    int newTTL;
    try { newTTL = Integer.parseInt(newTTLStr); }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_TTL_NOTINT", rUser, subId, newTTLStr));
    }

    // ----------------- Make update --------------------
    dao.updateSubscriptionTTL(rUser, resourceTenantId, subId, newTTL, Subscription.computeExpiryFromNow(newTTL));
    return 1;
  }

//  /**
//   * Hard delete a subscription record given the subscription name.
//   * Also remove artifacts from the Security Kernel.
//   * NOTE: This is package-private. Only test code should ever use it.
//   *
//   * @param rUser - ResourceRequestUser containing tenant, user and request info
//   * @param subId - name of subscription
//   * @return Number of items deleted
//   * @throws TapisException - for Tapis related exceptions
//   * @throws NotAuthorizedException - unauthorized
//   */
//  int hardDeleteSubscription(ResourceRequestUser rUser, String resourceTenantId, String subId)
//          throws TapisException, TapisClientException, NotAuthorizedException
//  {
//    SubscriptionOperation op = SubscriptionOperation.delete;
//    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
//    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
//
//    // If subscription does not exist then 0 changes
//    if (!dao.checkForSubscription(resourceTenantId, subId)) return 0;
//
//    // ------------------------- Check service level authorization -------------------------
//    checkAuth(rUser, op, subId, null, null, null);
//
//    // Remove SK artifacts
//// TODO/TBD    removeSKArtifacts(resourceTenantId, subId);
//
//    // Delete the subscription
//    return dao.deleteSubscription(resourceTenantId, subId);
//  }
//
  /**
   * Hard delete all resources in the "test" tenant.
   * Also remove artifacts from the Security Kernel.
   * NOTE: This is package-private. Only test code should ever use it.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @return Number of items deleted
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  int hardDeleteAllTestTenantResources(ResourceRequestUser rUser)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    // For safety hard code the tenant name
    String resourceTenantId = "test";
    // Fetch all resource Ids including deleted items
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    var resourceIdSet = dao.getSubscriptionIDs(resourceTenantId);
    for (String id : resourceIdSet)
    {
//      hardDeleteSubscription(rUser, resourceTenantId, id);
      deleteSubscription(rUser, id);
    }
    return resourceIdSet.size();
  }

  /**
   * Check that we can connect with DB and that the main table of the service exists.
   * @return null if all OK else return an Exception
   */
  public Exception checkDB()
  {
    return dao.checkDB();
  }

  /**
   * Check that we can connect with our message broker.
   * @return null if all OK else return an Exception
   */
  public Exception checkMessageBroker()
  {
    MessageBroker mb = MessageBroker.getInstance();
    if (mb == null) return new TapisException(LibUtils.getMsg("NTFLIB_MSGBRKR_NULL"));
    return mb.checkConnection();
  }

  /**
   * checkForSubscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - Name of the subscription
   * @return true if subscription exists, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public boolean checkForSubscription(ResourceRequestUser rUser, String subId)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    String resourceTenantId = rUser.getOboTenantId();
 
    // We need owner to check auth and if subscription not there cannot find owner, so cannot do auth check if no subscription
    if (dao.checkForSubscription(resourceTenantId, subId)) {
      // ------------------------- Check service level authorization -------------------------
      checkAuth(rUser, op, subId, null, null, null);
      return true;
    }
    return false;
  }

  /**
   * isEnabled
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - Name of the subscription
   * @return true if subscription is enabled, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public boolean isEnabled(ResourceRequestUser rUser, String subId)
          throws TapisException, NotFoundException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    String resourceTenantId = rUser.getOboTenantId();

    // Resource must exist
    if (!dao.checkForSubscription(resourceTenantId, subId))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, subId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, null, null, null);
    return dao.isEnabled(resourceTenantId, subId);
  }

  /**
   * getSubscription
   * Retrieve a subscription.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - Name of the subscription
   * @return populated instance of an Subscription or null if not found or user not authorized.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public Subscription getSubscription(ResourceRequestUser rUser, String subId)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    // We need owner to check auth and if sub not there cannot find owner, so
    // if sub does not exist then return null
    if (!dao.checkForSubscription(resourceTenantId, subId)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, null, null, null);

    Subscription result = dao.getSubscription(resourceTenantId, subId);
    return result;
  }

  /**
   * Get count of all subscriptions matching certain criteria and for which user has READ permission
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return Count of subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public int getSubscriptionsTotalCount(ResourceRequestUser rUser, List<String> searchList,
                                        List<OrderBy> orderByList, String startAfter)
          throws TapisException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));

    // Build verified list of search conditions
    var verifiedSearchList = new ArrayList<String>();
    if (searchList != null && !searchList.isEmpty())
    {
      try
      {
        for (String cond : searchList)
        {
          // Use SearchUtils to validate condition
          String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(cond);
          verifiedSearchList.add(verifiedCondStr);
        }
      }
      catch (Exception e)
      {
        String msg = LibUtils.getMsgAuth("NTFLIB_SEARCH_ERROR", rUser, e.getMessage());
        log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // Get list of IDs of resources for which requester has view permission.
    // This is either all resources (null) or a list of IDs.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionIDs(rUser);

    // If none are allowed we know count is 0
    if (allowedIDs != null && allowedIDs.isEmpty()) return 0;

    // Count all allowed resources matching the search conditions
    return dao.getSubscriptionsCount(rUser.getOboTenantId(), verifiedSearchList, null, allowedIDs, orderByList, startAfter);
  }

  /**
   * Get all subscriptions for which user has READ permission
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param searchList - optional list of conditions used for searching
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return List of Subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<Subscription> getSubscriptions(ResourceRequestUser rUser, List<String> searchList, int limit,
                                             List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));

    // Build verified list of search conditions
    var verifiedSearchList = new ArrayList<String>();
    if (searchList != null && !searchList.isEmpty())
    {
      try
      {
        for (String cond : searchList)
        {
          // Use SearchUtils to validate condition
          String verifiedCondStr = SearchUtils.validateAndProcessSearchCondition(cond);
          verifiedSearchList.add(verifiedCondStr);
        }
      }
      catch (Exception e)
      {
        String msg = LibUtils.getMsgAuth("NTFLIB_SEARCH_ERROR", rUser, e.getMessage());
        log.error(msg, e);
        throw new IllegalArgumentException(msg);
      }
    }

    // Get list of IDs of subscriptions for which requester has READ permission.
    // This is either all subscriptions (null) or a list of IDs.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionIDs(rUser);

    // Get all allowed subscriptions matching the search conditions
    return dao.getSubscriptions(rUser.getOboTenantId(), verifiedSearchList, null, allowedIDs, limit,
                                orderByList, skip, startAfter);
  }

  /**
   * Get all subscriptions for which user has view permission.
   * Use provided string containing a valid SQL where clause for the search.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param sqlSearchStr - string containing a valid SQL where clause
   * @return List of Subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<Subscription> getSubscriptionsUsingSqlSearchStr(ResourceRequestUser rUser, String sqlSearchStr, int limit,
                                                              List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException
  {
    // If search string is empty delegate to getSubscriptions()
    if (StringUtils.isBlank(sqlSearchStr)) return getSubscriptions(rUser, null, limit, orderByList, skip, startAfter);

    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));

    // Validate and parse the sql string into an abstract syntax tree (AST)
    // The activemq parser validates and parses the string into an AST but there does not appear to be a way
    //          to use the resulting BooleanExpression to walk the tree. How to now create a usable AST?
    //   I believe we don't want to simply try to run the where clause for various reasons:
    //      - SQL injection
    //      - we want to verify the validity of each <attr>.<op>.<value>
    //        looks like activemq parser will ensure the leaf nodes all represent <attr>.<op>.<value> and in principle
    //        we should be able to check each one and generate of list of errors for reporting.
    //  Looks like jOOQ can parse an SQL string into a jooq Condition. Do this in the Dao? But still seems like no way
    //    to walk the AST and check each condition so we can report on errors.
//    BooleanExpression searchAST;
    ASTNode searchAST;
    try { searchAST = ASTParser.parse(sqlSearchStr); }
    catch (Exception e)
    {
      String msg = LibUtils.getMsgAuth("NTFLIB_SEARCH_ERROR", rUser, e.getMessage());
      log.error(msg, e);
      throw new IllegalArgumentException(msg);
    }

    // Get list of IDs of subscriptions for which requester has READ permission.
    // This is either all subscriptions (null) or a list of IDs.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionIDs(rUser);

    // Get all allowed subscriptions matching the search conditions
    return dao.getSubscriptions(rUser.getOboTenantId(), null, searchAST, allowedIDs, limit, orderByList, skip, startAfter);
  }

//  /**
//   * Get list of all subscription IDs that an rUser is authorized to view
//   * TODO/TBD: Use Perms model as with Apps/Systems ore return all resources owned by the user.
//   * @param rUser - ResourceRequestUser containing tenant, user and request info
//   * @return - set of subscription IDs
//   * @throws TapisException - for Tapis related exceptions
//   */
//  @Override
//  public Set<String> getAllowedSubscriptionIDs(ResourceRequestUser rUser) throws TapisException, TapisClientException
//  {
//    return prvtGetAllowedSubscriptionIDs(rUser);
//// TODO/TBD    SubscriptionOperation op = SubscriptionOperation.read;
////    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
////    // Get all subscription names
////    Set<String> subIds = dao.getSubscriptionIDs(rUser.getOboTenantId());
////    var allowedNames = new HashSet<String>();
////    // Filter based on user authorization
////    for (String name: subIds)
////    {
////      try {
////        checkAuth(rUser, op, name, null, null, null);
////        allowedNames.add(name);
////      }
////      catch (NotAuthorizedException | TapisClientException e) { }
////    }
////    return allowedNames;
//  }

  /**
   * Get subscription owner
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - Name of the subscription
   * @return - Owner or null if subscription not found or user not authorized
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public String getSubscriptionOwner(ResourceRequestUser rUser, String subId)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    // We need owner to check auth and if subscription not there cannot find owner, so
    // if subscription does not exist then return null
    if (!dao.checkForSubscription(rUser.getOboTenantId(), subId)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subId, null, null, null);

    return dao.getSubscriptionOwner(rUser.getOboTenantId(), subId);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------

  /**
   * Post an Event to the queue.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param event - Pre-populated Event object
   * @throws IOException - on error
   */
  @Override
  public void postEvent(ResourceRequestUser rUser, Event event) throws IOException
  {
    MessageBroker.getInstance().publishEvent(rUser, event);
  }

  /**
   * Read an Event from the queue.
   * Event is removed from the queue.
   *   NOTE: currently only used for testing. Not part of public interface of service.
   * @throws TapisException - on error
   *
   */
//  @Override
  public Event readEvent(boolean autoAck) throws TapisException
  {
    return MessageBroker.getInstance().readEvent(autoAck);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Test Sequences ------------------------------
  // -----------------------------------------------------------------------
  /**
   * Start a test sequence by creating a subscription and publishing an event
   * Subscription id will be created as a UUID.
   * If baseServiceUrl is https://dev.tapis.io then
   *   event source will be https://dev.tapis.io/v3/notifications
   *   delivery callback will have the form https://dev.tapis.io/v3/notifications/640ad5a8-1a6e-4189-a334-c4c7226fb9ba
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param baseServiceUrl - Base URL for service. Used for callback and event source.
   * @param ttl - optional TTL for the auto-generated subscription
   * @return subscription Id
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - subscription exists OR subscription in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public String beginTestSequence(ResourceRequestUser rUser, String baseServiceUrl, String ttl)
          throws TapisException, IOException, URISyntaxException, IllegalStateException, IllegalArgumentException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    String tenantId = rUser.getOboTenantId();
    String owner = rUser.getOboUserId();

    // Use uuid as the subscription Id
    String subscrId = UUID.randomUUID().toString();
    log.trace(LibUtils.getMsgAuth("NTFLIB_CREATE_TRACE", rUser, subscrId));

    // Determine the subscription TTL
    int subscrTTL = DEFAULT_TEST_TTL;
    if (StringUtils.isBlank(ttl))
    {
      // If TTL provided is not an integer then it is an error
      try { subscrTTL = Integer.parseInt(ttl); }
      catch (NumberFormatException e)
      {
        throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_TEST_TTL_NOTINT", rUser, ttl));
      }
    }

    // Build the callback delivery method
    String callbackStr = String.format("%s/%s", baseServiceUrl, subscrId);
    DeliveryMethod dm = new DeliveryMethod(DeliveryMethod.DeliveryType.WEBHOOK, callbackStr);
    var dmList = Collections.singletonList(dm);

    // Set other test subscription properties
    String typeFilter = TEST_SUBSCR_TYPE_FILTER;
    String subjFilter = subscrId;

    // Create the subscription
    // NOTE: Might be able to call the svc method createSubscription() but creating here avoids some overhead.
    //   For example, the auth check is not needed and could potentially cause problems.
    Subscription sub1 = new Subscription(-1, tenantId, subscrId, null, owner, true, typeFilter, subjFilter, dmList,
                                         subscrTTL, null, null, null, null, null);
    // If subscription already exists it is an error. Unlikely since it is a UUID
    if (dao.checkForSubscription(tenantId, subscrId))
    {
      throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_EXISTS", rUser, subscrId));
    }

    // Set defaults, resolve variables and check constraints.
    sub1.setDefaults();
    sub1.resolveVariables(rUser.getOboUserId());
    validateSubscription(rUser, sub1);

    // Construct Json string representing the Subscription about to be created
    Subscription scrubbedSubscription = new Subscription(sub1);
    String createJsonStr = TapisGsonUtils.getGson().toJson(scrubbedSubscription);

    // Compute the expiry time from now
    Instant expiry = Subscription.computeExpiryFromNow(sub1.getTtl());

    // Persist the subscription
    // NOTE: no need to rollback since only one DB transaction
    // If publishing event fails let user cleanup using delete since this is for a test
    dao.createSubscription(rUser, sub1, expiry, createJsonStr, null);

    // Persist the initial test sequence record
    dao.createTestSequence(rUser, subscrId);

    // Create and publish an event
    URI eventSource = new URI(baseServiceUrl);
    String eventType = TEST_EVENT_TYPE;
    String eventSubject = subscrId;
    String eventSeries = null;
    String eventTime = OffsetDateTime.now().toString();
    UUID eventUUID = UUID.randomUUID();
    Event event = new Event(tenantId, eventSource, eventType, eventSubject, eventSeries, eventTime, eventUUID);
    MessageBroker.getInstance().publishEvent(rUser, event);

    return subscrId;
  }

  /**
   * Retrieve a test sequence
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subscriptionId - UUID of the subscription associated with the test sequence.
   */
  @Override
  public TestSequence getTestSequence(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subscriptionId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    // Extract various names for convenience
    String resourceTenantId = rUser.getOboTenantId();

    // We need owner to check auth and if sub not there cannot find owner, so if sub does not exist then return null
    if (!dao.checkForSubscription(resourceTenantId, subscriptionId)) return null;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subscriptionId, null, null, null);

    TestSequence result = dao.getTestSequence(resourceTenantId, subscriptionId);
    return result;
  }

  /**
   * Delete events and subscription associated with a test sequence
   * Provided subscription must have been created using the beginTestSequence endpoint.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subscriptionId - UUID of the subscription associated with the test sequence.
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int deleteTestSequence(ResourceRequestUser rUser, String subscriptionId)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    SubscriptionOperation op = SubscriptionOperation.delete;
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subscriptionId)) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    // If subscription does not exist then 0 changes
    if (!dao.checkForSubscription(rUser.getOboTenantId(), subscriptionId)) return 0;

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, op, subscriptionId, null, null, null);

    // Check that subscription exists in the notifications_test table.
    // If not it is an error
    if (!dao.checkForTestSequence(rUser.getOboTenantId(), subscriptionId))
    {
      throw new TapisException(LibUtils.getMsgAuth("NTFLIB_TEST_DEL_NOT_SEQ", rUser, subscriptionId));
    }
    // Delete the subscription, the cascade should delete all events, so we are done
    return dao.deleteSubscription(rUser.getOboTenantId(), subscriptionId);
  }

  /**
   * Record an event received as part of a test sequence.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subscriptionId - UUID of the subscription associated with the test sequence.
   * @param event - event received
   * @throws IllegalStateException - if test sequence does not exist
   * @throws TapisException - on error
   */
  @Override
  public void recordTestEvent(ResourceRequestUser rUser, String subscriptionId, Event event)
          throws TapisException, IllegalStateException
  {
    dao.addTestSequenceEvent(rUser, subscriptionId, event);
  }


  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   * Update enabled attribute for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subId - name of subscription
   * @param subscriptionOp - operation, enable or disable
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  private int updateEnabled(ResourceRequestUser rUser, String subId, SubscriptionOperation subscriptionOp)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(subId))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    String resourceTenantId = rUser.getOboTenantId();

    // Subscription must already exist
    if (!dao.checkForSubscription(resourceTenantId, subId))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, subId));

    // ------------------------- Check service level authorization -------------------------
    checkAuth(rUser, subscriptionOp, subId, null, null, null);

    // ----------------- Make update --------------------
    if (subscriptionOp == SubscriptionOperation.enable)
      dao.updateEnabled(rUser, resourceTenantId, subId, true);
    else
      dao.updateEnabled(rUser, resourceTenantId, subId, false);
    return 1;
  }

  /**
   * Get Security Kernel client
   * Note: The service always calls SK as itself.
   * @return SK client
   * @throws TapisException - for Tapis related exceptions
   */
  private SKClient getSKClient() throws TapisException
  {
    SKClient skClient;
    String tenantName = siteAdminTenantId;
    String userName = SERVICE_NAME;
    try
    {
      skClient = serviceClients.getClient(userName, tenantName, SKClient.class);
    }
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("TAPIS_CLIENT_NOT_FOUND", TapisConstants.SERVICE_NAME_SECURITY, tenantName, userName);
      throw new TapisException(msg, e);
    }

    return skClient;
  }

  /**
   * Check constraints on Subscription attributes.
   * Collect and report as many errors as possible, so they can all be fixed before next attempt
   * @param sub - the Subscription to check
   * @throws IllegalStateException - if any constraints are violated
   */
  private void validateSubscription(ResourceRequestUser rUser, Subscription sub) throws IllegalStateException
  {
    // Make api level checks, i.e. checks that do not involve a dao or service call.
    List<String> errMessages = sub.checkAttributeRestrictions();

    // Now make checks that do require a dao or service call.
    // NOTE: Currently no such checks

    // If validation failed throw an exception
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(rUser, sub.getId(), errMessages);
      log.error(allErrors);
      throw new IllegalStateException(allErrors);
    }
  }

  /**
   * Retrieve set of user permissions given sk client, user, tenant, id
   * @param skClient - SK client
   * @param userName - name of user
   * @param tenantName - name of tenant
   * @param resourceId - Id of resource
   * @return - Set of Permissions for the user
   */
  private static Set<Permission> getUserPermSet(SKClient skClient, String userName, String tenantName,
                                                String resourceId)
          throws TapisClientException
  {
    var userPerms = new HashSet<Permission>();
    for (Permission perm : Permission.values())
    {
      String permSpec = String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), resourceId);
      if (skClient.isPermitted(tenantName, userName, permSpec)) userPerms.add(perm);
    }
    return userPerms;
  }

  /**
   * Create a set of individual permSpec entries based on the list passed in
   * @param permList - list of individual permissions
   * @return - Set of permSpec entries based on permissions
   */
  private static Set<String> getPermSpecSet(String tenantName, String subscriptionId, Set<Permission> permList)
  {
    var permSet = new HashSet<String>();
    for (Permission perm : permList) { permSet.add(getPermSpecStr(tenantName, subscriptionId, perm)); }
    return permSet;
  }

  /**
   * Create a permSpec given a permission
   * @param perm - permission
   * @return - permSpec entry based on permission
   */
  private static String getPermSpecStr(String tenantName, String subscriptionId, Permission perm)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, perm.name(), subscriptionId);
  }

  /**
   * Create a permSpec for all permissions
   * @return - permSpec entry for all permissions
   */
  private static String getPermSpecAllStr(String tenantName, String subId)
  {
    return String.format(PERM_SPEC_TEMPLATE, tenantName, "*", subId);
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(ResourceRequestUser rUser, String subId, List<String> msgList) {
    var sb = new StringBuilder(LibUtils.getMsgAuth("NTFLIB_CREATE_INVALID_ERRORLIST", rUser, subId));
    sb.append(System.lineSeparator());
    if (msgList == null || msgList.isEmpty()) return sb.toString();
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   * Check to see if owner is trying to update permissions for themselves.
   * If so throw an exception because this would be confusing since owner always has full permissions.
   * For an owner permissions are never checked directly.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param tenant Subscription tenant
   * @param id Subscription id
   * @param userName user for whom perms are being updated
   * @param opStr Operation in progress, for logging
   * @return name of owner
   */
  private String checkForOwnerPermUpdate(ResourceRequestUser rUser, String tenant, String id,
                                         String userName, String opStr)
          throws TapisException, NotAuthorizedException
  {
    // Look up owner. If not found then consider not authorized. Very unlikely at this point.
    String owner = dao.getSubscriptionOwner(rUser.getOboTenantId(), id);
    if (StringUtils.isBlank(owner))
      throw new NotAuthorizedException(LibUtils.getMsgAuth("NTFLIB_UNAUTH", rUser, id, opStr), NO_CHALLENGE);
    // If owner making the request and owner is the target user for the perm update then reject.
    if (owner.equals(rUser.getOboUserId()) && owner.equals(userName))
    {
      // If it is a svc making request reject with no auth, if user making request reject with special message.
      // Need this check since svc not allowed to update perms but checkAuth happens after checkForOwnerPermUpdate.
      // Without this the op would be denied with a misleading message.
      // Unfortunately this means auth check for svc in 2 places but not clear how to avoid it.
      //   On the bright side it means at worst operation will be denied when maybe it should be allowed which is better
      //   than the other way around.
      if (rUser.isServiceRequest()) throw new NotAuthorizedException(LibUtils.getMsgAuth("NTFLIB_UNAUTH", rUser, id, opStr), NO_CHALLENGE);
      else throw new TapisException(LibUtils.getMsgAuth("NTFLIB_PERM_OWNER_UPDATE", rUser, id, opStr));
    }
    return owner;
  }

  /**
   * Standard service level authorization check. Check is different for service and user requests.
   * A check should be made for subscription existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and authorization is denied.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param subId - name of the subscription
   * @param owner - subscription owner
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuth(ResourceRequestUser rUser, SubscriptionOperation op, String subId,
                         String owner, String userIdToCheck, Set<Permission> perms)
      throws TapisException, TapisClientException, NotAuthorizedException
  {
    // Check service and user requests separately to avoid confusing a service name with a user name
    if (rUser.isServiceRequest())
    {
      // This is a service request. The user name will be the service name. E.g. files, jobs, streams, etc
// TODO/TBD: Any service can read subscriptions
//      if (op == SubscriptionOperation.read && SVCLIST_READ.contains(rUser.getJwtUserId())) return;
      if (op == SubscriptionOperation.read) return;
    }
    else
    {
      // User check
      checkAuthUser(rUser, op, null, null, subId, owner, userIdToCheck, perms);
      return;
    }
    // Not authorized, throw an exception
    throw new NotAuthorizedException(LibUtils.getMsgAuth("NTFLIB_UNAUTH", rUser, subId, op.name()), NO_CHALLENGE);
  }

  /**
   * User based authorization check.
   * Can be used for OBOUser type checks.
   * By default, use JWT tenant and user from rUser, allow for optional tenant or user.
   * A check should be made for subscription existence before calling this method.
   * If no owner is passed in and one cannot be found then an error is logged and
   *   authorization is denied.
   * Operations:
   *  Create -      must be owner or have admin role
   *  Delete -      must be owner or have admin role
   *  ChangeOwner - must be owner or have admin role
   *  GrantPerm -   must be owner or have admin role
   *  Read -     must be owner or have admin role or have READ or MODIFY permission or be in list of allowed services
   *  getPerms - must be owner or have admin role or have READ or MODIFY permission or be in list of allowed services
   *  Modify - must be owner or have admin role or have MODIFY permission
   *  RevokePerm -  must be owner or have admin role or apiUserId=targetUser and meet certain criteria (allowUserRevokePerm)
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param op - operation name
   * @param tenantIdToCheck - optional name of the tenant to use. Default is to use authenticatedUser.
   * @param userIdToCheck - optional name of the user to check. Default is to use authenticatedUser.
   * @param subId - id of the subscription
   * @param owner - owner of the subscription
   * @param perms - List of permissions for the revokePerm case
   * @throws NotAuthorizedException - apiUserId not authorized to perform operation
   */
  private void checkAuthUser(ResourceRequestUser rUser, SubscriptionOperation op,
                             String tenantIdToCheck, String userIdToCheck,
                             String subId, String owner, String targetUser, Set<Permission> perms)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantIdToCheck) ? rUser.getOboTenantId() : tenantIdToCheck);
    String userName = (StringUtils.isBlank(userIdToCheck) ? rUser.getJwtUserId() : userIdToCheck);

    // Most checks require owner. If no owner specified and owner cannot be determined then log an error and deny.
    if (StringUtils.isBlank(owner)) owner = dao.getSubscriptionOwner(tenantName, subId);
    if (StringUtils.isBlank(owner)) {
      String msg = LibUtils.getMsgAuth("NTFLIB_AUTH_NO_OWNER", rUser, subId, op.name());
      log.error(msg);
      throw new NotAuthorizedException(msg, NO_CHALLENGE);
    }
    switch(op) {
      case create:
      case enable:
      case disable:
      case delete:
      case changeOwner:
//      case grantPerms:
        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName)) return;
        break;
      case read:
//      case getPerms:
        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName)) return;
        break;
//        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName) ||
//              isPermittedAny(rUser, tenantName, userName, subId, READMODIFY_PERMS))
//          return;
//        break;
      case modify:
      case updateTTL:
        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName)) return;
        break;
//        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName) ||
//                isPermitted(rUser, tenantName, userName, subId, Permission.MODIFY))
//          return;
//        break;
//      case revokePerms:
//        if (owner.equals(userName) || hasAdminRole(rUser, tenantName, userName) ||
//                (userName.equals(targetUser) &&
//                        allowUserRevokePerm(rUser, tenantName, userName, subId, perms)))
//          return;
//        break;
    }
    // Not authorized, throw an exception
    throw new NotAuthorizedException(LibUtils.getMsgAuth("NTFLIB_UNAUTH", rUser, subId, op.name()), NO_CHALLENGE);
  }

  /**
   * Determine all subscriptions that a user is allowed to see.
   * If all subscriptions return null else return list of subscription IDs
   * An empty list indicates no subscriptions allowed.
   * TODO/TBD: User Perm model as in Apps/Systems or return all resources owned by the user.
   */
  private Set<String> prvtGetAllowedSubscriptionIDs(ResourceRequestUser rUser)
          throws TapisException, TapisClientException
  {
    // If requester is a service calling as itself or an admin then all resources allowed
    if (rUser.isServiceRequest() && rUser.getJwtUserId().equals(rUser.getOboUserId()) ||
            hasAdminRole(rUser, null, null))
    {
      return null;
    }
    return dao.getSubscriptionIDsByOwner(rUser.getOboTenantId(), rUser.getOboUserId());
//    var subscriptionIDs = new HashSet<String>();
//    var userPerms = getSKClient().getUserPerms(rUser.getOboTenantId(), rUser.getOboUserId());
//    // Check each perm to see if it allows user READ access.
//    for (String userPerm : userPerms)
//    {
//      if (StringUtils.isBlank(userPerm)) continue;
//      // Split based on :, permSpec has the format subscr:<tenant>:<perms>:<system_name>
//      // NOTE: This assumes value in last field is always an id and never a wildcard.
//      String[] permFields = COLON_SPLIT.split(userPerm);
//      if (permFields.length < 4) continue;
//      if (permFields[0].equals(PERM_SPEC_PREFIX) &&
//           (permFields[2].contains(Permission.READ.name()) ||
//            permFields[2].contains(Permission.MODIFY.name()) ||
//            permFields[2].contains(Subscription.PERMISSION_WILDCARD)))
//      {
//        subscriptionIDs.add(permFields[3]);
//      }
//    }
//    return subscriptionIDs;
  }

  /**
   * Check to see if a user has the service admin role
   * By default use rUser, allow for optional tenant or user.
   */
  private boolean hasAdminRole(ResourceRequestUser rUser, String tenantToCheck, String userToCheck)
          throws TapisException, TapisClientException
  {
    // Use JWT tenant and user from authenticatedUsr or optional provided values
    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
    return getSKClient().isAdmin(tenantName, userName);
  }

//  /**
//   * Check to see if a user has the specified permission
//   * By default use JWT tenant and user from rUser, allow for optional tenant or user.
//   */
//  private boolean isPermitted(ResourceRequestUser rUser, String tenantToCheck, String userToCheck,
//                              String subId, Permission perm)
//          throws TapisException, TapisClientException
//  {
//    // Use JWT tenant and user from authenticatedUsr or optional provided values
//    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
//    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
//    var skClient = getSKClient();
//    String permSpecStr = getPermSpecStr(tenantName, subId, perm);
//    return skClient.isPermitted(tenantName, userName, permSpecStr);
//  }
//
//  /**
//   * Check to see if a user has any of the set of permissions
//   * By default use JWT tenant and user from rUser, allow for optional tenant or user.
//   */
//  private boolean isPermittedAny(ResourceRequestUser rUser, String tenantToCheck, String userToCheck,
//                                 String subId, Set<Permission> perms)
//          throws TapisException, TapisClientException
//  {
//    // Use JWT tenant and user from authenticatedUsr or optional provided values
//    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
//    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
//    var skClient = getSKClient();
//    var permSpecs = new ArrayList<String>();
//    for (Permission perm : perms) {
//      permSpecs.add(getPermSpecStr(tenantName, subId, perm));
//    }
//    return skClient.isPermittedAny(tenantName, userName, permSpecs.toArray(Subscription.EMPTY_STR_ARRAY));
//  }
//
//  /**
//   * Check to see if a user who is not owner or admin is authorized to revoke permissions
//   * By default use JWT tenant and user from rUser, allow for optional tenant or user.
//   */
//  private boolean allowUserRevokePerm(ResourceRequestUser rUser, String tenantToCheck, String userToCheck,
//                                      String subId, Set<Permission> perms)
//          throws TapisException, TapisClientException
//  {
//    // Perms should never be null. Fall back to deny as best security practice.
//    if (perms == null) return false;
//    // Use JWT tenant and user from authenticatedUsr or optional provided values
//    String tenantName = (StringUtils.isBlank(tenantToCheck) ? rUser.getOboTenantId() : tenantToCheck);
//    String userName = (StringUtils.isBlank(userToCheck) ? rUser.getJwtUserId() : userToCheck);
//    if (perms.contains(Permission.MODIFY)) return isPermitted(rUser, tenantName, userName, subId, Permission.MODIFY);
//    if (perms.contains(Permission.READ)) return isPermittedAny(rUser, tenantName, userName, subId, READMODIFY_PERMS);
//    return false;
//  }
//
//  /**
//   * Remove all SK artifacts associated with an Subscription: user permissions, Subscription role
//   * No checks are done for incoming arguments and the subscription must exist
//   */
//  private void removeSKArtifacts(String resourceTenantId, String subId)
//          throws TapisException, TapisClientException
//  {
//    var skClient = getSKClient();
//
//    // Use Security Kernel client to find all users with perms associated with the subscription.
//    String permSpec = String.format(PERM_SPEC_TEMPLATE, resourceTenantId, "%", subId);
//    var userNames = skClient.getUsersWithPermission(resourceTenantId, permSpec);
//    // Revoke all perms for all users
//    for (String userName : userNames)
//    {
//      revokePermissions(skClient, resourceTenantId, subId, userName, ALL_PERMS);
//      // Remove wildcard perm
//      skClient.revokeUserPermission(resourceTenantId, userName, getPermSpecAllStr(resourceTenantId, subId));
//    }
//  }
//
//  /**
//   * Revoke permissions
//   * No checks are done for incoming arguments and the subscription must exist
//   */
//  private static int revokePermissions(SKClient skClient, String resourceTenantId, String subId, String userName, Set<Permission> permissions)
//          throws TapisClientException
//  {
//    // Create a set of individual permSpec entries based on the list passed in
//    Set<String> permSpecSet = getPermSpecSet(resourceTenantId, subId, permissions);
//    // Remove perms from default user role
//    for (String permSpec : permSpecSet)
//    {
//      skClient.revokeUserPermission(resourceTenantId, userName, permSpec);
//    }
//    return permSpecSet.size();
//  }

  /**
   * Create an updated Subscription based on the subscription created from a PUT request.
   * Attributes that cannot be updated and must be filled in from the original resource:
   *   tenant, id, owner, enabled
   */
  private Subscription createUpdatedSubscription(Subscription origSub, Subscription putSub)
  {
    // Rather than exposing otherwise unnecessary setters we use a special constructor.
    Subscription updatedSub = new Subscription(putSub, origSub.getTenant(), origSub.getId());
    updatedSub.setOwner(origSub.getOwner());
    updatedSub.setEnabled(origSub.isEnabled());
    return updatedSub;
  }

  /**
   * Merge a patch into an existing Subscription
   * Attributes that can be updated:
   *   description, typeFilter, subjectFilter, deliveryMethods, notes.
   */
  private Subscription createPatchedSubscription(Subscription o, PatchSubscription p)
  {
    // Start off with the current subscription
    Subscription sub1 = new Subscription(o);
    // Now update fields that are being patched
    if (p.getDescription() != null) sub1.setDescription(p.getDescription());
    if (p.getTypeFilter() != null) sub1.setTypeFilter(p.getTypeFilter());
    if (p.getSubjectFilter() != null) sub1.setSubjectFilter(p.getSubjectFilter());
    if (p.getDeliveryMethods() != null) sub1.setDeliveryMethods(p.getDeliveryMethods());
    if (p.getTtl() != null) sub1.setTtl(p.getTtl());
    if (p.getNotes() != null) sub1.setNotes(p.getNotes());
    return sub1;
  }
}
