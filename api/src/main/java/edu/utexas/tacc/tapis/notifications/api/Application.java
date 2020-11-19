package edu.utexas.tacc.tapis.notifications.api;

import edu.utexas.tacc.tapis.notifications.api.factories.ServiceJWTCacheFactory;
import edu.utexas.tacc.tapis.notifications.api.factories.TenantCacheFactory;
import edu.utexas.tacc.tapis.notifications.api.resources.Healthcheck;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.sharedapi.security.ServiceJWT;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;

// The path here is appended to the context root and
// is configured to work when invoked in a standalone
// container (command line) and in an IDE (eclipse).
@OpenAPIDefinition(
    info = @Info(
        title = "Tapis Notifications API",
        version = "1.0",
        description = "Tapis Notifications API definition",
        license = @License(name = "Apache 2.0", url = "http://foo.bar"),
        contact = @Contact(url = "http://tacc.utexas.edu", name = "CicSupport", email = "cicsupport@tacc.utexas.edu")
    ),
    tags = {
        @Tag(name = "topics"),
    },
    security = {
        @SecurityRequirement(name = "Bearer"),
    },
    servers = {
        @Server(
            description = "localhost",
            url = "http://localhost:8080/"
        ),
        @Server(
            description = "development",
            url = "https://dev.develop.tapis.io"
        )
    }
)
@ApplicationPath("v3/notifications")
public class Application extends ResourceConfig {

    public Application() {
        super();
        JWTValidateRequestFilter.setSiteId("tacc");
        JWTValidateRequestFilter.setService("notifications");
        setApplicationName("files");
        //OpenAPI jazz
        register(JWTValidateRequestFilter.class);
        register(OpenApiResource.class);
        register(Healthcheck.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(ServiceJWTCacheFactory.class).to(ServiceJWT.class).in(Singleton.class);
                bindFactory(TenantCacheFactory.class).to(TenantManager.class).in(Singleton.class);
            }
        });



    }


    public static void main(String[] args) {

    }



}
