package edu.utexas.tacc.tapis.notifications.api.resources;


import io.swagger.v3.oas.annotations.Operation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/v3/notifications/health/healthcheck")
public class Healthcheck {

    @GET
    @Operation(summary = "Am I alive?", tags = {"health"})
    public Response health(
        @Context SecurityContext securityContext
    ) {
        return Response.ok("alive").build();
    }
}
