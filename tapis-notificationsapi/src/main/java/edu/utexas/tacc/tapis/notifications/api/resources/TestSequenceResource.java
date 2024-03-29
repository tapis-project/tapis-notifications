package edu.utexas.tacc.tapis.notifications.api.resources;

import com.google.gson.JsonSyntaxException;
import edu.utexas.tacc.tapis.notifications.api.responses.RespSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Pattern;

import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;
import edu.utexas.tacc.tapis.sharedapi.responses.RespChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostNotification;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.notifications.api.responses.RespTestSequence;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import static edu.utexas.tacc.tapis.notifications.api.resources.EventResource.INVALID_JSON_INPUT;
import static edu.utexas.tacc.tapis.notifications.api.resources.EventResource.JSON_VALIDATION_ERR;
import static edu.utexas.tacc.tapis.notifications.api.resources.EventResource.PRETTY;

/*
 * JAX-RS REST resource for managing a sequence of test notifications
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see repo openapi-notifications, file NotificationsAPI.yaml
 */
@Path("/v3/notifications/test")
public class TestSequenceResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(TestSequenceResource.class);

  private static final String BASE_SVC_URI
          = String.format("/%s/%s",TapisConstants.API_VERSION, TapisConstants.SERVICE_NAME_NOTIFICATIONS);

  private static final String HTTP_SCHEME = "http://";
  private static final String HTTPS_SCHEME = "https://";

  // Pattern for determining if base url is from a Tapis k8s environment
  // e.g. http://dev.develop.tapis.io/v3/notifications or http://dev.tapis.io/v3/notifications
  // TBD What about non-Tapis sites and associate sites? Will this need to change for them?
  // Start with "http://", then 0 or more characters, then ".tapis.io", then 0 or more characters
  // ^http://.*\.tapis\.io.*
  private static final Pattern TAPIS_BASEURL_K8S_PATTERN = Pattern.compile("^http://.*\\.tapis\\.io.*");

  // Json schema resource files.
  public static final String FILE_NOTIF_POST_REQUEST="/edu/utexas/tacc/tapis/notifications/api/jsonschema/NotificationPostRequest.json";

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  @Context
  private Request _request;

  // **************** Inject Services using HK2 ****************
  @Inject
  private NotificationsService notificationsService;

  private final String className = getClass().getSimpleName();

  // ************************************************************************
  // *********************** Public Methods *********************************
  // ************************************************************************

  /**
   * Start a test sequence by creating a subscription and publishing an event.
   * The URL to the created subscription will be returned in the result.
   * The name for the subscription should be saved for future calls to other test endpoints.
   *
   * @param subscriptionTTL - optional TTL for the auto-generated subscription
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Path("begin")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response beginTestSequence(@QueryParam("subscriptionTTL") @DefaultValue("60") String subscriptionTTL,
                                    @QueryParam("numberOfEvents") @DefaultValue("1") Integer numberOfEvents,
                                    @QueryParam("endSeries") @DefaultValue("true") Boolean endSeries,
                                    @Context SecurityContext securityContext)
  {
    String opName = "beginTestSequence";
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled()) ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(),
                                                   "subscriptionTTL="+subscriptionTTL, "endSeries="+endSeries);

    // Determine the base service url for the request. This is needed for the callback and the event source.
    // URI should be /v3/notifications/begin and URL should have the form http://localhost:8080/v3/notifications/begin
    // Note that when running in standard Tapis k8s environment the URL might look like this:
    //   http://dev.develop.tapis.io/v3/notifications or http://dev.tapis.io/v3/notifications
    // in which case we need to replace http: with https:
    String baseServiceUrl = StringUtils.removeEnd(_request.getRequestURL().toString(), _request.getRequestURI());
    if (TAPIS_BASEURL_K8S_PATTERN.matcher(baseServiceUrl).matches())
    {
      baseServiceUrl = StringUtils.replace(baseServiceUrl, HTTP_SCHEME, HTTPS_SCHEME, 1);
    }
    baseServiceUrl = baseServiceUrl + BASE_SVC_URI;

    // Trace the final baseUrl
    if (_log.isTraceEnabled()) { _log.trace(ApiUtils.getMsgAuth("NTFAPI_TEST_BASEURL", rUser, baseServiceUrl)); }

    // ---------------------------- Make service call  -------------------------------
    String msg;
    Subscription subscription;
    try
    {
      subscription = notificationsService.beginTestSequence(rUser, baseServiceUrl, subscriptionTTL, numberOfEvents, endSeries);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_TEST_ERR1", rUser, opName, "null", e.getMessage());
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (subscription == null)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_TEST_ERR2", rUser, opName);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    RespSubscription resp1 = new RespSubscription(subscription, null);
    msg = ApiUtils.getMsgAuth("NTFAPI_TEST_BEGIN", rUser, subscription.getName());
    return createSuccessResponse(Status.CREATED, msg, resp1);
  }

  /**
   * getTestResults
   * Retrieve status and history for a test sequence.
   * Provided subscription must have been created using the beginTestSequence endpoint.
   * @param name - name of the subscription
   * @param securityContext - user identity
   * @return Response with test results
   */
  @GET
  @Path("{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTestResults(@PathParam("name") String name,
                                 @Context SecurityContext securityContext)
  {
    String opName = "getTestResults";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name);

    TestSequence testSequence;
    try
    {
      testSequence = notificationsService.getTestSequence(rUser, name);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_TEST_GET_ERR", rUser, name, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (testSequence == null)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_NOT_FOUND", rUser, rUser.getOboUserId(), name);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the subscription information.
    RespTestSequence resp1 = new RespTestSequence(testSequence);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg("TAPIS_FOUND", "TestSequence", name), resp1);
  }

  /**
   * Delete a test sequence.
   * This removes all results associated with a subscription as well as the subscription
   * Provided subscription must have been created using the beginTestSequence endpoint.
   * @param name - name of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTestSequence(@PathParam("name") String name,
                                     @Context SecurityContext securityContext)
  {
    String opName = "deleteTestSequence";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name);

    // ---------------------------- Make service call to delete the profile -------------------------------
    int changeCount;
    String msg;
    try
    {
      changeCount = notificationsService.deleteTestSequence(rUser, name);
    }
    catch (NotAuthorizedException e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_UNAUTH", rUser, rUser.getOboUserId(), name, opName);
      _log.warn(msg);
      return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_TEST_ERR1", rUser, opName, name, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth("NTFAPI_TEST_DELETE", rUser, name), resp1);
  }

  /**
   * Receive a notification as a callback and record it as a test result.
   * Provided subscription must be associated with a test sequence.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return basic response
   */
  @POST
  @Path("callback/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response recordTestNotification(@PathParam("name") String name,
                                         InputStream payloadStream, @Context SecurityContext securityContext)
  {
    String opName = "recordTestNotification";
    // Trace this request.
    if (_log.isTraceEnabled())
    {
      String msg = ApiUtils.getMsg("NTFAPI_TRACE_REQUEST", "N/A", "N/A", "N/A", "N/A", className, opName,
                                   _request.getRequestURL(), "name="+name);
      _log.trace(msg);
    }
    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    String msg;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    if (_log.isTraceEnabled()) _log.trace("{} - rawJson: {}", opName, rawJson);

    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_NOTIF_POST_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    ReqPostNotification req;
    // ------------------------- Create a notification from the json -------------------------
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPostNotification.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_CB_REQ_NULL", name);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req.event is null that is an unrecoverable error
    if (req.event == null)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_NULL", name);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If deliveryTarget is null that is an unrecoverable error
    if (req.deliveryTarget == null)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_DM_NULL", name);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    String notifUuidStr = req.uuid;
    String notifCreatedStr = req.created;

    // Make sure the notification UUID in the request is valid
    UUID notifUuid;
    try { notifUuid = UUID.fromString(notifUuidStr); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_NOTIF_UUID_ERR", name, notifUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Make sure the notification created timestamp is valid
    Instant notifCreated;
    try { notifCreated = TapisUtils.getUTCTimeFromString(notifCreatedStr).toInstant(ZoneOffset.UTC); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_NOTIF_UUID_ERR", name, notifUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Now that we have a valid request we can set the tenant and user associated with the event
    String tenant = req.event.tenant;
    String user = req.event.user;
    String sourceStr = req.event.source;
    String subject = req.event.subject;
    String data = req.event.data;
    String type = req.event.type;
    String seriesId = req.event.seriesId;
    long seriesSeqCount = req.event.seriesSeqCount;
    String timestamp = req.event.timestamp;
    boolean deleteSubscriptionsMatchingSubject = req.event.deleteSubscriptionsMatchingSubject;
    boolean endSeries = req.event.endSeries;
    String eventUuidStr = req.event.uuid;
    Instant received = TapisUtils.getUTCTimeFromString(req.event.received).toInstant(ZoneOffset.UTC);
    // Tenant and user should both have values
    if (StringUtils.isBlank(tenant) || StringUtils.isBlank(user))
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_USR_ERR", tenant, user, sourceStr, type, subject, timestamp, name);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Validate the event type
    if (!Event.isValidType(type))
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_TYPE_ERR", tenant, user, sourceStr, type, subject, timestamp, name);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Extract the event source from the request making sure it is a URI
    String source = TapisConstants.SERVICE_NAME_NOTIFICATIONS;

    // Extract the event UUID from the request making sure it is a UUID
    UUID eventUuid;
    try { eventUuid = UUID.fromString(eventUuidStr); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_UUID_ERR", name, eventUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create an Event from the request
    Event event = new Event(source, type, subject, data, seriesId, seriesSeqCount, timestamp,
                            deleteSubscriptionsMatchingSubject, endSeries, tenant, user, received, eventUuid);
    // Create a notification from the request
    Notification notification = new Notification(notifUuid, -1, tenant, name, -1, eventUuid, event,
                                                 req.deliveryTarget, notifCreated);

    // ---------------------------- Make service call to record the event -------------------------------
    try
    {
      notificationsService.recordTestNotification(tenant, user, name, notification);
    }
    catch (IllegalStateException e)
    {
      // IllegalStateException means test sequence not found
      msg = ApiUtils.getMsg("NTFAPI_TEST_RECORD_NO_TEST", tenant, user, name, event.getType());
      _log.warn(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_CB_ERR", tenant, user, sourceStr, type, subject, seriesId, seriesSeqCount, timestamp,
                            name, e.getMessage());
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means the object was created.
    RespBasic resp1 = new RespBasic();
    msg = ApiUtils.getMsg("NTFAPI_TEST_RECORD", tenant, user, name, event.getType());
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(msg, PRETTY, resp1)).build();
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

  /**
   * Create an OK response given message and base response to put in result
   * @param msg - message for resp.message
   * @param resp - base response (the result)
   * @return - Final response to return to client
   */
  private static Response createSuccessResponse(Status status, String msg, RespAbstract resp)
  {
    return Response.status(status).entity(TapisRestUtils.createSuccessResponse(msg, PRETTY, resp)).build();
  }
}