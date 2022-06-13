package edu.utexas.tacc.tapis.notifications;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget.DeliveryMethod;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/*
 * Utilities and data for integration testing
 */
public final class IntegrationUtils
{
  public static final Gson gson =  TapisGsonUtils.getGson();

  // Test data
  public static final String siteId = "tacc";
  public static final String adminTenantName = "admin";
  public static final String tenantName = "dev";
  public static final String svcName = "notificationss";
  public static final String adminUser = "testadmin";

  public static final String testUser0 = "testuser0";
  public static final String testUser1 = "testuser1";
  public static final String testUser2 = "testuser2";
  public static final String testUser3 = "testuser3";
  public static final String testUser4 = "testuser4";
  public static final String apiUser = "testApiUser";
  public static final String subscrIdPrefix = "TestSubscr";
  public static final String description1 = "Subscription description 1";
  public static final String description2 = "Subscription description 2";
  public static final String typeFilter1 = "jobs.job.complete";
  public static final String typeFilter1_1 = "jobs";
  public static final String typeFilter1_2 = "job";
  public static final String typeFilter1_3 = "complete";
  public static final String typeFilter2 = "systems.system.create";
  public static final String typeFilter2_1 = "systems";
  public static final String typeFilter2_2 = "system";
  public static final String typeFilter2_3 = "create";
  public static final String subjectFilter0 = "subject_filter_0";
  public static final String subjectFilter1 = "subject_filter_1";
  public static final String subjectFilter2 = "subject_filter_2";
  public static final String webhookUrlA1 = "https://my.fake.webhook/urlA1";
  public static final String webhookUrlA2 = "https://my.fake.webhook/urlA2";
  public static final String emailAddressB1 = "my.fake.emailB1@my.example.com";
  public static final String emailAddressB2 = "my.fake.emailB2@my.example.com";
  public static final String scrubbedJson = "{}";

  public static final String  ownerNull = null;
  public static final String ownerEmpty = "";

  // Delivery Methods
  public static final DeliveryTarget dmA1 = new DeliveryTarget(DeliveryMethod.WEBHOOK, webhookUrlA1);
  public static final DeliveryTarget dmB1 = new DeliveryTarget(DeliveryMethod.EMAIL, emailAddressB1);
  public static final List<DeliveryTarget> dmList1 = new ArrayList<>(List.of(dmA1, dmB1));
  public static final DeliveryTarget dmA2 = new DeliveryTarget(DeliveryMethod.WEBHOOK, webhookUrlA2);
  public static final DeliveryTarget dmB2 = new DeliveryTarget(DeliveryMethod.EMAIL, emailAddressB2);
  public static final List<DeliveryTarget> dmList2 = new ArrayList<>(List.of(dmA2, dmB2));

  public static final int ttl1 = 1000;
  public static final int ttl2 = 2000;

  public static final boolean isEnabledTrue = true;
  public static final UUID uuidNull = null;
  public static final Instant expiryNull = null;
  public static final Instant createdNull = null;
  public static final Instant updatedNull = null;

  public static final List<OrderBy> orderByListNull = null;
  public static final List<OrderBy> orderByListAsc = Collections.singletonList(OrderBy.fromString("id(asc)"));
  public static final List<OrderBy> orderByListDesc = Collections.singletonList(OrderBy.fromString("id(desc)"));
  public static final List<OrderBy> orderByList2Asc = new ArrayList<>(List.of(OrderBy.fromString("type_filter(asc)"),
                                                                              OrderBy.fromString("subject_filter(asc)")));
  public static final List<OrderBy> orderByList2Desc = new ArrayList<>(List.of(OrderBy.fromString("type_filter(asc)"),
                                                                       OrderBy.fromString("subject_filter(desc)")));
  public static final List<OrderBy> orderByList3Asc = new ArrayList<>(List.of(OrderBy.fromString("id(asc)"),
                                                                              OrderBy.fromString("owner(asc)")));
  public static final List<OrderBy> orderByList3Desc = new ArrayList<>(List.of(OrderBy.fromString("subject_filter(desc)"),
                                                                               OrderBy.fromString("type_filter(desc)")));
  public static final String startAfterNull = null;

  // Search and sort
  public static final boolean anyOwnerTrue = true;
  public static final boolean anyOwnerFalse = false;
  public static final List<String> searchListNull = null;
  public static final ASTNode searchASTNull = null;
  public static final Set<String> setOfIDsNull = null;
  public static final int limitNone = -1;
  public static final List<String> orderByAttrEmptyList = Arrays.asList("");
  public static final List<String> orderByDirEmptyList = Arrays.asList("");
  public static final int skipZero = 0;
  public static final String startAferEmpty = "";

  // Events
  public static final int bucketNum1 = 1;
  public static final String eventSource1 = "Jobs";
//  static
//  {
//    URI eventSource;
//    try { eventSource = new URI("https://dev.develop.tapis.io/v3/jobs");
//    }
//    catch (URISyntaxException e) { eventSource = null; e.printStackTrace(); }
//    eventSource1 = eventSource;
//  }
  public static final String eventType1 = "jobs.job.complete";
  public static final String eventSubject1 = "640ad5a8-1a6e-4189-a334-c4c7226fb9ba-007";
  public static final String seriesId1 = "111a2228-1a6e-4189-a334-c4c722666666-007";
  public static final String eventDataNull = null;
  public static final String eventTime = TapisUtils.getUTCTimeNow().toString();
  public static final boolean eventDeleteSubscriptionsMatchingSubjectFalse = false;

  public static final Event event1 = new Event(eventSource1, eventType1, eventSubject1, eventDataNull, seriesId1,
                                               eventTime, eventDeleteSubscriptionsMatchingSubjectFalse, tenantName,
                                               testUser1, UUID.randomUUID());

  /**
   * Create an array of Subscription objects in memory
   * Names will be of format TestSub_K_NNN where K is the key and NNN runs from 000 to 999
   * We need a key because maven runs the tests in parallel so each set of items created by an integration
   *   test will need its own namespace.
   * @param n number of items to create
   * @return array of Subscription objects
   */
  public static Subscription[] makeSubscriptions(int n, String key)
  {
    Subscription[] subscriptions = new Subscription[n];

    for (int i = 0; i < n; i++)
    {
      // Suffix which should be unique for each item within each integration test
      String iStr = String.format("%03d", i+1);
      String suffix = key + "_" + iStr;
      String subscrName = getSubscrName(key, i+1);
      // Constructor initializes all attributes
      subscriptions[i] = new Subscription(-1, tenantName, testUser1, subscrName, description1+suffix, isEnabledTrue,
                                          typeFilter1, subjectFilter1, dmList1, ttl1, uuidNull,
                                          expiryNull, createdNull, updatedNull);
    }
    return subscriptions;
  }

  /**
   * Create a PatchSubscription in memory for use in testing.
   * All attributes are to be updated.
   */
  public static PatchSubscription makePatchSubscriptionFull()
  {
    return new PatchSubscription(description2, typeFilter2, subjectFilter2, dmList2, ttl2);
  }

  public static String getSubscrName(String key, int idx)
  {
    String suffix = key + "_" + String.format("%03d", idx);
    return subscrIdPrefix + "_" + suffix;
  }

  /**
   * Create an array of Notification objects in memory
   * We need a key because maven runs the tests in parallel so each set of items created by an integration
   *   test will need its own namespace.
   * @param n number of items to create
   * @return array of Notification objects
   */
  public static List<Notification> makeNotifications(int n, String key, int subscrSeqId, String subscrId)
  {
    List<Notification> notifications = new ArrayList<>();
    UUID eventUuid = event1.getUuid();
    for (int i = 0; i < n; i++)
    {
      String iStr = String.format("%03d", i+1);
      String suffix = key + "_" + iStr;
      String dmAddress = suffix + ".fake.person@example.com";
      DeliveryTarget dm = new DeliveryTarget(DeliveryMethod.EMAIL, dmAddress);
      Notification ntf = new Notification(null, subscrSeqId, tenantName, subscrId, bucketNum1, eventUuid, event1, dm,
                                          createdNull);
      notifications.add(ntf);
    }
    return notifications;
  }
}
