package edu.utexas.tacc.tapis.notifications.api.resources;

import com.google.gson.JsonSyntaxException;

import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostEvent;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.model.Event;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBasic;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
   * Post an event to the queue
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postEvent(InputStream payloadStream, @Context SecurityContext securityContext)
  {
    String opName = "postEvent";
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

    // Only services may publish. Reject if not a service.
    if (!rUser.isServiceRequest())
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_UNAUTH", rUser);
      _log.warn(msg);
      return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

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

    // Validate the event type
    if (!Event.isValidType(req.type))
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_TYPE_ERR", rUser, req.source, req.type, req.subject, req.timestamp);
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Extract the source from the request making sure it is a URI
    String source = TapisConstants.SERVICE_NAME_NOTIFICATIONS;
    // Create an Event from the request
    Event event = new Event(source, req.type, req.subject, req.data, req.seriesId, req.timestamp,
                             req.deleteSubscriptionsMatchingSubject, rUser.getOboTenantId(), rUser.getOboUserId(),
                             UUID.randomUUID());

    // If first field of type is not the service name then reject
    if (!event.getType1().equals(rUser.getJwtUserId()))
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_EVENT_TYPE_NOTSVC", rUser, req.source, req.type, req.subject, req.timestamp);
      _log.warn(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Make service call to post the event -------------------------------
    try
    {
      notificationsService.publishEvent(rUser, event);
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

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */
}