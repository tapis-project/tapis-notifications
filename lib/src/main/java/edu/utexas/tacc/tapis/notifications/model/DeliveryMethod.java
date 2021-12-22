package edu.utexas.tacc.tapis.notifications.model;

import edu.utexas.tacc.tapis.notifications.model.Subscription.DeliveryType;

public final class DeliveryMethod
{
  private final DeliveryType deliveryType;
  private final String webhookUrl;
  private final String emailAddress;

  public DeliveryMethod(DeliveryType deliveryType1, String webhookUrl1, String emailAddress1)
  {
    deliveryType = deliveryType1;
    webhookUrl = webhookUrl1;
    emailAddress = emailAddress1;
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

  public String getEmailAddress() { return emailAddress; }
}
