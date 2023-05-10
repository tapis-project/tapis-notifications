package edu.utexas.tacc.tapis.notifications.service;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDaoImpl;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
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

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  private ResourceRequestUser rJobsSvc1, rJobsSvc2, rUser1, rUser2, rUser5, rAdminUser, rFilesSvc;
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
  int numSubscriptions = 19;
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
    rJobsSvc2 = new ResourceRequestUser(new AuthenticatedUser(jobsSvcName, adminTenantName, TapisThreadContext.AccountType.service.name(),
            null, testUser2, tenantName, null, null, null));
    rUser1 = new ResourceRequestUser(new AuthenticatedUser(testUser1, tenantName, TapisThreadContext.AccountType.user.name(),
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
    // Remove all objects created by tests
    // Since subscriptions are scoped by owner+name and we have various owners set after subscriptions[] is
    //   is created we cannot use just owner+name from subscriptions[]
    // Instead get all subscriptions matching a pattern and remove those.
    // Since tests are run against a local DB this should be OK.
    String owner = subscriptions[0].getOwner();
    var searchList = new ArrayList<String>();
    searchList.add(String.format("name.like.%s_%s*", subscrIdPrefix, testKey));
    List<Subscription> testSubscriptions = svcImpl.getSubscriptions(rJobsSvc1, owner, searchList, -1, null, -1,
                                                                    null, anyOwnerTrue);
    int count;
    for (Subscription subsc : testSubscriptions)
    {
      count = svcImpl.deleteSubscriptionByName(rAdminUser, subsc.getOwner(), subsc.getName());
      if (count > 0)   System.out.println("Deleted subscription: " + subsc.getName());
    }
    // Use subscription[0] to check we have cleaned up - this one should have original owner+name
    Subscription tmpSub = svcImpl.getSubscriptionByName(rAdminUser, owner, subscriptions[0].getName());
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription: " + subscriptions[0].getName());

    // Cleanup subscriptions with auto-generated names
    // jobs~testuser1~dev~subject_filter_1~Zk5p
    searchList = new ArrayList<String>();
    searchList.add("name.like.jobs\\~testuser1\\~dev\\~subject_filter_1*");
    testSubscriptions = svcImpl.getSubscriptions(rJobsSvc1, owner, searchList, -1, null, -1, null, anyOwnerTrue);
    for (Subscription subsc : testSubscriptions)
    {
      count = svcImpl.deleteSubscriptionByName(rAdminUser, subsc.getOwner(), subsc.getName());
      if (count > 0)   System.out.println("Deleted subscription: " + subsc.getName());
    }
  }

  @BeforeTest
  public void initTest()
  {

  }
  // -----------------------------------------------------------------------
  // ------------------------- Subscriptions -------------------------------
  // -----------------------------------------------------------------------
  // Test creating a subscription
  @Test
  public void testCreateSubscription() throws Exception
  {
    Subscription sub0 = subscriptions[0];
    // Regular user should not be able to create a subscription
    boolean pass = false;
    try { svcImpl.createSubscription(rUser1, sub0, scrubbedJson); }
    catch (NotAuthorizedException e)
    {
      pass = true;
    }
    Assert.assertTrue(pass);

    // Service user should be able to create
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, sub0.getOwner(), sub0.getName());
    Assert.assertNotNull(tmpSub, "Failed to create item: " + sub0.getName());
    System.out.println("Found item: " + sub0.getName());
  }

  // Test creating a subscription without specifying a name.
  // Service should create the subscription with a name matching a specific pattern
  @Test
  public void testCreateSubscriptionAutoName() throws Exception
  {
    // Create in-memory subscription with name = null.
    Subscription sub0 = new Subscription(subscriptions[0], tenantName, subscriptions[0].getOwner(), null);
    String subscrName = svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Assert.assertFalse(subscrName.isBlank());
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, sub0.getOwner(), subscrName);
    Assert.assertNotNull(tmpSub, "Failed to create subscription with unique name: " + subscrName);
    System.out.println("Found subscription with unique name: " + tmpSub.getName());
  }

  // Test retrieving a subscription
  @Test
  public void testGetSubscription() throws Exception
  {
    // Create and retrieve a subscription as the jobs service with oboUser = testuser1.
    Subscription sub0 = subscriptions[1];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, sub0.getOwner(), sub0.getName());
    checkCommonSubscriptionAttrs(sub0, tmpSub);
    // should also be able to retrieve the subscription as testuser1
    tmpSub = svcImpl.getSubscriptionByName(rUser1, sub0.getOwner(), sub0.getName());
    checkCommonSubscriptionAttrs(sub0, tmpSub);
    // should not be able to get the subscription as testuser2
    boolean pass = false;
    try { svcImpl.getSubscriptionByName(rUser2, sub0.getOwner(), sub0.getName()); }
    catch (NotAuthorizedException e)
    {
      pass = true;
    }
    Assert.assertTrue(pass);
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
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, name);
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
    Subscription tmpSubFull = svcImpl.getSubscriptionByName(rJobsSvc1, owner, name);
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
    sub0.setDeliveryTargets(dtList2);
    //Check common attributes:
    checkCommonSubscriptionAttrs(sub0, tmpSubFull);
  }

  // Test retrieving all
  @Test
  public void testGetSubscriptions() throws Exception
  {
    Subscription sub0 = subscriptions[5];
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    List<Subscription> subscriptions = svcImpl.getSubscriptions(rJobsSvc1, sub0.getOwner(), null, -1, null, -1, null, false);
    for (Subscription sub : subscriptions)
    {
      System.out.println("Found subscription: " + sub.getName());
    }
  }

  // Check enable/disable/delete as well as isEnabled
  @Test
  public void testEnableDisableDelete() throws Exception
  {
    // Create the resource
    Subscription sub0 = subscriptions[9];
    String subName = sub0.getName();
    String owner = sub0.getOwner();
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    // Enabled should start off true, then become false and finally true again.
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svcImpl.isEnabled(rJobsSvc1, owner, subName));
    int changeCount = svcImpl.disableSubscription(rJobsSvc1, owner, subName);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
    Assert.assertFalse(tmpSub.isEnabled());
    Assert.assertFalse(svcImpl.isEnabled(rJobsSvc1, owner, subName));
    changeCount = svcImpl.enableSubscription(rJobsSvc1, owner, subName);
    Assert.assertEquals(changeCount, 1, "Change count incorrect when updating.");
    tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
    Assert.assertTrue(tmpSub.isEnabled());
    Assert.assertTrue(svcImpl.isEnabled(rJobsSvc1, owner, subName));

    // Delete should remove the resource
    // Delete should return 1 and then 0
    Assert.assertEquals(svcImpl.deleteSubscriptionByName(rJobsSvc1, owner, subName), 1);
    Assert.assertEquals(svcImpl.deleteSubscriptionByName(rJobsSvc1, owner, subName), 0);
    tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription: " + subName);
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
    String subName = sub0.getName();
    String owner = sub0.getOwner();
    // Get the current time
    Instant now = Instant.now();
    svcImpl.createSubscription(rJobsSvc1, sub0, scrubbedJson);
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
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
    svcImpl.updateSubscriptionTTL(rJobsSvc1, owner, subName, newTTLStr);
    tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
    expiry = tmpSub.getExpiry();
    expirySeconds = expiry.truncatedTo(ChronoUnit.SECONDS).getEpochSecond() - now.truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
    Assert.assertEquals(tmpSub.getTtlMinutes()*60L, expirySeconds);

    // Test that setting TTL to 0 results in expiry of null
    svcImpl.updateSubscriptionTTL(rJobsSvc1, owner, subName, "0");
    tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, owner, subName);
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
    Assert.assertNull(svcImpl.getSubscriptionByName(rJobsSvc1, testUser1, fakeSubscriptionName));

    // Get should return null
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, testUser1, fakeSubscriptionName);
    Assert.assertNull(tmpSub, "Subscription not null for non-existent subscription");

    // Delete should return 0
    Assert.assertEquals(svcImpl.deleteSubscriptionByName(rJobsSvc1, testUser1, fakeSubscriptionName), 0);

    // isEnabled check should throw a NotFound exception
    pass = false;
    try { svcImpl.isEnabled(rJobsSvc1, testUser1, fakeSubscriptionName); }
    catch (NotFoundException nfe)
    {
      pass = true;
    }
    Assert.assertTrue(pass);
  }

  /*
   * Test various use cases around having many subscriptions owned by different users.
   * Many of these use cases are driven by the Jobs service (getBySubjectFilter, deleteBySubjectFilter, deleteByUUID).
   * Create 6 subscriptions all with the same subjectFilter, 3 owned by testuser1 and 3 owned by testuser2
   *  - Verify that Jobs service can get all subscriptions matching the subjectFilter.
   *  - Verify that testuser1 and testuser2 see only 3 subscriptions
   *  - Verify that Admin user can get subscriptions owned by another user.
   *  - Verify that Jobs service can delete a subscription by UUID
   *  - Verify that Jobs service can delete all subscriptions matching a subjectFilter.
   */
  @Test
  public void testManySubscriptionsForJobsSvc() throws Exception
  {
    var nameSet1 = new HashSet<String>();
    var nameSet2 = new HashSet<String>();
    // Create 6 subscriptions, 3 owned by testuser1 and 3 owned by testuser2
    Subscription sub1 = createSubscriptionSameSubjectFilter(rJobsSvc1, subscriptions[13], nameSet1);
    Subscription sub2 = createSubscriptionSameSubjectFilter(rJobsSvc1, subscriptions[14], nameSet1);
    Subscription sub3 = createSubscriptionSameSubjectFilter(rJobsSvc1, subscriptions[15], nameSet1);
    Subscription sub4 = createSubscriptionSameSubjectFilter(rJobsSvc2, subscriptions[16], nameSet2);
    Subscription sub5 = createSubscriptionSameSubjectFilter(rJobsSvc2, subscriptions[17], nameSet2);
    Subscription sub6 = createSubscriptionSameSubjectFilter(rJobsSvc2, subscriptions[18], nameSet2);

    // As Jobs service get all subscriptions matching subjectFilter. Owner should not matter
    var searchBySubjectFilter = new ArrayList<String>();
    searchBySubjectFilter.add(String.format("subject_filter.eq.%s", subjectFilter0));
    List<Subscription> subscriptions = svcImpl.getSubscriptions(rJobsSvc1, sub1.getOwner(), searchBySubjectFilter, -1,
                                                                null, -1, null, anyOwnerTrue);
    Assert.assertNotNull(subscriptions);
    Assert.assertFalse(subscriptions.isEmpty());
    for (Subscription sub : subscriptions)
    {
      System.out.printf("As user: %s Found subscription: %s%n", rJobsSvc1.getJwtUserId(), sub.getName());
      Assert.assertTrue(nameSet1.contains(sub.getName()) || nameSet2.contains(sub.getName()));
    }
    Assert.assertEquals(subscriptions.size(), 6);

    // As testuser1 get all subscriptions matching subjectFilter. Should only have 3
    subscriptions = svcImpl.getSubscriptions(rUser1, rUser1.getJwtUserId(), searchBySubjectFilter, -1, null, -1, null, anyOwnerFalse);
    Assert.assertNotNull(subscriptions);
    Assert.assertFalse(subscriptions.isEmpty());
    for (Subscription sub : subscriptions)
    {
      System.out.printf("As user: %s Found subscription: %s%n", rUser1.getJwtUserId(), sub.getName());
      Assert.assertTrue(nameSet1.contains(sub.getName()));
    }
    Assert.assertEquals(subscriptions.size(), 3);

    // As testuser2 get all subscriptions matching subjectFilter. Should only have 3
    subscriptions = svcImpl.getSubscriptions(rUser2, rUser2.getJwtUserId(), searchBySubjectFilter, -1, null, -1, null, anyOwnerFalse);
    Assert.assertNotNull(subscriptions);
    Assert.assertFalse(subscriptions.isEmpty());
    for (Subscription sub : subscriptions)
    {
      System.out.printf("As user: %s Found subscription: %s%n", rUser2.getJwtUserId(), sub.getName());
      Assert.assertTrue(nameSet2.contains(sub.getName()));
    }
    Assert.assertEquals(subscriptions.size(), 3);

    // As admin user get all subscriptions owned by testuser2 and matching subjectFilter. Should only have 3
    subscriptions = svcImpl.getSubscriptions(rAdminUser, rUser2.getJwtUserId(), searchBySubjectFilter, -1, null, -1, null, anyOwnerFalse);
    Assert.assertNotNull(subscriptions);
    Assert.assertFalse(subscriptions.isEmpty());
    for (Subscription sub : subscriptions)
    {
      System.out.printf("As user: %s Found subscription: %s%n", rAdminUser.getJwtUserId(), sub.getName());
      Assert.assertTrue(nameSet2.contains(sub.getName()));
    }
    Assert.assertEquals(subscriptions.size(), 3);

    // As testuser2 when passing in testuser1 as owner should get auth denied
    boolean pass = false;
    try { svcImpl.getSubscriptions(rUser2, rUser1.getJwtUserId(), searchBySubjectFilter, -1, null, -1, null, anyOwnerFalse); }
    catch (NotAuthorizedException e)
    {
      pass = true;
    }
    Assert.assertTrue(pass);

    // As Jobs service delete a subscription by UUID
    // Delete should return 1 and then 0
    Assert.assertEquals(svcImpl.deleteSubscriptionByUuid(rJobsSvc1, sub1.getUuid().toString()), 1);
    Assert.assertEquals(svcImpl.deleteSubscriptionByUuid(rJobsSvc1, sub1.getUuid().toString()), 0);
    Subscription tmpSub = svcImpl.getSubscriptionByName(rJobsSvc1, sub1.getOwner(), sub1.getName());
    Assert.assertNull(tmpSub, "Subscription not deleted. Subscription: " + sub1.getName());

    // As Jobs service delete all subscriptions for a subjectFilter
    // Delete should return 5 and then 0
    Assert.assertEquals(svcImpl.deleteSubscriptionsBySubject(rJobsSvc1, ownerNull, subjectFilter0, true), 5);
    Assert.assertEquals(svcImpl.deleteSubscriptionsBySubject(rJobsSvc1, ownerEmpty, subjectFilter0, true), 0);
    subscriptions = svcImpl.getSubscriptions(rJobsSvc1, sub1.getOwner(), searchBySubjectFilter, -1, null, -1, null, anyOwnerTrue);
    Assert.assertNotNull(subscriptions);
    Assert.assertTrue(subscriptions.isEmpty());
  }

  // Check DeliveryTarget validation
  @Test
  public void testDeliveryTargetValidation() throws Exception
  {
    // Check positive
    String domain;
    domain = DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, "abc@example1.com");
    Assert.assertEquals(domain, "example1.com");
    domain = DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "http://example2.com/test");
    Assert.assertEquals(domain, "example2.com");
    domain = DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "https://www.fakeorg.org/test");
    Assert.assertEquals(domain, "www.fakeorg.org");
    domain = DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "http://localhost");
    Assert.assertEquals(domain, "localhost");
    domain = DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "http://127.0.0.1/test_it");
    Assert.assertEquals(domain, "127.0.0.1");

    // Check negative
    boolean pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, null); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, ""); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, "abc"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, "abc@"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, "@example.com"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.EMAIL, "abc@example com"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "abc"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "abc.com"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
    pass = false;
    try { DeliveryTarget.validateTargetAndExtractDomain(DeliveryMethod.WEBHOOK, "/abc"); }
    catch (IllegalArgumentException e) { pass = true; }
    Assert.assertTrue(pass);
  }

  // -----------------------------------------------------------------------
  // ------------------------- Events --------------------------------------
  // -----------------------------------------------------------------------

  // Test posting an event to the queue
  // NOTE: Event is left on the queue, so it can be processed later.
  //       This method is useful for manually testing the processing of an event when the
  //       NotificationApplication and DispatchApplication have been started up.
  @Test(enabled = false)
  public void testPostEventAndLeave() throws Exception
  {
    Event event = new Event(eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1, eventTime,
                            eventDeleteSubscriptionsMatchingSubjectFalse, tenantName, testUser1, UUID.randomUUID());
    System.out.println("Placing event on queue. Event: " + event);
    // Put an event on the queue as a message
    svcImpl.publishEvent(rJobsSvc1, eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1, eventTime,
                         eventDeleteSubscriptionsMatchingSubjectFalse, tenantName);
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
    svcImpl.publishEvent(rJobsSvc1, eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1, eventTime,
                         eventDeleteSubscriptionsMatchingSubjectFalse, tenantName);
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

  /*
   * Create a subscription owned by rUser.getOboUser with a constant subjectFilter.
   * Retrieve the subscription and return it.
   */
  private Subscription createSubscriptionSameSubjectFilter(ResourceRequestUser rSvc, Subscription subscr, Set<String> nameSet)
          throws TapisException, TapisClientException
  {
    subscr.setOwner(rSvc.getOboUserId());
    subscr.setSubjectFilter(subjectFilter0);
    svcImpl.createSubscription(rSvc, subscr, scrubbedJson);
    subscr = svcImpl.getSubscriptionByName(rSvc, subscr.getOwner(), subscr.getName());
    Assert.assertNotNull(subscr);
    nameSet.add(subscr.getName());
    return  subscr;
  }
}
