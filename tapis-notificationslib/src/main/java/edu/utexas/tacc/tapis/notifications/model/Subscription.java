package edu.utexas.tacc.tapis.notifications.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import com.google.gson.JsonElement;

import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

/*
 * Tapis Subscription
 * Each subscription is associated with a specific tenant.
 * Id of the subscription must be URI safe, see RFC 3986.
 *   Allowed characters: Alphanumeric  [0-9a-zA-Z] and special characters [-._~].
 * Each subscription has an owner and flag indicating if it is currently enabled.
 *
 * Tenant + id must be unique
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class Subscription
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************

  // Filter wildcard (SUBJECT field or one of the TYPE fields)
  public static final String FILTER_WILDCARD = "*";
  public static final String FILTER_WILDCARD_NAME = "ALL";

  // Maximum number of attempts allowed when attempting to create a unique name
  public static final int MAX_GEN_NAME_TRIES = 100;

  // Allowed substitution variables
  public static final String APIUSERID_VAR = "${apiUserId}";

  // Default values
  public static final String DEFAULT_OWNER = APIUSERID_VAR;
  public static final boolean DEFAULT_ENABLED = true;
  public static final JsonElement DEFAULT_DELIVERY_TARGETS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final int DEFAULT_TTL = 7*24*60; // One week in minutes

  // Attribute names, also used as field names in Json
  public static final String TENANT_FIELD = "tenant";
  public static final String NAME_FIELD = "name";
  public static final String DESCRIPTION_FIELD = "description";
  public static final String OWNER_FIELD = "owner";
  public static final String ENABLED_FIELD = "enabled";
  public static final String TYPE_FILTER_FIELD = "typeFilter";
  public static final String SUBJECT_FILTER_FIELD = "subjectFilter";
  public static final String DELIVERY_TARGETS_FIELD = "deliveryTargets";
  public static final String TTL_FIELD = "ttlMinutes";
  public static final String UUID_FIELD = "uuid";
  public static final String EXPIRY_FIELD = "expiry";
  public static final String CREATED_FIELD = "created";
  public static final String UPDATED_FIELD = "updated";

  // Validation patterns
  //NAME Must start alphanumeric and contain only alphanumeric and 4 special characters: - . _ ~
  //Note that we allow starting with a number, so we can use a UUID.
  public static final String NAME_PATTERN = "^[a-zA-Z0-9]([a-zA-Z0-9]|[-\\._~])*";

  // typeFilter must be 3 sections separated by a '.'
  // First section must contain a series of lower case letters and may not be empty
  // Second section must start alphabetic, contain only alphanumeric or 3 special characters: - _ ~ and may not be empty
  //    OR be single wildcard character '*'
  // Third section must start alphabetic, contain only alphanumeric or 3 special characters: - _ ~ and may not be empty
  //    OR be single wildcard character '*'
  private static final Pattern SUBSCR_TYPE_FILTER_PATTERN =
          Pattern.compile("^[a-z]+\\.([a-zA-Z]([a-zA-Z0-9]|[-_~])*|\\*)\\.([a-zA-Z]([a-zA-Z0-9]|[-_~])*|\\*)$");

  // Message keys
  private static final String CREATE_MISSING_ATTR = "NTFLIB_CREATE_MISSING_ATTR";
  private static final String INVALID_STR_ATTR = "NTFLIB_INVALID_STR_ATTR";
  private static final String TOO_LONG_ATTR = "NTFLIB_TOO_LONG_ATTR";

  // Validation constants
  private static final Integer MAX_NAME_LEN = 512;
  private static final Integer MAX_DESCRIPTION_LEN = 2048;
  private static final Integer MAX_USERNAME_LEN = 60;

  // Max length of subjectFilter when generating a unique name
  private static final int SUBJ_FILTER_IN_NAME_MAX = 40;

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  public enum SubscriptionOperation {create, read, modify, delete, changeOwner, enable, disable, updateTTL,
                                     getPerms, grantPerms, revokePerms}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final int seqId;
  private final String tenant;
  private String owner;
  private final String name;
  private String description;
  private final boolean enabled;
  private String typeFilter;
  private String typeFilter1;
  private String typeFilter2;
  private String typeFilter3;
  private String subjectFilter;
  private List<DeliveryTarget> deliveryTargets;
  private final int ttlMinutes;
  private final UUID uuid;
  private final Instant expiry;
  private final Instant created; // UTC time for when record was created
  private final Instant updated; // UTC time for when record was last updated

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor for when the name has been or is to be filled in by the service.
   * Rather than exposing otherwise unnecessary setters we use a special constructor.
   */
  public Subscription(Subscription s, String tenant1, String owner1, String name1)
  {
    if (s==null || StringUtils.isBlank(tenant1) || StringUtils.isBlank(owner1))
      throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT"));
    seqId = s.getSeqId();
    tenant = tenant1;
    owner = owner1;
    name = name1;
    description = s.getDescription();
    enabled = s.isEnabled();
    setTypeFilter(s.getTypeFilter());
    subjectFilter = s.getSubjectFilter();
    deliveryTargets = s.getDeliveryTargets();
    ttlMinutes = s.getTtlMinutes();
    uuid = s.getUuid();
    expiry = s.getExpiry();
    created = s.getCreated();
    updated = s.getUpdated();
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public Subscription(int seqId1, String tenant1, String owner1, String name1, String description1, boolean enabled1,
                      String tf, String subjectFilter1, List<DeliveryTarget> dmList1, int ttl1, UUID uuid1,
                      Instant expiry1, Instant created1, Instant updated1)
  {
    seqId = seqId1;
    tenant = tenant1;
    owner = owner1;
    name = name1;
    description = description1;
    enabled = enabled1;
    setTypeFilter(tf);
    subjectFilter = (subjectFilter1 == null) ? "*" : subjectFilter1;
    deliveryTargets = (dmList1 == null) ? null : new ArrayList<>(dmList1);
    ttlMinutes = ttl1;
    uuid = uuid1;
    expiry = expiry1;
    created = created1;
    updated = updated1;
  }

  /**
   * Copy constructor. Returns a deep copy.
   * The getters make defensive copies as needed.
   */
  public Subscription(Subscription s)
  {
    if (s==null) throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT"));
    seqId = s.getSeqId();
    tenant = s.getTenant();
    owner = s.getOwner();
    name = s.getName();
    description = s.getDescription();
    enabled = s.isEnabled();
    setTypeFilter(s.getTypeFilter());
    subjectFilter = s.getSubjectFilter();
    deliveryTargets = s.getDeliveryTargets();
    ttlMinutes = s.getTtlMinutes();
    uuid = s.getUuid();
    expiry = s.getExpiry();
    created = s.getCreated();
    updated = s.getUpdated();
  }

  // ************************************************************************
  // *********************** Public methods *********************************
  // ************************************************************************

  /**
   * Resolve variables for attributes
   */
  public void resolveVariables(String oboUser)
  {
    // Resolve owner if necessary. If empty or "${apiUserId}" then fill in with oboUser.
    // Note that for a user request oboUser and jwtUser are the same and for a service request we want oboUser here.
    if (StringUtils.isBlank(owner) || owner.equalsIgnoreCase(APIUSERID_VAR)) owner = oboUser;
  }

  /**
   *  Check constraints on attributes.
   *  Make checks that do not require a dao or service call.
   *  Check only internal consistency and restrictions.
   *
   * @return  list of error messages, empty list if no errors
   */
  public List<String> checkAttributeRestrictions()
  {
    var errMessages = new ArrayList<String>();
    checkAttrRequired(errMessages);
    checkAttrValidity(errMessages);
    checkAttrStringLengths(errMessages);
    return errMessages;
  }

  /*
   *  Compute a new expiry based on the current time and the new TTL
   *  TTL is the number of minutes
   *  A value of <= 0 indicates no expiration, null is returned
   */
  public static Instant computeExpiryFromNow(int newTTL)
  {
    // A ttl of 0 or less indicates no expiration, return null.
    if (newTTL <= 0) return null;

    // Compute expiry as epoch time in seconds.
    long epochSecond = Instant.now().getEpochSecond() + 60L * newTTL;
    // Convert epoch time to LocalDateTime
    return Instant.ofEpochSecond(epochSecond);
  }

  /*
   * Build a descriptive unique name for a subscription
   * Template is:
   *   <jwtUser>~<owner>~<tenant>~<subjectFilter>~<random4>
   * Wildcard subjectFilter is replaced with the string ALL
   * subjectFilter is truncated to 40 characters
   */
  public String buildUniqueName(ResourceRequestUser rUser)
  {
    // Process subjectFilter as needed - replace wildcard, truncate
    String sFilt = subjectFilter;
    if (FILTER_WILDCARD.equals(sFilt)) sFilt = FILTER_WILDCARD_NAME;
    if (sFilt.length() > SUBJ_FILTER_IN_NAME_MAX) sFilt = sFilt.substring(0, SUBJ_FILTER_IN_NAME_MAX);
    String random4 = Random4Generator.generateString();
    return String.format("%s~%s~%s~%s~%s", rUser.getJwtUserId(), owner, rUser.getOboTenantId(), sFilt, random4);
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public int getSeqId() { return seqId; }

  public String getTenant() { return tenant; }

  public String getOwner() { return owner; }
  public void setOwner(String s) { owner = s; }

  public String getName() { return name; }

  public String getDescription() { return description; }
  public void setDescription(String d) { description = d; }

  public boolean isEnabled() { return enabled; }

  public String getTypeFilter() { return typeFilter; }
  public void setTypeFilter(String s)
  {
    typeFilter = s;
    // Split the typeFilter into 3 separate fields and set the object properties.
    String[] tmpArr = s.split("\\.");
    typeFilter1 = tmpArr[0];
    typeFilter2 = tmpArr[1];
    typeFilter3 = tmpArr[2];
  }

  public String getTypeFilter1() { return typeFilter1; }
  public String getTypeFilter2() { return typeFilter2; }
  public String getTypeFilter3() { return typeFilter3; }

  public String getSubjectFilter() { return subjectFilter; }
  public void setSubjectFilter(String s) { subjectFilter = s; }

  public List<DeliveryTarget> getDeliveryTargets()
  {
    return (deliveryTargets == null) ? null : new ArrayList<>(deliveryTargets);
  }
  public void setDeliveryTargets(List<DeliveryTarget> dmList1)
  {
    deliveryTargets = (dmList1 == null) ? null : new ArrayList<>(dmList1);
  }

  public int getTtlMinutes() { return ttlMinutes; }

  public UUID getUuid() { return uuid; }

  @Schema(type = "string")
  public Instant getExpiry() { return expiry; }

  @Schema(type = "string")
  public Instant getCreated() { return created; }

  @Schema(type = "string")
  public Instant getUpdated() { return updated; }

  /*
   * Check if string is a valid Subscription type filter
   */
  public static boolean isValidTypeFilter(String tf1)
  {
    if (StringUtils.isBlank(tf1)) return false;
    return SUBSCR_TYPE_FILTER_PATTERN.matcher(tf1).matches();
  }

  // ************************************************************************
  // ******************** Private methods ***********************************
  // ************************************************************************

  /**
   * Check for missing required attributes
   *   name, typeFilter, subjectFilter and deliveryTargets are required
   *   deliveryTargets must have at least one entry
   */
  private void checkAttrRequired(List<String> errMessages)
  {
    if (StringUtils.isBlank(owner)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, OWNER_FIELD));
    if (StringUtils.isBlank(typeFilter)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, TYPE_FILTER_FIELD));
    if (StringUtils.isBlank(subjectFilter)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, SUBJECT_FILTER_FIELD));
    if (deliveryTargets == null) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, DELIVERY_TARGETS_FIELD));
    if (deliveryTargets != null && deliveryTargets.isEmpty()) errMessages.add(LibUtils.getMsg("NTFLIB_SUBSCR_NO_DT"));
  }

  /**
   * Check for invalid attributes: name, typeFilter
   */
  private void checkAttrValidity(List<String> errMessages)
  {
    // Check that id is not empty and contains a valid pattern
    if (!StringUtils.isBlank(name) && !isValidName(name)) errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, NAME_FIELD, name));
    // Validate the subscription type filter
    if (!Subscription.isValidTypeFilter(typeFilter)) errMessages.add(LibUtils.getMsg("NTFLIB_SUBSCR_TYPE_ERR", name, typeFilter));
  }

  /**
   * Check for attribute strings that exceed limits
   *   owner, name, description
   */
  private void checkAttrStringLengths(List<String> errMessages)
  {
    if (!StringUtils.isBlank(owner) && owner.length() > MAX_USERNAME_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, OWNER_FIELD, MAX_USERNAME_LEN));
    }

    if (!StringUtils.isBlank(name) && name.length() > MAX_NAME_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, NAME_FIELD, MAX_NAME_LEN));
    }

    if (!StringUtils.isBlank(description) && description.length() > MAX_DESCRIPTION_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, DESCRIPTION_FIELD, MAX_DESCRIPTION_LEN));
    }
  }

  /**
   * Validate a name string.
   * Must start alphabetic and contain only alphanumeric and 4 special characters: - . _ ~
   */
  private boolean isValidName(String name) { return name.matches(NAME_PATTERN); }

  /*
   * Singleton class to create a sequence of 4 random alphanumeric characters
   */
  static final class Random4Generator
  {
    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUM = ALPHA + ALPHA.toLowerCase() + "0123456789";
    private static final Random random = new SecureRandom();
    private static final char[] FULL_SET = ALPHANUM.toCharArray();
    static String generateString()
    {
      char[] buf = new char[4];
      for (int i=0; i < 4; ++i) { buf[i] = FULL_SET[random.nextInt(FULL_SET.length)]; }
      return new String(buf);
    }
  }
}
