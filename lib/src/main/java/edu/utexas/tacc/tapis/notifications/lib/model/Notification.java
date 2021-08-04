package edu.utexas.tacc.tapis.notifications.lib.model;


import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class Notification
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  private static final Logger _log = LoggerFactory.getLogger(Notification.class);
  public static final String SPECVERSION = "1.0";

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private static final String specversion = SPECVERSION;
  private final String id; // Unique identifier for event. Required
  private final URI source; // Context in which event happened. Required
  private final String topic; // Type of event related to originating occurrence. Required
  private final String subject; // Subject of event in context of event producer.
  private final OffsetDateTime time; // Timestamp of when the occurrence happened. RFC 3339 (ISO 8601)
  private final String datacontenttype; // Content type of data value. RFC 2046. E.g. application/xml, text/xml, etc.
  private final Object data; // Data associated with the event.
  private final String data_base64; // If data is binary it must be base64 encoded.

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public Notification(String id1, URI source1, String type1, String subject1, String datacontenttype1,
                      OffsetDateTime time1, Object data1, String data_base64_1)
  {
    id = id1;
    source = source1;
    topic = type1;
    subject = subject1;
    datacontenttype = datacontenttype1;
    time = time1;
    data = data1;
    data_base64 = data_base64_1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSpecversion() { return specversion; }
  public String getId() { return id; }
  public URI getSource() { return source; }
  public String getTopic() { return topic; }
  public String getType() { return topic; }
  public String getSubject() { return subject; }
  public OffsetDateTime getTime() { return time; }
  public String getDatacontenttype() { return datacontenttype; }
  public Object getData() { return data; }
  public String getData_base64() { return data_base64; }

  @Override
  public String toString() {return TapisUtils.toString(this);}
}
