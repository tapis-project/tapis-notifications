package edu.utexas.tacc.tapis.notifications.model;

import edu.utexas.tacc.tapis.notifications.model.Subscription.DeliveryType;

public final class DeliveryMethod
{
  private DeliveryType deliveryType;
  private String webhookUrl;

  public DeliveryMethod(DeliveryType deliveryType1, String webhookUrl1)
  {
    deliveryType = deliveryType1;
    webhookUrl = webhookUrl1;
  }

//  private void validate() throws ValidationException
//  {
//    EmailValidator emailValidator = EmailValidator.getInstance();
//    UrlValidator urlValidator = UrlValidator.getInstance();
//    if ((deliveryType == DeliveryMethodEnum.EMAIL) && (!(emailValidator.isValid(webhookUrl))))
//    {
//      throw new ValidationException("Invalid email");
//    }
//
//    if ((deliveryType == DeliveryMethodEnum.WEBHOOK) && (!(urlValidator.isValid(webhookUrl))))
//    {
//      throw new ValidationException("Invalid URL");
//    }
//  }
//
  public DeliveryType getDeliveryType() {
    return deliveryType;
  }

  public String getWebhookUrl() { return webhookUrl; }

//  @JsonIgnore
//  public int getSubscriptionId() { return subscriptionId; }
//  public void setSubscriptionId(int i) { subscriptionId = i; }
//
//  public Instant getCreated() { return created; }
//  public void setCreated(Instant c) { created = c; }
//
//  public UUID getUuid() { return uuid; }
//  public void setUuid(UUID u) { uuid = u; }
//
//  public String getTenantId() { return tenantId; }
//  public void setTenantId(String s) { tenantId = s; }
}
