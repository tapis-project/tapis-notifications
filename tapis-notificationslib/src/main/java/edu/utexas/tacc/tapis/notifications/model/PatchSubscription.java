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
  private final String typeFilter;
  private final String subjectFilter;
  private final List<DeliveryTarget> deliveryTargets;
  private final Integer ttlMinutes;

  // ************************************************************************
  // *********************** Constructors ***********************************
  // ************************************************************************

  /**
   * Constructor setting all attributes.
   */
  public PatchSubscription(String description1, String typeFilter1, String subjectFilter1,
                           List<DeliveryTarget> dMList1, Integer ttl1)
  {
    description = description1;
    typeFilter = typeFilter1;
    subjectFilter = subjectFilter1;
    ttlMinutes = ttl1;
    deliveryTargets = (dMList1 == null) ? null : new ArrayList<>(dMList1);
  }

  // ************************************************************************
  // *********************** Accessors **************************************
  // ************************************************************************
  public String getDescription() { return description; }
  public String getTypeFilter() { return typeFilter; }
  public String getSubjectFilter() { return subjectFilter; }
  public List<DeliveryTarget> getDeliveryMethods()
  {
    return (deliveryTargets == null) ? null : new ArrayList<>(deliveryTargets);
  }
  public Integer getTtlMinutes() { return ttlMinutes; }
}
