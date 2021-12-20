package edu.utexas.tacc.tapis.notifications.api.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostSubscription;
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPutSubscription;
import edu.utexas.tacc.tapis.notifications.api.responses.RespSubscription;
import edu.utexas.tacc.tapis.notifications.api.responses.RespSubscriptions;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;

import edu.utexas.tacc.tapis.search.SearchUtils;
import edu.utexas.tacc.tapis.shared.TapisConstants;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.threadlocal.OrderBy;
import edu.utexas.tacc.tapis.shared.threadlocal.SearchParameters;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadContext;
import edu.utexas.tacc.tapis.shared.threadlocal.TapisThreadLocal;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedapi.responses.RespAbstract;
import edu.utexas.tacc.tapis.sharedapi.responses.RespBoolean;
import edu.utexas.tacc.tapis.sharedapi.responses.RespChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.RespResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultBoolean;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultChangeCount;
import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultResourceUrl;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ResourceRequestUser;
import edu.utexas.tacc.tapis.sharedapi.utils.TapisRestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.utexas.tacc.tapis.notifications.model.Subscription.ID_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.OWNER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.TOPIC_FILTER_FIELD;

/*
 * JAX-RS REST resource for a Tapis Subscription (edu.utexas.tacc.tapis.notifications.model.Subscription)
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see repo openapi-notifications, file NotificationsAPI.yaml
 */
@Path("/v3/notifications")
public class SubscriptionResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(SubscriptionResource.class);

  private static final String NOTIFICATIONS_SVC = StringUtils.capitalize(TapisConstants.SERVICE_NAME_NOTIFICATIONS);

  // Json schema resource files.
  private static final String FILE_SUBSCR_POST_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionPostRequest.json";
  private static final String FILE_SUBSCR_PUT_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionPutRequest.json";
  private static final String FILE_SUBSCR_UPDATE_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionPatchRequest.json";
  private static final String FILE_SUBSCR_SEARCH_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionSearchRequest.json";

  // Message keys
  private static final String INVALID_JSON_INPUT = "NET_INVALID_JSON_INPUT";
  private static final String JSON_VALIDATION_ERR = "TAPIS_JSON_VALIDATION_ERROR";
  private static final String UPDATE_ERR = "NTFAPI_UPDATE_ERROR";
  private static final String CREATE_ERR = "NTFAPI_CREATE_ERROR";
  private static final String SELECT_ERR = "NTFAPI_SELECT_ERROR";
  private static final String LIB_UNAUTH = "NTFLIB_UNAUTH";
  private static final String API_UNAUTH = "NTFAPI_SUBCSR_UNAUTH";
  private static final String TAPIS_FOUND = "TAPIS_FOUND";
  private static final String NOT_FOUND = "NTFAPI_NOT_FOUND";
  private static final String UPDATED = "NTFAPI_UPDATED";

  // Format strings
  private static final String NTF_CNT_STR = "%d subscriptions";

  // Operation names
  private static final String OP_ENABLE = "enableSubscription";
  private static final String OP_DISABLE = "disableSubscription";
  private static final String OP_CHANGEOWNER = "changeSubscriptionOwner";
  private static final String OP_DELETE = "deleteSubscription";

  // Always return a nicely formatted response
  private static final boolean PRETTY = true;

  // Top level summary attributes to be included by default in some cases.
  public static final List<String> SUMMARY_ATTRS = new ArrayList<>(List.of(ID_FIELD, OWNER_FIELD, TOPIC_FILTER_FIELD));

  // ************************************************************************
  // *********************** Fields *****************************************
  // ************************************************************************
  @Context
  private HttpHeaders _httpHeaders;
  @Context
  private Application _application;
  @Context
  private UriInfo _uriInfo;
  @Context
  private SecurityContext _securityContext;
  @Context
  private ServletContext _servletContext;
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
   * Create a subscription
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to created object
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createSubscription(InputStream payloadStream,
                                     @Context SecurityContext securityContext)
  {
    String opName = "postSubscription";
    // Note that although the following approximately 30 lines of code is very similar for many endpoints the slight
    //   variations and use of fetched data makes it difficult to refactor into common routines. Common routines
    //   might make the code even more complex and difficult to follow.
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

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
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_SUBSCR_POST_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    ReqPostSubscription req;
    // ------------------------- Create a subscription from the json and validate constraints -------------------------
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPostSubscription.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, "N/A", "ReqPostSubscription == null");
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create a subscription from the request
    Subscription subscription = createSubscriptionFromPostRequest(rUser.getOboTenantId(), req, rawJson);

    // So far no need to scrub out secrets, so scrubbed and raw are the same.
    String scrubbedJson = rawJson;
    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("NTFAPI_CREATE_TRACE", rUser, scrubbedJson));

    // Fill in defaults and check constraints on Subscription attributes
    subscription.setDefaults();
    resp = validateSubscription(subscription, rUser);
    if (resp != null) return resp;

    // ---------------------------- Make service call to create the subscription -------------------------------
    String subscriptionId = subscription.getId();
    try
    {
      notificationsService.createSubscription(rUser, subscription, scrubbedJson);
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("NTFLIB_SUBSCR_EXISTS"))
      {
        // IllegalStateException with msg containing SUBSCR_EXISTS indicates object exists - return 409 - Conflict
        msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_EXISTS", rUser, subscriptionId);
        _log.warn(msg);
        return Response.status(Status.CONFLICT).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscriptionId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid Subscription was passed in
        msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, subscriptionId, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, subscriptionId, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, subscriptionId, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success ------------------------------- 
    // Success means the object was created.
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString() + "/" + subscriptionId;
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.CREATED, ApiUtils.getMsgAuth("NTFAPI_CREATED", rUser, subscriptionId), resp1);
  }

  /**
   * Update selected attributes of a subscription
   * @param subscriptionId - id of the subscription
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PATCH
  @Path("{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response patchSubscription(@PathParam("subscriptionId") String subscriptionId,
                           InputStream payloadStream,
                           @Context SecurityContext securityContext)
  {
    String opName = "patchSubscription";
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "subscriptionId="+subscriptionId);

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
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_SUBSCR_UPDATE_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ------------------------- Create a PatchSubscription from the json -------------------------
    PatchSubscription patchSubscription;
    try
    {
      patchSubscription = TapisGsonUtils.getGson().fromJson(rawJson, PatchSubscription.class);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("NTFAPI_PATCH_TRACE", rUser, rawJson));

    // Notes require special handling. Else they end up as a LinkedTreeMap which causes trouble when attempting to
    // convert to a JsonObject.
    patchSubscription.setNotes(extractNotes(rawJson));

    // No attributes are required. Constraints validated and defaults filled in on server side.
    // No secrets in PatchSubscription so no need to scrub

    // ---------------------------- Make service call to update the subscription -------------------------------
    try
    {
      notificationsService.patchSubscription(rUser, subscriptionId, patchSubscription, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscriptionId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PatchSubscription was passed in
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, subscriptionId, opName), resp1);
  }

  /**
   * Update all updatable attributes of a subscription
   * @param subscriptionId - id of the subscription
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PUT
  @Path("{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response putSubscription(@PathParam("subscriptionId") String subscriptionId,
                         InputStream payloadStream,
                         @Context SecurityContext securityContext)
  {
    String opName = "putSubscription";
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "subscriptionId="+subscriptionId);

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
    // Create validator specification and validate the json against the schema
    // NOTE that CREATE and PUT are very similar schemas.
    // Only difference should be for PUT there are no required properties.
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_SUBSCR_PUT_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ------------------------- Create a subscription from the json and validate constraints -------------------------
    ReqPutSubscription req;
    try { req = TapisGsonUtils.getGson().fromJson(rawJson, ReqPutSubscription.class); }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    // If req is null that is an unrecoverable error
    if (req == null)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_UPDATE_ERROR", rUser, subscriptionId, opName, "ReqPutSubscription == null");
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Create a subscription from the request
    Subscription putSubscription = createSubscriptionFromPutRequest(rUser.getOboTenantId(), subscriptionId, req, rawJson);

    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("NTFAPI_PUT_TRACE", rUser, rawJson));

    // Fill in defaults and check constraints on Subscription attributes
    // NOTE: We do not have all the Tapis Subscription attributes yet so we cannot validate it
    putSubscription.setDefaults();

    // ---------------------------- Make service call to update the subscription -------------------------------
    try
    {
       notificationsService.putSubscription(rUser, putSubscription, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscriptionId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PutSubscription was passed in
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, subscriptionId, opName), resp1);
  }

  /**
   * Enable a subscription
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{subscriptionId}/enable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response enableSubscription(@PathParam("subscriptionId") String subscriptionId,
                            @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_ENABLE, subscriptionId, null, securityContext);
  }

  /**
   * Disable a subscription
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{subscriptionId}/disable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response disableSubscription(@PathParam("subscriptionId") String subscriptionId,
                             @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_DISABLE, subscriptionId, null, securityContext);
  }

  /**
   * Delete a subscription
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{subscriptionId}/delete")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteSubscription(@PathParam("subscriptionId") String subscriptionId,
                            @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_DELETE, subscriptionId, null, securityContext);
  }

  /**
   * Change owner of a subscription
   * @param subscriptionId - name of the subscription
   * @param userName - name of the new owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{subscriptionId}/changeOwner/{userName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response changeSubscriptionOwner(@PathParam("subscriptionId") String subscriptionId,
                                 @PathParam("userName") String userName,
                                 @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_CHANGEOWNER, subscriptionId, userName, securityContext);
  }

  /**
   * getSubscription
   * Retrieve a subscription
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return Response with subscription object as the result
   */
  @GET
  @Path("{subscriptionId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscription(@PathParam("subscriptionId") String subscriptionId,
                         @Context SecurityContext securityContext)
  {
    String opName = "getSubscription";
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

    List<String> selectList = threadContext.getSearchParameters().getSelectList();

    Subscription subscription;
    try
    {
      subscription = notificationsService.getSubscription(rUser, subscriptionId);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_GET_NAME_ERROR", rUser, subscriptionId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (subscription == null)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the subscription information.
    RespSubscription resp1 = new RespSubscription(subscription, selectList);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "Subscription", subscriptionId), resp1);
  }

  /**
   * getSubscriptions
   * Retrieve all subscriptions accessible by requester and matching any search conditions provided.
   * NOTE: The query parameters search, limit, orderBy, skip, startAfter are all handled in the filter
   *       QueryParametersRequestFilter. No need to use @QueryParam here.
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptions(@Context SecurityContext securityContext)
  {
    String opName = "getSubscriptions";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();

    // ------------------------- Retrieve records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, null, srchParms);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    return successResponse;
  }

  /**
   * searchSubscriptionsQueryParameters
   * Dedicated search endpoint for Subscription resource. Search conditions provided as query parameters.
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @GET
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchSubscriptionsQueryParameters(@Context SecurityContext securityContext)
  {
    String opName = "searchSubscriptionsGet";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

    // Create search list based on query parameters
    // Note that some validation is done for each condition but the back end will handle translating LIKE wildcard
    //   characters (* and !) and deal with escaped characters.
    List<String> searchList;
    try
    {
      searchList = SearchUtils.buildListFromQueryParms(_uriInfo.getQueryParameters());
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_SEARCH_ERROR", rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();
    srchParms.setSearchList(searchList);

    // ------------------------- Retrieve all records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, null, srchParms);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    return successResponse;
  }

  /**
   * searchSubscriptionsRequestBody
   * Dedicated search endpoint for Subscription resource. Search conditions provided in a request body.
   * Request body contains an array of strings that are concatenated to form the full SQL-like search string.
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @POST
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchSubscriptionsRequestBody(InputStream payloadStream,
                                        @Context SecurityContext securityContext)
  {
    String opName = "searchSubscriptionsPost";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString());

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
    // Create validator specification and validate the json against the schema
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_SUBSCR_SEARCH_REQUEST);
    try { JsonValidator.validate(spec); }
    catch (TapisJSONException e)
    {
      msg = MsgUtils.getMsg(JSON_VALIDATION_ERR, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Construct final SQL-like search string using the json
    // When put together full string must be a valid SQL-like where clause. This will be validated in the service call.
    // Not all SQL syntax is supported. See SqlParser.jj in tapis-shared-searchlib.
    String sqlSearchStr;
    try
    {
      sqlSearchStr = SearchUtils.getSearchFromRequestJson(rawJson);
    }
    catch (JsonSyntaxException e)
    {
      msg = MsgUtils.getMsg(INVALID_JSON_INPUT, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();

    // ------------------------- Retrieve all records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, sqlSearchStr, srchParms);
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(SELECT_ERR, rUser, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    return successResponse;
  }

  /**
   * isEnabled
   * Check if subscription is enabled.
   * @param subscriptionId - name of the subscription
   * @param securityContext - user identity
   * @return Response with boolean result
   */
  @GET
  @Path("{subscriptionId}/isEnabled")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response isEnabled(@PathParam("subscriptionId") String subscriptionId,
                         @Context SecurityContext securityContext)
  {
    String opName = "isEnabled";
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

    boolean isEnabled;
    try
    {
      isEnabled = notificationsService.isEnabled(rUser, subscriptionId);
    }
    catch (NotFoundException e)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_GET_NAME_ERROR", rUser, subscriptionId, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we made the check
    ResultBoolean respResult = new ResultBoolean();
    respResult.aBool = isEnabled;
    RespBoolean resp1 = new RespBoolean(respResult);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "Subscription", subscriptionId), resp1);
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

  /**
   * changeOwner, enable, disable, delete follow same pattern
   * Note that userName only used for changeOwner
   * @param opName Name of operation.
   * @param subscriptionId Id of subscription to update
   * @param userName new owner name for op changeOwner
   * @param securityContext Security context from client call
   * @return Response to be returned to the client.
   */
  private Response postSubscriptionSingleUpdate(String opName, String subscriptionId, String userName,
                                                SecurityContext securityContext)
  {
    // ------------------------- Retrieve and validate thread context -------------------------
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
    {
      // NOTE: We deliberately do not check for blank. If empty string passed in we want to record it here.
      if (userName!=null)
      {
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(),
                            "subscriptionId=" + subscriptionId, "userName=" + userName);
      }
      else
      {
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(),
                            "subscriptionId=" + subscriptionId);
      }
    }

    // ---------------------------- Make service call to update the subscription -------------------------------
    int changeCount;
    String msg;
    try
    {
      if (OP_ENABLE.equals(opName))
        changeCount = notificationsService.enableSubscription(rUser, subscriptionId);
      else if (OP_DISABLE.equals(opName))
        changeCount = notificationsService.disableSubscription(rUser, subscriptionId);
      else if (OP_DELETE.equals(opName))
        changeCount = notificationsService.deleteSubscription(rUser, subscriptionId);
      else
        changeCount = notificationsService.changeSubscriptionOwner(rUser, subscriptionId, userName);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscriptionId);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscriptionId, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates resulting subscription would be invalid
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscriptionId, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, subscriptionId, opName), resp1);
  }

  /**
   * Create a subscription from a ReqPostSubscription
   * Check for req == null should have already been done
   */
  private static Subscription createSubscriptionFromPostRequest(String tenantId, ReqPostSubscription req, String rawJson)
  {
    // Extract Notes from the raw json.
    Object notes = extractNotes(rawJson);
    // Create Subscription
    return new Subscription(-1, tenantId, req.id, req.description, req.owner, req.enabled,
                            req.topicFilter, req.subjectFilter, req.deliveryMethods,
                            notes, null, null, null);
  }

  /**
   * Create a subscription from a ReqPutSubscription
   */
  private static Subscription createSubscriptionFromPutRequest(String tenantId, String id, ReqPutSubscription req, String rawJson)
  {
    // Extract Notes from the raw json.
    Object notes = extractNotes(rawJson);

    // NOTE: Following attributes are not updatable and must be filled in on service side.
    String owner = null;
    boolean enabled = true;
    return new Subscription(-1, tenantId, id, req.description, owner, enabled,
                            req.topicFilter, req.subjectFilter, req.deliveryMethods,
                            notes, null, null, null);
  }

  /**
   * Fill in defaults and check constraints on Subscription attributes
   * Check values. Id must be set.
   * Collect and report as many errors as possible so they can all be fixed before next attempt
   * NOTE: JsonSchema validation should handle some of these checks but we check here again just in case
   *
   * @return null if OK or error Response
   */
  private static Response validateSubscription(Subscription subscription1, ResourceRequestUser rUser)
  {
    // Make call for lib level validation
    List<String> errMessages = subscription1.checkAttributeRestrictions();

    // Now validate attributes that have special handling at API level.
    // Currently, no additional checks.

    // If validation failed log error message and return response
    if (!errMessages.isEmpty())
    {
      // Construct message reporting all errors
      String allErrors = getListOfErrors(errMessages, rUser, subscription1.getId());
      _log.error(allErrors);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(allErrors, PRETTY)).build();
    }
    return null;
  }

  /**
   * Extract notes from the incoming json
   * This explicit method to extract is needed because notes is an unstructured object and other seemingly simpler
   * approaches caused problems with the json marshalling. This method ensures notes end up as a JsonObject rather
   * than a LinkedTreeMap.
   */
  private static Object extractNotes(String rawJson)
  {
    Object notes = null;
    // Check inputs
    if (StringUtils.isBlank(rawJson)) return notes;
    // Turn the request string into a json object and extract the notes object
    JsonObject topObj = TapisGsonUtils.getGson().fromJson(rawJson, JsonObject.class);
    if (!topObj.has(Subscription.NOTES_FIELD)) return notes;
    notes = topObj.getAsJsonObject(Subscription.NOTES_FIELD);
    return notes;
  }

  /**
   * Construct message containing list of errors
   */
  private static String getListOfErrors(List<String> msgList, ResourceRequestUser rUser, Object... parms) {
    if (msgList == null || msgList.isEmpty()) return "";
    var sb = new StringBuilder(ApiUtils.getMsgAuth("NTFAPI_CREATE_INVALID_ERRORLIST", rUser, parms));
    sb.append(System.lineSeparator());
    for (String msg : msgList) { sb.append("  ").append(msg).append(System.lineSeparator()); }
    return sb.toString();
  }

  /**
   *  Common method to return a list of subscriptions given a search list and search parameters.
   *  srchParms must be non-null
   *  One of srchParms.searchList or sqlSearchStr must be non-null
   */
  private Response getSearchResponse(ResourceRequestUser rUser, String sqlSearchStr, SearchParameters srchParms)
          throws Exception
  {
    RespAbstract resp1;
    List<Subscription> subscriptions;
    int totalCount = -1;
    String itemCountStr;

    List<String> searchList = srchParms.getSearchList();
    List<String> selectList = srchParms.getSelectList();
    if (selectList == null || selectList.isEmpty()) selectList = SUMMARY_ATTRS;

    // If limit was not specified then use the default
    int limit = (srchParms.getLimit() == null) ? SearchParameters.DEFAULT_LIMIT : srchParms.getLimit();
    // Set some variables to make code easier to read
    int skip = srchParms.getSkip();
    String startAfter = srchParms.getStartAfter();
    boolean computeTotal = srchParms.getComputeTotal();
    String orderBy = srchParms.getOrderBy();
    List<OrderBy> orderByList = srchParms.getOrderByList();

    if (StringUtils.isBlank(sqlSearchStr))
      subscriptions = notificationsService.getSubscriptions(rUser, searchList, limit, orderByList, skip, startAfter);
    else
      subscriptions = notificationsService.getSubscriptionsUsingSqlSearchStr(rUser, sqlSearchStr, limit, orderByList, skip,
                                                  startAfter);
    if (subscriptions == null) subscriptions = Collections.emptyList();
    itemCountStr = String.format(NTF_CNT_STR, subscriptions.size());
    if (computeTotal && limit <= 0) totalCount = subscriptions.size();

    // If we need the count and there was a limit then we need to make a call
    if (computeTotal && limit > 0)
    {
      totalCount = notificationsService.getSubscriptionsTotalCount(rUser, searchList, orderByList, startAfter);
    }

    // ---------------------------- Success -------------------------------
    resp1 = new RespSubscriptions(subscriptions, limit, orderBy, skip, startAfter, totalCount, selectList);

    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, NOTIFICATIONS_SVC, itemCountStr), resp1);
  }

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
