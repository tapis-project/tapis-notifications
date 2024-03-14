package edu.utexas.tacc.tapis.notifications.api.requests;

import static edu.utexas.tacc.tapis.notifications.model.Event.*;

/*
 * Class representing all attributes that can be set in an incoming POST event endSeries request json body
 */
public final class ReqEndEventSeries
{
  public String source;
  public String subject;
  public String seriesId;
}

