package edu.utexas.tacc.tapis.notifications.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

  // Constants indicating uuid or seq_id is not relevant.
  public static final int INVALID_SEQ_ID = -1;
  public static final UUID INVALID_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  // Allowed substitution variables
  public static final String APIUSERID_VAR = "${apiUserId}";

  public static final String PERMISSION_WILDCARD = "*";

  // Default values
  public static final String[] EMPTY_STR_ARRAY = new String[0];
  public static final String DEFAULT_OWNER = APIUSERID_VAR;
  public static final boolean DEFAULT_ENABLED = true;
  private static final String EMPTY_JSON_OBJ = "{}";
  public static final JsonElement DEFAULT_DELIVERY_METHODS = TapisGsonUtils.getGson().fromJson("[]", JsonElement.class);
  public static final int DEFAULT_TTL = 7*24*60; // One week in minutes
  public static final JsonObject DEFAULT_NOTES = TapisGsonUtils.getGson().fromJson("{}", JsonObject.class);

  // Attribute names, also used as field names in Json
  public static final String TENANT_FIELD = "tenant";
  public static final String ID_FIELD = "id";
  public static final String DESCRIPTION_FIELD = "description";
  public static final String OWNER_FIELD = "owner";
  public static final String ENABLED_FIELD = "enabled";
  public static final String TYPE_FILTER_FIELD = "typeFilter";
  public static final String SUBJECT_FILTER_FIELD = "subjectFilter";
  public static final String DELIVERY_METHODS_FIELD = "deliveryMethods";
  public static final String TTL_FIELD = "ttl";
  public static final String NOTES_FIELD = "notes";
  public static final String UUID_FIELD = "uuid";
  public static final String EXPIRY_FIELD = "expiry";
  public static final String CREATED_FIELD = "created";
  public static final String UPDATED_FIELD = "updated";

  // Message keys
  private static final String CREATE_MISSING_ATTR = "NTFLIB_CREATE_MISSING_ATTR";
  private static final String INVALID_STR_ATTR = "NTFLIB_INVALID_STR_ATTR";
  private static final String TOO_LONG_ATTR = "NTFLIB_TOO_LONG_ATTR";

  // Validation patterns
  //ID Must start alphanumeric and contain only alphanumeric and 4 special characters: - . _ ~
  //Note that we allow starting with a number so we can use a UUID.
  public static final String PATTERN_VALID_ID = "^[a-zA-Z0-9]([a-zA-Z0-9]|[-\\._~])*";

  // Validation constants
  private static final Integer MAX_ID_LEN = 80;
  private static final Integer MAX_DESCRIPTION_LEN = 2048;
  private static final Integer MAX_USERNAME_LEN = 60;

  // ************************************************************************
  // *********************** Enums ******************************************
  // ************************************************************************
  public enum SubscriptionOperation {create, read, modify, delete, changeOwner, enable, disable, updateTTL,
                                     getPerms, grantPerms, revokePerms}
  public enum Permission {READ, MODIFY}
  public enum DeliveryType {WEBHOOK, EMAIL}

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private int seqId;
  private String tenant;
  private String id;
  private String description;
  private String owner;
  private boolean enabled;
  private String typeFilter;
  private String subjectFilter;
  private List<DeliveryMethod> deliveryMethods;
  private int ttl;
  private Object notes;   // Simple metadata as json.
  private UUID uuid;
  private Instant expiry;
  private Instant created; // UTC time for when record was created
  private Instant updated; // UTC time for when record was last updated

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor using only required attributes.
   */
  public Subscription(String typeFilter1, List<DeliveryMethod> dmList1)
  {
    typeFilter = typeFilter1;
    deliveryMethods = dmList1;
  }

  /**
   * Constructor using non-updatable attributes.
   * Rather than exposing otherwise unnecessary setters we use a special constructor.
   */
  public Subscription(Subscription s, String tenant1, String id1)
  {
    if (s==null || StringUtils.isBlank(tenant1) || StringUtils.isBlank(id1))
      throw new IllegalArgumentException(LibUtils.getMsg("NTFLIB_NULL_INPUT"));
    seqId = s.getSeqId();
    tenant = tenant1;
    id = id1;
    description = s.getDescription();
    owner = s.getOwner();
    enabled = s.isEnabled();
    typeFilter = s.getTypeFilter();
    subjectFilter = s.getSubjectFilter();
    deliveryMethods = s.getDeliveryMethods();
    ttl = s.getTtl();
    notes = s.getNotes();
    uuid = s.getUuid();
    expiry = s.getExpiry();
    created = s.getCreated();
    updated = s.getUpdated();
  }

  /**
   * Constructor for jOOQ with input parameter matching order of columns in DB
   * Also useful for testing
   */
  public Subscription(int seqId1, String tenant1, String id1, String description1, String owner1, boolean enabled1,
                      String typeFilter1, String subjectFilter1, List<DeliveryMethod> dmList1, int ttl1, Object notes1,
                      UUID uuid1, Instant expiry1, Instant created1, Instant updated1)
  {
    seqId = seqId1;
    tenant = tenant1;
    id = id1;
    description = description1;
    owner = owner1;
    enabled = enabled1;
    typeFilter = typeFilter1;
    subjectFilter = subjectFilter1;
    deliveryMethods = (dmList1 == null) ? null : new ArrayList<>(dmList1);
    ttl = ttl1;
    notes = notes1;
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
    id = s.getId();
    description = s.getDescription();
    owner = s.getOwner();
    enabled = s.isEnabled();
    typeFilter = s.getTypeFilter();
    subjectFilter = s.getSubjectFilter();
    deliveryMethods = s.getDeliveryMethods();
    ttl = s.getTtl();
    notes = s.getNotes();
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
  public void resolveVariables(String apiUserId)
  {
    // Resolve owner if necessary. If empty or "${apiUserId}" then fill in with apiUser.
    // Note that for a user request oboUser and apiUserId are the same and for a service request we want oboUser here.
    if (StringUtils.isBlank(owner) || owner.equalsIgnoreCase(APIUSERID_VAR)) setOwner(apiUserId);
  }

  /**
   * Fill in defaults
   */
  public void setDefaults()
  {
    if (StringUtils.isBlank(owner)) setOwner(DEFAULT_OWNER);
    if (notes == null) setNotes(DEFAULT_NOTES);
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
    checkAttrMisc(errMessages);
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

  // ************************************************************************
  // ******************** Private methods ***********************************
  // ************************************************************************

  /**
   * Check for missing required attributes
   *   Id, typeFilter and deliveryMethods are required
   *   deliveryMethods must have at least one entry
   */
  private void checkAttrRequired(List<String> errMessages)
  {
    if (StringUtils.isBlank(id)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, ID_FIELD));
    if (StringUtils.isBlank(typeFilter)) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, TYPE_FILTER_FIELD));
    if (deliveryMethods == null) errMessages.add(LibUtils.getMsg(CREATE_MISSING_ATTR, DELIVERY_METHODS_FIELD));
    if (deliveryMethods != null && deliveryMethods.isEmpty()) errMessages.add(LibUtils.getMsg("NTFLIB_NO_DM"));
  }

  /**
   * Check for invalid attributes
   *   id
   */
  private void checkAttrValidity(List<String> errMessages)
  {
    // Check that id is not empty and contains a valid pattern
    if (!StringUtils.isBlank(id) && !isValidId(id)) errMessages.add(LibUtils.getMsg(INVALID_STR_ATTR, ID_FIELD, id));
  }

  /**
   * Check for attribute strings that exceed limits
   *   id, description, owner
   */
  private void checkAttrStringLengths(List<String> errMessages)
  {
    if (!StringUtils.isBlank(id) && id.length() > MAX_ID_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, ID_FIELD, MAX_ID_LEN));
    }

    if (!StringUtils.isBlank(description) && description.length() > MAX_DESCRIPTION_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, DESCRIPTION_FIELD, MAX_DESCRIPTION_LEN));
    }

    if (!StringUtils.isBlank(owner) && owner.length() > MAX_USERNAME_LEN)
    {
      errMessages.add(LibUtils.getMsg(TOO_LONG_ATTR, OWNER_FIELD, MAX_USERNAME_LEN));
    }
  }

  /**
   * Check misc attribute restrictions
   *   TODO/TBD
   */
  private void checkAttrMisc(List<String> errMessages)
  {
//    // If containerized is true then containerImage must be set
//    if (containerized && StringUtils.isBlank(containerImage))
//    {
//      errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_NOIMAGE"));
//    }
//
//    // If containerized and SINGULARITY then RuntimeOptions must be provided and include one and only one of
//    //   SINGULARITY_START, SINGULARITY_RUN
//    if (containerized && Runtime.SINGULARITY.equals(runtime))
//    {
//      // If options list contains both or neither of START and RUN then reject.
//      if ( runtimeOptions == null ||
//              (runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) && runtimeOptions.contains(RuntimeOption.SINGULARITY_START))
//              ||
//              !(runtimeOptions.contains(RuntimeOption.SINGULARITY_RUN) || runtimeOptions.contains(RuntimeOption.SINGULARITY_START)))
//      {
//        errMessages.add(LibUtils.getMsg("APPLIB_CONTAINERIZED_SING_OPT", SING_OPT_LIST));
//      }
//    }
//
//    // If dynamicExecSystem then execSystemConstraints must be given
//    if (dynamicExecSystem)
//    {
//      if (execSystemConstraints == null || execSystemConstraints.length == 0)
//      {
//        errMessages.add(LibUtils.getMsg("APPLIB_DYNAMIC_NOCONSTRAINTS"));
//      }
//    }
//
//    // If archiveSystem given then archive dir must be given
//    if (!StringUtils.isBlank(archiveSystemId) && StringUtils.isBlank(archiveSystemDir))
//    {
//      errMessages.add(LibUtils.getMsg("APPLIB_ARCHIVE_NODIR"));
//    }
  }

  /**
   * Validate an ID string.
   * Must start alphabetic and contain only alphanumeric and 4 special characters: - . _ ~
   */
  private boolean isValidId(String id) { return id.matches(PATTERN_VALID_ID); }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************

  public int getSeqId() { return seqId; }

  public String getTenant() { return tenant; }

  public String getId() { return id; }

  public String getDescription() { return description; }
  public void setDescription(String d) { description = d;  }

  public String getOwner() { return owner; }
  public void setOwner(String s) { owner = s;  }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean b) { enabled = b;   }

  public String getTypeFilter() { return typeFilter; }
  public void setTypeFilter(String s) { typeFilter = s;  }

  public String getSubjectFilter() { return subjectFilter; }
  public void setSubjectFilter(String s) { subjectFilter = s;  }

  public List<DeliveryMethod> getDeliveryMethods()
  {
    return (deliveryMethods == null) ? null : new ArrayList<>(deliveryMethods);
  }
  public void setDeliveryMethods(List<DeliveryMethod> dmList1)
  {
    deliveryMethods = (dmList1 == null) ? null : new ArrayList<>(dmList1);
  }

  public int getTtl() { return ttl; }
  public void setTtl(int i) { ttl = i;  }

  public Object getNotes() { return notes; }
  public void setNotes(Object n) { notes = n;  }

  public UUID getUuid() { return uuid; }
  public void setUuid(UUID u) { uuid = u;  }

  @Schema(type = "string")
  public Instant getExpiry() { return expiry; }

  @Schema(type = "string")
  public Instant getCreated() { return created; }

  @Schema(type = "string")
  public Instant getUpdated() { return updated; }
}
