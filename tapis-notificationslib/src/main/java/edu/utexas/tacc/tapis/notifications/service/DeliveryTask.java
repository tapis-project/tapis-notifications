package edu.utexas.tacc.tapis.notifications.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import javax.ws.rs.core.Response.Status;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
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

  private NotificationsDao dao;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private final String tenant;
  private final Notification notification; // The notification to be processed
  private final int bucketNum; // Bucket that generated the notification
  private final DeliveryMethod deliveryMethod;

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
  }
  
  /* ********************************************************************** */
  /*                             Public Methods                             */
  /* ********************************************************************** */

  /*
   * Main method for thread.start
   */
  @Override
  public Notification call() throws TapisException
  {
    Thread.currentThread().setName("ThreadDelivery-bucket-"+ bucketNum + "-method-" + deliveryMethod);
    log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY_START", bucketNum, Thread.currentThread().getId(), Thread.currentThread().getName()));


    boolean delivered = false;
    log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY_1", bucketNum, deliveryMethod));
    try
    {
      delivered = deliverNotification();
    }
    catch (Exception e)
    {
      // TODO
      log.warn("First delivery attempt failed. Caught exception: " + e.getMessage(), e);
    }

    if (delivered)
    {
      // First attempt succeeded
      // TODO: What if we crash before removing notification?
      //       and what if dao call throws exception?
//      dao.deleteNotification(tenant, notification);
      return notification;
    }

    // Pause and then try one more time
    try {log.debug("Sleep 15 seconds"); Thread.sleep(15000); } catch (InterruptedException e) {}
    log.debug(LibUtils.getMsg("NTFLIB_DSP_DLVRY_2", bucketNum, deliveryMethod));
    try
    {
      delivered = deliverNotification();
    }
    catch (Exception e)
    {
      // TODO
      log.warn("Second delivery attempt failed. Caught exception: " + e.getMessage(), e);
      return null;
    }
    if (delivered)
    {
      // Second attempt succeeded
      // TODO: What if we crash before removing notification?
      //       and what if dao call throws exception?
//      dao.deleteNotification(tenant, notification);
      return notification;
    }
    return null;
  }

  /* ********************************************************************** */
  /*                             Accessors                                  */
  /* ********************************************************************** */

  public int getBucketNum() { return bucketNum; }

  /* ********************************************************************** */
  /*                             Private Methods                            */
  /* ********************************************************************** */

  /*
   * TODO Send out the notification
   */
  private boolean deliverNotification()
  {
    Event event = notification.getEvent();
    log.info("Processing notification for event. Source: {} Type: {} Subject: {} SeriesId: {} Time: {} UUID {}",
             event.getSource(), event.getType(), event.getSubject(), event.getSeriesId(),
             event.getTime(), event.getUuid());
    log.info("Deliver notification. DeliveryType: {} DeliveryAddress: {}", deliveryMethod.getDeliveryType(),
             deliveryMethod.getDeliveryAddress());

    boolean deliveryStatus = false;
    try
    {
      switch (deliveryMethod.getDeliveryType())
      {
        case WEBHOOK -> deliveryStatus = deliverByWebhook();
        case EMAIL -> deliveryStatus = deliverByEmail();
      }
    }
    catch (IOException | URISyntaxException | InterruptedException e)
    {
      // TODO
      log.warn("Caught exception during notification delivery: " + e.getMessage(), e);
    }
    return deliveryStatus;
  }

  /*
   * TODO Send out the notification via Webhook
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
    // Request body is the event as json
    String eventJsonStr = notification.getEvent().toJsonString();

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
    RequestBody body = RequestBody.create(eventJsonStr, MediaType.parse("application/json"));
    Request.Builder requestBuilder = new Request.Builder().url(deliveryMethod.getDeliveryAddress()).post(body);
    Request request = requestBuilder.addHeader("User-Agent", "Tapis/%s".formatted(TapisConstants.API_VERSION)).build();
    Call call = client.newCall(request);
    Response response = call.execute();
    if (response.code() == Status.OK.getStatusCode()) return true;
    return false;
  }
  /*
   * TODO Send out the notification via email
   *
   */
  private boolean deliverByEmail() throws IOException, InterruptedException
  {
    return false;
  }
}