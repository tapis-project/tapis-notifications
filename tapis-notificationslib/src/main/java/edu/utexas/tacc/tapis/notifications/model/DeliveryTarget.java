package edu.utexas.tacc.tapis.notifications.model;

/*
 * A DeliveryTarget is used by subscribers to indicate how where they want to be notified of events.
 * Each subscription will contain 1 or more such targets.
 * Each target has a type and an address where the address is appropriate for the type:
 *  - EMAIL - email address
 *  - WEBHOOK - webhook url
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class DeliveryTarget
{
  public enum DeliveryMethod {WEBHOOK, EMAIL}

  private final DeliveryMethod deliveryMethod;
  private final String deliveryAddress;

  public DeliveryTarget(DeliveryMethod deliveryMethod1, String deliveryAddress1)
  {
    deliveryMethod = deliveryMethod1;
    deliveryAddress = deliveryAddress1;
  }

//  private void validate() throws ValidationException
//  {
//    EmailValidator emailValidator = EmailValidator.getInstance();
//    UrlValidator urlValidator = UrlValidator.getInstance();
//    if ((deliveryMethod == DeliveryMethodEnum.EMAIL) && (!(emailValidator.isValid(webhookUrl))))
//    {
//      throw new ValidationException("Invalid email");
//    }
//
//    if ((deliveryMethod == DeliveryMethodEnum.WEBHOOK) && (!(urlValidator.isValid(webhookUrl))))
//    {
//      throw new ValidationException("Invalid URL");
//    }
//  }
//
  public DeliveryMethod getDeliveryType() {
    return deliveryMethod;
  }

  public String getDeliveryAddress() { return deliveryAddress; }

  @Override
  public String toString()
  {
    String msg = "DeliveryMethod: %s DeliveryAddress: %s";
    return msg.formatted(deliveryMethod, deliveryAddress);
  }
}
