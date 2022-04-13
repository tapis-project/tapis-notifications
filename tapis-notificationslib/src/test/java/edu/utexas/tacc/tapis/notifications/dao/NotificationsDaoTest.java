package edu.utexas.tacc.tapis.notifications.dao;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.notifications.IntegrationUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import edu.utexas.tacc.tapis.notifications.model.Subscription;

import static edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters.*;
import static edu.utexas.tacc.tapis.notifications.IntegrationUtils.*;

/**
 * Test the SubscriptionsDao class against a DB running locally
 */
@Test(groups={"integration"})
public class NotificationsDaoTest
{
  private NotificationsDaoImpl dao;
  private ResourceRequestUser rUser;

  String testKey = "Dao";

  // Create test subscriptions and notifications in memory
  int numSubscriptions = 9;
  int numNotifications = 5;
  Subscription[] subscriptions = IntegrationUtils.makeSubscriptions(numSubscriptions, testKey);

  @BeforeSuite
  public void setup() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + NotificationsDaoTest.class.getSimpleName());
    dao = new NotificationsDaoImpl();
    // Initialize authenticated user
    rUser = new ResourceRequestUser(new AuthenticatedUser(apiUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                                          null, apiUser, tenantName, null, null, null));
    // Cleanup anything leftover from previous failed run
    teardown();
  }

  @AfterSuite
  public void teardown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + NotificationsDaoTest.class.getSimpleName());
    //Remove all objects created by tests
    for (int i = 0; i < numSubscriptions; i++)
    {
      dao.deleteSubscription(tenantName, subscriptions[i].getId());
    }
    Assert.assertFalse(dao.checkForSubscription(tenantName, subscriptions[0].getId()),
                       "Subscription not deleted. Subscription id: " + subscriptions[0].getId());
  }


  // ******************************************************************
  //   Subscriptions
  // ******************************************************************

  // Test create for a single item
  @Test
  public void testCreateSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[0];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
  }

  // Test retrieving a single item
  @Test
  public void testGetSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[1];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getId());
    System.out.println("Found item: " + sub0.getId());
    Assert.assertEquals(tmpSub.getId(), sub0.getId());
    Assert.assertEquals(tmpSub.getDescription(), sub0.getDescription());
    Assert.assertEquals(tmpSub.getOwner(), sub0.getOwner());
    Assert.assertEquals(tmpSub.getTypeFilter(), sub0.getTypeFilter());
    Assert.assertEquals(tmpSub.getTypeFilter1(), sub0.getTypeFilter1());
    Assert.assertEquals(tmpSub.getTypeFilter2(), sub0.getTypeFilter2());
    Assert.assertEquals(tmpSub.getTypeFilter3(), sub0.getTypeFilter3());
    Assert.assertEquals(tmpSub.getSubjectFilter(), sub0.getSubjectFilter());
    Assert.assertEquals(tmpSub.getTtl(), sub0.getTtl());
    Assert.assertNull(tmpSub.getExpiry());

    // Verify notes
    JsonObject obj = (JsonObject) tmpSub.getNotes();
    Assert.assertNotNull(obj, "Notes object was null");
    Assert.assertTrue(obj.has("project"));
    Assert.assertEquals(obj.get("project").getAsString(), notesObj1.get("project").getAsString());
    Assert.assertTrue(obj.has("testdata"));
    Assert.assertEquals(obj.get("testdata").getAsString(), notesObj1.get("testdata").getAsString());

    // Verify delivery methods
    List<DeliveryMethod> origDMs = sub0.getDeliveryMethods();
    List<DeliveryMethod> deliveryMethods = tmpSub.getDeliveryMethods();
    Assert.assertNotNull(origDMs, "Orig deliveryMethods was null");
    Assert.assertNotNull(deliveryMethods, "Fetched deliveryMethods was null");
    Assert.assertEquals(deliveryMethods.size(), origDMs.size());
    var dmAddrsFound = new ArrayList<String>();
    for (DeliveryMethod dmFound : deliveryMethods) {dmAddrsFound.add(dmFound.getDeliveryAddress());}
    for (DeliveryMethod dmSeedItem : origDMs)
    {
      Assert.assertTrue(dmAddrsFound.contains(dmSeedItem.getDeliveryAddress()),
              "List of delivery methods did not contain a method with address: " + dmSeedItem.getDeliveryAddress());
    }
    Assert.assertNotNull(tmpSub.getCreated(), "Fetched created timestamp should not be null");
    Assert.assertNotNull(tmpSub.getUpdated(), "Fetched updated timestamp should not be null");
  }

  // Test retrieving all subscriptions
  @Test
  public void testGetSubscriptions() throws Exception
  {
    Subscription sub0 = subscriptions[2];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    List<Subscription> subscriptions = dao.getSubscriptions(tenantName, null, null, null, DEFAULT_LIMIT, orderByListNull,
                                                            DEFAULT_SKIP, startAfterNull);
    for (Subscription subscription : subscriptions)
    {
      System.out.println("Found item with id: " + subscription.getId());
    }
  }

  // Test retrieving all subscriptions in a list of IDs
  @Test
  public void testGetSubscriptionsInIDList() throws Exception
  {
    var subIdList = new HashSet<String>();
    // Create 2 subscriptions
    Subscription sub0 = subscriptions[3];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    subIdList.add(sub0.getId());
    sub0 = subscriptions[4];
    itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    subIdList.add(sub0.getId());
    // Get all subscriptions in list of seqIDs
    List<Subscription> subscriptions = dao.getSubscriptions(tenantName, null, null, subIdList, DEFAULT_LIMIT,
                                                            orderByListNull, DEFAULT_SKIP, startAfterNull);
    for (Subscription subscription : subscriptions) {
      System.out.println("Found item with id: " + subscription.getId());
      Assert.assertTrue(subIdList.contains(subscription.getId()));
    }
    Assert.assertEquals(subIdList.size(), subscriptions.size());
  }

  // Test enable/disable/delete
  @Test
  public void testEnableDisableDeleteSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[5];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    System.out.println("Created item, id: " + sub0.getId() + " enabled: " + sub0.isEnabled());
    // Enabled should start off true, then become false and finally true again.
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertTrue(tmpSub.isEnabled());
    dao.updateEnabled(rUser, tenantName, sub0.getId(), false);
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertFalse(tmpSub.isEnabled());
    dao.updateEnabled(rUser, tenantName, sub0.getId(), true);
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertTrue(tmpSub.isEnabled());

    // Deleted should remove the item
    dao.deleteSubscription(tenantName, sub0.getId());
    Assert.assertFalse(dao.checkForSubscription(sub0.getTenant(), sub0.getId()),"Subscription not deleted. Subscription id: " + sub0.getId());
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertNull(tmpSub);
  }

  // Test change subscription owner
  @Test
  public void testChangeSubscriptionOwner() throws Exception
  {
    Subscription sub0 = subscriptions[6];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    System.out.println("Created item with subscriptionId: " + sub0.getId());
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    dao.updateSubscriptionOwner(rUser, tenantName, sub0.getId(), "newOwner");
    Subscription tmpSubscription = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertEquals(tmpSubscription.getOwner(), "newOwner");
  }

  // Test update TTL
  @Test
  public void testUpdateSubscriptionTTL() throws Exception
  {
    Subscription sub0 = subscriptions[7];
    Instant newExpiry = Instant.now();
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    System.out.println("Created item with subscriptionId: " + sub0.getId());
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getId());
    // Interesting, storing and retrieving from DB truncates/rounds from nanoseconds to microseconds.
    System.out.println("Old Expiry: " + sub0.getExpiry());
    System.out.println("New Expiry: " + newExpiry);
    dao.updateSubscriptionTTL(rUser, tenantName, sub0.getId(), ttl2, newExpiry);
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    System.out.println("Got Expiry: " + tmpSub.getExpiry());
    LocalDateTime newExpiryLDT =  LocalDateTime.ofInstant(newExpiry, ZoneOffset.UTC);
    LocalDateTime gotExpiryLDT =  LocalDateTime.ofInstant(tmpSub.getExpiry(), ZoneOffset.UTC);
    System.out.println("New ExpiryLDT: " + newExpiryLDT);
    System.out.println("Got ExpiryLDT: " + gotExpiryLDT);
    // Truncate expiry values to milliseconds. Since the new TTL is many seconds this is OK.
    LocalDateTime newExpiryLDTms = newExpiryLDT.truncatedTo(ChronoUnit.MILLIS);
    LocalDateTime gotExpiryLDTms = gotExpiryLDT.truncatedTo(ChronoUnit.MILLIS);
    System.out.println("New ExpiryLDTms: " + newExpiryLDTms);
    System.out.println("Got ExpiryLDTms: " + gotExpiryLDTms);
    Assert.assertEquals(tmpSub.getTtl(), ttl2);
    Assert.assertEquals(gotExpiryLDTms, newExpiryLDTms);
  }

  // Test behavior when subscription is missing, especially for cases where service layer depends on the behavior.
  //  update - throws not found exception
  //  get - returns null
  //  check - returns false
  //  getOwner - returns null
  @Test
  public void testMissingSubscription() throws Exception
  {
    String fakeSubscriptionName = "AMissingSubscriptionName";
    Subscription patchedSubscription =
            new Subscription(1, tenantName, fakeSubscriptionName, "description", "owner", isEnabledTrue,
                             typeFilter1, subjectFilter1, dmList1, ttl1, notes1, uuidNull, expiryNull, createdNull, updatedNull);
    // Make sure subscription does not exist
    Assert.assertFalse(dao.checkForSubscription(tenantName, fakeSubscriptionName));
    // update should throw not found exception
    boolean pass = false;
    try { dao.patchSubscription(rUser, fakeSubscriptionName, patchedSubscription, scrubbedJson, null); }
    catch (IllegalStateException e)
    {
      System.out.println("Exception msg: " + e.getMessage());
      Assert.assertTrue(e.getMessage().startsWith("NTFLIB_SUBSCR_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);
    Assert.assertNull(dao.getSubscription(tenantName, fakeSubscriptionName));
    Assert.assertNull(dao.getSubscriptionOwner(tenantName, fakeSubscriptionName));
  }

  // ******************************************************************
  //   Notifications
  // ******************************************************************

  // Test create for a batch of notifications
  @Test
  public void testPersistNotificationsUpdateLastEvent() throws Exception
  {
    // Create a Subscription to be referenced by notifications
    Subscription sub0 = subscriptions[8];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull, gson.toJson(sub0), scrubbedJson);
    Assert.assertTrue(itemCreated, "Subscription not created, id: " + sub0.getId());
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getId());
    Assert.assertNotNull(tmpSub, "Failed to create subscription: " + sub0.getId());

    // Create test notifications
    var notifications = IntegrationUtils.makeNotifications(numNotifications, testKey, tmpSub.getSeqId(), tmpSub.getId());

    // Persist test notifications
    boolean b = dao.persistNotificationsAndUpdateLastEvent(tenantName, event1, bucketNum1, notifications);
    Assert.assertTrue(b);

    // Check that they were persisted.
    List<Notification> tmpNotifications = dao.getNotificationsForEvent(tenantName, event1, bucketNum1);
    Assert.assertEquals(tmpNotifications.size(), notifications.size());
    DeliveryMethod.DeliveryType dType = notifications.get(0).getDeliveryMethod().getDeliveryType();
    for (Notification n : tmpNotifications)
    {
      System.out.println("Found notification: " + n);
      Assert.assertEquals(n.getBucketNum(), bucketNum1);
      Assert.assertEquals(n.getEvent().getUuid(), event1.getUuid());
      Assert.assertEquals(n.getSubscrSeqId(), tmpSub.getSeqId());
      Assert.assertEquals(n.getDeliveryMethod().getDeliveryType(), dType);
      Assert.assertNotNull(n.getCreated());
    }

    // Check that we can fetch one.
    Notification ntf = tmpNotifications.get(0);
    Notification tmpNtf = dao.getNotification(tenantName, ntf.getUuid());
    Assert.assertNotNull(tmpNtf, "Notification not found");
    Assert.assertEquals(tmpNtf.getBucketNum(), bucketNum1);
    Assert.assertEquals(tmpNtf.getEvent().getUuid(), event1.getUuid());
    Assert.assertEquals(tmpNtf.getSubscrSeqId(), tmpSub.getSeqId());
    Assert.assertNotNull(tmpNtf.getDeliveryMethod());
    Assert.assertFalse(StringUtils.isBlank(tmpNtf.getDeliveryMethod().getDeliveryAddress()));
    Assert.assertNotNull(tmpNtf.getDeliveryMethod().getDeliveryType());
    Assert.assertNotNull(tmpNtf.getCreated());

    // Check that we can delete one.
    ntf = tmpNotifications.get(0);
    dao.deleteNotification(tenantName, ntf);
    tmpNtf = dao.getNotification(tenantName, ntf.getUuid());
    Assert.assertNull(tmpNtf, "Notification not deleted");

    // Check that notifications_last_event table was updated.
    UUID u = dao.getLastEventUUID(bucketNum1);
    Assert.assertNotNull(u, "Last event not found");
    Assert.assertEquals(u, event1.getUuid());

  }
}
