package edu.utexas.tacc.tapis.notifications.service;

import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable process for cleaning up expired subscriptions.
 *
 */
public final class SubscriptionReaper implements Callable<String>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(SubscriptionReaper.class);

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private NotificationsDao dao;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  SubscriptionReaper(NotificationsDao dao1)
  {
    dao = dao1;
  }
  
  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for thread.start
   */
  @Override
  public String call()
  {
    log.info("**** Starting Subscription Reaper");
    Thread.currentThread().setName("SubscriptionReaper");
    log.info("ThreadId: {} ThreadName: {}", Thread.currentThread().getId(), Thread.currentThread().getName());
    try
    {
      // TODO
      log.info("TODO: For now, reaper only sleeps ...");
      Thread.sleep(3000000);
    }
    catch (InterruptedException e)
    {
      log.info("Subscription Reaper interrupted");
    }
    log.info("**** Stopping Subscription Reaper");
    return "shutdown";
  }


  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

}