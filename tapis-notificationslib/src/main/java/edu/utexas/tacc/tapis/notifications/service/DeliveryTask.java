package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.ws.rs.core.Response.Status;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.providers.email.EmailClient;
import edu.utexas.tacc.tapis.shared.providers.email.EmailClientFactory;
import edu.utexas.tacc.tapis.shared.utils.HTMLizer;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.notifications.config.RuntimeParameters;
import edu.utexas.tacc.tapis.notifications.utils.LibUtils;
import edu.utexas.tacc.tapis.notifications.dao.NotificationsDao;
import edu.utexas.tacc.tapis.notifications.model.DeliveryMethod;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.Notification;

/*
 * Callable for sending out a single notification generated by a bucket manager.
 */
public final class DeliveryTask implements Callable<Notification>
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // Tracing.
  private static final Logger log = LoggerFactory.getLogger(DeliveryTask.class);

  /* ********************************************************************** */
  /*                                Enums                                   */
  /* ********************************************************************** */

  /* ********************************************************************** */
  /*                                 Fields                                 */
  /* ********************************************************************** */

  private final NotificationsDao dao;

  private final String tenant;
  private final Notification notification; // The notification to be processed
  private final UUID uuid;
  private final int bucketNum; // Bucket that generated the notification
  private final DeliveryMethod deliveryMethod;
  private final String notifJsonStr;

  /* ********************************************************************** */
  /*                             Constructors                               */
  /* ********************************************************************** */

  DeliveryTask(NotificationsDao dao1, String tenant1, Notification n1)
  {
    dao = dao1;
    tenant = tenant1;
    notification = n1;
    bucketNum = n1.getBucketNum();
    deliveryMethod = n1.getDeliveryMethod();
    uuid = n1.getUuid();
    // Body is the notification as json
    notifJsonStr = TapisGsonUtils.getGson(true).toJson(notification);
  }

  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for thread.start
   * Make multiple attempts to deliver the notification. Pause after each attempt.
   * Number of attempts and interval are based on runtime settings.
   */
  @Override
  public Notification call() throws TapisException
  {
    Thread.currentThread().setName("ThreadDelivery-bucket-" + bucketNum + "-method-" + deliveryMethod);
    log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY_START", bucketNum, Thread.currentThread().getId(), Thread.currentThread().getName()));

    // Get number of attempts and attempt interval from settings.
    int numAttempts = RuntimeParameters.getInstance().getNtfDeliveryAttempts();
    int deliveryAttemptInterval = RuntimeParameters.getInstance().getNtfDeliveryRetryInterval() * 1000;
    for (int i = 1; i < numAttempts; i++)
    {
      log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY_ATTEMPT", bucketNum, uuid, i, deliveryMethod));
      try
      {
        if (deliverNotification()) return notificationDelivered();
        log.warn(LibUtils.getMsg("NTFLIB_DSP_DLVRY_FAIL1", bucketNum, uuid, i, deliveryMethod));
      }
      catch (Exception e)
      {
        log.warn(LibUtils.getMsg("NTFLIB_DSP_DLVRY_FAIL2", bucketNum, uuid, i, deliveryMethod, e.getMessage()), e);
      }
      // Pause for configured interval before trying again
      try {log.debug("Sleep 15 seconds"); Thread.sleep(deliveryAttemptInterval); } catch (InterruptedException e) {}
    }

    // We have failed to deliver. Add to recovery table
    addNotificationToRecovery();
    return null;
  }

  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */

  public int getBucketNum()
  {
    return bucketNum;
  }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * Send out the notification
   */
  private boolean deliverNotification()
  {
    Event event = notification.getEvent();
    log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY", bucketNum, notification.getUuid(), deliveryMethod.getDeliveryType(),
            deliveryMethod.getDeliveryAddress(), event.getSource(), event.getType(),
            event.getSubject(), event.getSeriesId(), event.getTime(), event.getUuid()));
    boolean deliveryStatus = false;
    try
    {
      switch (deliveryMethod.getDeliveryType())
      {
        case WEBHOOK -> deliveryStatus = deliverByWebhook();
        case EMAIL -> deliveryStatus = deliverByEmail();
      }
    } catch (IOException | URISyntaxException | InterruptedException e)
    {
      // TODO
      log.warn("Caught exception during notification delivery: " + e.getMessage(), e);
    }
    return deliveryStatus;
  }

  /*
   * Send out the notification via Webhook
   * TODO: Handle exceptions
   *
   * TODO:
   *   - Cache the client
   *   - set client timeout
   *   - special handling for https?
   *   - handle auth if provided?
   *   - other headers?
   *
   */
  private boolean deliverByWebhook() throws URISyntaxException, IOException, InterruptedException
  {
    boolean delivered = true;

    //    //??????????????????
//    // test GET - works
//    URI uri2 = new URI("http://localhost:8080/v3/notifications/healthcheck");
//    // TODO/TBD: timeout is 10 seconds
//    Duration timeout2 = Duration.of(10, ChronoUnit.SECONDS);
//    HttpRequest request2 = HttpRequest.newBuilder().uri(uri2)
//            .header("Accept", "application/json")
//            .header("Content-Type", "application/json")
//            .timeout(timeout2).build();
//    HttpResponse<String> response2 = httpClient.send(request2, HttpResponse.BodyHandlers.ofString());
//    // ?????????????????

    // TODO/TBD: Get works but POST hangs? maybe grizzly issue?
//    // Post to the delivery address which should be a URL
//    URI uri = new URI(deliveryMethod.getDeliveryAddress());
//    BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(eventJsonStr);
//    // TODO/TBD: timeout is 10 seconds
//    Duration timeout = Duration.of(30, ChronoUnit.SECONDS);
//    HttpRequest request = HttpRequest.newBuilder().uri(uri)
//            .header("Accept", "application/json")
//            .header("Content-Type", "application/json")
////            .method("POST", bodyPublisher).timeout(timeout).build();
////            .POST(bodyPublisher).timeout(timeout).build();
//            .POST(HttpRequest.BodyPublishers.noBody()).timeout(timeout).build();
//    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//    if (response.statusCode() == Status.OK.getStatusCode()) return true;

    OkHttpClient client = new OkHttpClient();
    RequestBody body = RequestBody.create(notifJsonStr, MediaType.parse("application/json"));
    Request.Builder requestBuilder = new Request.Builder().url(deliveryMethod.getDeliveryAddress()).post(body);
    Request request = requestBuilder.addHeader("User-Agent", "Tapis/%s".formatted(TapisConstants.API_VERSION)).build();
    Call call = client.newCall(request);
    Response response = call.execute();
    // If response status code is not 200 assume delivery failed.
    if (response.code() != Status.OK.getStatusCode())
    {
      log.error(LibUtils.getMsg("NTFLIB_DSP_DLVRY_WH_FAIL_ERR", bucketNum, notification.getUuid(),
              deliveryMethod.getDeliveryType(), deliveryMethod.getDeliveryAddress(), response.code()));
      delivered = false;
    }
    return delivered;
  }

  /*
   * Send out the notification via email
   * TODO: Handle exceptions
   */
  private boolean deliverByEmail() throws IOException, InterruptedException
  {
    boolean delivered = true;
    String eventType = notification.getEvent().getType();
    String eventSubj = notification.getEvent().getSubject();
    String mailSubj;
    if (StringUtils.isBlank(eventSubj))
      mailSubj = LibUtils.getMsg("NTFLIB_DSP_DLVRY_MAIL_SUBJ1", TapisConstants.API_VERSION, eventType);
    else
      mailSubj = LibUtils.getMsg("NTFLIB_DSP_DLVRY_MAIL_SUBJ2", TapisConstants.API_VERSION, eventType, eventSubj);
    String mailBody = notifJsonStr;
    String sendToAddress = deliveryMethod.getDeliveryAddress();
    String sendToName = deliveryMethod.getDeliveryAddress();
    RuntimeParameters runtime = RuntimeParameters.getInstance();
    try
    {
      EmailClient client = EmailClientFactory.getClient(runtime);
      client.send(sendToName, sendToAddress, mailSubj, mailBody, HTMLizer.htmlize(mailBody));
    } catch (TapisException e)
    {
      log.error(LibUtils.getMsg("NTFLIB_DSP_DLVRY_EM_FAIL_ERR", bucketNum, notification.getUuid(),
                            deliveryMethod.getDeliveryType(), deliveryMethod.getDeliveryAddress(), e.getMessage(), e));
      delivered = false;
    }
    return delivered;
  }

  /*
   * Notification has been delivered. Remove it from the table.
   */
  private Notification notificationDelivered()
  {
    try { dao.deleteNotification(tenant, notification); }
    catch (TapisException e)
    {
      String msg = LibUtils.getMsg("NTFLIB_DSP_DLVRY_DEL_ERR", bucketNum, notification.getUuid(),
                             deliveryMethod.getDeliveryType(), deliveryMethod.getDeliveryAddress(), e.getMessage(), e);
      log.error(msg);
      return null;
    }
    return notification;
  }

  /*
   * Initial Notification delivery failed. Add it to the recovery table.the table.
   */
  private Notification addNotificationToRecovery()
  {
    try { dao.deleteNotification(tenant, notification); }
    catch (TapisException e)
    {
      String msg = LibUtils.getMsg("NTFLIB_DSP_DLVRY_DEL_ERR", bucketNum, notification.getUuid(),
              deliveryMethod.getDeliveryType(), deliveryMethod.getDeliveryAddress(), e.getMessage(), e);
      log.error(msg);
      return null;
    }
    return notification;
  }
}