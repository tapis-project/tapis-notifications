package edu.utexas.tacc.tapis.notifications.api.resources;


import edu.utexas.tacc.tapis.notifications.api.model.CreateQueueRequest;
import edu.utexas.tacc.tapis.notifications.lib.model.Queue;
import edu.utexas.tacc.tapis.notifications.lib.model.Topic;
import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsService;
import edu.utexas.tacc.tapis.notifications.lib.service.QueueService;
import edu.utexas.tacc.tapis.sharedapi.responses.TapisResponse;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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

@Path("/v3/notifications/queues/")
public class QueuesResource {

    private static class QueueResponse extends TapisResponse<List<Topic>> {}

    @Inject
    NotificationsService notificationsService;

    @Inject
    QueueService queueService;


    @GET
    @Operation(summary = "Get a listing of all queues.", tags = {"queues"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<List<Topic>> getQueues(
        @Parameter(description = "pagination limit", example = "100") @DefaultValue("1000") @QueryParam("limit") @Max(1000) int limit,
        @Parameter(description = "pagination offset", example = "1000") @DefaultValue("0") @QueryParam("offset") @Min(0) long offset,
        @Context SecurityContext securityContext
    ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();

        List<Topic> topicListing = new ArrayList<>();
        TapisResponse<List<Topic>> resp = TapisResponse.createSuccessResponse("ok", topicListing);
        return resp;
    }

    @POST
    @Operation(summary = "Create a new queue.", tags = {"queues"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<Queue> createQueue(
        @Context SecurityContext securityContext,
        @NotNull  @Valid CreateQueueRequest queueRequest
        ) {
        AuthenticatedUser user = (AuthenticatedUser) securityContext.getUserPrincipal();

        Queue queue = new Queue();
        queue.setName(queueRequest.getName());
        queue.setTenantId(user.getTenantId());
        queue.setOwner(user.getName());
// TODO
      return null;
//TODO        try {
//            queue = notificationsService.createQueue(queue);
//            TapisResponse<Queue> resp = TapisResponse.createSuccessResponse("ok", queue);
//            return resp;
//        } catch (DuplicateEntityException ex) {
//            throw new BadRequestException("A queue with this name already exists, please choose another name");
//        } catch (ServiceException ex) {
//            throw new WebApplicationException("Something went wrong, could not create queue");
//        }
    }

    @DELETE
    @Path("/{queueId}")
    @Operation(summary = "Delete a queue.", tags = {"queues"})
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "OK"),
        @ApiResponse(
            responseCode = "401",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authenticated"),
        @ApiResponse(
            responseCode = "403",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Not Authorized"),
        @ApiResponse(
            responseCode = "500",
            content = @Content(schema = @Schema(implementation = QueueResponse.class)),
            description = "Internal Error")
    })
    public TapisResponse<String> deleteQueue(
        @Parameter(description = "ID of the queue", required = true, example = "queuue12345") @PathParam("queueId") String topicID,
        @Context SecurityContext securityContext
    ) {
        return TapisResponse.createSuccessResponse("ok");
    }




}
