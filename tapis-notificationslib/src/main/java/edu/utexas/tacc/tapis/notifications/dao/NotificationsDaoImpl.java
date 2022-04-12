package edu.utexas.tacc.tapis.notifications.dao;

import java.sql.Connection;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsLastEventRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsRecoveryRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flywaydb.core.Flyway;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import edu.utexas.tacc.tapis.search.parser.ASTBinaryExpression;
import edu.utexas.tacc.tapis.search.parser.ASTLeaf;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.search.parser.ASTUnaryExpression;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.OrderByDir;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.search.SearchUtils.SearchOperator;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.exceptions.recoverable.TapisDBConnectionException;
import edu.utexas.tacc.tapis.shareddb.datasource.TapisDataSource;

import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription.SubscriptionOperation;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.NotificationsTestsRecord;
import edu.utexas.tacc.tapis.notifications.gen.jooq.tables.records.SubscriptionsRecord;

import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.NOTIFICATIONS_RECOVERY;
import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.SUBSCRIPTIONS;
import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.SUBSCRIPTION_UPDATES;
import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.NOTIFICATIONS;
import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.NOTIFICATIONS_LAST_EVENT;
import static edu.utexas.tacc.tapis.notifications.gen.jooq.Tables.NOTIFICATIONS_TESTS;
import static edu.utexas.tacc.tapis.shared.threadlocal.OrderBy.DEFAULT_ORDERBY_DIRECTION;

/*
 * Class to handle persistence and queries for Tapis Subscription objects.
 */
public class NotificationsDaoImpl implements NotificationsDao
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(NotificationsDao.class);

  private static final String EMPTY_JSON = "{}";
  private static final int INVALID_SEQ_ID = -1;

  public static final JsonElement EMPTY_JSON_ARRAY = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final JsonElement EMPTY_JSON_ELEM = TapisGsonUtils.getGson().fromJson("{}", JsonElement.class);

  // Create a static Set of column names for table SUBSCRIPTIONS
  private static final Set<String> SUBSCRIPTIONS_FIELDS = new HashSet<>();
  static
  {
    for (Field<?> field : SUBSCRIPTIONS.fields()) { SUBSCRIPTIONS_FIELDS.add(field.getName()); }
  }

  // Compiled regexes for splitting around "\." and "\$"
  private static final Pattern DOT_SPLIT = Pattern.compile("\\.");
  private static final Pattern DOLLAR_SPLIT = Pattern.compile("\\$");

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  // -----------------------------------------------------------------------
  // ------------------------- General -------------------------------------
  // -----------------------------------------------------------------------

  /**
   * checkDB
   * Check that we can connect with DB and that the main table of the service exists.
   * @return null if all OK else return an exception
   */
  @Override
  public Exception checkDB()
  {
    Exception result = null;
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // execute SELECT to_regclass('tapis_ntf.subscriptions');
      // Build and execute a simple postgresql statement to check for the table
      String sql = "SELECT to_regclass('" + SUBSCRIPTIONS.getName() + "')";
      Result<Record> ret = db.resultQuery(sql).fetch();
      if (ret == null || ret.isEmpty() || ret.getValue(0,0) == null)
      {
        result = new TapisException(LibUtils.getMsg("NTFLIB_CHECKDB_NO_TABLE", SUBSCRIPTIONS.getName()));
      }
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      result = e;
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * migrateDB
   * Use Flyway to make sure DB schema is at the latest version
   */
  @Override
  public void migrateDB() throws TapisException
  {
    Flyway flyway = Flyway.configure().dataSource(getDataSource()).load();
    // TODO remove workaround if possible. Figure out how to deploy X.Y.Z-SNAPSHOT repeatedly.
    // Use repair as workaround to avoid checksum error during develop/deploy of SNAPSHOT versions when it is not
    // a true migration.
    flyway.repair();
    flyway.migrate();
  }

  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a new subscription.
   *
   * @return true if created
   * @throws TapisException - on error
   * @throws IllegalStateException - if resource already exists
   */
  @Override
  public boolean createSubscription(ResourceRequestUser rUser, Subscription subscr, Instant expiryI,
                                    String changeDescription, String rawData)
          throws TapisException, IllegalStateException
  {
    String opName = "createSubscription";
    // ------------------------- Check Input -------------------------
    if (subscr == null) LibUtils.logAndThrowNullParmException(opName, "subscription");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "changeDescription");
    if (StringUtils.isBlank(subscr.getTenant())) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(subscr.getId())) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");
    
    // Make sure owner, notes and deliveryMethods are set
    String owner = Subscription.DEFAULT_OWNER;
    if (StringUtils.isNotBlank(subscr.getOwner())) owner = subscr.getOwner();
    JsonObject notesObj = Subscription.DEFAULT_NOTES;
    if (subscr.getNotes() != null) notesObj = (JsonObject) subscr.getNotes();
    JsonElement deliveryMethodsJson = Subscription.DEFAULT_DELIVERY_METHODS;
    if (subscr.getDeliveryMethods() != null) deliveryMethodsJson = TapisGsonUtils.getGson().toJsonTree(subscr.getDeliveryMethods());

    // Convert expiry from Instant to LocalDateTime
    LocalDateTime expiry = (expiryI == null) ? null :  LocalDateTime.ofInstant(expiryI, ZoneOffset.UTC);

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if resource exists. If yes then throw IllegalStateException
      boolean doesExist = checkForSubscription(db, subscr.getTenant(), subscr.getId());
      if (doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_EXISTS", rUser, subscr.getId()));

      // Generate uuid for the new resource
      subscr.setUuid(UUID.randomUUID());

      // Insert the record
      Record record = db.insertInto(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.TENANT, subscr.getTenant())
              .set(SUBSCRIPTIONS.ID, subscr.getId())
              .set(SUBSCRIPTIONS.DESCRIPTION, subscr.getDescription())
              .set(SUBSCRIPTIONS.OWNER, owner)
              .set(SUBSCRIPTIONS.ENABLED, subscr.isEnabled())
              .set(SUBSCRIPTIONS.TYPE_FILTER, subscr.getTypeFilter())
              .set(SUBSCRIPTIONS.TYPE_FILTER1, subscr.getTypeFilter1())
              .set(SUBSCRIPTIONS.TYPE_FILTER2, subscr.getTypeFilter2())
              .set(SUBSCRIPTIONS.TYPE_FILTER3, subscr.getTypeFilter3())
              .set(SUBSCRIPTIONS.SUBJECT_FILTER, subscr.getSubjectFilter())
              .set(SUBSCRIPTIONS.DELIVERY_METHODS, deliveryMethodsJson)
              .set(SUBSCRIPTIONS.TTL, subscr.getTtl())
              .set(SUBSCRIPTIONS.NOTES, notesObj)
              .set(SUBSCRIPTIONS.UUID, subscr.getUuid())
              .set(SUBSCRIPTIONS.EXPIRY, expiry)
              .returningResult(SUBSCRIPTIONS.SEQ_ID)
              .fetchOne();

      // If record is null it is an error
      if (record == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("NTFLIB_DB_NULL_RESULT", rUser, subscr.getId(), opName));
      }

      // Generated sequence id
      int seqId = record.getValue(SUBSCRIPTIONS.SEQ_ID);

      if (seqId < 1) return false;

      // Persist update record
      addUpdate(db, rUser, subscr.getId(), seqId, SubscriptionOperation.create, changeDescription, rawData, subscr.getUuid());

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "subscriptions");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return true;
  }

  /**
   * Update all updatable attributes of an existing subscription.
   * Following columns will be updated:
   *   description, typeFilter, subjectFilter, deliveryMethods, notes.
   * @throws TapisException - on error
   * @throws IllegalStateException - if resource already exists
   */
  @Override
  public void putSubscription(ResourceRequestUser rUser, Subscription putSubscr, String changeDescription, String rawData)
          throws TapisException, IllegalStateException
  {
    String opName = "putSubscription";
    // ------------------------- Check Input -------------------------
    if (putSubscr == null) LibUtils.logAndThrowNullParmException(opName, "putSubscription");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    // Pull out some values for convenience
    String tenantId = putSubscr.getTenant();
    String subscrId = putSubscr.getId();
    // Check required attributes have been provided
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "changeDescription");
    if (StringUtils.isBlank(tenantId)) LibUtils.logAndThrowNullParmException(opName, "tenantId");
    if (StringUtils.isBlank(subscrId)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");
    if (StringUtils.isBlank(putSubscr.getTypeFilter())) LibUtils.logAndThrowNullParmException(opName, "typeFilter");

    // Make sure owner, notes and deliveryMethods are set
    String owner = Subscription.DEFAULT_OWNER;
    if (StringUtils.isNotBlank(putSubscr.getOwner())) owner = putSubscr.getOwner();
    JsonObject notesObj = Subscription.DEFAULT_NOTES;
    if (putSubscr.getNotes() != null) notesObj = (JsonObject) putSubscr.getNotes();
    JsonElement deliveryMethodsJson = Subscription.DEFAULT_DELIVERY_METHODS;
    if (putSubscr.getDeliveryMethods() != null) deliveryMethodsJson = TapisGsonUtils.getGson().toJsonTree(putSubscr.getDeliveryMethods());

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Make sure subscription exists.
      boolean doesExist = checkForSubscription(db, tenantId, subscrId);
      if (!doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NOT_FOUND", rUser, subscrId));

      // Make sure UUID filled in, needed for update record. Pre-populated putSubscription may not have it.
      UUID uuid = putSubscr.getUuid();
      if (uuid == null) uuid = getUUIDUsingDb(db, tenantId, subscrId);

      var result = db.update(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.DESCRIPTION, putSubscr.getDescription())
              .set(SUBSCRIPTIONS.TYPE_FILTER, putSubscr.getTypeFilter())
              .set(SUBSCRIPTIONS.TYPE_FILTER1, putSubscr.getTypeFilter1())
              .set(SUBSCRIPTIONS.TYPE_FILTER2 , putSubscr.getTypeFilter2())
              .set(SUBSCRIPTIONS.TYPE_FILTER3, putSubscr.getTypeFilter3())
              .set(SUBSCRIPTIONS.SUBJECT_FILTER, putSubscr.getSubjectFilter())
              .set(SUBSCRIPTIONS.DELIVERY_METHODS, deliveryMethodsJson)
              .set(SUBSCRIPTIONS.NOTES, notesObj)
              .set(SUBSCRIPTIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(subscrId))
              .returningResult(SUBSCRIPTIONS.SEQ_ID)
              .fetchOne();

      // If result is null it is an error
      if (result == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("NTFLIB_DB_NULL_RESULT", rUser, subscrId, opName));
      }

      int seqId = result.getValue(SUBSCRIPTIONS.SEQ_ID);

      // Persist update record
      addUpdate(db, rUser, putSubscr.getId(), seqId, SubscriptionOperation.modify, changeDescription, rawData, uuid);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "subscriptions");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Patch selected attributes of an existing subscription.
   * Following columns will be updated:
   *   description, typeFilter, subjectFilter, deliveryMethods, notes.
   * @throws TapisException - on error
   * @throws IllegalStateException - if resource already exists
   */
  @Override
  public void patchSubscription(ResourceRequestUser rUser, String subscriptionId, Subscription patchedSubscription,
                          String changeDescription, String rawData)
          throws TapisException, IllegalStateException {
    String opName = "patchSubscription";
    // ------------------------- Check Input -------------------------
    if (patchedSubscription == null) LibUtils.logAndThrowNullParmException(opName, "patchedSubscription");
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    // Pull out some values for convenience
    String tenant = rUser.getOboTenantId();
    if (StringUtils.isBlank(changeDescription)) LibUtils.logAndThrowNullParmException(opName, "changeDescription");
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(subscriptionId)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");

    // Make sure owner, notes and deliveryMethods are set
    String owner = Subscription.DEFAULT_OWNER;
    if (StringUtils.isNotBlank(patchedSubscription.getOwner())) owner = patchedSubscription.getOwner();
    JsonElement deliveryMethodsJson = Subscription.DEFAULT_DELIVERY_METHODS;
    if (patchedSubscription.getDeliveryMethods() != null) deliveryMethodsJson = TapisGsonUtils.getGson().toJsonTree(patchedSubscription.getDeliveryMethods());
    JsonObject notesObj =  Subscription.DEFAULT_NOTES;
    if (patchedSubscription.getNotes() != null) notesObj = (JsonObject) patchedSubscription.getNotes();

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Make sure subscription exists.
      boolean doesExist = checkForSubscription(db, tenant, subscriptionId);
      if (!doesExist) throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_SUBSCR_NOT_FOUND", rUser, subscriptionId));

      var result = db.update(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.DESCRIPTION, patchedSubscription.getDescription())
              .set(SUBSCRIPTIONS.TYPE_FILTER, patchedSubscription.getTypeFilter())
              .set(SUBSCRIPTIONS.TYPE_FILTER1, patchedSubscription.getTypeFilter1())
              .set(SUBSCRIPTIONS.TYPE_FILTER2, patchedSubscription.getTypeFilter2())
              .set(SUBSCRIPTIONS.TYPE_FILTER3, patchedSubscription.getTypeFilter3())
              .set(SUBSCRIPTIONS.SUBJECT_FILTER, patchedSubscription.getSubjectFilter())
              .set(SUBSCRIPTIONS.DELIVERY_METHODS, deliveryMethodsJson)
              .set(SUBSCRIPTIONS.NOTES, notesObj)
              .set(SUBSCRIPTIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(SUBSCRIPTIONS.TENANT.eq(tenant),SUBSCRIPTIONS.ID.eq(subscriptionId))
              .returningResult(SUBSCRIPTIONS.SEQ_ID)
              .fetchOne();

      // If result is null it is an error
      if (result == null)
      {
        throw new TapisException(LibUtils.getMsgAuth("NTFLIB_DB_NULL_RESULT", rUser, subscriptionId, opName));
      }

      int seqId = result.getValue(SUBSCRIPTIONS.SEQ_ID);

      // Persist update record
      addUpdate(db, rUser, subscriptionId, seqId, SubscriptionOperation.modify, changeDescription, rawData,
                patchedSubscription.getUuid());

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "subscriptions");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update attribute enabled for a subscription given subscription Id and value
   */
  @Override
  public void updateEnabled(ResourceRequestUser rUser, String tenantId, String id, boolean enabled)
          throws TapisException
  {
    String opName = "updateEnabled";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(id)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");

    // SubscriptionOperation needed for recording the update
    SubscriptionOperation subscriptionOp = enabled ? SubscriptionOperation.enable : SubscriptionOperation.disable;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.ENABLED, enabled)
              .set(SUBSCRIPTIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id))
              .execute();
      // Persist update record
      String changeDescription = "{\"enabled\":" +  enabled + "}";
      addUpdate(db, rUser, id, INVALID_SEQ_ID, subscriptionOp, changeDescription , null, getUUIDUsingDb(db, tenantId, id));
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "subscriptions", id);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update owner of a subscription given subscription Id and new owner name
   *
   */
  @Override
  public void updateSubscriptionOwner(ResourceRequestUser rUser, String tenantId, String id, String newOwnerName)
          throws TapisException
  {
    String opName = "changeOwner";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(id)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");
    if (StringUtils.isBlank(newOwnerName)) LibUtils.logAndThrowNullParmException(opName, "newOwnerName");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.OWNER, newOwnerName)
              .set(SUBSCRIPTIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id))
              .execute();
      // Persist update record
      String changeDescription = TapisGsonUtils.getGson().toJson(newOwnerName);
      addUpdate(db, rUser, id, INVALID_SEQ_ID, SubscriptionOperation.changeOwner, changeDescription , null,
                getUUIDUsingDb(db, tenantId, id));
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "subscriptions", id);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Update attribute TTL for a subscription given subscription Id and value
   * Also update expiry since update of TTL should always include update of expiry
   */
  @Override
  public void updateSubscriptionTTL(ResourceRequestUser rUser, String tenantId, String id, int newTTL, Instant newExpiry)
          throws TapisException
  {
    String opName = "updateTTL";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(id)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");

    // Convert expiry from Instant to LocalDateTime
    LocalDateTime expiry = (newExpiry == null) ? null :  LocalDateTime.ofInstant(newExpiry, ZoneOffset.UTC);

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.update(SUBSCRIPTIONS)
              .set(SUBSCRIPTIONS.TTL, newTTL)
              .set(SUBSCRIPTIONS.EXPIRY, expiry)
              .set(SUBSCRIPTIONS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id))
              .execute();
      // Persist update record
      String changeDescription = "{\"ttl\":" +  newTTL + ", \"expiry\":\"" + expiry + "\"}";
      addUpdate(db, rUser, id, INVALID_SEQ_ID, SubscriptionOperation.updateTTL, changeDescription , null,
                getUUIDUsingDb(db, tenantId, id));
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_UPDATE_FAILURE", "subscriptions", id);
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Delete a subscription
   */
  @Override
  public int deleteSubscription(String tenantId, String subscrId) throws TapisException
  {
    String opName = "deleteSubscription";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenantId)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(subscrId)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.deleteFrom(SUBSCRIPTIONS).where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(subscrId)).execute();
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "subscriptions");
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
    return 1;
  }

  /**
   * checkForSubscription
   * @param id - subscription name
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForSubscription(String tenantId, String id) throws TapisException
  {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = checkForSubscription(db, tenantId, id);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "Subscription", tenantId, id, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * isEnabled - check if resource with specified Id is enabled
   * @param subId - resource Id
   * @return true if enabled else false
   * @throws TapisException - on error
   */
  @Override
  public boolean isEnabled(String tenantId, String subId) throws TapisException {
    // Initialize result.
    boolean result = false;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      Boolean b = db.selectFrom(SUBSCRIPTIONS)
              .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(subId))
              .fetchOne(SUBSCRIPTIONS.ENABLED);
      if (b != null) result = b;
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "Subscription", tenantId, subId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getSubscription
   * @param id - subscription name
   * @return Subscription object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public Subscription getSubscription(String tenantId, String id) throws TapisException
  {
    // Initialize result.
    Subscription result = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      SubscriptionsRecord r;
      r = db.selectFrom(SUBSCRIPTIONS).where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id)).fetchOne();
      if (r == null) return null;
      else result = getSubscriptionFromRecord(r);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "Subscription", tenantId, id, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getSubscriptionsCount
   * Count all Subscriptions matching various search and sort criteria.
   *     Search conditions given as a list of strings or an abstract syntax tree (AST).
   * Conditions in searchList must be processed by SearchUtils.validateAndExtractSearchCondition(cond)
   *   prior to this call for proper validation and treatment of special characters.
   * WARNING: If both searchList and searchAST provided only searchList is used.
   * @param tenantId - tenant name
   * @param searchList - optional list of conditions used for searching
   * @param searchAST - AST containing search conditions
   * @param setOfIDs - list of subscription IDs to consider. null indicates no restriction.
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param startAfter - where to start when sorting, e.g. orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return - count of Subscription objects
   * @throws TapisException - on error
   */
  @Override
  public int getSubscriptionsCount(String tenantId, List<String> searchList, ASTNode searchAST, Set<String> setOfIDs,
                             List<OrderBy> orderByList, String startAfter)
          throws TapisException
  {
    // If no IDs in list then we are done.
    if (setOfIDs != null && setOfIDs.isEmpty()) return 0;

    // Ensure we have a non-null orderByList
    List<OrderBy> tmpOrderByList = new ArrayList<>();
    if (orderByList != null) tmpOrderByList = orderByList;

    // Determine the primary orderBy column (i.e. first in list). Used for startAfter
    String majorOrderByStr = null;
    OrderByDir majorSortDirection = DEFAULT_ORDERBY_DIRECTION;
    if (!tmpOrderByList.isEmpty())
    {
      majorOrderByStr = tmpOrderByList.get(0).getOrderByAttr();
      majorSortDirection = tmpOrderByList.get(0).getOrderByDir();
    }

    // Determine if we are doing an asc sort, important for startAfter
    boolean sortAsc = majorSortDirection != OrderByDir.DESC;

    // If startAfter is given then orderBy is required
    if (!StringUtils.isBlank(startAfter) && StringUtils.isBlank(majorOrderByStr))
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SORT_START", SUBSCRIPTIONS.getName()));
    }

    // Validate orderBy columns
    // If orderBy column not found then it is an error
    // For count we do not need the actual column so we just check that the column exists.
    //   Down below in getSubscriptions() we need the actual column
    for (OrderBy orderBy : tmpOrderByList)
    {
      String orderByStr = orderBy.getOrderByAttr();
      if (StringUtils.isBlank(orderByStr) || !SUBSCRIPTIONS_FIELDS.contains(SearchUtils.camelCaseToSnakeCase(orderByStr)))
      {
        String msg = LibUtils.getMsg("NTFLIB_DB_NO_COLUMN_SORT", SUBSCRIPTIONS.getName(), DSL.name(orderByStr));
        throw new TapisException(msg);
      }
    }

    // Begin where condition for the query
    Condition whereCondition = SUBSCRIPTIONS.TENANT.eq(tenantId);

    // Add searchList or searchAST to where condition
    if (searchList != null)
    {
      whereCondition = addSearchListToWhere(whereCondition, searchList);
    }
    else if (searchAST != null)
    {
      Condition astCondition = createConditionFromAst(searchAST);
      if (astCondition != null) whereCondition = whereCondition.and(astCondition);
    }

    // Add startAfter.
    if (!StringUtils.isBlank(startAfter))
    {
      // Build search string so we can re-use code for checking and adding a condition
      String searchStr;
      if (sortAsc) searchStr = majorOrderByStr + ".gt." + startAfter;
      else searchStr = majorOrderByStr + ".lt." + startAfter;
      whereCondition = addSearchCondStrToWhere(whereCondition, searchStr, "AND");
    }

    // Add IN condition for list of IDs
    if (setOfIDs != null && !setOfIDs.isEmpty()) whereCondition = whereCondition.and(SUBSCRIPTIONS.ID.in(setOfIDs));

    // ------------------------- Build and execute SQL ----------------------------
    int count = 0;
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Execute the select including startAfter
      // NOTE: This is much simpler than the same section in getSubscriptions() because we are not ordering since
      //       we only want the count and we are not limiting (we want a count of all records).
      Integer countInt = db.selectCount().from(SUBSCRIPTIONS).where(whereCondition).fetchOne(0,Integer.class);
      count = (countInt == null) ? 0 : countInt;

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return count;
  }

  /**
   * getSubscriptionIDs
   * Fetch all resource IDs in a tenant
   * @param tenant - tenant name
   * @return - List of resource IDs
   * @throws TapisException - on error
   */
  @Override
  public Set<String> getSubscriptionIDs(String tenant) throws TapisException
  {
    // The result list is always non-null.
    var idList = new HashSet<String>();

    Condition whereCondition = SUBSCRIPTIONS.TENANT.eq(tenant);

    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      // ------------------------- Call SQL ----------------------------
      // Use jOOQ to build query string
      DSLContext db = DSL.using(conn);
      Result<?> result = db.select(SUBSCRIPTIONS.ID).from(SUBSCRIPTIONS).where(whereCondition).fetch();
      // Iterate over result
      for (Record r : result) { idList.add(r.get(SUBSCRIPTIONS.ID)); }
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return idList;
  }

  /**
   * getSubscriptionIDsByOwner
   * Fetch all resource IDs in a tenant owned by a specific user.
   * @param tenant - tenant name
   * @param owner - user
   * @return - List of resource IDs
   * @throws TapisException - on error
   */
  @Override
  public Set<String> getSubscriptionIDsByOwner(String tenant, String owner) throws TapisException
  {
    // The result list is always non-null.
    var idList = new HashSet<String>();
    if (StringUtils.isBlank(tenant) || StringUtils.isBlank(owner)) return idList;

    Condition whereCondition = SUBSCRIPTIONS.TENANT.eq(tenant).and(SUBSCRIPTIONS.OWNER.eq(owner));

    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      // ------------------------- Call SQL ----------------------------
      // Use jOOQ to build query string
      DSLContext db = DSL.using(conn);
      Result<?> result = db.select(SUBSCRIPTIONS.ID).from(SUBSCRIPTIONS).where(whereCondition).fetch();
      // Iterate over result
      for (Record r : result) { idList.add(r.get(SUBSCRIPTIONS.ID)); }
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return idList;
  }

  /**
   * getSubscriptions
   * Retrieve all Subscriptions matching various search and sort criteria.
   *     Search conditions given as a list of strings or an abstract syntax tree (AST).
   * Conditions in searchList must be processed by SearchUtils.validateAndExtractSearchCondition(cond)
   *   prior to this call for proper validation and treatment of special characters.
   * WARNING: If both searchList and searchAST provided only searchList is used.
   * @param tenantId - tenant name
   * @param searchList - optional list of conditions used for searching
   * @param searchAST - AST containing search conditions
   * @param setOfIDs - list of subscription IDs to consider. null indicates no restriction.
   * @param limit - indicates maximum number of results to be included, -1 for unlimited
   * @param orderByList - orderBy entries for sorting, e.g. orderBy=created(desc).
   * @param skip - number of results to skip (may not be used with startAfter)
   * @param startAfter - where to start when sorting, e.g. limit=10&orderBy=id(asc)&startAfter=101 (may not be used with skip)
   * @return - list of Subscription objects
   * @throws TapisException - on error
   */
  @Override
  public List<Subscription> getSubscriptions(String tenantId, List<String> searchList, ASTNode searchAST,
                                             Set<String> setOfIDs, int limit, List<OrderBy> orderByList,
                                             int skip, String startAfter)
          throws TapisException
  {
    // The result list should always be non-null.
    List<Subscription> retList = new ArrayList<>();

    // If no IDs in list then we are done.
    if (setOfIDs != null && setOfIDs.isEmpty()) return retList;

    // Ensure we have a non-null orderByList
    List<OrderBy> tmpOrderByList = new ArrayList<>();
    if (orderByList != null) tmpOrderByList = orderByList;

    // Determine the primary orderBy column (i.e. first in list). Used for startAfter
    String majorOrderByStr = null;
    OrderByDir majorSortDirection = DEFAULT_ORDERBY_DIRECTION;
    if (!tmpOrderByList.isEmpty())
    {
      majorOrderByStr = tmpOrderByList.get(0).getOrderByAttr();
      majorSortDirection = tmpOrderByList.get(0).getOrderByDir();
    }

    // Negative skip indicates no skip
    if (skip < 0) skip = 0;

    // Determine if we are doing an asc sort, important for startAfter
    boolean sortAsc = majorSortDirection != OrderByDir.DESC;

    // If startAfter is given then orderBy is required
    if (!StringUtils.isBlank(startAfter) && StringUtils.isBlank(majorOrderByStr))
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SORT_START", SUBSCRIPTIONS.getName()));
    }

// DEBUG Iterate over all columns and show the type
//      Field<?>[] cols = SUBSCRIPTIONS.fields();
//      for (Field<?> col : cols) {
//        var dataType = col.getDataType();
//        int sqlType = dataType.getSQLType();
//        String sqlTypeName = dataType.getTypeName();
//        _log.debug("Column name: " + col.getName() + " type: " + sqlTypeName);
//      }
// DEBUG

    // Determine and check orderBy columns, build orderFieldList
    // Each OrderField contains the column and direction
    List<OrderField> orderFieldList = new ArrayList<>();
    for (OrderBy orderBy : tmpOrderByList)
    {
      String orderByStr = orderBy.getOrderByAttr();
      Field<?> colOrderBy = SUBSCRIPTIONS.field(DSL.name(SearchUtils.camelCaseToSnakeCase(orderByStr)));
      if (StringUtils.isBlank(orderByStr) || colOrderBy == null)
      {
        String msg = LibUtils.getMsg("NTFLIB_DB_NO_COLUMN_SORT", SUBSCRIPTIONS.getName(), DSL.name(orderByStr));
        throw new TapisException(msg);
      }
      if (orderBy.getOrderByDir() == OrderBy.OrderByDir.ASC) orderFieldList.add(colOrderBy.asc());
      else orderFieldList.add(colOrderBy.desc());
    }

    // Begin where condition for the query
    Condition whereCondition = SUBSCRIPTIONS.TENANT.eq(tenantId);

    // Add searchList or searchAST to where condition
    if (searchList != null)
    {
      whereCondition = addSearchListToWhere(whereCondition, searchList);
    }
    else if (searchAST != null)
    {
      Condition astCondition = createConditionFromAst(searchAST);
      if (astCondition != null) whereCondition = whereCondition.and(astCondition);
    }

    // Add startAfter
    if (!StringUtils.isBlank(startAfter))
    {
      // Build search string so we can re-use code for checking and adding a condition
      String searchStr;
      if (sortAsc) searchStr = majorOrderByStr + ".gt." + startAfter;
      else searchStr = majorOrderByStr + ".lt." + startAfter;
      whereCondition = addSearchCondStrToWhere(whereCondition, searchStr, "AND");
    }

    // Add IN condition for list of IDs
    if (setOfIDs != null && !setOfIDs.isEmpty()) whereCondition = whereCondition.and(SUBSCRIPTIONS.ID.in(setOfIDs));

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Execute the select including limit, orderByAttrList, skip and startAfter
      // NOTE: LIMIT + OFFSET is not standard among DBs and often very difficult to get right.
      //       Jooq claims to handle it well.
      Result<SubscriptionsRecord> results;
      org.jooq.SelectConditionStep condStep = db.selectFrom(SUBSCRIPTIONS).where(whereCondition);
      if (!StringUtils.isBlank(majorOrderByStr) &&  limit >= 0)
      {
        // We are ordering and limiting
        results = condStep.orderBy(orderFieldList).limit(limit).offset(skip).fetch();
      }
      else if (!StringUtils.isBlank(majorOrderByStr))
      {
        // We are ordering but not limiting
        results = condStep.orderBy(orderFieldList).fetch();
      }
      else if (limit >= 0)
      {
        // We are limiting but not ordering
        results = condStep.limit(limit).offset(skip).fetch();
      }
      else
      {
        // We are not limiting and not ordering
        results = condStep.fetch();
      }

      if (results == null || results.isEmpty()) return retList;

      for (Record r : results) { Subscription s = getSubscriptionFromRecord(r); retList.add(s); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * getSubscriptionsForEvent
   * Retrieve all Subscriptions matching an event.
   * Event must be non-null and have tenantId and type filled in.
   * If no subscriptions match or the event is null then return an empty list
   * @param event - Event to match
   * @return - list of Subscription objects
   * @throws TapisException - on error
   */
  @Override
  public List<Subscription> getSubscriptionsForEvent(Event event) throws TapisException
  {
    // The result list should always be non-null.
    List<Subscription> retList = new ArrayList<>();

    // If event is null or any attributes required for matching are missing return an empty list.
    if (event == null || StringUtils.isBlank(event.getTenant()) || StringUtils.isBlank(event.getType()))
    {
      return retList;
    }

    // Make sure the types are set
    event.setTypeFields();

    String tenantId = event.getTenant();

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      Result<SubscriptionsRecord> results;

      // Build up the WHERE clause.
      // WHERE tenant = '<tenantId>'
      //    AND (type_filter1 = '<typeFilter1>' OR type_filter1 = '*')
      //    AND (type_filter2 = '<typeFilter2>' OR type_filter2 = '*')
      //    AND (type_filter3 = '<typeFilter3>' OR type_filter3 = '*')
      //    AND (subject_filter = '<subjectFilter>' OR subject_filter = '*')
      Condition whereCondition = SUBSCRIPTIONS.TENANT.eq(tenantId);
      Condition tmpCond = SUBSCRIPTIONS.TYPE_FILTER1.eq(event.getType1()).or(SUBSCRIPTIONS.TYPE_FILTER1.eq("*"));
      whereCondition = whereCondition.and(tmpCond);
      tmpCond = SUBSCRIPTIONS.TYPE_FILTER2.eq(event.getType2()).or(SUBSCRIPTIONS.TYPE_FILTER2.eq("*"));
      whereCondition = whereCondition.and(tmpCond);
      tmpCond = SUBSCRIPTIONS.TYPE_FILTER3.eq(event.getType3()).or(SUBSCRIPTIONS.TYPE_FILTER3.eq("*"));
      whereCondition = whereCondition.and(tmpCond);
      tmpCond = SUBSCRIPTIONS.SUBJECT_FILTER.eq(event.getSubject()).or(SUBSCRIPTIONS.SUBJECT_FILTER.eq("*"));
      whereCondition = whereCondition.and(tmpCond);

      results = db.selectFrom(SUBSCRIPTIONS).where(whereCondition).fetch();

      if (results.isEmpty()) return retList;

      for (Record r : results) { Subscription s = getSubscriptionFromRecord(r); retList.add(s); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * getExpiredSubscriptions
   * Retrieve all Subscriptions passed their expiry.
   * This is for all tenants
   * If no subscriptions are found an empty list is returned
   * @return - list of expired Subscriptions across all tenants
   * @throws TapisException - on error
   */
  @Override
  public List<Subscription> getExpiredSubscriptions() throws TapisException
  {
    // The result list should always be non-null.
    List<Subscription> retList = new ArrayList<>();

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      Result<SubscriptionsRecord> results;

      // Build up the WHERE clause.
      // Look for where the expiry timestamp is before the current timestamp
      LocalDateTime now = TapisUtils.getUTCTimeNow();
      Condition whereCondition = SUBSCRIPTIONS.EXPIRY.lt(now);
      results = db.selectFrom(SUBSCRIPTIONS).where(whereCondition).fetch();

      if (results.isEmpty()) return retList;

      for (Record r : results) { Subscription s = getSubscriptionFromRecord(r); retList.add(s); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * getSubscriptionOwner
   * @param tenantId - name of tenant
   * @param id - name of subscription
   * @return Owner or null if no subscription found
   * @throws TapisException - on error
   */
  @Override
  public String getSubscriptionOwner(String tenantId, String id) throws TapisException
  {
    String owner = null;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      owner = db.selectFrom(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id))
                .fetchOne(SUBSCRIPTIONS.OWNER);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "subscriptions", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return owner;
  }

  /**
   * Add an update record given the subscription Id and operation type
   *
   */
  @Override
  public void addUpdateRecord(ResourceRequestUser rUser, String id, SubscriptionOperation op, String changeDescription, String rawData)
          throws TapisException
  {
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      addUpdate(db, rUser, id, INVALID_SEQ_ID, op, changeDescription, rawData, getUUIDUsingDb(db, rUser.getOboTenantId(), id));
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "subscription_updates");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  // -----------------------------------------------------------------------
  // ------------------------- Notifications -------------------------------
  // -----------------------------------------------------------------------

  /**
   * Persist a batch of notifications associated with an Event and bucket
   * Also update the last_event table as part of the transaction.
   * @param tenant - name of tenant
   * @param event - Event associated with the notifications
   * @param bucketNum - Bucket associated with the notifications
   * @return true on success
   * @throws TapisException - on error
   */
  @Override
  public boolean persistNotificationsUpdateLastEvent(String tenant, Event event, int bucketNum, List<Notification> notifications)
          throws TapisException
  {
    String opName = "persistNotificationsForEvent";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (event == null) LibUtils.logAndThrowNullParmException(opName, "event");
    if (notifications == null || notifications.isEmpty()) return true;

    // Event is same for all items.
    UUID eventUUID = event.getUuid();
    JsonElement eventJson = TapisGsonUtils.getGson().toJsonTree(event);

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Create template for inserts
      BatchBindStep batch = db.batch(db.insertInto(NOTIFICATIONS,
              NOTIFICATIONS.UUID,
              NOTIFICATIONS.SUBSCR_SEQ_ID,
              NOTIFICATIONS.TENANT,
              NOTIFICATIONS.SUBSCR_ID,
              NOTIFICATIONS.BUCKET_NUMBER,
              NOTIFICATIONS.EVENT_UUID,
              NOTIFICATIONS.EVENT,
              NOTIFICATIONS.CREATED,
              NOTIFICATIONS.DELIVERY_METHOD).values((UUID) null, null, null, null, null, null, null, null, null));

      // Put together all the records we will be inserting.
      for (Notification n : notifications)
      {
        DeliveryMethod dm =  n.getDeliveryMethod();
        // Convert deliveryMethod to json
        JsonElement deliveryMethodJson = EMPTY_JSON_ELEM;
        if (dm != null) deliveryMethodJson = TapisGsonUtils.getGson().toJsonTree(dm);

        batch.bind(n.getUuid(), n.getSubscrSeqId(), tenant, n.getSubscriptionId(), bucketNum, eventUUID, eventJson,
                   n.getCreated(), deliveryMethodJson);
      }

      // Now execute the final batch statement
      batch.execute();

      // Update/create row in last_event table
      db.insertInto(NOTIFICATIONS_LAST_EVENT).values(bucketNum, eventUUID).onDuplicateKeyUpdate()
              .set(NOTIFICATIONS_LAST_EVENT.EVENT_UUID, eventUUID)
              .execute();

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "notifications");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return true;
  }

  /**
   * Retrieve all Notifications associated with an Event and bucket
   *
   * @param tenant - tenant name
   * @param event - Event associated with the notifications
   * @param bucketNum - Bucket associated with the notifications
   * @return - list of Notification objects
   * @throws TapisException - on error
   */
  @Override
  public List<Notification> getNotificationsForEvent(String tenant, Event event, int bucketNum)
          throws TapisException
  {
    // The result list should always be non-null.
    List<Notification> retList = new ArrayList<>();

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      Result<NotificationsRecord> results = db.selectFrom(NOTIFICATIONS)
                       .where(NOTIFICATIONS.TENANT.eq(tenant),
                              NOTIFICATIONS.BUCKET_NUMBER.eq(bucketNum),
                              NOTIFICATIONS.EVENT_UUID.eq(event.getUuid())).fetch();

      if (results == null || results.isEmpty()) return retList;

      for (Record r : results) { retList.add(getNotificationFromRecord(r)); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "notifications", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * checkForLastEvent
   * @param eventUuid -
   * @param bucketNum -
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForLastEvent(UUID eventUuid, int bucketNum) throws TapisException
  {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = db.fetchExists(NOTIFICATIONS_LAST_EVENT,NOTIFICATIONS_LAST_EVENT.BUCKET_NUMBER.eq(bucketNum),
                              NOTIFICATIONS_LAST_EVENT.EVENT_UUID.eq(eventUuid));
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_UUID_ERROR", "notifications_last_event", eventUuid, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * getNotification
   * @param tenantId tenant
   * @param uuid - uuid of notification
   * @return object if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public Notification getNotification(String tenantId, UUID uuid) throws TapisException
  {
    // Initialize result.
    Notification result = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      NotificationsRecord r;
      r = db.selectFrom(NOTIFICATIONS).where(NOTIFICATIONS.TENANT.eq(tenantId),NOTIFICATIONS.UUID.eq(uuid)).fetchOne();
      if (r == null) return null;
      else result = getNotificationFromRecord(r);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "Notification", tenantId, uuid, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * Delete a notification and add it to the recovery table in one transaction
   */
  @Override
  public void deleteNotificationAndAddToRecovery(String tenant, Notification notification) throws TapisException
  {
    String opName = "deleteNotificationAndAddToRecovery";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (notification == null) LibUtils.logAndThrowNullParmException(opName, "notification");

    JsonElement eventJson = TapisGsonUtils.getGson().toJsonTree(notification.getEvent());
    JsonElement deliveryMethodJson = EMPTY_JSON_ELEM;
    if (notification.getDeliveryMethod() != null) deliveryMethodJson =
            TapisGsonUtils.getGson().toJsonTree(notification.getDeliveryMethod());

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Delete it from the main table
      db.deleteFrom(NOTIFICATIONS)
              .where(NOTIFICATIONS.TENANT.eq(tenant), NOTIFICATIONS.UUID.eq(notification.getUuid()))
              .execute();

      // Add it to the recovery table initializing recovery attempts to 0
      // Let created and updated default to now()
      // Insert the record
      db.insertInto(NOTIFICATIONS_RECOVERY)
              .set(NOTIFICATIONS_RECOVERY.UUID, notification.getUuid())
              .set(NOTIFICATIONS_RECOVERY.SUBSCR_SEQ_ID, notification.getSubscrSeqId())
              .set(NOTIFICATIONS_RECOVERY.TENANT, tenant)
              .set(NOTIFICATIONS_RECOVERY.SUBSCR_ID, notification.getSubscriptionId())
              .set(NOTIFICATIONS_RECOVERY.BUCKET_NUMBER, notification.getBucketNum())
              .set(NOTIFICATIONS_RECOVERY.EVENT_UUID, notification.getEvent().getUuid())
              .set(NOTIFICATIONS_RECOVERY.EVENT, eventJson)
              .set(NOTIFICATIONS_RECOVERY.DELIVERY_METHOD, deliveryMethodJson)
              .set(NOTIFICATIONS_RECOVERY.RECOVERY_ATTEMPT, 0)
              .execute();

      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "subscriptions");
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Delete a notification
   */
  @Override
  public void deleteNotification(String tenant, Notification notification) throws TapisException
  {
    String opName = "deleteNotification";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenant)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (notification == null) LibUtils.logAndThrowNullParmException(opName, "notification");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.deleteFrom(NOTIFICATIONS)
            .where(NOTIFICATIONS.TENANT.eq(tenant), NOTIFICATIONS.UUID.eq(notification.getUuid()))
            .execute();
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "notifications");
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Get UUID for last event processed by the bucket manager
   * @param bucketNum - bucket manager
   * @return uuid
   * @throws TapisException on error
   */
  public UUID getLastEventUUID(int bucketNum) throws TapisException
  {
    // Initialize result.
    UUID result = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      NotificationsLastEventRecord r;
      r = db.selectFrom(NOTIFICATIONS_LAST_EVENT).where(NOTIFICATIONS_LAST_EVENT.BUCKET_NUMBER.eq(bucketNum)).fetchOne();
      if (r == null) return null;

      result = r.get(NOTIFICATIONS_LAST_EVENT.EVENT_UUID);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "Notifications_Last_Event", "N/A", result, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * Retrieve all Notifications waiting on recovery
   *
   * @param bucketNum - Bucket associated with the notifications
   * @return - list of Notification objects
   * @throws TapisException - on error
   */
  @Override
  public List<Notification> getNotificationsInRecovery(int bucketNum) throws TapisException
  {

    // The result list should always be non-null.
    List<Notification> retList = new ArrayList<>();

    // ------------------------- Build and execute SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      Result<NotificationsRecoveryRecord> results =
              db.selectFrom(NOTIFICATIONS_RECOVERY).where(NOTIFICATIONS_RECOVERY.BUCKET_NUMBER.eq(bucketNum)).fetch();

      if (results == null || results.isEmpty()) return retList;

      for (Record r : results) { retList.add(getNotificationFromRecoveryRecord(r)); }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_QUERY_ERROR", "notifications_recovery", e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return retList;
  }

  /**
   * Delete a notification from recovery table
   */
  @Override
  public void deleteNotificationFromRecovery(Notification notification) throws TapisException
  {
    String opName = "deleteNotification";
    // ------------------------- Check Input -------------------------
    if (notification == null) LibUtils.logAndThrowNullParmException(opName, "notification");

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      db.deleteFrom(NOTIFICATIONS_RECOVERY)
              .where(NOTIFICATIONS_RECOVERY.UUID.eq(notification.getUuid()))
              .execute();
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      LibUtils.rollbackDB(conn, e,"DB_DELETE_FAILURE", "notifications_recovery");
    }
    finally
    {
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * Get attempt count for a notification in the recovery table
   * @param notification - the notification
   * @return the number of attempts so far
   * @throws TapisException on error
   */
  public int getNotificationRecoveryAttemptCount(Notification notification) throws TapisException
  {
    int attemptCount;
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      r = db.selectFrom(NOTIFICATIONS_RECOVERY).where(NOTIFICATIONS_RECOVERY.UUID.eq(notification.getUuid())).fetchOne();
      if (r == null) return null;

      result = r.get(NOTIFICATIONS_LAST_EVENT.EVENT_UUID);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "Notifications_Last_Event", "N/A", result, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return attemptCount;
  }
  // -----------------------------------------------------------------------
  // ------------------------- Test Sequences ------------------------------
  // -----------------------------------------------------------------------

  /**
   * Create a test sequence record
   *
   * @return true if created, false if subscription does not exist
   * @throws IllegalStateException - if resource already exists
   * @throws TapisException - on error
   */
  @Override
  public boolean createTestSequence(ResourceRequestUser rUser, String subscrId)
          throws TapisException, IllegalStateException
  {
    String opName = "createTestSequence";
    // ------------------------- Check Input -------------------------
    if (rUser == null) LibUtils.logAndThrowNullParmException(opName, "resourceRequestUser");
    if (StringUtils.isBlank(subscrId)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");

    String oboTenant = rUser.getOboTenantId();
    String oboUser = rUser.getOboUserId();
    Subscription subscription = getSubscription(oboTenant, subscrId);
    if (subscription == null) return false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);

      // Check to see if resource exists. If yes then throw IllegalStateException
      boolean doesExist = checkForTestSequence(db, oboTenant, subscrId);
      if (doesExist)
        throw new IllegalStateException(LibUtils.getMsgAuth("NTFLIB_TEST_EXISTS", rUser, subscrId));

      // Start with an empty list of events
      JsonElement eventsJson = TestSequence.EMPTY_EVENTS;
      Record record = db.insertInto(NOTIFICATIONS_TESTS)
              .set(NOTIFICATIONS_TESTS.SUBSCR_SEQ_ID, subscription.getSeqId())
              .set(NOTIFICATIONS_TESTS.TENANT, oboTenant)
              .set(NOTIFICATIONS_TESTS.SUBSCR_ID, subscrId)
              .set(NOTIFICATIONS_TESTS.OWNER, oboUser)
              .set(NOTIFICATIONS_TESTS.NOTIFICATION_COUNT, 0)
              .set(NOTIFICATIONS_TESTS.NOTIFICATIONS, eventsJson)
              .returningResult(NOTIFICATIONS_TESTS.SEQ_ID)
              .fetchOne();
      // If record is null or sequence id is invalid it is an error
      if (record == null || record.getValue(NOTIFICATIONS_TESTS.SEQ_ID) < 1)
      {
        throw new TapisException(LibUtils.getMsgAuth("NTFLIB_DB_NULL_RESULT", rUser, subscrId, opName));
      }

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "notifications_tests");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return true;
  }

  /**
   * getTestSequence
   * @param tenantId - oboTenant
   * @param subscriptionId - subscription name
   * @return TestSequence if found, null if not found
   * @throws TapisException - on error
   */
  @Override
  public TestSequence getTestSequence(String tenantId, String subscriptionId) throws TapisException
  {
    // Initialize result.
    TestSequence result = null;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      result = getTestSequence(db, tenantId, subscriptionId);

      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"NTFLIB_DB_SELECT_ERROR", "TestSequence", tenantId, subscriptionId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /**
   * Add an event to a test sequence record
   *
   * @throws IllegalStateException - if test sequence does not exist
   * @throws TapisException - on error
   */
  @Override
  public void addTestSequenceNotification(String tenantId, String user, String subscrId, Notification notification)
          throws TapisException, IllegalStateException
  {
    String opName = "addTestSequenceNotification";
    // ------------------------- Check Input -------------------------
    if (StringUtils.isBlank(tenantId)) LibUtils.logAndThrowNullParmException(opName, "tenant");
    if (StringUtils.isBlank(user)) LibUtils.logAndThrowNullParmException(opName, "user");
    if (StringUtils.isBlank(subscrId)) LibUtils.logAndThrowNullParmException(opName, "subscriptionId");
    if (notification == null) LibUtils.logAndThrowNullParmException(opName, "notification");
    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Get test sequence. If not found throw an exception
      TestSequence testSequence = getTestSequence(db, tenantId, subscrId);
      if (testSequence == null)
        throw new IllegalStateException(LibUtils.getMsg("NTFLIB_TEST_NOT_FOUND", tenantId, user, opName, subscrId));
      // Figure out the notificationsJson for the update
      JsonElement notificationsJson;
      var newNotifications = testSequence.getReceivedNotifications();
      newNotifications.add(notification);
      notificationsJson = TapisGsonUtils.getGson().toJsonTree(newNotifications);
      // Make the update
      db.update(NOTIFICATIONS_TESTS)
              .set(NOTIFICATIONS_TESTS.NOTIFICATION_COUNT, newNotifications.size())
              .set(NOTIFICATIONS_TESTS.NOTIFICATIONS, notificationsJson)
              .set(NOTIFICATIONS_TESTS.UPDATED, TapisUtils.getUTCTimeNow())
              .where(NOTIFICATIONS_TESTS.TENANT.eq(tenantId),NOTIFICATIONS_TESTS.SUBSCR_ID.eq(subscrId))
              .execute();
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_INSERT_FAILURE", "notifications_tests");
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
  }

  /**
   * checkForTestSequence
   * @param tenantId - obo tenant
   * @param subscriptionId - subscription name
   * @return true if found else false
   * @throws TapisException - on error
   */
  @Override
  public boolean checkForTestSequence(String tenantId, String subscriptionId) throws TapisException
  {
    // Initialize result.
    boolean result = false;

    // ------------------------- Call SQL ----------------------------
    Connection conn = null;
    try
    {
      // Get a database connection.
      conn = getConnection();
      DSLContext db = DSL.using(conn);
      // Run the sql
      result = checkForTestSequence(db, tenantId, subscriptionId);
      // Close out and commit
      LibUtils.closeAndCommitDB(conn, null, null);
    }
    catch (Exception e)
    {
      // Rollback transaction and throw an exception
      LibUtils.rollbackDB(conn, e,"DB_SELECT_NAME_ERROR", "Subscription", tenantId, subscriptionId, e.getMessage());
    }
    finally
    {
      // Always return the connection back to the connection pool.
      LibUtils.finalCloseDB(conn);
    }
    return result;
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /**
   *  Return a connection from the static datasource. Create the datasource if it does not exist.
   *
   * @return a database connection
   * @throws TapisException on error
   */
  private static synchronized Connection getConnection() throws TapisException
  {
    // Use the existing datasource.
    DataSource ds = getDataSource();

    // Get the connection.
    Connection conn = null;
    try {conn = ds.getConnection();}
    catch (Exception e)
    {
      String msg = MsgUtils.getMsg("DB_FAILED_CONNECTION");
      _log.error(msg, e);
      throw new TapisDBConnectionException(msg, e);
    }
    return conn;
  }

  /* ---------------------------------------------------------------------- */
  /* getDataSource:                                                         */
  /* ---------------------------------------------------------------------- */
  private static DataSource getDataSource() throws TapisException
  {
    // Use the existing datasource.
    DataSource ds = TapisDataSource.getDataSource();
    if (ds == null)
    {
      try
      {
        // Get a database connection.
        RuntimeParameters parms = RuntimeParameters.getInstance();
        ds = TapisDataSource.getDataSource(parms.getInstanceName(),
                parms.getDbConnectionPoolName(),
                parms.getJdbcURL(),
                parms.getDbUser(),
                parms.getDbPassword(),
                parms.getDbConnectionPoolSize(),
                parms.getDbMeterMinutes());
      }
      catch (TapisException e)
      {
        // Details are already logged at exception site.
        String msg = MsgUtils.getMsg("DB_FAILED_DATASOURCE");
        _log.error(msg, e);
        throw new TapisException(msg, e);
      }
    }
    return ds;
  }

  /**
   * Given an sql connection and basic info add a change history record
   * If seqId <= 0 then seqId is fetched.
   * NOTE: Both subscription tenant and user tenant are recorded. If a service makes an update on behalf of itself
   *       the tenants will differ.
   *
   * @param db - Database connection
   * @param rUser - ResourceRequestUser containing tenant, user and request info
   * @param id - Id of the subscription being updated
   * @param seqId - Sequence Id of subscription being updated
   * @param op - Operation, such as create, modify, etc.
   * @param changeDescription - JSON representing the update - with secrets scrubbed
   * @param rawData - Text data supplied by client - secrets should be scrubbed
   */
  private void addUpdate(DSLContext db, ResourceRequestUser rUser, String id, int seqId,
                         SubscriptionOperation op, String changeDescription, String rawData, UUID uuid)
  {
    String updJsonStr = (StringUtils.isBlank(changeDescription)) ? EMPTY_JSON : changeDescription;
    if (seqId < 1)
    {
      seqId = db.selectFrom(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.TENANT.eq(rUser.getOboTenantId()),SUBSCRIPTIONS.ID.eq(id))
                .fetchOne(SUBSCRIPTIONS.SEQ_ID);
    }
    // Persist update record
    db.insertInto(SUBSCRIPTION_UPDATES)
            .set(SUBSCRIPTION_UPDATES.SUBSCRIPTION_SEQ_ID, seqId)
            .set(SUBSCRIPTION_UPDATES.JWT_TENANT, rUser.getJwtTenantId())
            .set(SUBSCRIPTION_UPDATES.JWT_USER, rUser.getJwtUserId())
            .set(SUBSCRIPTION_UPDATES.OBO_TENANT, rUser.getOboTenantId())
            .set(SUBSCRIPTION_UPDATES.OBO_USER, rUser.getOboUserId())
            .set(SUBSCRIPTION_UPDATES.SUBSCRIPTION_ID, id)
            .set(SUBSCRIPTION_UPDATES.OPERATION, op)
            .set(SUBSCRIPTION_UPDATES.DESCRIPTION, TapisGsonUtils.getGson().fromJson(updJsonStr, JsonElement.class))
            .set(SUBSCRIPTION_UPDATES.RAW_DATA, rawData)
            .set(SUBSCRIPTION_UPDATES.UUID, uuid)
            .execute();
  }

  /**
   * Given an sql connection check to see if specified subscription exists
   * @param db - jooq context
   * @param tenantId - name of tenant
   * @param id - name of subscription
   * @return - true if subscription exists, else false
   */
  private static boolean checkForSubscription(DSLContext db, String tenantId, String id)
  {
    return db.fetchExists(SUBSCRIPTIONS,SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id));
  }

  /**
   * Get all subscription sequence IDs for specified tenant
   * @param db - DB connection
   * @param tenantId - tenant name
   * @return list of sequence IDs
   */
  private static List<Integer> getAllSubscriptionSeqIdsInTenant(DSLContext db, String tenantId)
  {
    List<Integer> retList = new ArrayList<>();
    if (db == null || StringUtils.isBlank(tenantId)) return retList;
    retList = db.select(SUBSCRIPTIONS.SEQ_ID).from(SUBSCRIPTIONS).where(SUBSCRIPTIONS.TENANT.eq(tenantId)).fetchInto(Integer.class);
    return retList;
  }

  /**
   * Add searchList to where condition. All conditions are joined using AND
   * Validate column name, search comparison operator
   *   and compatibility of column type + search operator + column value
   * @param whereCondition base where condition
   * @param searchList List of conditions to add to the base condition
   * @return resulting where condition
   * @throws TapisException on error
   */
  private static Condition addSearchListToWhere(Condition whereCondition, List<String> searchList)
          throws TapisException
  {
    if (searchList == null || searchList.isEmpty()) return whereCondition;
    // Parse searchList and add conditions to the WHERE clause
    for (String condStr : searchList)
    {
      whereCondition = addSearchCondStrToWhere(whereCondition, condStr, "AND");
    }
    return whereCondition;
  }

  /**
   * Create a condition for abstract syntax tree nodes by recursively walking the tree
   * @param astNode Abstract syntax tree node to add to the base condition
   * @return resulting condition
   * @throws TapisException on error
   */
  private static Condition createConditionFromAst(ASTNode astNode) throws TapisException
  {
    if (astNode == null || astNode instanceof ASTLeaf)
    {
      // A leaf node is a column name or value. Nothing to process since we only process a complete condition
      //   having the form column_name.op.value. We should never make it to here
      String msg = LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST1", (astNode == null ? "null" : astNode.toString()));
      throw new TapisException(msg);
    }
    else if (astNode instanceof ASTUnaryExpression)
    {
      // A unary node should have no operator and contain a binary node with two leaf nodes.
      // NOTE: Currently unary operators not supported. If support is provided for unary operators (such as NOT) then
      //   changes will be needed here.
      ASTUnaryExpression unaryNode = (ASTUnaryExpression) astNode;
      if (!StringUtils.isBlank(unaryNode.getOp()))
      {
        String msg = LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_UNARY_OP", unaryNode.getOp(), unaryNode.toString());
        throw new TapisException(msg);
      }
      // Recursive call
      return createConditionFromAst(unaryNode.getNode());
    }
    else if (astNode instanceof ASTBinaryExpression)
    {
      // It is a binary node
      ASTBinaryExpression binaryNode = (ASTBinaryExpression) astNode;
      // Recursive call
      return createConditionFromBinaryExpression(binaryNode);
    }
    return null;
  }

  /**
   * Create a condition from an abstract syntax tree binary node
   * @param binaryNode Abstract syntax tree binary node to add to the base condition
   * @return resulting condition
   * @throws TapisException on error
   */
  private static Condition createConditionFromBinaryExpression(ASTBinaryExpression binaryNode) throws TapisException
  {
    // If we are given a null then something went very wrong.
    if (binaryNode == null)
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST2"));
    }
    // If operator is AND or OR then make recursive call for each side and join together
    // For other operators build the condition left.op.right and add it
    String op = binaryNode.getOp();
    ASTNode leftNode = binaryNode.getLeft();
    ASTNode rightNode = binaryNode.getRight();
    if (StringUtils.isBlank(op))
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST3", binaryNode.toString()));
    }
    else if (op.equalsIgnoreCase("AND"))
    {
      // Recursive calls
      Condition cond1 = createConditionFromAst(leftNode);
      Condition cond2 = createConditionFromAst(rightNode);
      if (cond1 == null || cond2 == null)
      {
        throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST4", binaryNode.toString()));
      }
      return cond1.and(cond2);

    }
    else if (op.equalsIgnoreCase("OR"))
    {
      // Recursive calls
      Condition cond1 = createConditionFromAst(leftNode);
      Condition cond2 = createConditionFromAst(rightNode);
      if (cond1 == null || cond2 == null)
      {
        throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST4", binaryNode.toString()));
      }
      return cond1.or(cond2);

    }
    else
    {
      // End of recursion. Create a single condition.
      // Since operator is not an AND or an OR we should have 2 unary nodes or a unary and leaf node
      String lValue;
      String rValue;
      if (leftNode instanceof ASTLeaf) lValue = ((ASTLeaf) leftNode).getValue();
      else if (leftNode instanceof ASTUnaryExpression) lValue =  ((ASTLeaf) ((ASTUnaryExpression) leftNode).getNode()).getValue();
      else
      {
        throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST5", binaryNode.toString()));
      }
      if (rightNode instanceof ASTLeaf) rValue = ((ASTLeaf) rightNode).getValue();
      else if (rightNode instanceof ASTUnaryExpression) rValue =  ((ASTLeaf) ((ASTUnaryExpression) rightNode).getNode()).getValue();
      else
      {
        throw new TapisException(LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_AST6", binaryNode.toString()));
      }
      // Build the string for the search condition, left.op.right
      String condStr = String.format("%s.%s.%s", lValue, binaryNode.getOp(), rValue);
      // Validate and create a condition from the string
      return addSearchCondStrToWhere(null, condStr, null);
    }
  }

  /**
   * Take a string containing a single condition and create a new condition or join it to an existing condition.
   * Validate column name, search comparison operator and compatibility of column type + search operator + column value
   * @param whereCondition existing condition. If null a new condition is returned.
   * @param searchStr Single search condition in the form column_name.op.value
   * @param joinOp If whereCondition is not null use AND or OR to join the condition with the whereCondition
   * @return resulting where condition
   * @throws TapisException on error
   */
  private static Condition addSearchCondStrToWhere(Condition whereCondition, String searchStr, String joinOp)
          throws TapisException
  {
    // If we have no search string then return what we were given
    if (StringUtils.isBlank(searchStr)) return whereCondition;
    // If we are given a condition but no indication of how to join new condition to it then return what we were given
    if (whereCondition != null && StringUtils.isBlank(joinOp)) return whereCondition;
    if (whereCondition != null && joinOp != null && !joinOp.equalsIgnoreCase("AND") && !joinOp.equalsIgnoreCase("OR"))
    {
      return whereCondition;
    }

    // Parse search value into column name, operator and value
    // Format must be column_name.op.value
    String[] parsedStrArray = DOT_SPLIT.split(searchStr, 3);
    // Validate column name
    String column = parsedStrArray[0];
    Field<?> col = SUBSCRIPTIONS.field(DSL.name(column));
    // Check for column name passed in as camelcase
    if (col == null)
    {
      col = SUBSCRIPTIONS.field(DSL.name(SearchUtils.camelCaseToSnakeCase(column)));
    }
    // If column not found then it is an error
    if (col == null)
    {
      throw new TapisException(LibUtils.getMsg("NTFLIB_DB_NO_COLUMN", SUBSCRIPTIONS.getName(), DSL.name(column)));
    }
    // Validate and convert operator string
    String opStr = parsedStrArray[1].toUpperCase();
    SearchOperator op = SearchUtils.getSearchOperator(opStr);
    if (op == null)
    {
      String msg = MsgUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_OP", opStr, SUBSCRIPTIONS.getName(), DSL.name(column));
      throw new TapisException(msg);
    }

    // Check that column value is compatible for column type and search operator
    String val = parsedStrArray[2];
    checkConditionValidity(col, op, val);

     // If val is a timestamp then convert the string(s) to a form suitable for SQL
    // Use a utility method since val may be a single item or a list of items, e.g. for the BETWEEN operator
    if (col.getDataType().getSQLType() == Types.TIMESTAMP)
    {
      val = SearchUtils.convertValuesToTimestamps(op, val);
    }

    // Create the condition
    Condition newCondition = createCondition(col, op, val);
    // If specified add the condition to the WHERE clause
    if (StringUtils.isBlank(joinOp) || whereCondition == null) return newCondition;
    else if (joinOp.equalsIgnoreCase("AND")) return whereCondition.and(newCondition);
    else if (joinOp.equalsIgnoreCase("OR")) return whereCondition.or(newCondition);
    return newCondition;
  }

  /**
   * Validate condition expression based on column type, search operator and column string value.
   * Use java.sql.Types for validation.
   * @param col jOOQ column
   * @param op Operator
   * @param valStr Column value as string
   * @throws TapisException on error
   */
  private static void checkConditionValidity(Field<?> col, SearchOperator op, String valStr) throws TapisException
  {
    var dataType = col.getDataType();
    int sqlType = dataType.getSQLType();
    String sqlTypeName = dataType.getTypeName();
//    var t2 = dataType.getSQLDataType();
//    var t3 = dataType.getCastTypeName();
//    var t4 = dataType.getSQLType();
//    var t5 = dataType.getType();

    // Make sure we support the sqlType
    if (SearchUtils.ALLOWED_OPS_BY_TYPE.get(sqlType) == null)
    {
      String msg = LibUtils.getMsg("NTFLIB_DB_UNSUPPORTED_SQLTYPE", SUBSCRIPTIONS.getName(), col.getName(), op.name(), sqlTypeName);
      throw new TapisException(msg);
    }
    // Check that operation is allowed for column data type
    if (!SearchUtils.ALLOWED_OPS_BY_TYPE.get(sqlType).contains(op))
    {
      String msg = LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_TYPE", SUBSCRIPTIONS.getName(), col.getName(), op.name(), sqlTypeName);
      throw new TapisException(msg);
    }

    // Check that value (or values for op that takes a list) are compatible with sqlType
    if (!SearchUtils.validateTypeAndValueList(sqlType, op, valStr, sqlTypeName, SUBSCRIPTIONS.getName(), col.getName()))
    {
      String msg = LibUtils.getMsg("NTFLIB_DB_INVALID_SEARCH_VALUE", op.name(), sqlTypeName, valStr, SUBSCRIPTIONS.getName(), col.getName());
      throw new TapisException(msg);
    }
  }

  /**
   * Add condition to SQL where clause given column, operator, value info
   * @param col jOOQ column
   * @param op Operator
   * @param val Column value
   * @return Resulting where clause
   */
  private static Condition createCondition(Field col, SearchOperator op, String val)
  {
    List<String> valList = Collections.emptyList();
    if (SearchUtils.listOpSet.contains(op)) valList = SearchUtils.getValueList(val);
    Condition c = null;
    switch (op) {
      case EQ -> c = col.eq(val);
      case NEQ -> c = col.ne(val);
      case LT -> c =  col.lt(val);
      case LTE -> c = col.le(val);
      case GT -> c =  col.gt(val);
      case GTE -> c = col.ge(val);
      case LIKE -> c = col.like(val);
      case NLIKE -> c = col.notLike(val);
      case IN -> c = col.in(valList);
      case NIN -> c = col.notIn(valList);
      case BETWEEN -> c = col.between(valList.get(0), valList.get(1));
      case NBETWEEN -> c = col.notBetween(valList.get(0), valList.get(1));
    }
    return c;
  }

  /**
   * Given an sql connection retrieve the subscription uuid.
   * @param db - jooq context
   * @param tenantId - name of tenant
   * @param id - Id of subscription
   * @return - uuid
   */
  private static UUID getUUIDUsingDb(DSLContext db, String tenantId, String id)
  {
    return db.selectFrom(SUBSCRIPTIONS)
             .where(SUBSCRIPTIONS.TENANT.eq(tenantId),SUBSCRIPTIONS.ID.eq(id))
             .fetchOne(SUBSCRIPTIONS.UUID);
  }


  /**
   * Check items in select list against DB field names
   * @param selectList - list of items to check
   */
  private static void checkSelectListAgainstColumnNames(List<String> selectList) throws TapisException
  {
    for (String selectItem : selectList)
    {
      Field<?> colSelectItem = SUBSCRIPTIONS.field(DSL.name(SearchUtils.camelCaseToSnakeCase(selectItem)));
      if (!StringUtils.isBlank(selectItem) && colSelectItem == null)
      {
        String msg = LibUtils.getMsg("NTFLIB_DB_NO_COLUMN_SELECT", SUBSCRIPTIONS.getName(), DSL.name(selectItem));
        throw new TapisException(msg);
      }
    }
  }

  /*
   * Given a record from a select, create a subscription object
   */
  private static Subscription getSubscriptionFromRecord(Record r)
  {
    Subscription subscription;
    int seqId = r.get(SUBSCRIPTIONS.SEQ_ID);

    // Convert LocalDateTime to Instant. Note that although "Local" is in the type, timestamps from the DB are in UTC.
    LocalDateTime ldt = r.get(SUBSCRIPTIONS.EXPIRY);
    Instant expiry = (ldt == null) ? null : ldt.toInstant(ZoneOffset.UTC);
    Instant created = r.get(SUBSCRIPTIONS.CREATED).toInstant(ZoneOffset.UTC);
    Instant updated = r.get(SUBSCRIPTIONS.UPDATED).toInstant(ZoneOffset.UTC);

    JsonElement deliveryMethodsJson = r.get(SUBSCRIPTIONS.DELIVERY_METHODS);
    List<DeliveryMethod> deliveryMethods =
            Arrays.asList(TapisGsonUtils.getGson().fromJson(deliveryMethodsJson, DeliveryMethod[].class));
    subscription = new Subscription(seqId, r.get(SUBSCRIPTIONS.TENANT), r.get(SUBSCRIPTIONS.ID),
            r.get(SUBSCRIPTIONS.DESCRIPTION), r.get(SUBSCRIPTIONS.OWNER), r.get(SUBSCRIPTIONS.ENABLED),
            r.get(SUBSCRIPTIONS.TYPE_FILTER), r.get(SUBSCRIPTIONS.SUBJECT_FILTER), deliveryMethods,
            r.get(SUBSCRIPTIONS.TTL), r.get(SUBSCRIPTIONS.NOTES), r.get(SUBSCRIPTIONS.UUID), expiry, created, updated);
    return subscription;
  }

  /**
   * Given an sql connection retrieve the subscription sequence id.
   * @param db - jooq context
   * @param tenant - name of tenant
   * @param subscrId - Id of the subscription
   * @return - sequence id
   */
  private static int getSubscriptionSeqIdUsingDb(DSLContext db, String tenant, String subscrId)
  {
    Integer sid = db.selectFrom(SUBSCRIPTIONS).where(SUBSCRIPTIONS.TENANT.eq(tenant),SUBSCRIPTIONS.ID.eq(subscrId))
            .fetchOne(SUBSCRIPTIONS.SEQ_ID);
    if (sid == null) return 0;
    else return sid;
  }

  /**
   * Given a record from a select, create a Notification object
   */
  private static Notification getNotificationFromRecord(Record r)
  {
    Notification ntf;
    // Convert LocalDateTime to Instant. Note that although "Local" is in the type, timestamps from the DB are in UTC.
    Instant created = r.get(NOTIFICATIONS.CREATED).toInstant(ZoneOffset.UTC);

    // Convert JSONB columns to native types
    JsonElement eventJson = r.get(NOTIFICATIONS.EVENT);
    Event event = TapisGsonUtils.getGson().fromJson(eventJson, Event.class);
    JsonElement dmJson = r.get(NOTIFICATIONS.DELIVERY_METHOD);
    DeliveryMethod dm = TapisGsonUtils.getGson().fromJson(dmJson, DeliveryMethod.class);

    ntf = new Notification(r.get(NOTIFICATIONS.UUID), r.get(NOTIFICATIONS.SUBSCR_SEQ_ID), r.get(NOTIFICATIONS.TENANT),
                           r.get(NOTIFICATIONS.SUBSCR_ID), r.get(NOTIFICATIONS.BUCKET_NUMBER),
                           r.get(NOTIFICATIONS.EVENT_UUID), event, dm, created);
    return ntf;
  }

  /**
   * Given a recovery record from a select, create a Notification object
   */
  private static Notification getNotificationFromRecoveryRecord(Record r)
  {
    Notification ntf;
    // Convert LocalDateTime to Instant. Note that although "Local" is in the type, timestamps from the DB are in UTC.
    Instant created = r.get(NOTIFICATIONS_RECOVERY.CREATED).toInstant(ZoneOffset.UTC);

    // Convert JSONB columns to native types
    JsonElement eventJson = r.get(NOTIFICATIONS_RECOVERY.EVENT);
    Event event = TapisGsonUtils.getGson().fromJson(eventJson, Event.class);
    JsonElement dmJson = r.get(NOTIFICATIONS_RECOVERY.DELIVERY_METHOD);
    DeliveryMethod dm = TapisGsonUtils.getGson().fromJson(dmJson, DeliveryMethod.class);

    ntf = new Notification(r.get(NOTIFICATIONS_RECOVERY.UUID), r.get(NOTIFICATIONS_RECOVERY.SUBSCR_SEQ_ID),
            r.get(NOTIFICATIONS_RECOVERY.TENANT), r.get(NOTIFICATIONS_RECOVERY.SUBSCR_ID),
            r.get(NOTIFICATIONS_RECOVERY.BUCKET_NUMBER), r.get(NOTIFICATIONS_RECOVERY.EVENT_UUID), event, dm, created);
    return ntf;
  }
  /**
   * Given an sql connection check to see if specified TestSequence exists
   * @param db - jooq context
   * @param tenantId - name of tenant
   * @param subscrId -  Id of the subscription (NOT the sequence Id)
   * @return - true if test sequence exists, else false
   */
  private static boolean checkForTestSequence(DSLContext db, String tenantId, String subscrId)
  {
    return db.fetchExists(NOTIFICATIONS_TESTS,NOTIFICATIONS_TESTS.TENANT.eq(tenantId),
                          NOTIFICATIONS_TESTS.SUBSCR_ID.eq(subscrId));
  }

  /**
   * Given an sql connection retrieve the test sequence.
   * return null if not found.
   * @param db - jooq context
   * @param tenantId - name of tenant
   * @param subscrId -  Id of the subscription (NOT the sequence Id)
   * @return - TestSequence or null if not found
   */
  private static TestSequence getTestSequence(DSLContext db, String tenantId, String subscrId)
  {
    TestSequence testSequence;
    NotificationsTestsRecord r;
    r = db.selectFrom(NOTIFICATIONS_TESTS).where(NOTIFICATIONS_TESTS.TENANT.eq(tenantId),
                                                 NOTIFICATIONS_TESTS.SUBSCR_ID.eq(subscrId)).fetchOne();
    if (r == null) return null;
    else testSequence = getTestSequenceFromRecord(r);

    return testSequence;
  }

  /*
   * Given a record from a select, create a test sequence object
   */
  private static TestSequence getTestSequenceFromRecord(Record r)
  {
    TestSequence testSequence;
    int seqId = r.get(NOTIFICATIONS_TESTS.SEQ_ID);
    // Convert LocalDateTime to Instant. Note that although "Local" is in the type, timestamps from the DB are in UTC.
    Instant created = r.get(NOTIFICATIONS_TESTS.CREATED).toInstant(ZoneOffset.UTC);
    Instant updated = r.get(NOTIFICATIONS_TESTS.UPDATED).toInstant(ZoneOffset.UTC);

    JsonElement notificationsJson = r.get(NOTIFICATIONS_TESTS.NOTIFICATIONS);
    List<Notification> notifications = Arrays.asList(TapisGsonUtils.getGson().fromJson(notificationsJson, Notification[].class));
    testSequence = new TestSequence(seqId, r.get(NOTIFICATIONS_TESTS.TENANT), r.get(NOTIFICATIONS_TESTS.OWNER),
                                    r.get(NOTIFICATIONS_TESTS.SUBSCR_ID), r.get(NOTIFICATIONS_TESTS.NOTIFICATION_COUNT),
                                    notifications, created, updated);
    return testSequence;
  }
}
