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


	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptorAdapter() {

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);

				if (StompCommand.CONNECT.equals(accessor.getCommand())) {
					String authToken = accessor.getFirstNativeHeader("Authentication");
					String jwt = JwtUtils.resolveToken(authToken);
					if (jwtTokenProvider.validateToken(jwt)) {
						Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
						accessor.setUser(authentication);
						String itemId = accessor.getFirstNativeHeader("item_id");
						accessor.setDestination("/topic" + privateChatService.getChannelId(itemId, authentication.getName()));
						logger.info(accessor.getDestination()); //ex: /topic/chat/3434/chat_with/user3797474342423
					}
				}
				return message;
			}
		});
	}