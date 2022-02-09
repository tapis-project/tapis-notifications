package edu.utexas.tacc.tapis.notifications.api.responses.results;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Subscription;

import static edu.utexas.tacc.tapis.notifications.api.resources.SubscriptionResource.SUMMARY_ATTRS;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.CREATED_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DESCRIPTION_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.ENABLED_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.ID_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.NOTES_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.OWNER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.TENANT_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.UPDATED_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.UUID_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.TYPE_FILTER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.SUBJECT_FILTER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DELIVERY_METHODS_FIELD;

/*
  Classes representing a subscription result to be returned
 */
public final class TapisSubscriptionDTO
{
  private static final Gson gson = TapisGsonUtils.getGson();

  public String tenant;
  public String id;
  public String description;
  public String owner;
  public boolean enabled;
  public String typeFilter;
  public String subjectFilter;
  public List<DeliveryMethod> deliveryMethods;
  public Object notes;
  public UUID uuid;
  public Instant created;
  public Instant updated;

  public TapisSubscriptionDTO(Subscription s)
  {
    tenant = s.getTenant();
    id = s.getId();
    description = s.getDescription();
    owner = s.getOwner();
    enabled = s.isEnabled();
    typeFilter = s.getTypeFilter();
    subjectFilter = s.getSubjectFilter();
    deliveryMethods = s.getDeliveryMethods();
    notes = s.getNotes();
    uuid = s.getUuid();
    created = s.getCreated();
    updated = s.getUpdated();
  }

  /**
   * Create a JsonObject containing the id attribute and any attribute in the selectSet that matches the name
   * of a public field in this class
   * If selectSet is null or empty then all attributes are included.
   * If selectSet contains "allAttributes" then all attributes are included regardless of other items in set
   * If selectSet contains "summaryAttributes" then summary attributes are included regardless of other items in set
   * @return JsonObject containing attributes in the select list.
   */
  public JsonObject getDisplayObject(List<String> selectList)
  {
    // Check for special case of returning all attributes
    if (selectList == null || selectList.isEmpty() || selectList.contains("allAttributes"))
    {
      return allAttrs();
    }

    var retObj = new JsonObject();

    // If summaryAttrs included then add them
    if (selectList.contains("summaryAttributes")) addSummaryAttrs(retObj);

    // Include specified list of attributes
    // If ID not in list we add it anyway.
    if (!selectList.contains(ID_FIELD)) addDisplayField(retObj, ID_FIELD);
    for (String attrName : selectList)
    {
      addDisplayField(retObj, attrName);
    }
    return retObj;
  }

  // Build a JsonObject with all displayable attributes
  private JsonObject allAttrs()
  {
    String jsonStr = gson.toJson(this);
    return gson.fromJson(jsonStr, JsonObject.class).getAsJsonObject();
  }

  // Add summary attributes to a json object
  private void addSummaryAttrs(JsonObject jsonObject)
  {
    for (String attrName: SUMMARY_ATTRS)
    {
      addDisplayField(jsonObject, attrName);
    }
  }

  /**
   * Add specified attribute name to the JsonObject that is to be returned as the displayable object.
   * If attribute does not exist in this class then it is a no-op.
   *
   * @param jsonObject Base JsonObject that will be returned.
   * @param attrName Attribute name to add to the JsonObject
   */
  private void addDisplayField(JsonObject jsonObject, String attrName)
  {
    String jsonStr;
    switch (attrName) {
      case TENANT_FIELD -> jsonObject.addProperty(TENANT_FIELD, tenant);
      case ID_FIELD -> jsonObject.addProperty(ID_FIELD, id);
      case DESCRIPTION_FIELD ->jsonObject.addProperty(DESCRIPTION_FIELD, description);
      case OWNER_FIELD -> jsonObject.addProperty(OWNER_FIELD, owner);
      case ENABLED_FIELD -> jsonObject.addProperty(ENABLED_FIELD, Boolean.toString(enabled));
      case TYPE_FILTER_FIELD -> jsonObject.addProperty(TYPE_FILTER_FIELD, typeFilter);
      case SUBJECT_FILTER_FIELD -> jsonObject.addProperty(SUBJECT_FILTER_FIELD, subjectFilter);
      case DELIVERY_METHODS_FIELD -> {
        jsonStr = gson.toJson(deliveryMethods);
        jsonObject.add(DELIVERY_METHODS_FIELD, gson.fromJson(jsonStr, JsonObject.class));
      }
      case NOTES_FIELD -> {
        jsonStr = gson.toJson(notes);
        jsonObject.add(NOTES_FIELD, gson.fromJson(jsonStr, JsonObject.class));
      }
      case UUID_FIELD -> jsonObject.addProperty(UUID_FIELD, uuid.toString());
      case CREATED_FIELD -> jsonObject.addProperty(CREATED_FIELD, created.toString());
      case UPDATED_FIELD -> jsonObject.addProperty(UPDATED_FIELD, updated.toString());
    }
  }
}