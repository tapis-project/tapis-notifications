package edu.utexas.tacc.tapis.notifications.frontend;

import edu.utexas.tacc.tapis.sharedapi.security.ITenantManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final static Logger logger = LoggerFactory.getLogger(WebSocketConfig.class.getName());

	@Autowired
	ITenantManager tenantManager;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(new SocketHandler(), "/")
			.setHandshakeHandler(new JWTAuthenticationHandshakeHandler(tenantManager));
	}

}