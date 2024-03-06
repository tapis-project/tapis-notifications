package edu.utexas.tacc.tapis.notifications.model;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/*
 * Notification event in the Tapis ecosystem.
 * Subscriptions are used to request that notifications be sent when Events are received.
 * Receipt of an event can result in 0 or more notifications.
 * It is possible for an event be received and then effectively discarded because there are no subscriptions for it.
 *
 * SeriesId:
 *   Each series is intended to sequentially track events of various types coming from a
 *   given tenant, source and subject. So for each tenant, source and subject the seriesId is considered unique.
 *   For example, the Jobs service (the source) sends out events with the jobUuid as the subject and
 *   sets the seriesId to the jobUuid. That way a subscription can be created to follow (in order) all
 *   events of various types related to the job.
 *   Examples of event types defined in the Jobs service: JOB_NEW_STATUS, JOB_ERROR_MESSAGE, JOB_USER_EVENT
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
  public static final boolean DEFAULT_END_SERIES = false;
  public static final long DEFAULT_SERIES_SEQ_COUNT = -1L;

  // Valid pattern for event type, must be 3 sections separated by a '.'
  // First section must contain a series of lower case letters and may not be empty
  // Second and third sections must start alphabetic, contain only alphanumeric
  //   or 3 special characters: - _ ~ and may not be empty
  private static final Pattern EVENT_TYPE_PATTERN =
          Pattern.compile("^[a-z]+\\.[a-zA-Z]([a-zA-Z0-9]|[-_~])*\\.[a-zA-Z]([a-zA-Z0-9]|[-_~])*$");

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private static final String specversion = SPECVERSION;
  private final String source; // Context in which event happened. Required
  private final String type; // Type of event related to originating occurrence. Required
  private final String subject; // Subject of event in context of event producer.
  private final String data; // Data associated with the event.
  private final String seriesId; // Optional Id for grouping events from same source.
  private final long seriesSeqCount; // Sequence counter associated with seriesId for ordering of events from same source.
  private final String timestamp; // Timestamp of when the occurrence happened. RFC 3339 (ISO 8601)
  private final boolean deleteSubscriptionsMatchingSubject;  // Indicates all subscriptions associated with subject
                                                             //   should be removed after deliveries are complete.
  private final boolean endSeries;  // Indicates that this is the last event in a series.
                                    // Tracking data will be removed after deliveries are complete.
  private final String tenant; // Tenant associated with the event
  private final String user; // User associated with the event
  private final UUID uuid;
  private final String received; // Timestamp of when the event was received by the Notifications service.
  // Mark as transient so Gson will not include it.
  private transient String type1; // Field 1 of type (service name)
  private transient String type2; // Field 2 of type (resource type)
  private transient String type3; // Field 3 of type ( action or state)

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public Event(String source1, String type1, String subject1, String data1, String seriesId1,
               long seriesSeqCount1, String timestamp1, boolean deleteSubscriptionsMatchingSubject1,
               boolean endSeries1, String tenant1, String user1, UUID uuid1)
  {
    source = source1;
    type = type1;
    subject = subject1;
    data = data1;
    seriesId = seriesId1;
    seriesSeqCount = seriesSeqCount1;
    timestamp = timestamp1;
    deleteSubscriptionsMatchingSubject = deleteSubscriptionsMatchingSubject1;
    endSeries = endSeries1;
    tenant = tenant1;
    user = user1;
    uuid = uuid1;
    setTypeFields();
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSource() { return source; }
  public String getType() { return type; }
  public String getSubject() { return subject; }
  public String getData() { return data; }
  public String getSeriesId() { return seriesId; }
  public long getSeriesSeqCount() { return seriesSeqCount; }
  public String getTimestamp() { return timestamp; }
  public boolean getDeleteSubscriptionsMatchingSubject() { return deleteSubscriptionsMatchingSubject; }
  public boolean getEndSeries() { return endSeries; }

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
    String msg =
"""
 Tenant: %s Source: %s Type: %s Subject: %s Data: %s SeriesId: %s SeriesSeqCount: %d Timestamp: %s
 deleteSubscriptionsMatchingSubject: %b endSeries: %b user: %s UUID: %s";
""";
    return msg.formatted(tenant, source, type, subject, data, seriesId, seriesSeqCount, timestamp,
                         deleteSubscriptionsMatchingSubject, endSeries, user, uuid);
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
