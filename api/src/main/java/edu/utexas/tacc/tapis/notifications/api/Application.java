package edu.utexas.tacc.tapis.notifications.api;

import edu.utexas.tacc.tapis.notifications.api.factories.ServiceJWTCacheFactory;
import edu.utexas.tacc.tapis.notifications.api.factories.TenantCacheFactory;
import edu.utexas.tacc.tapis.notifications.api.resources.Healthcheck;
import edu.utexas.tacc.tapis.notifications.api.resources.TopicsResource;
import edu.utexas.tacc.tapis.notifications.lib.config.IRuntimeConfig;
import edu.utexas.tacc.tapis.notifications.lib.config.RuntimeSettings;
import edu.utexas.tacc.tapis.sharedapi.jaxrs.filters.JWTValidateRequestFilter;
import edu.utexas.tacc.tapis.shared.security.ServiceJWT;
import edu.utexas.tacc.tapis.shared.security.TenantManager;
import edu.utexas.tacc.tapis.sharedapi.providers.ObjectMapperContextResolver;
import edu.utexas.tacc.tapis.sharedapi.providers.TapisExceptionMapper;
import edu.utexas.tacc.tapis.sharedapi.providers.ValidationExceptionMapper;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import java.net.URI;

// The path here is appended to the context root and
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
        @Tag(name = "subscriptions"),
        @Tag(name = "health"),
        @Tag(name = "queues")
    },
    security = {
        @SecurityRequirement(name = "x-tapis-token"),
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

    IRuntimeConfig runtimeConfig = RuntimeSettings.get();

    public Application() {
        super();
        JWTValidateRequestFilter.setSiteId(runtimeConfig.getSiteId());
        JWTValidateRequestFilter.setService("notifications");
        setApplicationName("files");
        //OpenAPI jazz
        register(JWTValidateRequestFilter.class);
        register(OpenApiResource.class);
        register(Healthcheck.class);
        register(TopicsResource.class);
        // Serialization
        register(JacksonFeature.class);
        // Custom Timestamp/Instant serialization
        register(ObjectMapperContextResolver.class);
        // ExceptionMappers, need both because ValidationMapper is a custom Jersey thing and
        // can't be implemented in a generic mapper
        register(TapisExceptionMapper.class);
        register(ValidationExceptionMapper.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(ServiceJWTCacheFactory.class).to(ServiceJWT.class).in(Singleton.class);
                bindFactory(TenantCacheFactory.class).to(TenantManager.class).in(Singleton.class);
            }
        });

    }


    public static void main(String[] args) throws Exception {
        final URI BASE_URI = URI.create("http://0.0.0.0:8080/");
        Application config = new Application();

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);
        server.start();
    }



}
