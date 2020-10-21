package edu.utexas.tacc.tapis.notifications.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final static Logger logger = LoggerFactory.getLogger(WebSocketConfig.class.getName());

//	@Autowired
//	private JwtTokenProvider jwtTokenProvider;
//
//	@Autowired
//	private PrivateChatService privateChatService;
//
//	private static final String MESSAGE_PREFIX = "/topic";
//	private static final String END_POINT = "/chat";
//	private static final String APPLICATION_DESTINATION_PREFIX = "/live";

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(new SocketHandler(), "/");
	}

}