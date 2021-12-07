package edu.utexas.tacc.tapis.notifications.api.resources;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.notifications.api.model.CreateNotificationRequest;
import edu.utexas.tacc.tapis.notifications.api.model.CreateSubscriptionRequest;
import edu.utexas.tacc.tapis.notifications.api.model.CreateTopicRequest;
import edu.utexas.tacc.tapis.notifications.api.providers.TopicsAuthorization;
import edu.utexas.tacc.tapis.notifications.lib.model.Event;
import edu.utexas.tacc.tapis.notifications.lib.model.Subscription;
import edu.utexas.tacc.tapis.notifications.lib.model.Topic;
import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsService;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
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
//import reactor.core.publisher.Flux;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.util.List;
import java.util.Map;

@Path("/v3/notifications/topics")
public class TopicsResource {

    private static class TopicListResponse extends TapisResponse<List<Topic>> {}
    private static class TopicResponse extends TapisResponse<Topic> {}
    private static class NotificationResponse extends TapisResponse<Event> {}
    private static class SubscriptionListResponse extends TapisResponse<List<Subscription>> {}
    private static class SubscriptionResponse extends TapisResponse<Subscription> {}
    private static final ObjectMapper mapper = TapisObjectMapper.getMapper();
    private static final Logger log = LoggerFactory.getLogger(TopicsResource.class);
    private static final TypeReference<Map<String, Object>> JsonTypeRef = new TypeReference<>() {};

    @Inject
    NotificationsService notificationsService;

    @Inject
    TenantManager tenantManager;

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
// TODO
      return null;
// TODO        try {
//            AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
//            List<Topic> topicListing = notificationsService.getTopicsByTenantAndOwner(user.getTenantId(), user.getName());
//            TapisResponse<List<Topic>> resp = TapisResponse.createSuccessResponse("ok", topicListing);
//            return resp;
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Could not retrieve topics", ex);
//        }
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
        @NotNull(message = "payload required")  @Valid CreateTopicRequest topicRequest
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        // TODO
      return null;
//TODO        try {
//            Topic topic = new Topic();
//            topic.setTenantId(user.getTenantId());
//            topic.setOwner(user.getName());
//            topic.setName(topicRequest.getName());
//            topic.setDescription(topicRequest.getDescription());
//            topic = notificationsService.createTopic(topic);
//            TapisResponse<Topic> resp = TapisResponse.createSuccessResponse("ok", topic);
//            return resp;
//        } catch (DuplicateEntityException ex) {
//            throw new BadRequestException("Topic with this name already exists");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Well that is strange, if the problem persists, please contact support.", ex);
//        }
    }

    @GET
    @TopicsAuthorization
    @Path("/{topicName}")
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
        @Parameter(description = "Name of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        // TODO
      return null;
//TODO        try {
//            Topic topic = notificationsService.getTopic(user.getTenantId(), topicName);
//            if (topic == null)  {
//                throw new NotFoundException();
//            }
//            TapisResponse<Topic> resp = TapisResponse.createSuccessResponse("ok", topic);
//            return resp;
//        } catch (ServiceException ex) {
//            String msg = String.format("Could not retrieve topic %s in tenant %s", topicName, user.getTenantId());
//            throw new WebApplicationException(msg);
//        }
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
    @TopicsAuthorization
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
        // TODO
      return null;
//        try {
//            Topic topic = notificationsService.getTopic(user.getTenantId(), topicName);
//
//            if (topic == null) {
//                String msg = String.format("Could not find topic: %s", topicName);
//                throw new NotFoundException(msg);
//            }
//
//            if (!user.getName().equals(topic.getOwner())) {
//                throw new NotAuthorizedException("Only the owner of the topic may publish notifications");
//            }
//
//            // if there is an ID, keep it, if not give it a UUID
//            String id = Optional.ofNullable(notificationRequest.getId()).orElse(UUID.randomUUID().toString());
//            Notification notification = new Notification.Builder()
//                .setId(id)
//                .setTenantId(user.getTenantId())
//                .setTopicName(topicName)
//                .setSource(notificationRequest.getSource())
//                .setType(notificationRequest.getType())
//                .setSubject(notificationRequest.getSubject())
//                .setData(notificationRequest.getData())
//                .build();
//            notificationsService.sendNotification(notification);
//            return TapisResponse.createSuccessResponse("ok");
//
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Could not send notification");
//        } catch (ValidationException ex) {
//            throw new BadRequestException("Invalid notification.");
//        }
    }


    @DELETE
    @TopicsAuthorization
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
      // TODO
      return null;
//TODO        try {
//            AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
//            Topic topic = notificationsService.getTopic(user.getTenantId(), topicName);
//            if (topic == null) {
//                throw new NotFoundException("Topic not found");
//            }
//            notificationsService.deleteTopic(user.getTenantId(), topicName);
//            return TapisResponse.createSuccessResponse("ok");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException(ex.getMessage(), ex);
//        }
    }

    @GET
    @TopicsAuthorization
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Path("/{topicName}/messages")
    @Operation(summary = "Subscribe to and start receiving messages via ServerSentEvents.", tags = {"topics"})
    public void getMessageStream(
        @Parameter(description = "Name of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Context SecurityContext securityContext,
        @Context SseEventSink sseEventSink,
        @Context Sse sse
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
      // TODO
      return;
//TODO      try {
//            Topic topic = notificationsService.getTopic(user.getTenantId(), topicName);
//
//            //Create a queue
//            Queue tmpQueue = new Queue();
//            tmpQueue.setTenantId(user.getTenantId());
//            tmpQueue.setOwner(user.getName());
//            tmpQueue.setName("tmp-api-" + UUID.randomUUID().toString());
//            notificationsService.createQueue(tmpQueue);
//            //create new subscription to queue
//            Subscription tmpSub = new Subscription();
//            tmpSub.setTenantId(user.getTenantId());
//            tmpSub.setTopicId(topic.getId());
//            Map<String, Object> filters = new HashMap<>();
//            tmpSub.setFilters(filters);
//            NotificationDeliveryMethod deliveryMethod = new NotificationDeliveryMethod(NotificationDeliveryMethodEnum.QUEUE, tmpQueue.getName());
//            tmpSub.addDeliveryMethod(deliveryMethod);
//            //Save the subscription
//            Subscription subscription = notificationsService.createSubscription(topic, tmpSub);
//            //start listening to events on queue
//
//            notificationsService.streamNotificationsOnQueue(tmpQueue.getName())
//                .subscribe(notification -> {
//                    OutboundSseEvent event = sse.newEventBuilder()
//                        .name("message-to-client")
//                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
//                        .data(Notification.class, notification)
//                        .build();
//                    sseEventSink.send(event);
//                });
//            // TODO: Figure out how to delete the subscription after the connection is closed.
//            // onSocketClose -> delete subscription
//        } catch (DuplicateEntityException ex) {
//            throw new WebApplicationException("Something went really wrong here");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Hmmmmmm...");
//        }
    }



    @GET
    @TopicsAuthorization
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a list of all subscriptions on this topic", tags = {"subscriptions"})
    @Path("/{topicName}/subscriptions")
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
    public SubscriptionListResponse getSubscriptions(
        @Parameter(description = "Name of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Context SecurityContext securityContext
    ) {
      // TODO
      return null;
//TODO
//        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
//        try {
//            List<Subscription> subscriptions = notificationsService.getSubscriptionsForTopic(user.getTenantId(), topicName);
//            return (SubscriptionListResponse) TapisResponse.createSuccessResponse(subscriptions);
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Could not retrieve subscriptions for this topic.");
//        }
    }


    @POST
    @TopicsAuthorization
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new subscription to this topic channel", tags = {"subscriptions"})
    @Path("/{topicName}/subscriptions")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<Subscription> createSubscription(
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Context SecurityContext securityContext,
        @NotNull(message = "payload required") @Valid CreateSubscriptionRequest subscriptionRequest
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        Subscription subscription = new Subscription();
        subscription.setTenantId(user.getTenantId());
      // TODO
      return null;
//TODO
//        Topic topic;
//        try {
//            topic = notificationsService.getTopic(user.getTenantId(), topicName);
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("This shouldn't happen.", ex);
//        }
//
//        if (topic == null) {
//            throw new NotFoundException("Not Found.");
//        }
//
//        Map<String, Object> filters;
//        //Is it even valid json?
//        try {
//            filters = mapper.readValue(subscriptionRequest.getFilter(), JsonTypeRef);
//        } catch (JsonProcessingException ex) {
//            throw new BadRequestException("Invalid filters.", ex);
//        }
//        subscription.setFilters(filters);
//        subscription.setDeliveryMethods(subscriptionRequest.getNotificationDeliveryMethods());
//        try {
//            subscription = notificationsService.createSubscription(topic, subscription);
//            TapisResponse<Subscription> resp = TapisResponse.createSuccessResponse("Topic created", subscription);
//            return resp;
//        } catch (DuplicateEntityException ex) {
//            throw new BadRequestException("A topic already exists with this name");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Could not subscribe to topic" + topicName);
//        }
    }

    @GET
    @TopicsAuthorization
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a subscription by ID", tags = {"subscriptions"})
    @Path("/{topicName}/subscriptions/{subscriptionId}")
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
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("subscriptionId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
        return TapisResponse.createSuccessResponse("ok");
    }

    @DELETE
    @TopicsAuthorization
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a subscription", tags = {"subscriptions"})
    @Path("/{topicName}/subscriptions/{subscriptionId}")
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
        @Parameter(description = "ID of the topic", required = true, example = "mySuperTopic") @PathParam("topicName") String topicName,
        @Parameter(description = "ID of the subscription", required = true, example = "1234-123-123") @PathParam("subscriptionId") String subscriptionID,
        @Context SecurityContext securityContext
    ) {
      // TODO
      return null;
//TODO        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();
//        Subscription sub;
//        try {
//            sub = notificationsService.getSubscription(UUID.fromString(subscriptionID));
//            if (sub == null) {
//                throw new NotFoundException("Subscription not found");
//            }
//            notificationsService.deleteSubscription(sub);
//            return TapisResponse.createSuccessResponse("ok");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Hmm, could not delete subscription?", ex);
//        }
    }



}
