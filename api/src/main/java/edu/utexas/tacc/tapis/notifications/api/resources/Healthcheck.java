package edu.utexas.tacc.tapis.notifications.api.resources;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/v3/notifications/health/healthcheck")
public class Healthcheck {

    @GET
    @Path("/")
    @Operation(summary = "Am I alive?", tags = {"health"})
    public Response health(
        @Context SecurityContext securityContext
    ) {
        return Response.ok("alive").build();
    }
}
