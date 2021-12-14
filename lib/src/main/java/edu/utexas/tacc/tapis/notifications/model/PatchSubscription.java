package edu.utexas.tacc.tapis.notifications.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Class representing a patch to a Tapis subscription.
 * Fields set to null indicate attribute not updated.
 *
 * Make defensive copies as needed on get/set to keep this class as immutable as possible.
 */
public final class PatchSubscription
{
  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  private final String description;
  private final String topicFilter;
  private final String subjectFilter;
  private final List<DeliveryMethod> deliveryMethods;
  private Object notes; // Not final since may require special handling and need to be updated. See AppResource.java

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all attributes.
   */
  public PatchSubscription(String description1, String topicFilter1, String subjectFilter1,
                           List<DeliveryMethod> dMList1, Object notes1)
  {
    description = description1;
    topicFilter = topicFilter1;
    subjectFilter = subjectFilter1;
    deliveryMethods = (dMList1 == null) ? null : new ArrayList<>(dMList1);
    notes = notes1;
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getDescription() { return description; }
  public String getTopicFilter() { return topicFilter; }
  public String getSubjectFilter() { return subjectFilter; }
  public List<DeliveryMethod> getDeliveryMethods()
  {
    return (deliveryMethods == null) ? null : new ArrayList<>(deliveryMethods);
  }
  public Object getNotes() {
    return notes;
  }
  public void setNotes(Object o) { notes = o; }
}
