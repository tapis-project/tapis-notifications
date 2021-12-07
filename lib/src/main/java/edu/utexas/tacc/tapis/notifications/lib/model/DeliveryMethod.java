package edu.utexas.tacc.tapis.notifications.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import javax.validation.ValidationException;
import java.time.Instant;
import java.util.UUID;

public class DeliveryMethod
{
  private DeliveryMethodEnum deliveryMethod;
  private String target;
  private int subscriptionId;
  private Instant created;
  private UUID uuid;
  private String tenantId;

  public enum DeliveryMethodEnum { WEBHOOK, EMAIL, QUEUE, ACTOR }

  public DeliveryMethod(){}

  public DeliveryMethod(DeliveryMethodEnum deliveryMethod1, String target1) throws ValidationException
  {
    deliveryMethod = deliveryMethod1;
    target = target1;
    validate();
  }

  private void validate() throws ValidationException
  {
    EmailValidator emailValidator = EmailValidator.getInstance();
    UrlValidator urlValidator = UrlValidator.getInstance();
    if ((deliveryMethod == DeliveryMethodEnum.EMAIL) && (!(emailValidator.isValid(target))))
    {
      throw new ValidationException("Invalid email");
    }

    if ((deliveryMethod == DeliveryMethodEnum.WEBHOOK) && (!(urlValidator.isValid(target))))
    {
      throw new ValidationException("Invalid URL");
    }
  }

  public DeliveryMethodEnum getDeliveryMethod() {
    return deliveryMethod;
  }

  public String getTarget() { return target; }

  @JsonIgnore
  public int getSubscriptionId() { return subscriptionId; }
  public void setSubscriptionId(int i) { subscriptionId = i; }

  public Instant getCreated() { return created; }
  public void setCreated(Instant c) { created = c; }

  public UUID getUuid() { return uuid; }
  public void setUuid(UUID u) { uuid = u; }

  public String getTenantId() { return tenantId; }
  public void setTenantId(String s) { tenantId = s; }
}
