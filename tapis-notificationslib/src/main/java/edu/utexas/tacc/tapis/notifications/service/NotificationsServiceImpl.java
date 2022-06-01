package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import edu.utexas.tacc.tapis.notifications.client.NotificationsClient;
import edu.utexas.tacc.tapis.notifications.client.gen.model.TapisSubscription;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
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
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
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

  private static final String SERVICE_NAME = TapisConstants.SERVICE_NAME_NOTIFICATIONS;
  // Message keys
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
   * Check dispatcher service
   * @return null if all OK else return an Exception
   */
  public Exception checkDispatcher()
  {
    Exception retValue = null;

    // Create a client to talk to ourselves in order to kick off a TestSequence
    NotificationsClient ntfClient;
    String tenantName = siteAdminTenantId;
    String userName = SERVICE_NAME;
    // Create a ResourceRequestUser to match the client so we can make service calls as if they had been received by the client
    AuthenticatedUser authUser = new AuthenticatedUser(SERVICE_NAME, siteAdminTenantId, TapisThreadContext.AccountType.service.name(),
            null, userName, tenantName, null, null, null);
    ResourceRequestUser rUser = new ResourceRequestUser(authUser);
    // Note we could call service beginSequence() directly, but we would have to duplicate code for computing baseUrl
    // Although slower it is probably a better check to call ourselves via the client.
    String owner = "N/A";
    String name = "N/A";
    try
    {
      ntfClient = serviceClients.getClient(userName, tenantName, NotificationsClient.class);
      // Must add Content-Type header
      ntfClient.addDefaultHeader("Content-Type", "application/json");
      // If running in local test mode then reset the base url so we can talk to ourselves locally
      if (RuntimeParameters.getInstance().isLocalTest())
      {
        ntfClient.setBasePath("http://localhost:8080");
      }
      TapisSubscription subscription = ntfClient.beginTestSequence(DEFAULT_TEST_TTL);
      owner = subscription.getOwner();
      name = subscription.getName();
      log.debug(LibUtils.getMsg("NTFLIB_DSP_CHECK_BEGIN", owner, name));
      waitForTestSequenceStart(tenantName, owner, name);
      dao.deleteSubscription(tenantName, owner, name);
      log.debug(LibUtils.getMsg("NTFLIB_DSP_CHECK_END", owner, name));
    }
    catch (Exception e)
    {
      String msg = LibUtils.getMsg("NTFLIB_DSP_CHECK_ERR", owner, name, e.getMessage());
      log.warn(msg);
      retValue = new TapisException(msg, e);
    }
    return retValue;
  }

  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a new resource given a subscription and the text used to create the subscription.
   * Secrets in the text (if any) should be masked.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param subscription - Pre-populated Subscription object
   * @param scrubbedText - Text used to create the Subscription object - secrets should be scrubbed.
   * @return subscription name
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - subscription exists OR subscription in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public String createSubscription(ResourceRequestUser rUser, Subscription subscription, String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException, NotAuthorizedException
  {
    SubscriptionOperation op = SubscriptionOperation.create;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (subscription == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));

    // Only services may create subscriptions. Reject if not a service.
    if (!rUser.isServiceRequest())
    {
      String msg = LibUtils.getMsgAuth("NTFLIB_SUBSCR_UNAUTH", rUser);
      log.warn(msg);
      throw new NotAuthorizedException(msg, NO_CHALLENGE);
    }

    // Trace the call
    log.trace(LibUtils.getMsgAuth("NTFLIB_CREATE_TRACE", rUser, scrubbedText));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();

    // If necessary create a unique name for the subscription
    // sub1 is the subscription object that will be passed to the dao layer.
    Subscription sub1;
// TODO   // If no name provided then fill in with something unique but descriptive
//   include owner + subject(?) + ? + sequence number of some kind(?)
//    if (StringUtils.isBlank(subscriptionName))
//    {
//      subscriptionName = UUID.randomUUID().toString();
//      sub1 = new Subscription(subscription, oboTenant, subscriptionName);
//    }
//    else
//    {
//      sub1 = subscription;
//    }
    sub1 = subscription;
    String name = sub1.getName();

    // Resolve variables. Currently, this is only for owner which can be set to ${apiUserId}
    // We need owner resolved before we check auth
    subscription.resolveVariables(oboUser);
    String owner = subscription.getOwner();

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    // Check if subscription already exists
    if (dao.checkForSubscription(oboTenant, owner, name))
      throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_EXISTS", rUser, owner, name));

    // ---------------- Check constraints on Subscription attributes ------------------------
    validateSubscription(rUser, sub1);

    // Compute the expiry time from now
    Instant expiry = Subscription.computeExpiryFromNow(sub1.getTtlMinutes());

    // Creation of subscription in a single txn, no need for rollback
    dao.createSubscription(rUser, sub1, expiry);
    return name;
  }

  /**
   * Update existing subscription given a PatchSubscription and the text used to create the PatchSubscription.
   * Secrets in the text should be masked.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name subscription name
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
  public void patchSubscription(ResourceRequestUser rUser, String owner, String name, PatchSubscription patchSubscription,
                                String scrubbedText)
          throws TapisException, TapisClientException, IllegalStateException, IllegalArgumentException,
                 NotAuthorizedException, NotFoundException
  {
    SubscriptionOperation op = SubscriptionOperation.modify;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (patchSubscription == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NULL_INPUT", rUser));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name) || StringUtils.isBlank(scrubbedText))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    // Subscription must already exist
    if (!dao.checkForSubscription(oboTenant, owner, name))
      throw new NotFoundException(LibUtils.getMsgAuth("NTFLIB_VER_NOT_FOUND", rUser, owner, name));

    // Retrieve the subscription being patched and create fully populated Subscription with changes merged in
    Subscription origSubscription = dao.getSubscription(oboTenant, owner, name);
    Subscription patchedSubscription = createPatchedSubscription(origSubscription, patchSubscription);

    // ---------------- Check constraints on Subscription attributes ------------------------
    validateSubscription(rUser, patchedSubscription);

    // Patch subscription in a single txn, no need for rollback
    dao.patchSubscription(rUser, owner, name, patchedSubscription);
  }

  /**
   * Update enabled to true for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int enableSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    return updateEnabled(rUser, owner, name, SubscriptionOperation.enable);
  }

  /**
   * Update enabled to false for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public int disableSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    return updateEnabled(rUser, owner, name, SubscriptionOperation.disable);
  }

  /**
   * Delete a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - name of subscription
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int deleteSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, IllegalArgumentException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.delete;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    // If subscription does not exist then 0 changes
    if (!dao.checkForSubscription(rUser.getOboTenantId(), owner, name)) return 0;

    // Delete the subscription
    return dao.deleteSubscription(rUser.getOboTenantId(), owner, name);
  }

  /**
   * Update TTL for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - name of subscription
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
  public int updateSubscriptionTTL(ResourceRequestUser rUser, String owner, String name, String newTTLStr)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException,
                 NotFoundException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.updateTTL;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name) || StringUtils.isBlank(newTTLStr))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    // Subscription must already exist
    if (!dao.checkForSubscription(oboTenant, owner, name))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, owner, name));

    // If TTL provided is not an integer then throw an exception
    int newTTL;
    try { newTTL = Integer.parseInt(newTTLStr); }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_TTL_NOTINT", rUser, name, newTTLStr));
    }

    // ----------------- Make update --------------------
    dao.updateSubscriptionTTL(oboTenant, owner, name, newTTL, Subscription.computeExpiryFromNow(newTTL));
    return 1;
  }

  /**
   * isEnabled
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - Name of the subscription
   * @return true if subscription is enabled, false otherwise
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  @Override
  public boolean isEnabled(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, NotFoundException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();

    // Resource must exist
    if (!dao.checkForSubscription(oboTenant, owner, name))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, owner, name));

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    return dao.isEnabled(oboTenant, owner, name);
  }

  /**
   * getSubscription
   * Retrieve a subscription.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param name - Name of the subscription
   * @return populated instance of an Subscription or null if not found or user not authorized.
   * @throws TapisException - for Tapis related exceptions
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public Subscription getSubscription(ResourceRequestUser rUser, String owner, String name)
          throws TapisException, NotAuthorizedException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();

    // if sub does not exist then return null
    if (!dao.checkForSubscription(oboTenant, owner, name)) return null;

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    Subscription result = dao.getSubscription(oboTenant, owner, name);
    return result;
  }

  /**
   * Get count of all subscriptions matching certain criteria
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param searchList - optional list of conditions used for searching
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return Count of subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public int getSubscriptionsTotalCount(ResourceRequestUser rUser, String owner, List<String> searchList,
                                        List<OrderBy> orderByList, String startAfter)
          throws TapisException, TapisClientException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, "N/A", SubscriptionOperation.read));

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

    // Get allowed list of subscription names
    // This is either all subscriptions (null) or a list of names.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionNames(rUser, owner);

    // If none are allowed we know count is 0
    if (allowedIDs != null && allowedIDs.isEmpty()) return 0;

    // Count all allowed resources matching the search conditions
    return dao.getSubscriptionsCount(rUser.getOboTenantId(), owner, verifiedSearchList, null, allowedIDs, orderByList, startAfter);
  }

  /**
   * Get all subscriptions
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param searchList - optional list of conditions used for searching
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return List of Subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<Subscription> getSubscriptions(ResourceRequestUser rUser, String owner, List<String> searchList,
                                             int limit, List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, "N/A", SubscriptionOperation.read));

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

    // Get list of subscription names
    // This is either all subscriptions (null) or a list of IDs.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionNames(rUser, owner);

    // Get all allowed subscriptions matching the search conditions
    return dao.getSubscriptions(rUser.getOboTenantId(), owner, verifiedSearchList, null, allowedIDs, limit,
                                orderByList, skip, startAfter);
  }

  /**
   * Get all subscriptions
   * Use provided string containing a valid SQL where clause for the search.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner subscription owner
   * @param sqlSearchStr - string containing a valid SQL where clause
   * @return List of Subscription objects
   * @throws TapisException - for Tapis related exceptions
   */
  @Override
  public List<Subscription> getSubscriptionsUsingSqlSearchStr(ResourceRequestUser rUser, String owner, String sqlSearchStr,
                                                              int limit, List<OrderBy> orderByList, int skip, String startAfter)
          throws TapisException, TapisClientException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, "N/A", SubscriptionOperation.read));

    // If search string is empty delegate to getSubscriptions()
    if (StringUtils.isBlank(sqlSearchStr)) return getSubscriptions(rUser, owner, null, limit, orderByList, skip, startAfter);


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

    // Get list of IDs of subscriptions
    // This is either all subscriptions (null) or a list of IDs.
    Set<String> allowedIDs = prvtGetAllowedSubscriptionNames(rUser, owner);

    // Get all allowed subscriptions matching the search conditions
    return dao.getSubscriptions(rUser.getOboTenantId(), owner, null, searchAST, allowedIDs, limit, orderByList, skip, startAfter);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------

  /**
   * Post an Event to the queue. Only services may publish events.
   * First field of type must match the service name.
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param event - Pre-populated Event object
   * @throws IOException - on error
   * @throws IllegalArgumentException - if
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public void publishEvent(ResourceRequestUser rUser, Event event) throws IOException, IllegalArgumentException, NotAuthorizedException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (event == null) throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_NULL_INPUT_EVENT", rUser));

    String msg;
    // Only services may publish. Reject if not a service.
    if (!rUser.isServiceRequest())
    {
      msg = LibUtils.getMsgAuth("NTFLIB_EVENT_UNAUTH", rUser);
      log.warn(msg);
      throw new NotAuthorizedException(msg, NO_CHALLENGE);
    }

    // If first field of type is not the service name then reject
    if (!event.getType1().equals(rUser.getJwtUserId()))
    {
      msg = LibUtils.getMsgAuth("NTFLIB_EVENT_SVC_NOMATCH", rUser, event.getType(), rUser.getJwtUserId());
      log.warn(msg);
      throw new IllegalArgumentException(msg);
    }

    // Publish the event
    MessageBroker.getInstance().publishEvent(rUser, event);
  }

  /**
   * Read an Event from the queue.
   * Event is removed from the queue.
   *   NOTE: currently only used for testing. Not part of public interface of service.
   * @throws TapisException - on error
   *
   */
  public Event readEvent(boolean autoAck) throws TapisException
  {
    return MessageBroker.getInstance().readEvent(autoAck);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Test Sequences ------------------------------
  // -----------------------------------------------------------------------
  /**
   * Start a test sequence by creating a subscription and publishing an event
   * Subscription name will be created as a UUID.
   * Subscription owner will be oboUser
   * User associated with the event will be oboUser
   * If baseServiceUrl is https://dev.tapis.io then
   *   event source will be https://dev.tapis.io/v3/notifications
   *   delivery callback will have the form https://dev.tapis.io/v3/notifications/640ad5a8-1a6e-4189-a334-c4c7226fb9ba
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param baseServiceUrl - Base URL for service. Used for callback and event source.
   * @param ttl - optional TTL for the auto-generated subscription
   * @return subscription name
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - subscription exists OR subscription in invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   */
  @Override
  public Subscription beginTestSequence(ResourceRequestUser rUser, String baseServiceUrl, String ttl)
          throws TapisException, IOException, URISyntaxException, IllegalStateException, IllegalArgumentException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();

    // Use uuid as the subscription name
    String name = UUID.randomUUID().toString();
    log.trace(LibUtils.getMsgAuth("NTFLIB_CREATE_TRACE", rUser, name));

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
    // Example https://dev.develop.tapis.io/v3/notifications/test/callback/<subscriptionId>
    String callbackStr = String.format("%s/test/callback/%s", baseServiceUrl, name);
    DeliveryTarget dm = new DeliveryTarget(DeliveryTarget.DeliveryMethod.WEBHOOK, callbackStr);
    var dmList = Collections.singletonList(dm);

    // Set other test subscription properties
    String typeFilter = TEST_SUBSCR_TYPE_FILTER;
    String subjFilter = name;

    // Create the subscription
    // NOTE: Might be able to call the svc method createSubscription() but creating here avoids some overhead.
    //   For example, the auth check is not needed and could potentially cause problems.
    Subscription sub1 = new Subscription(-1, oboTenant, oboUser, name, null, true, typeFilter, subjFilter, dmList,
                                         subscrTTL, null, null, null, null);
    // If subscription already exists it is an error. Unlikely since it is a UUID
    if (dao.checkForSubscription(oboTenant, oboUser, name))
      throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_EXISTS", rUser, oboUser, name));

    // Resolve variables and check constraints.
    sub1.resolveVariables(oboUser);
    validateSubscription(rUser, sub1);

    // Construct Json string representing the Subscription about to be created
    Subscription scrubbedSubscription = new Subscription(sub1);
    String createJsonStr = TapisGsonUtils.getGson().toJson(scrubbedSubscription);

    // Compute the expiry time from now
    Instant expiry = Subscription.computeExpiryFromNow(sub1.getTtlMinutes());

    // Persist the subscription
    // NOTE: no need to rollback since only one DB transaction
    // If publishing event fails let user cleanup using delete since this is for a test
    dao.createSubscription(rUser, sub1, expiry);

    // Persist the initial test sequence record
    dao.createTestSequence(rUser, name);

    // Create and publish an event
    URI eventSource = new URI(baseServiceUrl);
    String eventType = TEST_EVENT_TYPE;
    String eventSubject = name;
    String eventSeriesId = null;
    String eventTimeStamp = OffsetDateTime.now().toString();
    String eventData = null;
    UUID eventUUID = UUID.randomUUID();
    Event event = new Event(eventSource, eventType, eventSubject, eventData, eventSeriesId, eventTimeStamp,
                            oboTenant, oboUser, eventUUID);
    MessageBroker.getInstance().publishEvent(rUser, event);

    return dao.getSubscription(oboTenant, name, oboUser);
  }

  /**
   * Retrieve a test sequence
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param name - UUID of the subscription associated with the test sequence.
   */
  @Override
  public TestSequence getTestSequence(ResourceRequestUser rUser, String name)
          throws TapisException, TapisClientException
  {
    SubscriptionOperation op = SubscriptionOperation.read;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, rUser.getOboUserId(), name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, oboUser, name, op);

    TestSequence result = dao.getTestSequence(oboTenant, name, oboUser);
    return result;
  }

  /**
   * Delete events and subscription associated with a test sequence
   * Provided subscription must have been created using the beginTestSequence endpoint.
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param name - UUID of the subscription associated with the test sequence.
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   */
  @Override
  public int deleteTestSequence(ResourceRequestUser rUser, String name)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    SubscriptionOperation op = SubscriptionOperation.delete;
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, rUser.getOboUserId(), name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();

    // If subscription does not exist then 0 changes
    if (!dao.checkForSubscription(oboTenant, oboUser, name)) return 0;

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, oboUser, name, op);

    // Check that subscription exists in the notifications_test table.
    // If not it is an error
    if (!dao.checkForTestSequence(oboTenant, name, oboUser))
    {
      throw new TapisException(LibUtils.getMsgAuth("NTFLIB_TEST_DEL_NOT_SEQ", rUser, name, oboUser));
    }
    // Delete the subscription, the cascade should delete all events, so we are done
    return dao.deleteSubscription(oboTenant, name, oboUser);
  }

  /**
   * Record a notification received as part of a test sequence.
   *
   * @param tenant - tenant associated with event
   * @param user - user who published the event
   * @param subscrId - UUID of the subscription associated with the test sequence.
   * @param notification - notification received
   * @throws IllegalStateException - if test sequence does not exist
   * @throws TapisException - on error
   */
  @Override
  public void recordTestNotification(String tenant, String user, String subscrId, Notification notification)
          throws TapisException, IllegalStateException
  {
    dao.addTestSequenceNotification(tenant, user, subscrId, notification);
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   * Update enabled attribute for a subscription
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner - owner of subscription
   * @param name - name of subscription
   * @param op - operation, enable or disable
   * @return Number of items updated
   *
   * @throws TapisException - for Tapis related exceptions
   * @throws IllegalStateException - Resulting Subscription would be in an invalid state
   * @throws IllegalArgumentException - invalid parameter passed in
   * @throws NotAuthorizedException - unauthorized
   * @throws NotFoundException - Resource not found
   */
  private int updateEnabled(ResourceRequestUser rUser, String owner, String name, SubscriptionOperation op)
          throws TapisException, IllegalStateException, IllegalArgumentException, NotAuthorizedException, NotFoundException, TapisClientException
  {
    // Check inputs
    if (rUser == null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT_AUTHUSR"));
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name))
      throw new IllegalArgumentException(LibUtils.getMsgAuth("NTFLIB_ERROR_ARG", rUser, owner, name, op));
    // Extract various names for convenience
    String oboTenant = rUser.getOboTenantId();

    // ------------------------- Check authorization -------------------------
    checkAuth(rUser, owner, name, op);

    // Subscription must already exist
    if (!dao.checkForSubscription(oboTenant, owner, name))
      throw new NotFoundException(LibUtils.getMsgAuth(NOT_FOUND, rUser, owner, name));

    // ----------------- Make update --------------------
    if (op == SubscriptionOperation.enable)
      dao.updateEnabled(oboTenant, owner, name, true);
    else
      dao.updateEnabled(oboTenant, owner, name, false);
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
      String allErrors = getListOfErrors(rUser, sub.getOwner(), sub.getName(), errMessages);
      log.error(allErrors);
      throw new IllegalStateException(allErrors);
    }
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(ResourceRequestUser rUser, String owner, String name, List<String> msgList) {
    var sb = new StringBuilder(LibUtils.getMsgAuth("NTFLIB_CREATE_INVALID_ERRORLIST", rUser, owner, name));
    sb.append(System.lineSeparator());
    if (msgList == null || msgList.isEmpty()) return sb.toString();
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   * Determine all subscriptions that a user is allowed to see.
   * If all subscriptions return null else return list of subscription names
   * An empty list indicates no subscriptions allowed.
   */
  private Set<String> prvtGetAllowedSubscriptionNames(ResourceRequestUser rUser, String owner)
          throws TapisException, TapisClientException
  {
    // If requester is a service calling as itself or an admin then all resources allowed
    if (rUser.isServiceRequest() && rUser.getJwtUserId().equals(rUser.getOboUserId()) || hasAdminRole(rUser))
    {
      return null;
    }
    return dao.getSubscriptionIDsByOwner(rUser.getOboTenantId(), owner);
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
    if (p.getDeliveryMethods() != null) sub1.setDeliveryTargets(p.getDeliveryMethods());
    if (p.getTtlMinutes() != null) sub1.setTtlMinutes(p.getTtlMinutes());
    return sub1;
  }

  /**
   * Wait for the TestSquence associated with the subscription Id to start.
   * Check a few times and then give up by throwing a TapisException at the end
   *
   * @param name name of subscription
   * @throws TapisException On error
   */
  private void waitForTestSequenceStart(String tenant, String owner, String name) throws TapisException
  {
    // Try 4 times with a 5 second poll interval
    int NUM_TEST_START_ATTEMPTS = 4;
    int DSP_CHECK_START_POLL_MS = 5000;
    try
    {
      // Give it a short time to work before first check
      Thread.sleep(1000);
      for (int i = 0; i < NUM_TEST_START_ATTEMPTS; i++)
      {
        log.debug(LibUtils.getMsg("NTFLIB_DSP_CHECK_POLL1", i + 1, owner, name));
        TestSequence testSeq = dao.getTestSequence(tenant, owner, name);
        log.debug(LibUtils.getMsg("NTFLIB_DSP_CHECK_POLL2", testSeq.getNotificationCount(), owner, name));
        if (testSeq.getNotificationCount() > 0) return;
        Thread.sleep(DSP_CHECK_START_POLL_MS);
      }
    }
    catch (Exception e)
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DSP_CHECK_FAIL2", NUM_TEST_START_ATTEMPTS, owner, name));
    }
    throw new TapisException(LibUtils.getMsg("NTFLIB_DSP_CHECK_FAIL1", NUM_TEST_START_ATTEMPTS, owner, name));
  }

  // ************************************************************************
  // **************************  Auth checking ******************************
  // ************************************************************************

  /**
   * Owner based authorization check.
   *
   * All operations: oboUser must be owner or have admin role
   *
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param owner - owner of the subscription
   * @param name - id of the subscription
   * @param op - operation name
   * @throws NotAuthorizedException - user not authorized to perform operation
   */
  private void checkAuth(ResourceRequestUser rUser, String owner, String name, SubscriptionOperation op)
          throws TapisException, TapisClientException, NotAuthorizedException
  {
    String oboUser =  rUser.getJwtUserId();

    // All arguments must be provided else log an error and deny.
    if (StringUtils.isBlank(owner) || StringUtils.isBlank(name) || op == null)
    {
      String msg = LibUtils.getMsgAuth("NTFLIB_AUTH_NULL_ARG", rUser, owner, name, op);
      log.error(msg);
      throw new NotAuthorizedException(msg, NO_CHALLENGE);
    }
    switch(op) {
      case create:
      case enable:
      case disable:
      case delete:
      case read:
      case modify:
      case updateTTL:
        if (owner.equals(oboUser) || hasAdminRole(rUser)) return;
        break;
    }
    // Not authorized, throw an exception
    throw new NotAuthorizedException(LibUtils.getMsgAuth("NTFLIB_UNAUTH", rUser, name, op.name()), NO_CHALLENGE);
  }

  /**
   * Check to see if a user has the service admin role
   * By default use rUser, allow for optional tenant or user.
   */
  private boolean hasAdminRole(ResourceRequestUser rUser) throws TapisException, TapisClientException
  {
    return getSKClient().isAdmin(rUser.getOboTenantId(), rUser.getOboUserId());
  }
}
