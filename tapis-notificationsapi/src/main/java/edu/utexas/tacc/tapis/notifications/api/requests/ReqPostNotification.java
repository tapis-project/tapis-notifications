package edu.utexas.tacc.tapis.notifications.api.requests;

import edu.utexas.tacc.tapis.notifications.model.DeliveryTarget;

/*
 * Class representing all attributes that can be set in an incoming POST notification request json body
 */
public final class ReqPostNotification
{
  public String uuid;
  public String tenant;
  public String subscriptionName;
  public DeliveryTarget deliveryTarget;
  public ReqPostEvent event;
  public String created;
}
