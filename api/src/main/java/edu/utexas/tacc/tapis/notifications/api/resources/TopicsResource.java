package edu.utexas.tacc.tapis.notifications.api.resources;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.api.models.CreateNotificationRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateSubscriptionRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.lib.exceptions.ServiceException;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Path("/v3/notifications/topics")
public class TopicsResource {

    private static class TopicListResponse extends TapisResponse<List<Topic>> {}
    private static class TopicResponse extends TapisResponse<Topic> {}
    private static class NotificationResponse extends TapisResponse<Notification> {}
    private static class SubscriptionListResponse extends TapisResponse<List<Subscription>> {}
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(TopicsResource.class);
    private static final TypeReference<Map<String, Object>> JsonTypeRef = new TypeReference<>() {};

    @Inject
    NotificationsService notificationsService;

    @GET
    @Operation(summary = "Get a list of all topics available to you.", tags = {"topics"})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<List<Topic>> getTopics(
        @Parameter(description = "pagination limit", example = "100") @DefaultValue("1000") @QueryParam("limit") @Max(1000) int limit,
        @Parameter(description = "pagination offset", example = "1000") @DefaultValue("0") @QueryParam("offset") @Min(0) long offset,
        @Context SecurityContext securityContext
    ) {
        //TODO: What topics should a service account see?
        try {
            AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
            List<Topic> topicListing = notificationsService.getTopicsByTenantAndOwner(user.getTenantId(), user.getName());
            TapisResponse<List<Topic>> resp = TapisResponse.createSuccessResponse("ok", topicListing);
            return resp;
        } catch (ServiceException ex) {
            throw new WebApplicationException("Could not retrieve topics", ex);
        }
    }




    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new topic to publish message to", tags = {"topics"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<Topic> createTopic(
        @Context SecurityContext securityContext,
        @Valid CreateTopicRequest topicRequest
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        try {
            Topic topic = new Topic();
            topic.setTenantId(user.getTenantId());
            topic.setOwner(user.getName());
            topic.setName(topicRequest.getName());
            topic.setDescription(topicRequest.getDescription());
            topic = notificationsService.createTopic(topic);
            TapisResponse<Topic> resp = TapisResponse.createSuccessResponse("ok", topic);
            return resp;
        } catch (ServiceException ex) {
            throw new WebApplicationException("Well that is strange, if the problem persists, please contact support.", ex);
        }
    }

    @GET
    @Path("/{topicId}")
    @Operation(summary = "Get details of a topic.", tags = {"topics"})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TopicResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<Topic> getTopic(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        try {
            Topic topic = notificationsService.getTopic(user.getTenantId(), topicID);
            TapisResponse<Topic> resp = TapisResponse.createSuccessResponse("ok", topic);
            return resp;
        } catch (ServiceException ex) {
            String msg = String.format("Could not retrieve topic %s in tenant %s", topicID, user.getTenantId());
            throw new WebApplicationException(msg);
        }
    }


    /**
     * This should probably only be allowed for the owner of the topic period. Not even other service accounts should be
     * creating notifications in other accounts topics?
     * @param topicName
     * @param notificationRequest
     * @param securityContext
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{topicName}")
    @Operation(summary = "Create a new Notification in the topic channel", tags = {"topics"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> sendNotification(
        @Parameter(description = "Name of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Valid CreateNotificationRequest notificationRequest,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        try {
            Topic topic = notificationsService.getTopic(user.getTenantId(), topicName);

            if (topic == null) {
                String msg = String.format("Could not find topic: %s", topicName);
                throw new NotFoundException(msg);
            }

            if (!user.getName().equals(topic.getOwner())) {
                throw new NotAuthorizedException("Only the owner of the topic may publish notifications");
            }

            String id = Optional.ofNullable(notificationRequest.getId()).orElse(UUID.randomUUID().toString());
            Notification notification = new Notification.Builder()
                .setId(id)
                .setTenantId(user.getTenantId())
                .setTopicName(topicName)
                .setSource(notificationRequest.getSource())
                .setType(notificationRequest.getType())
                .setSubject(notificationRequest.getSubject())
                .setData(notificationRequest.getData())
                .build();
            notificationsService.sendNotification(notification);
            return TapisResponse.createSuccessResponse("ok");

        } catch (ServiceException ex) {
            throw new WebApplicationException("Could not send notification");
        } catch (ValidationException ex) {
            throw new BadRequestException("Invalid notification.");
        }
    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{topicName}")
    @Operation(summary = "Delete a topic. Note, this will also delete any subscriptions that are attached to the topic", tags = {"topics"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> deleteTopic(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Valid CreateNotificationRequest notificationRequest,
        @Context SecurityContext securityContext
    ) {
        try {
            AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
            notificationsService.deleteTopic(user.getTenantId(), topicName);
            return TapisResponse.createSuccessResponse("ok");
        } catch (ServiceException ex) {
            throw new WebApplicationException(ex.getMessage(), ex);
        }
    }

    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/{topicName}/messages")
    @Operation(summary = "Subscribe to and start receiving messages via ServerSentEvents.", tags = {"topics"})
    public Response getMessageStream(
        @Parameter(description = "Name of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Context SecurityContext securityContext,
        @Context SseEventSink sseEventSink,
        @Context Sse sse
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();

        return Response.ok().build();
    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a list of all subscriptions on this topic", tags = {"subscriptions"})
    @Path("/{topicId}/subscriptions")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = SubscriptionListResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = SubscriptionListResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = SubscriptionListResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = SubscriptionListResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> getSubscriptions(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        return TapisResponse.createSuccessResponse("ok");
    }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new subscription to this topic channel", tags = {"subscriptions"})
    @Path("/{topicId}/subscriptions")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> createSubscription(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Context SecurityContext securityContext,
        @Valid CreateSubscriptionRequest subscriptionRequest
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        return TapisResponse.createSuccessResponse("ok");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a subscription by ID", tags = {"subscriptions"})
    @Path("/{topicId}/subscriptions/{subscriptionId}")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TopicListResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> getSubscriptionByID(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("subscriptionId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        return TapisResponse.createSuccessResponse("ok");
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a subscription", tags = {"subscriptions"})
    @Path("/{topicId}/subscriptions/{subscriptionId}")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = TapisResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> deleteSubscription(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("subscriptionId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        return TapisResponse.createSuccessResponse("ok");
    }



}
