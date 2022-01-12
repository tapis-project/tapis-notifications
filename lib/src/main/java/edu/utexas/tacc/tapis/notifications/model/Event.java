package edu.utexas.tacc.tapis.notifications.model;


import edu.utexas.tacc.tapis.shared.utils.TapisUtils;

import java.net.URI;
import java.time.OffsetDateTime;

/*
 * Notification event in the Tapis ecosystem.
 * Based on the CloudEvent specification.
 * Represents an event associated with an occurrence. An Occurrence may produce multiple events.
 * Based on version 1.0 of the CloudEvents specification
 *   For more information about CloudEvents and the specification please see
 *     https://cloudevents.io/
 *     and
 *     https://github.com/cloudevents/spec
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class Event
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  public static final String SPECVERSION = "1.0";

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private static final String specversion = SPECVERSION;
  private final String tenantId;
  private final URI source; // Context in which event happened. Required
  private final String type; // Type of event related to originating occurrence. Required
  private final String subject; // Subject of event in context of event producer.
  private final String time; // Timestamp of when the occurrence happened. RFC 3339 (ISO 8601)
  private final String datacontenttype; // Content type of data value. RFC 2046. E.g. application/xml, text/xml, etc.
  private final Object data; // Data associated with the event.
  private final String data_base64; // If data is binary it must be base64 encoded.

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public Event(String tenantId1, URI source1, String type1, String subject1, String time1)
  {
    tenantId = tenantId1;
    source = source1;
    type = type1;
    subject = subject1;
    time = time1;
    datacontenttype = null;
    data = null;
    data_base64 = null;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSpecversion() { return specversion; }
  public String getTenantId() { return tenantId; }
  public URI getSource() { return source; }
  public String getType() { return type; }
  public String getSubject() { return subject; }
  public String getTime() { return time; }
  public String getDatacontenttype() { return datacontenttype; }
  public Object getData() { return data; }
  public String getData_base64() { return data_base64; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
