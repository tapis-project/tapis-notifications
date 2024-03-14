package edu.utexas.tacc.tapis.notifications.api.requests;

import static edu.utexas.tacc.tapis.notifications.model.Event.DEFAULT_DELETE_SUBSCRIPTIONS_MATCHING_SUBJECT;
import static edu.utexas.tacc.tapis.notifications.model.Event.DEFAULT_END_SERIES;
import static edu.utexas.tacc.tapis.notifications.model.Event.DEFAULT_SERIES_SEQ_COUNT;
/*
 * Class representing all attributes that can be set in an incoming POST event request json body
 * The seriesSeqCount and uuid fields are only used when the event is contained in an incoming Notification
 */
public final class ReqPostEvent
{
  public String source;
  public String type;
  public String subject;
  public String data;
  public String seriesId;
  public long seriesSeqCount = DEFAULT_SERIES_SEQ_COUNT;;
  public String timestamp;
  public boolean deleteSubscriptionsMatchingSubject = DEFAULT_DELETE_SUBSCRIPTIONS_MATCHING_SUBJECT;
  public boolean endSeries = DEFAULT_END_SERIES;
  public String tenant;
  public String user;
  public String received;
  public String uuid;
}
