package com.hostelcomplaint.config;

import java.security.Principal;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.repository.AppUserRepository;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern STAFF_TOPIC =
            Pattern.compile("^/topic/staff/(\\d+)$");

    private final AppUserRepository userRepository;

    public WebSocketConfig(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();

                if (command == StompCommand.CONNECT) {
                    requireAuthenticatedPrincipal(accessor.getUser());
                }

                if (command == StompCommand.SEND) {
                    // This project currently sends updates from the server.
                    // Reject client-originated STOMP messages.
                    throw new AccessDeniedException(
                            "Client-originated STOMP messages are not allowed");
                }

                if (command == StompCommand.SUBSCRIBE) {
                    authorizeSubscription(
                            accessor.getUser(),
                            accessor.getDestination());
                }

                return message;
            }
        });
    }

    private void requireAuthenticatedPrincipal(Principal principal) {
        if (!(principal instanceof Authentication)) {
            throw new AccessDeniedException(
                    "An authenticated application session is required");
        }

        Authentication authentication = (Authentication) principal;

        if (!authentication.isAuthenticated()
                || authentication.getAuthorities().contains(
                        new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            throw new AccessDeniedException(
                    "An authenticated application session is required");
        }

        loadUser(authentication);
    }

    private void authorizeSubscription(
            Principal principal, String destination) {

        if (!(principal instanceof Authentication)) {
            throw new AccessDeniedException(
                    "Please log in before subscribing");
        }

        Authentication authentication = (Authentication) principal;

        if (!authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Please log in before subscribing");
        }

        AppUser user = loadUser(authentication);

        if (destination == null) {
            throw new AccessDeniedException(
                    "A subscription destination is required");
        }

        if ("/topic/complaints".equals(destination)) {
            if ("WARDEN".equals(user.getRole())
                    || "ADMIN".equals(user.getRole())) {
                return;
            }

            throw new AccessDeniedException(
                    "Only wardens and admins can subscribe to complaint updates");
        }

        java.util.regex.Matcher matcher =
                STAFF_TOPIC.matcher(destination);

        if (matcher.matches()) {
            if (!"STAFF".equals(user.getRole())
                    || user.getStaffId() == null) {
                throw new AccessDeniedException(
                        "This staff notification topic is not available to your role");
            }

            Long requestedStaffId = Long.valueOf(matcher.group(1));

            if (!Objects.equals(user.getStaffId(), requestedStaffId)) {
                throw new AccessDeniedException(
                        "You can only subscribe to your own staff notifications");
            }

            return;
        }

        throw new AccessDeniedException(
                "Subscription to this destination is not permitted");
    }

    private AppUser loadUser(Authentication authentication) {
        return userRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "The authenticated account could not be found"));
    }
}