package edu.utexas.tacc.tapis.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;

/*
 * Support cleaning up expired subscriptions.
 * Contains a single static cleanup method that is run at fixed intervals using a ScheduledExecutorService.
 */
public final class SubscriptionReaper
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(SubscriptionReaper.class);

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for cleanup
   */
  public static void cleanup(NotificationsDao dao)
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_REAPER_RUN"));
    try
    {
      // Get all subscriptions passed their expiration time
      var expiredSubscriptions = dao.getExpiredSubscriptions();
      if (expiredSubscriptions == null || expiredSubscriptions.isEmpty()) return;
      log.info(LibUtils.getMsg("NTFLIB_DSP_REAPER_COUNT", expiredSubscriptions.size()));
      for (Subscription s : expiredSubscriptions)
      {
        dao.deleteSubscription(s.getTenant(), s.getOwner(), s.getName());
      }
    }
    catch (Exception e)
    {
      log.error(LibUtils.getMsg("NTFLIB_DSP_REAPER_ERR", e.getMessage()), e);
    }
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

}