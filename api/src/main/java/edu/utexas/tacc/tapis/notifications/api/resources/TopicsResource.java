package edu.utexas.tacc.tapis.notifications.api.resources;


import edu.utexas.tacc.tapis.notifications.api.models.CreateNotificationRequest;
import edu.utexas.tacc.tapis.notifications.api.models.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.api.models.Topic;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;

@Path("/v3/notifications/topics")
public class TopicsResource {

    private static class TopicResponse extends TapisResponse<List<Topic>> {}


    @GET
    @Path("/")
    @Operation(summary = "Get a list of all topics available to you.", tags = {"topics"})
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
    @Path("/")
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
    @Path("/{topicId}")
    @Operation(summary = "Create a new Notification in the topic channel", tags = {"topics"})
    public TapisResponse<String> sendNotification(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        @Valid CreateNotificationRequest notificationRequest,
        SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }

    @GET
    @Operation(summary = "Get a list of all subscriptions on this topic", tags = {"topics"})
    @Path("/{topicId}/subscriptions")
    public TapisResponse<String> getSubscriptions(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }


    @POST
    @Operation(summary = "Create a new subscription to this topic channel", tags = {"topics"})
    @Path("/{topicId}/subscriptions")
    public TapisResponse<String> createSubscription(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicId") String topicID,
        SecurityContext securityContext,
        @Valid CreateNotificationRequest notificationRequest
    ) {

        return TapisResponse.createSuccessResponse("ok");
    }





}
