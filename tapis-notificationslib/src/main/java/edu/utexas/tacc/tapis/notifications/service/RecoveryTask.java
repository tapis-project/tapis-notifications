package edu.utexas.tacc.tapis.notifications.service;

import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;

/*
 * Callable for processing notifications that are in recovery for a specific bucket.
 * Designed to run until interrupted. Wakes up and processes at regular intervals.
 * When the process wakes up it makes a single delivery attempt for each notification in recovery.
 * Once the maximum number of attempts for a notification is reached an error is logged and the
 * notification is removed from the recovery table.
 *
 * Max number of attempts determined by runtime setting TAPIS_NTF_DELIVERY_RCVRY_ATTEMPTS
 * Attempt interval determined by runtime setting TAPIS_NTF_DELIVERY_RCVRY_RETRY_INTERVAL

 */
public final class RecoveryTask implements Callable<String>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(RecoveryTask.class);

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final int bucketNum;
  private final NotificationsDao dao;
  private final int sleepTimeMinutes;
  private final int maxAttempts;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  /*
   * Default constructor
   */
  RecoveryTask(int bucketNum1, NotificationsDao dao1)
  {
    bucketNum = bucketNum1;
    dao = dao1;
    sleepTimeMinutes = RuntimeParameters.getInstance().getNtfDeliveryRecoveryRetryInterval();
    maxAttempts = RuntimeParameters.getInstance().getNtfDeliveryRecoveryMaxAttempts();
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
    Thread.currentThread().setName("Recovery-bucket-"+ bucketNum);
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_START", bucketNum, sleepTimeMinutes,
                             Thread.currentThread().getId(), Thread.currentThread().getName()));

    // From here on we should only shut down on interrupt
    // Use an encompassing try/catch to handle errors and wait for interrupt
    boolean done = false;
    while (!done)
    {
      try
      {
        // Get all notifications in recovery for our bucket
        List<Notification> notifications = dao.getNotificationsInRecovery(bucketNum);
        // Make one pass through the list
        for (Notification ntf : notifications)
        {
          boolean delivered = DeliveryTask.deliverNotification(ntf);
          // If delivered ok we are done. Perform any post-delivery steps
          if (delivered)
          {
            recoveryAttemptSucceeded(ntf);
          }
          else
          {
            recoveryAttemptFailed(ntf);
          }
        }
      }
      catch (Exception e)
      {
        // Main processing loop has thrown an exception that we might be able to recover from, e.g. the DB is down.
        // Most likely this is IOException or TapisException, but catch all exceptions so we can keep going.
        // Pause for a while before resuming operations. If pause interrupted then we are done.
        log.error(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_ERR", bucketNum, e.getMessage()), e);
        done = pauseProcessing();
      }

      // If not done yet then pause for configured interval before checking for more work and trying again
      if (!done) done = pauseProcessing();
    }

    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_STOP", bucketNum, Thread.currentThread().getId(),
                             Thread.currentThread().getName()));
    return "shutdown";
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * Notification has been delivered.
   * Log a msg and remove it from the table.
   */
  private void recoveryAttemptSucceeded(Notification notification) throws TapisException
  {
    log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_SUCCESS", bucketNum, notification.getUuid()));
    dao.deleteNotificationFromRecovery(notification);
  }

  /*
   * Notification recovery attempt failed.
   * Either update the attempt count or remove the notification
   * Log a warning or error.
   */
  private void recoveryAttemptFailed(Notification notification) throws TapisException
  {
    // Get the current attempt number
    int currentAttemptNumber = dao.getNotificationRecoveryAttemptCount(notification) + 1;
    // if we hit the max then log an error and remove the notification
    // else log a warning and bump up the count
    if (currentAttemptNumber >= maxAttempts)
    {
      log.error(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_FAIL_MAX", bucketNum, notification.getUuid(), maxAttempts));
      dao.deleteNotificationFromRecovery(notification);
    }
    else
    {
      log.warn(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_FAIL", bucketNum, notification.getUuid(), currentAttemptNumber));
      dao.setNotificationRecoveryAttemptCount(notification, currentAttemptNumber);
    }
  }

  /**
   * Pause for given number of minutes for a process
   * @return true if interrupted, else false
   */
  private boolean pauseProcessing()
  {
    log.debug(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_PAUSE", bucketNum, sleepTimeMinutes));
    try
    {
      Thread.sleep(sleepTimeMinutes * 60L * 1000L);
    }
    catch (InterruptedException e)
    {
      log.info(LibUtils.getMsg("NTFLIB_DSP_BUCKET_RCVRY_INTRPT", bucketNum));
      return true;
    }
    return false;
  }
}