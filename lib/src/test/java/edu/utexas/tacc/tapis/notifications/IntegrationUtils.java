package edu.utexas.tacc.tapis.notifications;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription.DeliveryType;
import edu.utexas.tacc.tapis.search.parser.ASTNode;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import org.jooq.tools.StringUtils;
import org.testng.Assert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

  public static final String owner1 = "owner1";
  public static final String owner2 = "owner2";
  public static final String ownerNull = null;
  public static final String testUser0 = "testuser0";
  public static final String testUser1 = "testuser1";
  public static final String testUser2 = "testuser2";
  public static final String testUser3 = "testuser3";
  public static final String testUser4 = "testuser4";
  public static final String apiUser = "testApiUser";
  public static final String subscrNamePrefix = "TestSub";
  public static final String description1 = "Subscription description 1";
  public static final String description2 = "Subscription description 2";
  public static final String typeFilter1 = "tapis.jobs.job.complete";
  public static final String typeFilter2 = "tapis.systems.system.create";
  public static final String subjectFilter1 = "subject_filter_1";
  public static final String subjectFilter2 = "subject_filter_2";
  public static final String webhookUrlA1 = "https://my.fake.webhook/urlA1";
  public static final String webhookUrlA2 = "https://my.fake.webhook/urlA2";
  public static final String webhookUrlB1 = "https://my.fake.webhook/urlB1";
  public static final String webhookUrlB2 = "https://my.fake.webhook/urlB2";
  public static final String emailAddressA1 = "my.fake.emailA1@my.example.com";
  public static final String emailAddressA2 = "my.fake.emailA2@my.example.com";
  public static final String emailAddressB1 = "my.fake.emailB1@my.example.com";
  public static final String emailAddressB2 = "my.fake.emailB2@my.example.com";
  public static final Object notes1 = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj1\", \"testdata\": \"abc1\"}", JsonObject.class);
  public static final Object notes2 = TapisGsonUtils.getGson().fromJson("{\"project\": \"myproj2\", \"testdata\": \"abc2\"}", JsonObject.class);
  public static final JsonObject notesObj1 = (JsonObject) notes1;
  public static final Object notesNull = null;
  public static final String scrubbedJson = "{}";

  // Delivery Methods
  public static final DeliveryMethod dmA1 = new DeliveryMethod(DeliveryType.WEBHOOK, webhookUrlA1, emailAddressA1);
  public static final DeliveryMethod dmB1 = new DeliveryMethod(DeliveryType.EMAIL, webhookUrlB1, emailAddressB1);
  public static final List<DeliveryMethod> dmList1 = new ArrayList<>(List.of(dmA1, dmB1));
  public static final DeliveryMethod dmA2 = new DeliveryMethod(DeliveryType.WEBHOOK, webhookUrlA2, emailAddressA2);
  public static final DeliveryMethod dmB2 = new DeliveryMethod(DeliveryType.EMAIL, webhookUrlB2, emailAddressB2);
  public static final List<DeliveryMethod> dmList2 = new ArrayList<>(List.of(dmA2, dmB2));

  public static final boolean isEnabledTrue = true;
  public static final UUID uuidNull = null;
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
  public static final List<String> searchListNull = null;
  public static final ASTNode searchASTNull = null;
  public static final Set<String> setOfIDsNull = null;
  public static final int limitNone = -1;
  public static final List<String> orderByAttrEmptyList = Arrays.asList("");
  public static final List<String> orderByDirEmptyList = Arrays.asList("");
  public static final int skipZero = 0;
  public static final String startAferEmpty = "";

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
      String subscrId = getSubscrName(key, i+1);
      // Constructor initializes all attributes
      subscriptions[i] = new Subscription(-1, tenantName, subscrId, description1+suffix, owner1, isEnabledTrue,
                                          typeFilter1, subjectFilter1, dmList1,
                                          notes1, uuidNull, createdNull, updatedNull);
    }
    return subscriptions;
  }

  /**
   * Create a Subscription in memory for use in testing the PUT operation.
   * All updatable attributes are modified.
   */
  public static Subscription makePutSubscriptionFull(Subscription subscription)
  {
    Subscription putSubscr = new Subscription(subscription.getSeqId(), tenantName, subscription.getId(), description2,
                       subscription.getOwner(), subscription.isEnabled(), typeFilter2, subjectFilter2,
                       dmList2, notes2, null, null, null);
    return putSubscr;
  }

  /**
   * Create a PatchSubscription in memory for use in testing.
   * All attributes are to be updated.
   */
  public static PatchSubscription makePatchSubscriptionFull()
  {
    return new PatchSubscription(description2, typeFilter2, subjectFilter2, dmList2, notes2);
  }

  public static String getSubscrName(String key, int idx)
  {
    String suffix = key + "_" + String.format("%03d", idx);
    return subscrNamePrefix + "_" + suffix;
  }
}
