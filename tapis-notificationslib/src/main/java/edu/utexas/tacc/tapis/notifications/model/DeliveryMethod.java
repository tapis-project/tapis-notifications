package edu.utexas.tacc.tapis.notifications.model;

import edu.utexas.tacc.tapis.notifications.model.Subscription.DeliveryType;

/*
 * A DeliveryMethod is used by subscribers to indicate how they want to be notified of events.
 * Each subscription will contain 1 or more such methods.
 * Each method has a type and an address where the address is appropriate for the type:
 *  - EMAIL - email address
 *  - WEBHOOK - webhook url
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class DeliveryMethod
{
  private final DeliveryType deliveryType;
  private final String deliveryAddress;

  public DeliveryMethod(DeliveryType deliveryType1, String deliveryAddress1)
  {
    deliveryType = deliveryType1;
    deliveryAddress = deliveryAddress1;
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

  public String getDeliveryAddress() { return deliveryAddress; }

  @Override
  public String toString()
  {
    String msg = "DeliveryType: %s%nDeliveryAddress: %s";
    return msg.formatted(deliveryType, deliveryAddress);
  }
}
