package edu.utexas.tacc.tapis.notifications.api.requests;

/*
 * Class representing all attributes that can be set in an incoming POST event request json body
 * The uuid field is only used when the event is contained in an incoming Notification
 */
public final class ReqPostEvent
{
  public String source;
  public String type;
  public String subject;
  public String seriesId;
  public String time;
  public String tenant;
  public String user;
  public String uuid;
}
