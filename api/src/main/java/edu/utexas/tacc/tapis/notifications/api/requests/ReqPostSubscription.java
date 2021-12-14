package edu.utexas.tacc.tapis.notifications.api.requests;

import java.util.List;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_NOTES;

/*
 * Class representing all attributes that can be set in an incoming POST request json body
 */
public final class ReqPostSubscription
{
  public String id;
  public String description;
  public String owner = DEFAULT_OWNER;
  public boolean enabled = DEFAULT_ENABLED;
  public String topicFilter;
  public String subjectFilter;
  public List<DeliveryMethod> deliveryMethods;
  public Object notes = DEFAULT_NOTES;
}
