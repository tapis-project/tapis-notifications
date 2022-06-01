package edu.utexas.tacc.tapis.notifications.dao;

import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
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
      dao.deleteSubscription(tenantName, subscriptions[i].getOwner(), subscriptions[i].getName());
    }
    Assert.assertFalse(dao.checkForSubscription(tenantName, subscriptions[0].getOwner(), subscriptions[0].getName()),
                       "Subscription not deleted. Subscription id: " + subscriptions[0].getName());
  }


  // ******************************************************************
  //   Subscriptions
  // ******************************************************************

  // Test create for a single item
  @Test
  public void testCreateSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[0];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
  }

  // Test retrieving a single item
  @Test
  public void testGetSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[1];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getName());
    System.out.println("Found item: " + sub0.getName());
    Assert.assertEquals(tmpSub.getName(), sub0.getName());
    Assert.assertEquals(tmpSub.getDescription(), sub0.getDescription());
    Assert.assertEquals(tmpSub.getOwner(), sub0.getOwner());
    Assert.assertEquals(tmpSub.getTypeFilter(), sub0.getTypeFilter());
    Assert.assertEquals(tmpSub.getTypeFilter1(), sub0.getTypeFilter1());
    Assert.assertEquals(tmpSub.getTypeFilter2(), sub0.getTypeFilter2());
    Assert.assertEquals(tmpSub.getTypeFilter3(), sub0.getTypeFilter3());
    Assert.assertEquals(tmpSub.getSubjectFilter(), sub0.getSubjectFilter());
    Assert.assertEquals(tmpSub.getTtlMinutes(), sub0.getTtlMinutes());
    Assert.assertNull(tmpSub.getExpiry());

    // Verify delivery targets
    List<DeliveryTarget> origDMs = sub0.getDeliveryTargets();
    List<DeliveryTarget> deliveryTargets = tmpSub.getDeliveryTargets();
    Assert.assertNotNull(origDMs, "Orig deliveryTargets was null");
    Assert.assertNotNull(deliveryTargets, "Fetched deliveryTargets was null");
    Assert.assertEquals(deliveryTargets.size(), origDMs.size());
    var dmAddrsFound = new ArrayList<String>();
    for (DeliveryTarget dmFound : deliveryTargets) {dmAddrsFound.add(dmFound.getDeliveryAddress());}
    for (DeliveryTarget dmSeedItem : origDMs)
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
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    List<Subscription> subscriptions = dao.getSubscriptions(tenantName, sub0.getOwner(), null, null, null,
                                             DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP, startAfterNull);
    for (Subscription subscription : subscriptions)
    {
      System.out.println("Found item with id: " + subscription.getName());
    }
  }

  // Test retrieving all subscriptions in a list of IDs
  @Test
  public void testGetSubscriptionsInIDList() throws Exception
  {
    var subIdList = new HashSet<String>();
    // Create 2 subscriptions
    Subscription sub0 = subscriptions[3];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    subIdList.add(sub0.getName());
    sub0 = subscriptions[4];
    itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    subIdList.add(sub0.getName());
    // Get all subscriptions in list of seqIDs
    List<Subscription> subscriptions = dao.getSubscriptions(tenantName, sub0.getOwner(), null, null, subIdList,
                                                            DEFAULT_LIMIT, orderByListNull, DEFAULT_SKIP, startAfterNull);
    for (Subscription subscription : subscriptions) {
      System.out.println("Found item with id: " + subscription.getName());
      Assert.assertTrue(subIdList.contains(subscription.getName()));
    }
    Assert.assertEquals(subIdList.size(), subscriptions.size());
  }

  // Test enable/disable/delete
  @Test
  public void testEnableDisableDeleteSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[5];
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    System.out.println("Created item, id: " + sub0.getName() + " enabled: " + sub0.isEnabled());
    // Enabled should start off true, then become false and finally true again.
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertTrue(tmpSub.isEnabled());
    dao.updateEnabled(tenantName, sub0.getOwner(), sub0.getName(), false);
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertFalse(tmpSub.isEnabled());
    dao.updateEnabled(tenantName, sub0.getOwner(), sub0.getName(), true);
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertTrue(tmpSub.isEnabled());

    // Deleted should remove the item
    dao.deleteSubscription(tenantName, sub0.getOwner(), sub0.getName());
    Assert.assertFalse(dao.checkForSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName()),"Subscription not deleted. Subscription id: " + sub0.getName());
    tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertNull(tmpSub);
  }

  // Test update TTL
  @Test
  public void testUpdateSubscriptionTTL() throws Exception
  {
    Subscription sub0 = subscriptions[7];
    Instant newExpiry = Instant.now();
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    System.out.println("Created item with subscriptionId: " + sub0.getName());
    Assert.assertTrue(itemCreated, "Item not created, id: " + sub0.getName());
    // Interesting, storing and retrieving from DB truncates/rounds from nanoseconds to microseconds.
    System.out.println("Old Expiry: " + sub0.getExpiry());
    System.out.println("New Expiry: " + newExpiry);
    dao.updateSubscriptionTTL(tenantName, sub0.getOwner(), sub0.getName(), ttl2, newExpiry);
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
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
    Assert.assertEquals(tmpSub.getTtlMinutes(), ttl2);
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
    String fakeOwner = "owner";
    Subscription patchedSubscription =
            new Subscription(1, tenantName, fakeOwner, fakeSubscriptionName, "description", isEnabledTrue,
                             typeFilter1, subjectFilter1, dmList1, ttl1, uuidNull, expiryNull, createdNull, updatedNull);
    // Make sure subscription does not exist
    Assert.assertFalse(dao.checkForSubscription(tenantName, fakeOwner, fakeSubscriptionName));
    // update should throw not found exception
    boolean pass = false;
    try { dao.patchSubscription(rUser, fakeOwner, fakeSubscriptionName, patchedSubscription); }
    catch (IllegalStateException e)
    {
      System.out.println("Exception msg: " + e.getMessage());
      Assert.assertTrue(e.getMessage().startsWith("NTFLIB_SUBSCR_NOT_FOUND"));
      pass = true;
    }
    Assert.assertTrue(pass);
    Assert.assertNull(dao.getSubscription(tenantName, fakeOwner, fakeSubscriptionName));
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
    boolean itemCreated = dao.createSubscription(rUser, sub0, expiryNull);
    Assert.assertTrue(itemCreated, "Subscription not created, id: " + sub0.getName());
    Subscription tmpSub = dao.getSubscription(sub0.getTenant(), sub0.getOwner(), sub0.getName());
    Assert.assertNotNull(tmpSub, "Failed to create subscription: " + sub0.getName());

    // Create test notifications
    var notifications = IntegrationUtils.makeNotifications(numNotifications, testKey, tmpSub.getSeqId(), tmpSub.getName());

    // Persist test notifications
    boolean b = dao.persistNotificationsAndUpdateLastEvent(tenantName, event1, bucketNum1, notifications);
    Assert.assertTrue(b);

    // Check that they were persisted.
    List<Notification> tmpNotifications = dao.getNotificationsForEvent(tenantName, event1, bucketNum1);
    Assert.assertEquals(tmpNotifications.size(), notifications.size());
    DeliveryTarget.DeliveryMethod dType = notifications.get(0).getDeliveryTarget().getDeliveryType();
    for (Notification n : tmpNotifications)
    {
      System.out.println("Found notification: " + n);
      Assert.assertEquals(n.getBucketNum(), bucketNum1);
      Assert.assertEquals(n.getEvent().getUuid(), event1.getUuid());
      Assert.assertEquals(n.getSubscrSeqId(), tmpSub.getSeqId());
      Assert.assertEquals(n.getDeliveryTarget().getDeliveryType(), dType);
      Assert.assertNotNull(n.getCreated());
    }

    // Check that we can fetch one.
    Notification ntf = tmpNotifications.get(0);
    Notification tmpNtf = dao.getNotification(tenantName, ntf.getUuid());
    Assert.assertNotNull(tmpNtf, "Notification not found");
    Assert.assertEquals(tmpNtf.getBucketNum(), bucketNum1);
    Assert.assertEquals(tmpNtf.getEvent().getUuid(), event1.getUuid());
    Assert.assertEquals(tmpNtf.getSubscrSeqId(), tmpSub.getSeqId());
    Assert.assertNotNull(tmpNtf.getDeliveryTarget());
    Assert.assertFalse(StringUtils.isBlank(tmpNtf.getDeliveryTarget().getDeliveryAddress()));
    Assert.assertNotNull(tmpNtf.getDeliveryTarget().getDeliveryType());
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
