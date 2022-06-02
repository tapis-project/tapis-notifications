package edu.utexas.tacc.tapis.notifications.model;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

/*
 * Notification event in the Tapis ecosystem.
 * Subscriptions are used to request that notifications be sent when Events are received.
 * Receipt of an event can result in 0 or more notifications.
 * It is possible for an event be received and then effectively discarded because there are no subscriptions for it.
 *
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

  // Default values
  public static final boolean DEFAULT_DELETE_SUBSCRIPTIONS_MATCHING_SUBJECT = false;

  // Valid pattern for event type, must be 3 sections separated by a '.'
  // First section must contain a series of lower case letters and may not be empty
  // Second and third sections must start alphabetic, contain only alphanumeric or 3 special characters: - _ ~ and may not be empty
  private static final Pattern EVENT_TYPE_PATTERN =
          Pattern.compile("^[a-z]+\\.[a-zA-Z]([a-zA-Z0-9]|[-_~])*\\.[a-zA-Z]([a-zA-Z0-9]|[-_~])*$");

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private static final String specversion = SPECVERSION;
  private final URI source; // Context in which event happened. Required
  private final String type; // Type of event related to originating occurrence. Required
  private final String subject; // Subject of event in context of event producer.
  private final String data; // Data associated with the event.
  private final String seriesId; // Optional Id for grouping events from same source.
  private final String timestamp; // Timestamp of when the occurrence happened. RFC 3339 (ISO 8601)
  private final boolean deleteSubscriptionsMatchingSubject;
  private final String tenant; // Tenant associated with the event
  private final String user; // User associated with the event
  private final UUID uuid;
  // Mark as transient so Gson will not include it.
  private transient String type1; // Field 1 of type (service name)
  private transient String type2; // Field 2 of type (resource type)
  private transient String type3; // Field 3 of type ( action or state)

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public Event(URI source1, String type1, String subject1, String data1, String seriesId1, String timestamp1,
               boolean deleteSubscriptionsMatchingSubject1, String tenant1, String user1, UUID uuid1)
  {
    source = source1;
    type = type1;
    subject = subject1;
    data = data1;
    seriesId = seriesId1;
    timestamp = timestamp1;
    deleteSubscriptionsMatchingSubject = deleteSubscriptionsMatchingSubject1;
    tenant = tenant1;
    user = user1;
    uuid = uuid1;
    setTypeFields();
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public URI getSource() { return source; }
  public String getType() { return type; }
  public String getSubject() { return subject; }
  public String getData() { return data; }
  public String getSeriesId() { return seriesId; }
  public String getTimestamp() { return timestamp; }
  public boolean getDeleteSubscriptionsMatchingSubject() { return deleteSubscriptionsMatchingSubject; }

  public String getTenant() { return tenant; }
  public String getUser() { return user; }
  public UUID getUuid() { return uuid; }
  public String getType1() { return type1; }
  public String getType2() { return type2; }
  public String getType3() { return type3; }
  public String getSpecversion() { return specversion; }

  /*
   * Check if string is a valid Event type
   */
  public static boolean isValidType(String t1)
  {
    if (StringUtils.isBlank(t1)) return false;
    return EVENT_TYPE_PATTERN.matcher(t1).matches();
  }

  @Override
  public String toString()
  {
    String msg = "Source: %s Type: %s Subject: %s Data: %s SeriesId: %s Timestamp: %s UUID: %s";
    return msg.formatted(source, type, subject, data, seriesId, timestamp, uuid);
  }

  /*
   * Split the type into 3 separate fields and set the object properties.
   */
  public void setTypeFields()
  {
    if (!isValidType(type)) return;
    String[] tmpArr = type.split("\\.");
    type1 = tmpArr[0];
    type2 = tmpArr[1];
    type3 = tmpArr[2];
  }

  /* ********************************************************************** */
  /*                      Private methods                                   */
  /* ********************************************************************** */
}
