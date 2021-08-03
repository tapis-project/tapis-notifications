package edu.utexas.tacc.tapis.notifications.api.providers;


import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsPermissionsService;
import edu.utexas.tacc.tapis.notifications.lib.service.NotificationsService;
import edu.utexas.tacc.tapis.sharedapi.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

@TopicsAuthorization
public class TopicsAuthz implements ContainerRequestFilter {

        private Logger log = LoggerFactory.getLogger(TopicsAuthz.class);

        @Inject
        NotificationsPermissionsService permissionsService;

        @Inject
        NotificationsService notificationsService;


        @Context
        private ResourceInfo resourceInfo;

        @Override
        public void filter(ContainerRequestContext requestContext) throws WebApplicationException {

            //This will be the annotation on the api method, which is one of the FilePermissionsEnum values
            TopicsAuthorization requiredPerms = resourceInfo.getResourceMethod().getAnnotation(TopicsAuthorization.class);

            final AuthenticatedUser user = (AuthenticatedUser) requestContext.getSecurityContext().getUserPrincipal();
            String username = user.getName();
            String tenantId = user.getTenantId();
            MultivaluedMap<String, String> params = requestContext.getUriInfo().getPathParameters();
            String topicName = params.getFirst("topicName");

            // TODO
//TODO            try {
//                Topic topic = notificationsService.getTopic(tenantId, topicName);
//                if (topic == null) {
//                    String msg = String.format("Could not find topic %s", topicName);
//                    throw new NotFoundException(msg);
//                }
//
//                boolean isPermitted = permissionsService.isPermitted(tenantId, topicName, username, user.getAccountType());
//                if (!isPermitted) {
//                    throw new NotAuthorizedException("Authorization failed.");
//                }
//            } catch (ServiceException e) {
//                // This should only happen when there is a network issue.
//                log.error("ERROR: Files authorization failed", e);
//                throw new WebApplicationException(e.getMessage());
//            }
        }

}
