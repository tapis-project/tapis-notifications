package edu.utexas.tacc.tapis.notifications.api.requests;

import java.util.List;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_NOTES;

/*
 * Class representing all attributes that can be set in an incoming PUT request json body
 */
public final class ReqPutSubscription
{
  public String description;
  public String topicFilter;
  public String subjectFilter;
  public List<DeliveryMethod> deliveryMethods;
  public Object notes = DEFAULT_NOTES;
}
