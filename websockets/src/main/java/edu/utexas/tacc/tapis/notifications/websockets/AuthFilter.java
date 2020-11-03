package edu.utexas.tacc.tapis.notifications.websockets;

import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import edu.utexas.tacc.tapis.sharedapi.security.ITenantManager;
import edu.utexas.tacc.tapis.sharedapi.security.TenantManager;
import edu.utexas.tacc.tapis.tenants.client.gen.model.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Objects;

@Service
@WebFilter("/v3/notifications/*")
public class AuthFilter implements Filter {

    private TenantManager tenantManager;

    public AuthFilter(){}

    @Inject
    public AuthFilter(TenantManager tenantManager){
        this.tenantManager = tenantManager;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String encodedJWT = request.getHeader("x-tapis-token");

        if (encodedJWT == null || encodedJWT.trim().isEmpty()) {
            returnForbiddenError(response, "An access token is required to connect");
            return;
        }

        TapisJWTValidator validator = new TapisJWTValidator(encodedJWT);
        Claims claims = validator.getClaimsNoValidation();
        String tenantId = (String) claims.get(TapisClaims.TAPIS_TENANT);

        try {
            Tenant tenant = tenantManager.getTenant(tenantId);
            Jws<Claims> jwt = validator.validate(tenant.getPublicKey());
            AuthenticatedUser user = new AuthenticatedUser(
                (String) jwt.getBody().get(TapisClaims.TAPIS_USERNAME),
                (String) jwt.getBody().get(TapisClaims.TAPIS_TENANT),
                (String) jwt.getBody().get(TapisClaims.TAPIS_ACCOUNT_TYPE),
                (String) jwt.getBody().get(TapisClaims.TAPIS_DELEGATION),
                null,
                null,
                null,
                (String) jwt.getBody().get(TapisClaims.TAPIS_TARGET_SITE),
                encodedJWT
            );
            filterChain.doFilter(new AuthenticatedRequest(request, user), servletResponse);
        } catch (Exception ex) {
            returnForbiddenError(response, "Invalid access token");
        }

    }

    private void returnForbiddenError(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    @Override
    public void destroy() {

    }

    private static class AuthenticatedRequest extends HttpServletRequestWrapper {

        private Principal user;

        public AuthenticatedRequest(HttpServletRequest request, Principal user) {
            super(request);
            this.user = Objects.requireNonNull(user);
        }

        @Override
        public Principal getUserPrincipal() {
            return user;
        }
    }


}