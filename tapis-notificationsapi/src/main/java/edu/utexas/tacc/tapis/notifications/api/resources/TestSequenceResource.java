package edu.utexas.tacc.tapis.notifications.api.resources;

import com.google.gson.JsonSyntaxException;
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostNotification;
import edu.utexas.tacc.tapis.notifications.model.Notification;
import edu.utexas.tacc.tapis.shared.utils.TapisUtils;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import edu.utexas.tacc.tapis.notifications.api.responses.RespTestSequence;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.model.TestSequence;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
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
import edu.utexas.tacc.tapis.sharedapi.responses.RespResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
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
   * The Id for the subscription should be saved for future calls to other test endpoints.
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
    if (_log.isTraceEnabled()) ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

    // Determine the base service url for the request. This is needed for the callback and the event source.
    // URI should be /v3/notifications/begin and URL should have the form http://localhost:8080/v3/notifications/begin
    String baseServiceUrl = StringUtils.removeEnd(_request.getRequestURL().toString(), _request.getRequestURI());
    baseServiceUrl = baseServiceUrl + BASE_SVC_URI;

    // ---------------------------- Make service call  -------------------------------
    String msg;
    String testSubscriptionId;
    try
    {
      testSubscriptionId = notificationsService.beginTestSequence(rUser, baseServiceUrl, subscriptionTTL);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_TEST_ERR", rUser, opName, "null", e.getMessage());
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success ------------------------------- 
    // Success means test sequence started. Return the url to the subscription.
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString() + "/" + testSubscriptionId;
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    msg = ApiUtils.getMsgAuth("NTFAPI_TEST_BEGIN", rUser, testSubscriptionId);
    return createSuccessResponse(Status.CREATED, msg, resp1);
  }

  /**
   * getTestResults
   * Retrieve status and history for a test sequence.
   * Provided subscription must have been created using the beginTestSequence endpoint.
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return Response with test results
   */
  @GET
  @Path("{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTestResults(@PathParam("subscriptionId") String subscriptionId,
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "subscriptionId="+subscriptionId);

    TestSequence testSequence;
    try
    {
      testSequence = notificationsService.getTestSequence(rUser, subscriptionId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_TEST_GET_ERR", rUser, subscriptionId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (testSequence == null)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_NOT_FOUND", rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the subscription information.
    RespTestSequence resp1 = new RespTestSequence(testSequence);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg("TAPIS_FOUND", "TestSequence", subscriptionId), resp1);
  }

  /**
   * Delete a test sequence.
   * This removes all results associated with a subscription as well as the subscription
   * Provided subscription must have been created using the beginTestSequence endpoint.
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTestSequence(@PathParam("subscriptionId") String subscriptionId,
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "subscriptionId="+subscriptionId);

    // ---------------------------- Make service call to delete the profile -------------------------------
    int changeCount;
    String msg;
    try
    {
      changeCount = notificationsService.deleteTestSequence(rUser, subscriptionId);
    }
    catch (NotAuthorizedException e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_UNAUTH", rUser, subscriptionId, opName);
      _log.warn(msg);
      return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_TEST_ERR", rUser, opName, subscriptionId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth("NTFAPI_TEST_DELETE", rUser, subscriptionId), resp1);
  }

  /**
   * Receive a notification as a callback and record it as a test result.
   * Provided subscription must be associated with a test sequence.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return basic response
   */
  @POST
  @Path("callback/{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response recordTestNotification(@PathParam("subscriptionId") String subscriptionId,
                                         InputStream payloadStream, @Context SecurityContext securityContext)
  {
    String opName = "recordTestNotification";
    // Trace this request.
    if (_log.isTraceEnabled())
    {
      String msg = ApiUtils.getMsg("NTFAPI_TRACE_REQUEST", "N/A", "N/A", "N/A", "N/A", className, opName,
                                   _request.getRequestURL(), "subscriptionId="+subscriptionId);
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

    // TODO
    if (_log.isTraceEnabled()) _log.trace("TODO - REMOVE_ME - rawJson: " + rawJson);

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
      msg = ApiUtils.getMsg("NTFAPI_TEST_CB_REQ_NULL", subscriptionId);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req.event is null that is an unrecoverable error
    if (req.event == null)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_NULL", subscriptionId);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If deliveryMethod is null that is an unrecoverable error
    if (req.deliveryMethod == null)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_DM_NULL", subscriptionId);
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
      msg = ApiUtils.getMsg("NTFAPI_TEST_NOTIF_UUID_ERR", subscriptionId, notifUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Make sure the notification created timestamp is valid
    Instant notifCreated;
    try { notifCreated = TapisUtils.getUTCTimeFromString(notifCreatedStr).toInstant(ZoneOffset.UTC); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_NOTIF_UUID_ERR", subscriptionId, notifUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Now that we have a valid request we can set the tenant and user associated with the event
    String tenant = req.event.tenant;
    String user = req.event.user;
    String sourceStr = req.event.source;
    String subject = req.event.subject;
    String type = req.event.type;
    String seriesId = req.event.seriesId;
    String time = req.event.time;
    String eventUuidStr = req.event.uuid;

    // Tenant and user should both have values
    if (StringUtils.isBlank(tenant) || StringUtils.isBlank(user))
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_USR_ERR", tenant, user, sourceStr, type, subject, time, subscriptionId);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Validate the event type
    if (!Event.isValidType(type))
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_TYPE_ERR", tenant, user, sourceStr, type, subject, time, subscriptionId);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Extract the event source from the request making sure it is a URI
    URI source;
    try { source = new URI(sourceStr); }
    catch (URISyntaxException e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_SOURCE_ERR", tenant, user, sourceStr, type, subject, time, subscriptionId, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Extract the event UUID from the request making sure it is a UUID
    UUID eventUuid;
    try { eventUuid = UUID.fromString(eventUuidStr); }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_EVENT_UUID_ERR", subscriptionId, eventUuidStr, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create an Event from the request
    Event event = new Event(tenant, user, source, type, subject, seriesId, time, eventUuid);
    // Create a notification from the request
    Notification notification = new Notification(notifUuid, -1, tenant, subscriptionId, -1, eventUuid, event,
                                                 req.deliveryMethod, notifCreated);

    // ---------------------------- Make service call to record the event -------------------------------
    try
    {
      notificationsService.recordTestNotification(tenant, user, subscriptionId, notification);
    }
    catch (IllegalStateException e)
    {
      // IllegalStateException means test sequence not found
      msg = ApiUtils.getMsg("NTFAPI_TEST_RECORD_NO_TEST", tenant, user, subscriptionId, event.getType());
      _log.warn(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsg("NTFAPI_TEST_CB_ERR", tenant, user, sourceStr, type, subject, seriesId, time,
                            subscriptionId, e.getMessage());
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means the object was created.
    RespBasic resp1 = new RespBasic();
    msg = ApiUtils.getMsg("NTFAPI_TEST_RECORD", tenant, user, subscriptionId, event.getType());
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