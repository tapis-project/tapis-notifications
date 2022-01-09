package edu.utexas.tacc.tapis.notifications.api.requests;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;

import java.util.List;

import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_ENABLED;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_NOTES;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_OWNER;

/*
 * Class representing all attributes that can be set in an incoming POST event request json body
 */
public final class ReqPostEvent
{
  public String source;
  public String type;
  public String subject;
  public String time;
}
