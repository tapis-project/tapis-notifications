package edu.utexas.tacc.tapis.notifications.model;

/*
 * Class representing an Event series.
 *
 * SeriesId:
 *   Each series is intended to sequentially track events of various types coming from a
 *   given tenant, source and subject. So for each tenant, source and subject the seriesId is considered unique.
 *   For example, the Jobs service (the source) sends out events with the jobUuid as the subject and
 *   sets the seriesId to the jobUuid. That way a subscription can be created to follow (in order) all
 *   events of various types related to the job.
 *   Examples of event types defined in the Jobs service: JOB_NEW_STATUS, JOB_ERROR_MESSAGE, JOB_USER_EVENT
 *
 * This class is intended to represent an immutable object.
 * Please keep it immutable.
 *
 */
public final class EventSeries
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */

  // Default values
// TODO  public static final boolean DEFAULT_DELETE_SUBSCRIPTIONS_MATCHING_SUBJECT = false;
//  public static final long DEFAULT_SERIES_SEQ_COUNT = -1L;

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final String source; // Context in which event happened. Required
  private final String subject; // Subject of event in context of event producer.
  private final String seriesId; // Optional Id for grouping events from same source.
  private final long seriesSeqCount; // Sequence counter associated with seriesId for ordering of events from same source.
  private final String tenant; // Tenant associated with the event

  /* ********************************************************************** */
  /*                           Constructors                                 */
  /* ********************************************************************** */
  public EventSeries(String source1, String subject1, String seriesId1, long seriesSeqCount1, String tenant1)
  {
    source = source1;
    subject = subject1;
    seriesId = seriesId1;
    seriesSeqCount = seriesSeqCount1;
    tenant = tenant1;
  }

  /* ********************************************************************** */
  /*                               Accessors                                */
  /* ********************************************************************** */
  public String getSource() { return source; }
  public String getSubject() { return subject; }
  public String getSeriesId() { return seriesId; }
  public long getSeriesSeqCount() { return seriesSeqCount; }
  public String getTenant() { return tenant; }

  @Override
  public String toString()
  {
    String msg = "Tenant: %s Source: %s Subject: %s SeriesId: %s SeriesSeqCount: %d";
    return msg.formatted(tenant, source, subject, seriesId, seriesSeqCount);
  }

  /* ********************************************************************** */
  /*                      Private methods                                   */
  /* ********************************************************************** */
}
