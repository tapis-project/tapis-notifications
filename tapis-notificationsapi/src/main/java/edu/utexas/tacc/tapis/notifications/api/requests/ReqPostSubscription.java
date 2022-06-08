package edu.utexas.tacc.tapis.notifications.api.requests;

import java.util.List;

import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_TTL;

/*
 * Class representing all attributes that can be set in an incoming POST subscription request json body
 */
public final class ReqPostSubscription
{
  public String name;
  public String description;
  public String owner = DEFAULT_OWNER;
  public boolean enabled = DEFAULT_ENABLED;
  public String typeFilter;
  public String subjectFilter;
  public List<DeliveryTarget> deliveryTargets;
  public int ttlMinutes = DEFAULT_TTL;
}
