package edu.utexas.tacc.tapis.notifications.service;

import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.dao.SubscriptionsDao;
import edu.utexas.tacc.tapis.notifications.dao.SubscriptionsDaoImpl;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
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
import org.testng.annotations.Test;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class SubscriptionsServiceTest
{
  private NotificationsService svc;
  private NotificationsServiceImpl svcImpl;
  private ResourceRequestUser rUser0, rUser1, rUser2, rUser3, rUser4, rUser5, rAdminUser, rFilesSvc;
  // Test data
  private static final String testKey = "Svc";
  // Special case IDs that have caused problems.
  private static final String specialId1 = testKey + subIdPrefix + "-subscr";
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
  int numSubscriptions = 14;
  Subscription[] subscriptions = IntegrationUtils.makeSubscriptions(numSubscriptions, testKey);

  @BeforeSuite
  public void setUp() throws Exception
  {
    System.out.println("Executing BeforeSuite setup method: " + SubscriptionsServiceTest.class.getSimpleName());
    // Setup for HK2 dependency injection
    ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
    ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
      @Override
      protected void configure() {
        bind(NotificationsServiceImpl.class).to(NotificationsService.class);
        bind(NotificationsServiceImpl.class).to(NotificationsServiceImpl.class);
        bind(SubscriptionsDaoImpl.class).to(SubscriptionsDao.class);
        bindFactory(ServiceContextFactory.class).to(ServiceContext.class);
        bindFactory(ServiceClientsFactory.class).to(ServiceClients.class);
      }
    });
    locator.inject(this);

    // Initialize TenantManager and services
    String url = RuntimeParameters.getInstance().getTenantsSvcURL();
    TenantManager.getInstance(url).getTenants();

    // Initialize services
    svc = locator.getService(NotificationsService.class);
    svcImpl = locator.getService(NotificationsServiceImpl.class);
    svcImpl.initService(siteId, adminTenantName, RuntimeParameters.getInstance().getServicePassword());

    // Initialize authenticated user and service
    rAdminUser = new ResourceRequestUser(new AuthenticatedUser(adminUser, tenantName, TapisThreadContext.AccountType.user.name(),
                                          null, adminUser, tenantName, null, null, null));
    rFilesSvc = new ResourceRequestUser(new AuthenticatedUser(filesSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
                                         null, owner2, tenantName, null, null, null));
    rUser0 = new ResourceRequestUser(new AuthenticatedUser(testUser0, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser0, tenantName, null, null, null));
    rUser1 = new ResourceRequestUser(new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser1, tenantName, null, null, null));
    rUser2 = new ResourceRequestUser(new AuthenticatedUser(testUser2, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser2, tenantName, null, null, null));
    rUser3 = new ResourceRequestUser(new AuthenticatedUser(testUser3, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser3, tenantName, null, null, null));
    rUser4 = new ResourceRequestUser(new AuthenticatedUser(testUser4, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser4, tenantName, null, null, null));
    rUser5 = new ResourceRequestUser(new AuthenticatedUser(testUser5, tenantName, TapisThreadContext.AccountType.user.name(),
                                      null, testUser5, tenantName, null, null, null));

    // Cleanup anything leftover from previous failed run
    tearDown();
  }

  @AfterSuite
  public void tearDown() throws Exception
  {
    System.out.println("Executing AfterSuite teardown for " + SubscriptionsServiceTest.class.getSimpleName());
    //Remove all objects created by tests
    for (int i = 0; i < numSubscriptions; i++)
    {
      svcImpl.deleteSubscription(rAdminUser, subscriptions[i].getId());
    }
    svcImpl.deleteSubscription(rAdminUser, specialId1);

    Subscription tmpSub = svc.getSubscription(rAdminUser, subscriptions[0].getId());
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription Id: " + subscriptions[0].getId());
  }

  // Test creating a resource
  @Test
  public void testCreateSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[0];
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    Subscription tmpSub = svc.getSubscription(rUser1, sub0.getId());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getId());
    System.out.println("Found item: " + sub0.getId());
  }

  // Test retrieving a resource
  @Test
  public void testGetSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[1];
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    Subscription tmpSub = svc.getSubscription(rUser1, sub0.getId());
    checkCommonSubscriptionAttrs(sub0, tmpSub);
  }

  // Test update using PUT
  @Test
  public void testPutSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[2];
    String subId = sub0.getId();
    String createText = "{\"testPut\": \"0-create1\"}";
    svc.createSubscription(rUser1, sub0, createText);
    Subscription tmpSub = svc.getSubscription(rUser1, subId);
    checkCommonSubscriptionAttrs(sub0, tmpSub);

    // Get last updated timestamp
    LocalDateTime updated = LocalDateTime.ofInstant(tmpSub.getUpdated(), ZoneOffset.UTC);
    String updatedStr1 = TapisUtils.getSQLStringFromUTCTime(updated);
    Thread.sleep(300);

    // Create putSubscription where all updatable attributes are changed
    String put1Text = "{\"testPut\": \"1-put1\"}";
    Subscription putSubscription = IntegrationUtils.makePutSubscriptionFull(tmpSub);
    // Update using putSubscription
    svc.putSubscription(rUser1, putSubscription, put1Text);
    tmpSub = svc.getSubscription(rUser1, subId);

    // Get last updated timestamp
    updated = LocalDateTime.ofInstant(tmpSub.getUpdated(), ZoneOffset.UTC);
    String updatedStr2 = TapisUtils.getSQLStringFromUTCTime(updated);
    // Make sure update timestamp has been modified
    System.out.println("Updated timestamp before: " + updatedStr1 + " after: " + updatedStr2);
    Assert.assertNotEquals(updatedStr1, updatedStr2, "Update timestamp was not updated. Both are: " + updatedStr1);

    // Update original definition with PUT values so we can use the checkCommon method.
    sub0.setDescription(description2);
    sub0.setTypeFilter(typeFilter2);
    sub0.setSubjectFilter(subjectFilter2);
    sub0.setDeliveryMethods(dmList2);
    sub0.setNotes(notes2);
    //Check common attributes:
    checkCommonSubscriptionAttrs(sub0, tmpSub);

  }

  // Test update using PATCH
  @Test
  public void testPatchSubscription() throws Exception
  {
    // Test updating all attributes that can be updated.
    Subscription sub0 = subscriptions[3];
    String subId = sub0.getId();
    String createText = "{\"testPatch\": \"0-createFull\"}";
    svc.createSubscription(rUser1, sub0, createText);
    Subscription tmpSub = svc.getSubscription(rUser1, subId);
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
    svc.patchSubscription(rUser1, subId, patchSubscriptionFull, patchFullText);
    Subscription tmpSubFull = svc.getSubscription(rUser1, subId);
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
    sub0.setDeliveryMethods(dmList2);
    sub0.setNotes(notes2);
    //Check common attributes:
    checkCommonSubscriptionAttrs(sub0, tmpSubFull);
  }

  // Test changing owner
  @Test
  public void testChangeSubscriptionOwner() throws Exception
  {
    Subscription sub0 = subscriptions[4];
    String createText = "{\"testChangeOwner\": \"0-create\"}";
    String origOwnerName = testUser1;
    String newOwnerName = testUser3;
    ResourceRequestUser origOwnerAuth = rUser1;
    ResourceRequestUser newOwnerAuth = rUser3;

    svc.createSubscription(origOwnerAuth, sub0, createText);
    Subscription tmpSub = svc.getSubscription(origOwnerAuth, sub0.getId());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getId());

    // Change owner using api
    svc.changeSubscriptionOwner(origOwnerAuth, sub0.getId(), newOwnerName);

    // Confirm new owner
    tmpSub = svc.getSubscription(newOwnerAuth, sub0.getId());
    Assert.assertEquals(tmpSub.getOwner(), newOwnerName);

    // Original owner should not be able to modify
    try {
      svc.deleteSubscription(origOwnerAuth, sub0.getId());
      Assert.fail("Original owner should not have permission to update resource after change of ownership. Subscription name: " + sub0.getId() +
              " Old owner: " + origOwnerName + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().startsWith("NTFLIB_UNAUTH"));
    }
    // Original owner should not be able to read
    try {
      svc.getSubscription(origOwnerAuth, sub0.getId());
      Assert.fail("Original owner should not have permission to read resource after change of ownership. Subscription name: " + sub0.getId() +
              " Old owner: " + origOwnerName + " New Owner: " + newOwnerName);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().startsWith("NTFLIB_UNAUTH"));
    }
  }

//  @Test
//  public void testGetSubscriptionIDs() throws Exception
//  {
//    Subscription sub1 = subscriptions[5];
//    svc.createSubscription(rUser1, sub1, scrubbedJson);
//    Subscription sub2 = subscriptions[6];
//    svc.createSubscription(rUser1, sub2, scrubbedJson);
//    Set<String> subIDs = svc.getAllowedSubscriptionIDs(rUser1);
//    for (String name : subIDs)
//    {
//      System.out.println("Found item: " + name);
//    }
//    Assert.assertTrue(subIDs.contains(sub1.getId()), "List of subs did not contain sub name: " + sub1.getId());
//    Assert.assertTrue(subIDs.contains(sub2.getId()), "List of subs did not contain sub name: " + sub2.getId());
//  }

  // Test retrieving all
  @Test
  public void testGetSubscriptions() throws Exception
  {
    Subscription sub0 = subscriptions[7];
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    List<Subscription> subscriptions = svc.getSubscriptions(rUser1, null, -1, null, -1, null);
    for (Subscription sub : subscriptions)
    {
      System.out.println("Found item with id: " + sub.getId());
    }
  }

  // Check that user only sees resources they are authorized to see.
  @Test
  public void testGetSubscriptionsAuth() throws Exception
  {
    // Create 3 resources, 2 of which are owned by testUser5.
    Subscription sub0 = subscriptions[8];
    String sub1Name = sub0.getId();
    sub0.setOwner(rUser5.getOboUserId());
    svc.createSubscription(rUser5, sub0, scrubbedJson);

    sub0 = subscriptions[9];
    String sub2Name = sub0.getId();
    sub0.setOwner(rUser5.getOboUserId());
    svc.createSubscription(rUser5, sub0, scrubbedJson);

    sub0 = subscriptions[10];
    svc.createSubscription(rUser1, sub0, scrubbedJson);

    // When retrieving as testUser5 only 2 should be returned
    List<Subscription> subscriptions = svc.getSubscriptions(rUser5, searchListNull, -1, orderByListNull, -1, startAfterNull);
    System.out.println("Total number retrieved: " + subscriptions.size());
    Assert.assertEquals(subscriptions.size(), 2);
    for (Subscription sub : subscriptions)
    {
      System.out.println("Found item with subId: " + sub.getId());
      Assert.assertTrue(sub.getId().equals(sub1Name) || sub.getId().equalsIgnoreCase(sub2Name));
    }
  }

  // Check enable/disable/delete as well as isEnabled
  @Test
  public void testEnableDisableDelete() throws Exception
  {
    // Create the resource
    Subscription sub0 = subscriptions[11];
    String subId = sub0.getId();
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    // Enabled should start off true, then become false and finally true again.
    Subscription tmpSub = svc.getSubscription(rUser1, subId);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svc.isEnabled(rUser1, subId));
    int changeCount = svc.disableSubscription(rUser1, subId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svc.getSubscription(rUser1, subId);
    Assert.assertFalse(tmpSub.isEnabled());
    Assert.assertFalse(svc.isEnabled(rUser1, subId));
    changeCount = svc.enableSubscription(rUser1, subId);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svc.getSubscription(rUser1, subId);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svc.isEnabled(rUser1, subId));

    // Delete should remove the resource
    // Delete should return 1 and then 0
    Assert.assertEquals(svc.deleteSubscription(rUser1, subId), 1);
    Assert.assertEquals(svc.deleteSubscription(rUser1, subId), 0);
    tmpSub = svc.getSubscription(rUser1, subId);
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription Id: " + subId);
  }

  @Test
  public void testSubscriptionExists() throws Exception
  {
    Subscription sub0 = subscriptions[12];
    // If not there we should get false
    Assert.assertFalse(svc.checkForSubscription(rUser1, sub0.getId()));
    // After creating we should get true
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    Assert.assertTrue(svc.checkForSubscription(rUser1, sub0.getId()));
  }

  // Check that if resource already exists we get an IllegalStateException when attempting to create
  @Test(expectedExceptions = {IllegalStateException.class},  expectedExceptionsMessageRegExp = "^NTFLIB_SUBSCR_EXISTS.*")
  public void testCreateSubscriptionAlreadyExists() throws Exception
  {
    // Create the subscription
    Subscription sub0 = subscriptions[13];
    svc.createSubscription(rUser1, sub0, scrubbedJson);
    Assert.assertTrue(svc.checkForSubscription(rUser1, sub0.getId()));
    // Now attempt to create again, should get IllegalStateException with msg NTFLIB_SUBSCR_EXISTS
    svc.createSubscription(rUser1, sub0, scrubbedJson);
  }

  // Test various cases when resource is missing
  //  - isEnabled
  //  - get owner
  @Test
  public void testMissingSubscription() throws Exception
  {
    String fakeSubscriptionName = "AMissingSubscriptionName";
    String fakeUserName = "AMissingUserName";
    int changeCount;
    boolean pass;
    // Make sure resource does not exist
    Assert.assertFalse(svc.checkForSubscription(rUser1, fakeSubscriptionName));

    // Get should return null
    Subscription tmpSub = svc.getSubscription(rUser1, fakeSubscriptionName);
    Assert.assertNull(tmpSub, "Subscription not null for non-existent subscription");

    // Delete should return 0
    Assert.assertEquals(svc.deleteSubscription(rUser1, fakeSubscriptionName), 0);

    // isEnabled check should throw a NotFound exception
    pass = false;
    try { svc.isEnabled(rUser1, fakeSubscriptionName); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);

    // Get owner should return null
    String owner = svc.getSubscriptionOwner(rUser1, fakeSubscriptionName);
    Assert.assertNull(owner, "Owner not null for non-existent subscription.");
  }

  /**
   * Check common attributes after creating and retrieving aresource
   * @param origSub - Original test resource
   * @param fetchedSub - Retrieved resource
   */
  private static void checkCommonSubscriptionAttrs(Subscription origSub, Subscription fetchedSub)
  {
    Assert.assertNotNull(fetchedSub, "Failed to create item: " + origSub.getId());
    System.out.println("Found item: " + origSub.getId());
    Assert.assertEquals(fetchedSub.getTenant(), origSub.getTenant());
    Assert.assertEquals(fetchedSub.getId(), origSub.getId());
    Assert.assertEquals(fetchedSub.getDescription(), origSub.getDescription());
    Assert.assertEquals(fetchedSub.getTypeFilter(), origSub.getTypeFilter());
    Assert.assertEquals(fetchedSub.getSubjectFilter(), origSub.getSubjectFilter());
    Assert.assertEquals(fetchedSub.getOwner(), origSub.getOwner());
    Assert.assertEquals(fetchedSub.isEnabled(), origSub.isEnabled());
    // Verify deliveryMethods
    List<DeliveryMethod> fetchedDMList = fetchedSub.getDeliveryMethods();
    Assert.assertNotNull(fetchedDMList);
    List<DeliveryMethod> dmList0 = origSub.getDeliveryMethods();
    Assert.assertNotNull(dmList0);
    // Make sure the two dm lists are the same size
    Assert.assertEquals(fetchedDMList.size(), dmList0.size());
    // Make sure the fetched DMs contain all the expected webookUrls and emailAddresses
    // Put all the fetched webhookUrls and emailAddresses into lists
    var whSet = new HashSet<String>();
    var emailSet = new HashSet<String>();
    for (DeliveryMethod dm : fetchedDMList) { whSet.add(dm.getWebhookUrl()); emailSet.add(dm.getEmailAddress()); }
    for (DeliveryMethod dMethod : origSub.getDeliveryMethods())
    {
      Assert.assertTrue(whSet.contains(dMethod.getWebhookUrl()),
                        "List of webhookUrls did not contain: " + dMethod.getWebhookUrl());
      Assert.assertTrue(emailSet.contains(dMethod.getEmailAddress()),
                        "List of emailAddresses did not contain: " + dMethod.getEmailAddress());
    }
    // Verify notes
    Assert.assertNotNull(origSub.getNotes(), "Orig Notes should not be null");
    Assert.assertNotNull(fetchedSub.getNotes(), "Fetched Notes should not be null");
    System.out.println("Found notes: " + origSub.getNotes().toString());
    JsonObject tmpObj = (JsonObject) fetchedSub.getNotes();
    JsonObject origNotes = (JsonObject) origSub.getNotes();
    Assert.assertTrue(tmpObj.has("project"));
    String projStr = origNotes.get("project").getAsString();
    Assert.assertEquals(tmpObj.get("project").getAsString(), projStr);
    Assert.assertTrue(tmpObj.has("testdata"));
    String testdataStr = origNotes.get("testdata").getAsString();
    Assert.assertEquals(tmpObj.get("testdata").getAsString(), testdataStr);
  }
}
