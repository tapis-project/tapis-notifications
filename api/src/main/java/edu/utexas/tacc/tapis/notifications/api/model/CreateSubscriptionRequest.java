package edu.utexas.tacc.tapis.notifications.api.model;

import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;

import javax.validation.constraints.NotEmpty;
import java.util.List;

public class CreateSubscriptionRequest
{
  private String filter;

  @NotEmpty
  private List<DeliveryMethod> deliveryMethods;

  @NotEmpty
  public String getFilter() {
    return filter;
  }

  public void setFilter(String s) {
    filter = s;
  }

  public List<DeliveryMethod> getDeliveryMethods() {
    return deliveryMethods;
  }

  public void setDeliveryMethods(List<DeliveryMethod> deliveryMethods1) { deliveryMethods = deliveryMethods1; }
}
