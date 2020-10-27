package edu.utexas.tacc.tapis.notifications.frontend;

import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ITenantManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class JWTAuthenticationHandshakeHandler extends DefaultHandshakeHandler {

    private static final String TAPIS_JWT_HEADER = "x-tapis-token";
    private ITenantManager tenantManager;

    @Autowired
    JWTAuthenticationHandshakeHandler(ITenantManager tenantManager) {
        super();
        this.tenantManager = tenantManager;
    };


    @Override
    protected Principal determineUser(ServerHttpRequest request, @NotNull WebSocketHandler wsHandler,
                                      @NotNull Map<String, Object> attributes) {

        String unverifiedJWT = request.getHeaders().getFirst(TAPIS_JWT_HEADER);
        if (unverifiedJWT == null) return null;

        TapisJWTValidator validator = new TapisJWTValidator(unverifiedJWT);

        try {
            Claims claims = validator.getClaimsNoValidation();
            String tenantId = (String) claims.get("tapis/tenant_id");
            return new AuthenticatedUser(
                (String) claims.get("tapis/username"),
                (String) claims.get("tapis/tenant_id"),
                (String) claims.get("tapis/account_type"),
                null,
                null,
                null,
                null,
                (String) claims.get("tapis/site_id"),
                null
            );
        } catch (JwtException ex) {
            return null;
        }







    }

}
