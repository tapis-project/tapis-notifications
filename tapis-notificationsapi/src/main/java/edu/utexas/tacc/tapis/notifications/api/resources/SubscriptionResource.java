package edu.utexas.tacc.tapis.notifications.api.resources;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
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

import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import edu.utexas.tacc.tapis.notifications.api.requests.ReqPostSubscription;
import edu.utexas.tacc.tapis.notifications.api.responses.RespSubscription;
import edu.utexas.tacc.tapis.notifications.api.responses.RespSubscriptions;
import edu.utexas.tacc.tapis.notifications.api.utils.ApiUtils;
import edu.utexas.tacc.tapis.notifications.model.PatchSubscription;
import edu.utexas.tacc.tapis.notifications.model.Subscription;
import edu.utexas.tacc.tapis.notifications.service.NotificationsService;

import static edu.utexas.tacc.tapis.notifications.model.Subscription.DEFAULT_OWNER;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.NAME_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.OWNER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.SUBJECT_FILTER_FIELD;
import static edu.utexas.tacc.tapis.notifications.model.Subscription.TYPE_FILTER_FIELD;

/*
 * JAX-RS REST resource for a Tapis Subscription (edu.utexas.tacc.tapis.notifications.model.Subscription)
 * jax-rs annotations map HTTP verb + endpoint to method invocation and map query parameters.
 *  NOTE: For OpenAPI spec please see repo openapi-notifications, file NotificationsAPI.yaml
 */
@Path("/v3/notifications/subscriptions")
public class SubscriptionResource
{
  // ************************************************************************
  // *********************** Constants **************************************
  // ************************************************************************
  // Local logger.
  private static final Logger _log = LoggerFactory.getLogger(SubscriptionResource.class);

  private static final String NOTIFICATIONS_SVC = StringUtils.capitalize(TapisConstants.SERVICE_NAME_NOTIFICATIONS);
  private static final String RESOURCE_TYPE = "Subscription";

  // Json schema resource files.
  private static final String FILE_SUBSCR_POST_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionPostRequest.json";
  private static final String FILE_SUBSCR_PATCH_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionPatchRequest.json";
  private static final String FILE_SUBSCR_SEARCH_REQUEST = "/edu/utexas/tacc/tapis/notifications/api/jsonschema/SubscriptionSearchRequest.json";

  // Message keys
  private static final String INVALID_JSON_INPUT = "NET_INVALID_JSON_INPUT";
  private static final String JSON_VALIDATION_ERR = "TAPIS_JSON_VALIDATION_ERROR";
  private static final String UPDATE_ERR = "NTFAPI_SUBSCR_UPDATE_ERROR";
  private static final String CREATE_ERR = "NTFAPI_SUBSCR_CREATE_ERROR";
  private static final String SELECT_ERR = "NTFAPI_SELECT_ERROR";
  private static final String LIB_UNAUTH = "NTFLIB_UNAUTH";
  private static final String API_UNAUTH = "NTFAPI_SUBSCR_UNAUTH";
  private static final String TAPIS_FOUND = "TAPIS_FOUND";
  private static final String NOT_FOUND = "NTFAPI_NOT_FOUND";
  private static final String UPDATED = "NTFAPI_SUBSCR_UPDATED";

  // Format strings
  private static final String NTF_CNT_STR = "%d subscriptions";

  // Operation names
  private static final String OP_ENABLE = "enableSubscription";
  private static final String OP_DISABLE = "disableSubscription";
  private static final String OP_UPDATE_TTL = "updateTTL";
  private static final String OP_DELETE_BY_NAME = "deleteSubscriptionByName";
  private static final String OP_DELETE_BY_UUID = "deleteSubscriptionByUuid";

  // Always return a nicely formatted response
  private static final boolean PRETTY = true;

  // Top level summary attributes to be included by default in some cases.
  public static final List<String> SUMMARY_ATTRS =
          new ArrayList<>(List.of(OWNER_FIELD, NAME_FIELD, TYPE_FILTER_FIELD, SUBJECT_FILTER_FIELD));

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
    String msg;
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
    Subscription subscription = createSubscriptionFromPostRequest(rUser.getOboTenantId(), req);

    // So far no need to scrub out secrets, so scrubbed and raw are the same.
    String scrubbedJson = rawJson;
    if (_log.isTraceEnabled()) _log.trace(ApiUtils.getMsgAuth("NTFAPI_CREATE_TRACE", rUser, scrubbedJson));

    // Check constraints on Subscription attributes
    resp = validateSubscription(subscription, rUser);
    if (resp != null) return resp;

    // ---------------------------- Make service call to create the subscription -------------------------------
    String name = subscription.getName();
    String owner = subscription.getOwner();
    try
    {
      name = notificationsService.createSubscription(rUser, subscription, scrubbedJson);
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains("NTFLIB_SUBSCR_EXISTS"))
      {
        // IllegalStateException with msg containing SUBSCR_EXISTS indicates object exists - return 409 - Conflict
        msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_EXISTS", rUser, owner, name);
        _log.warn(msg);
        return Response.status(Status.CONFLICT).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, owner, name, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid Subscription was passed in
        msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, name, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, name, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(CREATE_ERR, rUser, name, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means the object was created.
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString() + "/" + name;
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.CREATED, ApiUtils.getMsgAuth("NTFAPI_SUBSCR_CREATED", rUser, owner, name), resp1);
  }

  /**
   * Update selected attributes of a subscription
   * @param name name of the subscription
   * @param ownedBy subscription owner
   * @param payloadStream - request body
   * @param securityContext - user identity
   * @return response containing reference to updated object
   */
  @PATCH
  @Path("byName/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response patchSubscriptionByName(@PathParam("name") String name,
                                    @QueryParam("ownedBy") String ownedBy,
                                    InputStream payloadStream,
                                    @Context SecurityContext securityContext)
  {
    String opName = "patchSubscriptionByName";
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name, "ownedBy="+ownedBy);

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
    JsonValidatorSpec spec = new JsonValidatorSpec(rawJson, FILE_SUBSCR_PATCH_REQUEST);
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

    // Validate the subscription type filter
    if (!Subscription.isValidTypeFilter(patchSubscription.getTypeFilter()))
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_TYPE_ERR", rUser, name, patchSubscription.getTypeFilter());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // No attributes are required. Constraints validated and defaults filled in on server side.
    // No secrets in PatchSubscription so no need to scrub

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

    // ---------------------------- Make service call to update the subscription -------------------------------
    try
    {
      notificationsService.patchSubscription(rUser, subscrOwner, name, patchSubscription, rawJson);
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscrOwner, name);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscrOwner, name, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates an Invalid PatchSubscription was passed in
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    ResultResourceUrl respUrl = new ResultResourceUrl();
    respUrl.url = _request.getRequestURL().toString();
    RespResourceUrl resp1 = new RespResourceUrl(respUrl);
    return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, subscrOwner, name, opName), resp1);
  }

  /**
   * Enable a subscription
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{name}/enable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response enableSubscription(@PathParam("name") String name,
                                     @QueryParam("ownedBy") String ownedBy,
                                     @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_ENABLE, ownedBy, name, null, securityContext);
  }

  /**
   * Disable a subscription
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{name}/disable")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response disableSubscription(@PathParam("name") String name,
                                      @QueryParam("ownedBy") String ownedBy,
                                      @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_DISABLE, ownedBy, name, null, securityContext);
  }

  /**
   * Delete a subscription by name
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("byName/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteSubscriptionByName(@PathParam("name") String name,
                                           @QueryParam("ownedBy") String ownedBy,
                                           @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_DELETE_BY_NAME, ownedBy, name, null, securityContext);
  }

  /**
   * Delete a subscription by uuid
   * @param uuid - UUID of the subscription
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("byUuid/{uuid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteSubscriptionByUuid(@PathParam("uuid") String uuid,
                                           @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_DELETE_BY_UUID, null, uuid, null, securityContext);
  }

  /**
   * Update TTL for a subscription
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @POST
  @Path("{name}/updateTTL/{ttlMinutes}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateTTL(@PathParam("name") String name,
                            @PathParam("ttlMinutes") String ttlMinutes,
                            @QueryParam("ownedBy") String ownedBy,
                            @Context SecurityContext securityContext)
  {
    return postSubscriptionSingleUpdate(OP_UPDATE_TTL, ownedBy, name, ttlMinutes, securityContext);
  }

  /**
   * Delete all subscriptions whose subjectFilter matches a specific subject.
   * @param subject - subject for matching
   * @param ownedBy - Use specified user in place of the requesting user. Leave null or blank to use requesting user.
   * @param anyOwner - If true delete all matching subscriptions owned by any user. ownedBy will be ignored.
   * @param securityContext - user identity
   * @return - response with change count as the result
   */
  @DELETE
  @Path("bySubject/{subject}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteSubscriptionsBySubject(@PathParam("subject") String subject,
                                               @QueryParam("ownedBy") String ownedBy,
                                               @QueryParam("anyOwner") @DefaultValue("false") boolean anyOwner,
                                               @Context SecurityContext securityContext)
  {
    String opName = "deleteSubscriptionsBySubject";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get();
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "subject="+subject, "ownedBy="+ownedBy, "anyOwner="+anyOwner);

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

    String msg;
    int changeCount;
    // ---------------------------- Make service call to delete subscriptions -------------------------------
    try
    {
      changeCount = notificationsService.deleteSubscriptionsBySubject(rUser, subscrOwner, subject, anyOwner);
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_DEL_BY_SUBJ_ERROR", rUser, subscrOwner, subject, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_DEL_BY_SUBJ_ERROR", rUser, subscrOwner, subject, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    msg = ApiUtils.getMsgAuth("NTFAPI_SUBSCR_DELETED_BY_SUBJ", rUser, subject, changeCount);
    return createSuccessResponse(Status.OK, msg, resp1);


  }

  /**
   * getSubscriptionByName
   * Retrieve a subscription
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return Response with subscription object as the result
   */
  @GET
  @Path("byName/{name}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptionByName(@PathParam("name") String name,
                                        @QueryParam("ownedBy") String ownedBy,
                                        @Context SecurityContext securityContext)
  {
    String opName = "getSubscriptionByName";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name, "ownedBy="+ownedBy);

    List<String> selectList = threadContext.getSearchParameters().getSelectList();

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

    Subscription subscription;
    try
    {
      subscription = notificationsService.getSubscriptionByName(rUser, subscrOwner, name);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_GET_NAME_ERROR", rUser, subscrOwner, name, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (subscription == null)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscrOwner, name);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the subscription information.
    RespSubscription resp1 = new RespSubscription(subscription, selectList);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "Subscription", name), resp1);
  }

  /**
   * getSubscriptionByUuid
   * Retrieve a subscription
   * @param uuid - UUID of the subscription
   * @param securityContext - user identity
   * @return Response with subscription object as the result
   */
  @GET
  @Path("byUuid/{uuid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptionByUuid(@PathParam("uuid") String uuid,
                                        @Context SecurityContext securityContext)
  {
    String opName = "getSubscriptionByUuid";
    // Check that we have all we need from the context, the jwtTenantId and jwtUserId
    // Utility method returns null if all OK and appropriate error response if there was a problem.
    TapisThreadContext threadContext = TapisThreadLocal.tapisThreadContext.get(); // Local thread context
    Response resp = ApiUtils.checkContext(threadContext, PRETTY);
    if (resp != null) return resp;

    // Create a user that collects together tenant, user and request information needed by the service call
    ResourceRequestUser rUser = new ResourceRequestUser((AuthenticatedUser) securityContext.getUserPrincipal());

    // Trace this request.
    if (_log.isTraceEnabled())
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "uuid="+uuid);

    List<String> selectList = threadContext.getSearchParameters().getSelectList();

    Subscription subscription;
    try
    {
      subscription = notificationsService.getSubscriptionByUuid(rUser, uuid);
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_GET_BY_UUID_ERROR", rUser, uuid, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // Resource was not found.
    if (subscription == null)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_NOT_FOUND_BY_UUID", rUser, uuid);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we retrieved the subscription information.
    RespSubscription resp1 = new RespSubscription(subscription, selectList);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "Subscription", uuid), resp1);
  }

  /**
   * getSubscriptions
   * Retrieve all subscriptions accessible by requester and matching any search conditions provided.
   * NOTE: The query parameters search, limit, orderBy, skip, startAfter are all handled in the filter
   *       QueryParametersRequestFilter. No need to use @QueryParam here.
   * @param ownedBy - Use specified user in place of the requesting user. Leave null or blank to use requesting user.
   * @param anyOwner - If true retrieve all subscriptions owned by any user. ownedBy will be ignored.
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptions(@QueryParam("ownedBy") String ownedBy,
                                   @QueryParam("anyOwner") @DefaultValue("false") boolean anyOwner,
                                   @Context SecurityContext securityContext)
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "ownedBy="+ownedBy, "anyOwner="+anyOwner);

    // ThreadContext designed to never return null for SearchParameters
    SearchParameters srchParms = threadContext.getSearchParameters();

    // ------------------------- Retrieve records -----------------------------
    Response successResponse;
    try
    {
      successResponse = getSearchResponse(rUser, null, srchParms, ownedBy, anyOwner);
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
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @GET
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchSubscriptionsQueryParameters(@QueryParam("ownedBy") String ownedBy,
                                                     @Context SecurityContext securityContext)
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "ownedBy="+ownedBy);

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
      boolean anyOwnerFalse = false;
      successResponse = getSearchResponse(rUser, null, srchParms, ownedBy, anyOwnerFalse);
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
   * @param ownedBy subscription owner
   * @param payloadStream - request body@quer
   * @param securityContext - user identity
   * @return - list of subscriptions accessible by requester and matching search conditions.
   */
  @POST
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response searchSubscriptionsRequestBody(@QueryParam("ownedBy") String ownedBy,
                                                 InputStream payloadStream,
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "ownedBy="+ownedBy);

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
      boolean anyOwnerFalse = false;
      successResponse = getSearchResponse(rUser, sqlSearchStr, srchParms, ownedBy, anyOwnerFalse);
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
   * @param name - name of the subscription
   * @param ownedBy subscription owner
   * @param securityContext - user identity
   * @return Response with boolean result
   */
  @GET
  @Path("{name}/isEnabled")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response isEnabled(@PathParam("name") String name,
                            @QueryParam("ownedBy") String ownedBy,
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
      ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name, "ownedBy="+ownedBy);

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

    boolean isEnabled;
    try
    {
      isEnabled = notificationsService.isEnabled(rUser, subscrOwner, name);
    }
    catch (NotFoundException e)
    {
      String msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, name);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      String msg = ApiUtils.getMsgAuth("NTFAPI_GET_NAME_ERROR", rUser, name, e.getMessage());
      _log.error(msg, e);
      return Response.status(TapisRestUtils.getStatus(e)).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means we made the check
    ResultBoolean respResult = new ResultBoolean();
    respResult.aBool = isEnabled;
    RespBoolean resp1 = new RespBoolean(respResult);
    return createSuccessResponse(Status.OK, MsgUtils.getMsg(TAPIS_FOUND, "Subscription", name), resp1);
  }

  /* **************************************************************************** */
  /*                                Private Methods                               */
  /* **************************************************************************** */

  /**
   * enable, disable, delete, updateTtl follow same pattern
   * @param opName Name of operation.
   * @param name Name or uuid of subscription to update/delete
   * @param ttlMinutes new value for updateTtl operation
   * @param securityContext Security context from client call
   * @return Response to be returned to the client.
   */
  private Response postSubscriptionSingleUpdate(String opName, String ownedBy, String name, String ttlMinutes,
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
      if (OP_UPDATE_TTL.equals(opName))
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name, "ownedBy="+ownedBy, "ttlMinutes="+ttlMinutes);
      else
        ApiUtils.logRequest(rUser, className, opName, _request.getRequestURL().toString(), "name="+name, "ownedBy="+ownedBy);
    }

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

    // ---------------------------- Make service call to update the subscription -------------------------------
    int changeCount;
    String msg;
    try
    {
      if (OP_ENABLE.equals(opName))
        changeCount = notificationsService.enableSubscription(rUser, subscrOwner, name);
      else if (OP_DISABLE.equals(opName))
        changeCount = notificationsService.disableSubscription(rUser, subscrOwner, name);
      else if (OP_DELETE_BY_NAME.equals(opName))
        changeCount = notificationsService.deleteSubscriptionByName(rUser, subscrOwner, name);
      else if (OP_DELETE_BY_UUID.equals(opName))
        changeCount = notificationsService.deleteSubscriptionByUuid(rUser, name);
      else if (OP_UPDATE_TTL.equals(opName))
        changeCount = notificationsService.updateSubscriptionTTL(rUser, subscrOwner, name, ttlMinutes);
      else
      {
        msg = ApiUtils.getMsgAuth("NTFAPI_OP_UNKNOWN", rUser, subscrOwner, name, opName);
        _log.warn(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (NotFoundException e)
    {
      msg = ApiUtils.getMsgAuth(NOT_FOUND, rUser, subscrOwner, name);
      _log.warn(msg);
      return Response.status(Status.NOT_FOUND).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (IllegalStateException e)
    {
      if (e.getMessage().contains(LIB_UNAUTH))
      {
        // IllegalStateException with msg containing NTF_UNAUTH indicates operation not authorized for apiUser - return 401
        msg = ApiUtils.getMsgAuth(API_UNAUTH, rUser, subscrOwner, name, opName);
        _log.warn(msg);
        return Response.status(Status.UNAUTHORIZED).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
      else
      {
        // IllegalStateException indicates resulting subscription would be invalid
        msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
        _log.error(msg);
        return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
      }
    }
    catch (IllegalArgumentException e)
    {
      // IllegalArgumentException indicates somehow a bad argument made it this far
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
      _log.error(msg);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }
    catch (Exception e)
    {
      msg = ApiUtils.getMsgAuth(UPDATE_ERR, rUser, subscrOwner, name, opName, e.getMessage());
      _log.error(msg, e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TapisRestUtils.createErrorResponse(msg, PRETTY)).build();
    }

    // ---------------------------- Success -------------------------------
    // Success means updates were applied
    // Return the number of objects impacted.
    ResultChangeCount count = new ResultChangeCount();
    count.changes = changeCount;
    RespChangeCount resp1 = new RespChangeCount(count);
    if (OP_DELETE_BY_NAME.equals(opName) || OP_DELETE_BY_UUID.equals(opName))
      return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth("NTFAPI_SUBSCR_DELETED", rUser, subscrOwner, name), resp1);
    else
      return createSuccessResponse(Status.OK, ApiUtils.getMsgAuth(UPDATED, rUser, subscrOwner, name, opName), resp1);
  }

  /**
   * Create a subscription from a ReqPostSubscription
   * Check for req == null should have already been done
   */
  private static Subscription createSubscriptionFromPostRequest(String tenantId, ReqPostSubscription req)
  {
    // If owner not specified fill in a default
    String ownerStr = req.owner;
    if (StringUtils.isBlank(ownerStr))  ownerStr = DEFAULT_OWNER;
    return new Subscription(-1, tenantId, ownerStr, req.name, req.description, req.enabled, req.typeFilter,
                            req.subjectFilter, req.deliveryTargets, req.ttlMinutes, null, null, null, null);
  }

  /**
   * Fill in defaults and check constraints on Subscription attributes
   * Check values. name and owner must be set.
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
      String allErrors = getListOfErrors(errMessages, rUser, RESOURCE_TYPE, subscription1.getOwner(), subscription1.getName());
      _log.error(allErrors);
      return Response.status(Status.BAD_REQUEST).entity(TapisRestUtils.createErrorResponse(allErrors, PRETTY)).build();
    }
    return null;
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
  private Response getSearchResponse(ResourceRequestUser rUser, String sqlSearchStr, SearchParameters srchParms,
                                     String ownedBy, boolean anyOwner)
          throws Exception
  {
    RespAbstract resp1;
    List<Subscription> subscriptions;
    int totalCount = -1;
    String itemCountStr;

    // For owner use oboUser or string specified in optional query parameter
    String subscrOwner =  StringUtils.isBlank(ownedBy) ? rUser.getOboUserId() : ownedBy;

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
      subscriptions = notificationsService.getSubscriptions(rUser, subscrOwner, searchList, limit, orderByList, skip,
                                                            startAfter, anyOwner);
    else
      subscriptions = notificationsService.getSubscriptionsUsingSqlSearchStr(rUser, subscrOwner, sqlSearchStr, limit, orderByList, skip,
                                                  startAfter);
    if (subscriptions == null) subscriptions = Collections.emptyList();
    itemCountStr = String.format(NTF_CNT_STR, subscriptions.size());
    if (computeTotal && limit <= 0) totalCount = subscriptions.size();

    // If we need the count and there was a limit then we need to make a call
    if (computeTotal && limit > 0)
    {
      totalCount = notificationsService.getSubscriptionsTotalCount(rUser, subscrOwner, searchList, orderByList, startAfter);
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
