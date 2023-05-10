package edu.utexas.tacc.tapis.notifications.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.jooq.tools.StringUtils;

import edu.utexas.tacc.tapis.notifications.utils.LibUtils;

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

  // Relatively simple pattern to validate that an email address has the form something@something_no_white_space
  private static final Pattern emailPattern = Pattern.compile("^(.+)@(\\S+)$");

  // URL validator
  private static final String[] URL_SCHEMES = {"http", "https"};
  private static final long URL_OPTIONS = UrlValidator.ALLOW_LOCAL_URLS;
  private static final UrlValidator URL_VALIDATOR = new UrlValidator(URL_SCHEMES, URL_OPTIONS);
  private final DeliveryMethod deliveryMethod;
  private final String deliveryAddress;
  private final String domain;

  public DeliveryTarget(DeliveryMethod deliveryMethod1, String deliveryAddress1) throws IllegalArgumentException
  {
    deliveryMethod = deliveryMethod1;
    deliveryAddress = deliveryAddress1;
    // Do basic validation and extract domain
    domain = validateTargetAndExtractDomain(deliveryMethod1, deliveryAddress1);
  }


  // Perform simple basic validation of deliveryAddress and extract the domain
  public static String validateTargetAndExtractDomain(DeliveryMethod deliveryMethod, String deliveryAddress)
          throws IllegalArgumentException
  {
    String domainStr;
    String msg;
    if (StringUtils.isBlank(deliveryAddress))
    {
      msg = LibUtils.getMsg("NTFLIB_SUBSCR_DLVRY_ADDR_INVALID", deliveryMethod, deliveryAddress);
      throw new IllegalArgumentException(msg);
    }

    if (DeliveryMethod.EMAIL.equals(deliveryMethod))
    {
      // Do basic validation based on a simple regex pattern.
      if (!emailPattern.matcher(deliveryAddress).matches())
      {
        msg = LibUtils.getMsg("NTFLIB_SUBSCR_DLVRY_ADDR_NOT_EMAIL", deliveryMethod, deliveryAddress);
        throw new IllegalArgumentException(msg);
      }
      domainStr = deliveryAddress.substring(deliveryAddress.indexOf("@") + 1);
    }
    else if (DeliveryMethod.WEBHOOK.equals(deliveryMethod))
    {
      // Do basic validation by attempting to construct a URI.
      URI uri;
      try
      {
        uri = new URI(deliveryAddress);
        domainStr = uri.getHost();
      }
      catch (URISyntaxException e)
      {
        msg = LibUtils.getMsg("NTFLIB_SUBSCR_DLVRY_ADDR_NOT_URI", deliveryMethod, deliveryAddress);
        throw new IllegalArgumentException(msg);
      }
      if (!URL_VALIDATOR.isValid(deliveryAddress))
      {
        msg = LibUtils.getMsg("NTFLIB_SUBSCR_DLVRY_ADDR_NOT_URL", deliveryMethod, deliveryAddress);
        throw new IllegalArgumentException(msg);
      }
    }
    else
    {
      // Unknown delivery method
      msg = LibUtils.getMsg("NTFLIB_SUBSCR_DLVRY_METH_INVALID", deliveryMethod, deliveryAddress);
      throw new IllegalArgumentException(msg);
    }
    return domainStr;
  }

  public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
  public String getDeliveryAddress() { return deliveryAddress; }
  public String getDomain() { return domain; }

  @Override
  public String toString()
  {
    String msg = "DeliveryMethod: %s DeliveryAddress: %s";
    return msg.formatted(deliveryMethod, deliveryAddress);
  }
}
