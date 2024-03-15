package edu.utexas.tacc.tapis.notifications.api.resources;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.notifications.api.requests.ReqEndEventSeries;
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostEvent;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;
import edu.utexas.tacc.tapis.sharedapi.responses.RespChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;

/*
 * JAX-RS REST resource for a Tapis Event (edu.utexas.tacc.tapis.notifications.model.Event)
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see repo openapi-notifications, file NotificationsAPI.yaml
 */
@Path("/v3/notifications/events")
public class EventResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(EventResource.class);

  // Json schema resource files.
  public static final String FILE_EVENT_POST_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/EventPostRequest.json";
  public static final String FILE_EVENT_ENDSERIES_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/EventEndSeriesRequest.json";

  // Message keys
  public static final String INVALID_JSON_INPUT = "NET_INVALID_JSON_INPUT";
  public static final String JSON_VALIDATION_ERR = "TAPIS_JSON_VALIDATION_ERROR";

  // Always return a nicely formatted response
  public static final boolean PRETTY = true;

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
   * Publish an event
   * @param tenant Set the tenant associated with the event. Only for services. By default, oboTenant is used.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Path("publish")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response publishEvent(InputStream payloadStream,
                               @QueryParam("tenant") String tenant,
                               @Context SecurityContext securityContext)
  {
    String opName = "publishEvent";
    String msg;
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

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_EVENT_POST_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    ReqPostEvent req;
    // ------------------------- Create an Event from the json and validate constraints -------------------------
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPostEvent.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_POST_REQ_NULL", rUser);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Make service call to post the event -------------------------------
    try
    {
      notificationsService.publishEvent(rUser, req.source, req.type, req.subject, req.data, req.seriesId,
              req.timestamp, req.deleteSubscriptionsMatchingSubject, req.endSeries, tenant);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_POST_ERR", rUser, req.source, req.type, req.subject, req.seriesId,
              req.timestamp, e.getMessage());
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means the object was created.
    RespBasic resp1 = new RespBasic();
    msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_POSTED", rUser);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(msg, PRETTY, resp1)).build();
  }

  /**
   * @deprecated
   * (DEPRECATED)
   * Post an event to the queue
   * @param tenant Set the tenant associated with the event. Only for services. By default, oboTenant is used.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Deprecated(since = "1.6.2", forRemoval = true)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postEvent(InputStream payloadStream,
                            @QueryParam("tenant") String tenant,
                            @Context SecurityContext securityContext)
  {
    return publishEvent(payloadStream, tenant, securityContext);
  }

  /**
   * End an event series. Series tracking data will be deleted.
   * A subsequent new event published with the same tenant, source, subject and seriesId will recreate the series
   * tracking data with the initial seriesSeqCount set to 1.
   * Associated event source, subject and seriesId must be provided in the request body.
   * @param tenant Set the tenant associated with the event. Only for services. By default, oboTenant is used.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("endSeries")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response endEventSeries(InputStream payloadStream,
                               @QueryParam("tenant") String tenant,
                               @Context SecurityContext securityContext)
  {
    String opName = "endEventSeries";
    String msg;
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
                                          "tenant="+tenant);

    // ------------------------- Extract and validate payload -------------------------
    // Read the payload into a string.
    String rawJson;
    try { rawJson = IOUtils.toString(payloadStream, StandardCharsets.UTF_8); }
    catch (Exception e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName , e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_EVENT_ENDSERIES_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    ReqEndEventSeries req;
    // ------------------------- Create an Event from the json and validate constraints -------------------------
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqEndEventSeries.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_ENDSERIES_REQ_NULL", rUser);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Log the rawJson for incoming request
    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("NTFAPI_EVENT_ENDSERIES_TRACE", rUser, rawJson));

    // ---------------------------- Make service call to post the event -------------------------------
    int changeCount;
    try
    {
      changeCount = notificationsService.endEventSeries(rUser, req.source, req.subject, req.seriesId, tenant);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_ENDSERIES_ERR", rUser, req.source, req.subject, req.seriesId, tenant);
      _log.error(msg);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_ENDSERIES", rUser, req.source, req.subject, req.seriesId, tenant);
    return Response.status(Status.OK).entity(TapisRestUtils.createSuccessResponse(msg, PRETTY, resp1)).build();
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */
}
