package edu.utexas.tacc.tapis.notifications.service;

import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDaoImpl;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.shared.security.ServiceClients;
import edu.utexas.tacc.tapis.shared.security.ServiceContext;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.notifications.IntegrationUtils;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static edu.utexas.tacc.tapis.notifications.IntegrationUtils.*;

/**
 * Test the SubscriptionsService implementation class against a DB running locally
 * Note that this test has the following dependencies running locally or in dev
 *    Database - typically local
 *    Tenants service - typically dev
 *    Tokens service - typically dev and obtained from tenants service
 *    Security Kernel service - typically dev and obtained from tenants service
 *
 * Subscriptions are mostly owned by testuser1
 *   testuser1, testuser3 and testuser4 are also used
 */
@Test(groups={"integration"})
public class NotificationsServiceTest
{
  private NotificationsServiceImpl svcImpl;
  private ResourceRequestUser rJobsSvc1, rUser2, rUser5, rAdminUser, rFilesSvc;
  // Test data
  private static final String testKey = "Svc";
  // Special case IDs that have caused problems.
  private static final String specialId1 = testKey + subscrIdPrefix + "-subscr";
  private static final String jobsSvcName = "jobs";
  private static final String filesSvcName = "files";
  private static final String adminUser = "testadmin";
  private static final String siteId = "tacc";
  private static final String adminTenantName = "admin";
  private static final String testUser0 = "testuser0";
  private static final String testUser1 = "testuser1";
  private static final String testUser2 = "testuser2";
  private static final String testUser3 = "testuser3";
  private static final String testUser4 = "testuser4";
  private static final String testUser5 = "testuser5";

  // Create test definitions in memory
  int numSubscriptions = 13;
  Subscription[] subscriptions = IntegrationUtils.makeSubscriptions(numSubscriptions, testKey);

  @BeforeSuite
  public void setUp() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + NotificationsServiceTest.class.getSimpleName());
    // Setup for HK2 dependency injection
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure() {
        bind(NotificationsServiceImpl.class).to(NotificationsService.class);
        bind(NotificationsServiceImpl.class).to(NotificationsServiceImpl.class);
        bind(NotificationsDaoImpl.class).to(NotificationsDao.class);
        bindFactory(ServiceContextFactory.class).to(ServiceContext.class);
        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class);
      }
    });
    locator.inject(this);

    // Initialize TenantManager and services
    String url = RuntimeParameters.getInstance().getTenantsSvcURL();
    TenantManager.getInstance(url).getTenants();

    // Initialize services
    svcImpl = locator.getService(NotificationsServiceImpl.class);
    svcImpl.initService(adminTenantName, RuntimeParameters.getInstance());

    // Initialize authenticated user and service
    rAdminUser = new ResourceRequestUser(new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                          null, adminUser, tenantName, null, null, null));
    rFilesSvc = new ResourceRequestUser(new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                         null, testUser2, tenantName, null, null, null));
    rJobsSvc1 = new ResourceRequestUser(new AuthenticatedUser(jobsSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                      null, testUser1, tenantName, null, null, null));
    rUser2 = new ResourceRequestUser(new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser2, tenantName, null, null, null));
    rUser5 = new ResourceRequestUser(new AuthenticatedUser(testUser5, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser5, tenantName, null, null, null));

        // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + NotificationsServiceTest.class.getSimpleName());
    //Remove all objects created by tests
    String owner = subscriptions[0].getOwner();
    for (int i = 0; i < numSubscriptions; i++)
    {
      svcImpl.deleteSubscription(rJobsSvc1, owner, subscriptions[i].getName());
    }
    svcImpl.deleteSubscription(rJobsSvc1, owner, specialId1);

    Subscription tmpSub = svcImpl.getSubscription(rAdminUser, owner, subscriptions[0].getName());
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription Id: " + subscriptions[0].getName());
  }

  @BeforeTest
  public void initTest()
  {

  }
  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
  // Test creating a resource
  @Test
  public void testCreateSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[0];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, sub0.getOwner(), sub0.getName());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getName());
    System.out.println("Found item: " + sub0.getName());
  }

  // Test retrieving a resource
  @Test
  public void testGetSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[1];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, sub0.getOwner(), sub0.getName());
    checkCommonSubscriptionAttrs(sub0, tmpSub);
  }

  // Test update using PATCH
  @Test
  public void testPatchSubscription() throws Exception
  {
    // Test updating all attributes that can be updated.
    Subscription sub0 = subscriptions[3];
    String name = sub0.getName();
    String owner = sub0.getOwner();
    String createText = "{\"testPatch\": \"0-createFull\"}";
    svcImpl.createSubscription(rJobsSvc1, sub0, createText);
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, name);
    // Get last updated timestamp
    LocalDateTime updated = LocalDateTime.ofInstant(tmpSub.getUpdated(), ZoneOffset.UTC);
    String updatedStr1 = TapisUtils.getSQLStringFromUTCTime(updated);
    Thread.sleep(300);
    // ===========================================================
    // Create patchSubscription where all updatable attributes are changed
    // ===========================================================
    String patchFullText = "{\"testPatch\": \"1-patchFull\"}";
    PatchSubscription patchSubscriptionFull = IntegrationUtils.makePatchSubscriptionFull();
    // Update using patchSubscription
    svcImpl.patchSubscription(rJobsSvc1, owner, name, patchSubscriptionFull, patchFullText);
    Subscription tmpSubFull = svcImpl.getSubscription(rJobsSvc1, owner, name);
    // Get last updated timestamp
    updated = LocalDateTime.ofInstant(tmpSubFull.getUpdated(), ZoneOffset.UTC);
    String updatedStr2 = TapisUtils.getSQLStringFromUTCTime(updated);
    // Make sure update timestamp has been modified
    System.out.println("Updated timestamp before: " + updatedStr1 + " after: " + updatedStr2);
    Assert.assertNotEquals(updatedStr1, updatedStr2, "Update timestamp was not updated. Both are: " + updatedStr1);
    // Update original definition with PATCH values so we can use the checkCommon method.
    sub0.setDescription(description2);
    sub0.setTypeFilter(typeFilter2);
    sub0.setSubjectFilter(subjectFilter2);
    sub0.setDeliveryTargets(dmList2);
    //Check common attributes:
    checkCommonSubscriptionAttrs(sub0, tmpSubFull);
  }

  // Test retrieving all
  @Test
  public void testGetSubscriptions() throws Exception
  {
    Subscription sub0 = subscriptions[5];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    List<Subscription> subscriptions = svcImpl.getSubscriptions(rJobsSvc1, sub0.getOwner(), null, -1, null, -1, null);
    for (Subscription sub : subscriptions)
    {
      System.out.println("Found item with id: " + sub.getName());
    }
  }

  // Check enable/disable/delete as well as isEnabled
  @Test
  public void testEnableDisableDelete() throws Exception
  {
    // Create the resource
    Subscription sub0 = subscriptions[9];
    String subId = sub0.getName();
    String owner = sub0.getOwner();
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    // Enabled should start off true, then become false and finally true again.
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svcImpl.isEnabled(rJobsSvc1, owner, subId));
    int changeCount = svcImpl.disableSubscription(rJobsSvc1, owner, subId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    Assert.assertFalse(tmpSub.isEnabled());
    Assert.assertFalse(svcImpl.isEnabled(rJobsSvc1, owner, subId));
    changeCount = svcImpl.enableSubscription(rJobsSvc1, owner, subId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svcImpl.isEnabled(rJobsSvc1, owner, subId));

    // Delete should remove the resource
    // Delete should return 1 and then 0
    Assert.assertEquals(svcImpl.deleteSubscription(rJobsSvc1, owner, subId), 1);
    Assert.assertEquals(svcImpl.deleteSubscription(rJobsSvc1, owner, subId), 0);
    tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription Id: " + subId);
  }

  // Check that if resource already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^NTFLIB_SUBSCR_EXISTS.*")
  public void testCreateSubscriptionAlreadyExists() throws Exception
  {
    // Create the subscription
    Subscription sub0 = subscriptions[11];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    // Now attempt to create again, should get IllegalStateException with msg NTFLIB_SUBSCR_EXISTS
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
  }

  // Test set and update of TTL, Expiry should also be set and updated.
  @Test
  public void testTTL() throws Exception
  {
    // Create the resource
    Subscription sub0 = subscriptions[12];
    String subId = sub0.getName();
    String owner = sub0.getOwner();
    // Get the current time
    Instant now = Instant.now();
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    // Get and check the initial expiry.
    // TTL is in minutes so it should be ttl*60 seconds after the time of creation.
    // Check to the nearest second, i.e., assume it took much less than one second to create the subscription
    Instant expiry = tmpSub.getExpiry();
    long expirySeconds = expiry.truncatedTo(ChronoUnit.SECONDS).getEpochSecond() - now.truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
    Assert.assertEquals(tmpSub.getTtlMinutes()*60L, expirySeconds);

    // Sleep a couple of seconds
    Thread.sleep(2000);
    // Update the TTL and make sure the expiry is also updated.
    String newTTLStr = "60";
    now = Instant.now();
    svcImpl.updateSubscriptionTTL(rJobsSvc1, owner, subId, newTTLStr);
    tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    expiry = tmpSub.getExpiry();
    expirySeconds = expiry.truncatedTo(ChronoUnit.SECONDS).getEpochSecond() - now.truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
    Assert.assertEquals(tmpSub.getTtlMinutes()*60L, expirySeconds);

    // Test that setting TTL to 0 results in expiry of null
    svcImpl.updateSubscriptionTTL(rJobsSvc1, owner, subId, "0");
    tmpSub = svcImpl.getSubscription(rJobsSvc1, owner, subId);
    Assert.assertNull(tmpSub.getExpiry());
  }

  // Test various cases when resource is missing
  //  - isEnabled
  @Test
  public void testMissingSubscription() throws Exception
  {
    String fakeSubscriptionName = "AMissingSubscriptionName";
    boolean pass;
    // Make sure resource does not exist
    Assert.assertNull(svcImpl.getSubscription(rJobsSvc1, testUser1, fakeSubscriptionName));

    // Get should return null
    Subscription tmpSub = svcImpl.getSubscription(rJobsSvc1, testUser1, fakeSubscriptionName);
    Assert.assertNull(tmpSub, "Subscription not null for non-existent subscription");

    // Delete should return 0
    Assert.assertEquals(svcImpl.deleteSubscription(rJobsSvc1, testUser1, fakeSubscriptionName), 0);

    // isEnabled check should throw a NotFound exception
    pass = false;
    try { svcImpl.isEnabled(rJobsSvc1, testUser1, fakeSubscriptionName); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------

  // Test posting an event to the queue
  // NOTE: Event is left on the queue so it can be processed later.
  //       This method is useful for manually testing the processing of an event when the
  //       NotificationApplication and DispatchApplication have been started up.
  @Test(enabled = false)
  public void testPostEventAndLeave() throws Exception
  {
    Event event = new Event(eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1, eventTime,
                            eventDeleteSubscriptionsMatchingSubjectFalse, tenantName, testUser1, UUID.randomUUID());
    System.out.println("Placing event on queue. Event: " + event);
    // Put an event on the queue as a message
    svcImpl.publishEvent(rJobsSvc1, event);
  }

  // Test posting an event to the queue and reading it back
  // TODO/TBD When using deliveryCallback with basicPublish found putting in sleeps and watching rabbitmq console can
  //  see message is posted and then read off queue but not able to get the test to fail when it should.
  //  And not able to see output. Where does DeliveryCallback output go?
  @Test(enabled = false)
  public void testPostReadEvent() throws Exception
  {
    Event event = new Event(eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1, eventTime,
                            eventDeleteSubscriptionsMatchingSubjectFalse, tenantName, testUser1, UUID.randomUUID());
    System.out.println("Placing event on queue. Event: " + event);
    // Put an event on the queue as a message
    svcImpl.publishEvent(rJobsSvc1, event);
//    // Create a consumer to handle messages read from the queue
//    DeliverCallback deliverCallback = (consumerTab, delivery) -> {
//      String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
//      System.out.println("Received msg: " + msg);
//      // Convert received msg into an Event
//      Event tmpEvent = TapisGsonUtils.getGson().fromJson(msg, Event.class);
//      // TODO test does not fail
//      Assert.fail("WE SHOULD FAIL. HOW?");
//      Assert.assertNotNull(msg, "Reading event resulted in null.");
//      Assert.assertEquals(tmpEvent.getTenantId(), event.getTenantId());
//      Assert.assertEquals(tmpEvent.getSource(), event.getSource());
//      Assert.assertEquals(tmpEvent.getType(), event.getType());
//      Assert.assertEquals(tmpEvent.getSubject(), event.getSubject());
//      Assert.assertEquals(tmpEvent.getTime(), event.getTime());
//    };
    System.out.println("Sleep 2 secs");
    Thread.sleep(2000);
    // Read msg off the queue and verify the details
    System.out.println("Take event off queue.");
//    svcImpl.readEvent(rUser1, deliverCallback);
    boolean autoAck = true;
    Event tmpEvent = svcImpl.readEvent(autoAck);
    System.out.println("Read event from queue. Event: " + tmpEvent);
    Assert.assertNotNull(tmpEvent);
    Assert.assertEquals(event.getTenant(), tmpEvent.getTenant());
    Assert.assertEquals(event.getSource(), tmpEvent.getSource());
    Assert.assertEquals(event.getType(), tmpEvent.getType());
    Assert.assertEquals(event.getSubject(), tmpEvent.getSubject());
    Assert.assertEquals(event.getSeriesId(), tmpEvent.getSeriesId());
    Assert.assertEquals(event.getTimestamp(), tmpEvent.getTimestamp());
    Assert.assertEquals(event.getUuid(), tmpEvent.getUuid());
//    System.out.println("Ack message");
//    svcImpl.ackMsg(rUser1, deliverCallback);
  }

  // ************************************************************************
  // **************************  Private Methods  ***************************
  // ************************************************************************

  /**
   * Check common attributes after creating and retrieving aresource
   * @param origSub - Original test resource
   * @param fetchedSub - Retrieved resource
   */
  private static void checkCommonSubscriptionAttrs(Subscription origSub, Subscription fetchedSub)
  {
    Assert.assertNotNull(fetchedSub, "Failed to create item: " + origSub.getName());
    System.out.println("Found item: " + origSub.getName());
    Assert.assertEquals(fetchedSub.getTenant(), origSub.getTenant());
    Assert.assertEquals(fetchedSub.getName(), origSub.getName());
    Assert.assertEquals(fetchedSub.getDescription(), origSub.getDescription());
    Assert.assertEquals(fetchedSub.getTypeFilter(), origSub.getTypeFilter());
    Assert.assertEquals(fetchedSub.getTypeFilter1(), origSub.getTypeFilter1());
    Assert.assertEquals(fetchedSub.getTypeFilter2(), origSub.getTypeFilter2());
    Assert.assertEquals(fetchedSub.getTypeFilter3(), origSub.getTypeFilter3());
    Assert.assertEquals(fetchedSub.getSubjectFilter(), origSub.getSubjectFilter());
    Assert.assertEquals(fetchedSub.getTtlMinutes(), origSub.getTtlMinutes());
    Assert.assertEquals(fetchedSub.getOwner(), origSub.getOwner());
    Assert.assertEquals(fetchedSub.isEnabled(), origSub.isEnabled());
    // Verify deliveryMethods
    List<DeliveryTarget> fetchedDMList = fetchedSub.getDeliveryTargets();
    Assert.assertNotNull(fetchedDMList);
    List<DeliveryTarget> dmList0 = origSub.getDeliveryTargets();
    Assert.assertNotNull(dmList0);
    // Make sure the two dm lists are the same size
    Assert.assertEquals(fetchedDMList.size(), dmList0.size());
    // Make sure the fetched DMs contain all the expected delivery addresses
    // Put all the fetched delivery addresses into a list
    var addrSet = new HashSet<String>();
    for (DeliveryTarget dm : fetchedDMList) { addrSet.add(dm.getDeliveryAddress()); }
    for (DeliveryTarget dMethod : origSub.getDeliveryTargets())
    {
      Assert.assertTrue(addrSet.contains(dMethod.getDeliveryAddress()),
                        "List of addresses did not contain: " + dMethod.getDeliveryAddress());
    }
  }
}
