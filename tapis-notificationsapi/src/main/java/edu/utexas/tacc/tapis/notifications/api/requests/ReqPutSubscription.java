package edu.utexas.tacc.tapis.notifications.api.requests;

import java.util.List;

import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;

import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_SUBJECT_FILTER;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_TTL;

/*
 * Class representing all attributes that can be set in an incoming PUT request json body
 */
public final class ReqPutSubscription
{
  public String description;
  public String typeFilter;
  public String subjectFilter = DEFAULT_SUBJECT_FILTER;
  public List<DeliveryTarget> deliveryTargets;
  public int ttl = DEFAULT_TTL;
}
