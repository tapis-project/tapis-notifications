package edu.utexas.tacc.tapis.notifications.frontend;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.security.Principal;
import java.util.Map;

public class JWTAuthenticationHandshakeHandler extends DefaultHandshakeHandler {

    private static final String TAPIS_JWT_HEADER = "x-tapis-token";

    @Autowired
    ITenantManager

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        String unverifiedJWT = request.getHeaders().getFirst(TAPIS_JWT_HEADER);
        if (unverifiedJWT == null) return null;

        TapisJWTValidator validator = new TapisJWTValidator(unverifiedJWT);

        try {
            Claims claims = validator.getClaimsNoValidation();
            String tenantId = (String) claims.get("tapis/tenant_id");

        }







    }

}
