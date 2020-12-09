package edu.utexas.tacc.tapis.notifications.api.resources;


import edu.utexas.tacc.tapis.notifications.api.models.CreateNotificationRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateSubscriptionRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.lib.models.Notification;
import edu.utexas.tacc.tapis.notifications.lib.models.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.models.Topic;
import edu.utexas.tacc.tapis.notifications.lib.services.NotificationsService;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Path("/v3/notifications/topics")
public class TopicsResource {

    private static class TopicListResponse extends TapisResponse<List<Topic>> {}
    private static class TopicResponse extends TapisResponse<Topic> {}
    private static class NotificationResponse extends TapisResponse<Notification> {}
    private static class SubscriptionListResponse extends TapisResponse<List<Subscription>> {}


    @Inject
    TenantManager tenantCache;

    @Inject
    ServiceJWT serviceJWTCache;

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
        List<Topic> topicListing = new ArrayList<>();
        TapisResponse<List<Topic>> resp = TapisResponse.createSuccessResponse("ok", topicListing);
        return resp;
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
        Topic topic = new Topic();
        TapisResponse<Topic> resp = TapisResponse.createSuccessResponse("ok", topic);
        return resp;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{topicId}")
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
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Valid CreateNotificationRequest notificationRequest,
        @Context SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{topicId}")
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
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Valid CreateNotificationRequest notificationRequest,
        @Context SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
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
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("topicId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a subscription by ID", tags = {"subscriptions"})
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
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("topicId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }



}
