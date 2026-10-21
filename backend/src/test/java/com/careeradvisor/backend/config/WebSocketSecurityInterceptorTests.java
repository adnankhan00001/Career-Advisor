package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ConversationParticipantRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.security.CustomUserDetailsService;
import com.careeradvisor.backend.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSecurityInterceptorTests {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private com.careeradvisor.backend.repository.UserRepository userRepository;

    @Mock
    private com.careeradvisor.backend.service.PresenceService presenceService;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketSecurityInterceptor interceptor;

    private User activeUser;
    private User suspendedUser;
    private User deactivatedUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(10L);
        activeUser.setName("Active User");
        activeUser.setEmail("active@example.com");
        activeUser.setPassword("hashed");
        activeUser.setRole(Role.USER);
        activeUser.setStatus(UserStatus.ACTIVE);

        suspendedUser = new User();
        suspendedUser.setId(11L);
        suspendedUser.setName("Suspended User");
        suspendedUser.setEmail("suspended@example.com");
        suspendedUser.setPassword("hashed");
        suspendedUser.setRole(Role.USER);
        suspendedUser.setStatus(UserStatus.SUSPENDED);

        deactivatedUser = new User();
        deactivatedUser.setId(12L);
        deactivatedUser.setName("Deactivated User");
        deactivatedUser.setEmail("deactivated@example.com");
        deactivatedUser.setPassword("hashed");
        deactivatedUser.setRole(Role.USER);
        deactivatedUser.setStatus(UserStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("CONNECT: Authenticates successfully when JWT is valid and user is active")
    void testConnect_Success() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer valid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.extractEmail("valid-token")).thenReturn("active@example.com");
        when(userDetailsService.loadUserByUsername("active@example.com")).thenReturn(CustomUserDetails.build(activeUser));

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("active@example.com", resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("CONNECT: Rejects connection when user account is suspended")
    void testConnect_RejectsSuspendedUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer suspended-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtils.validateToken("suspended-token")).thenReturn(true);
        when(jwtUtils.extractEmail("suspended-token")).thenReturn("suspended@example.com");
        when(userDetailsService.loadUserByUsername("suspended@example.com")).thenReturn(CustomUserDetails.build(suspendedUser));

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("CONNECT: Rejects connection when user account is deactivated")
    void testConnect_RejectsDeactivatedUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer deact-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtils.validateToken("deact-token")).thenReturn(true);
        when(jwtUtils.extractEmail("deact-token")).thenReturn("deactivated@example.com");
        when(userDetailsService.loadUserByUsername("deactivated@example.com")).thenReturn(CustomUserDetails.build(deactivatedUser));

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    @DisplayName("CONNECT: Rejects connection when token is invalid")
    void testConnect_RejectsInvalidToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer invalid-token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    @DisplayName("SUBSCRIBE: Allows participant to subscribe to conversation")
    void testSubscribe_AllowsParticipant() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Conversation conv = new Conversation();
        conv.setId(100L);
        conv.setType(ConversationType.USER_TO_USER);

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserId(100L, 10L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("SUBSCRIBE: Allows admin to subscribe to USER_TO_ADMIN support conversation")
    void testSubscribe_AllowsAdminOnUserToAdminConv() {
        User adminUser = new User();
        adminUser.setId(99L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails adminDetails = CustomUserDetails.build(adminUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/200");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Conversation conv = new Conversation();
        conv.setId(200L);
        conv.setType(ConversationType.USER_TO_ADMIN);

        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(conversationRepository.findById(200L)).thenReturn(Optional.of(conv));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("SUBSCRIBE: Forbids non-participant from subscribing to conversation")
    void testSubscribe_ForbidsNonParticipant() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Conversation conv = new Conversation();
        conv.setId(100L);
        conv.setType(ConversationType.USER_TO_USER);

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserId(100L, 10L)).thenReturn(false);

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("Forbidden"));
    }

    @Test
    @DisplayName("SUBSCRIBE: Rejects subscription if user account is suspended")
    void testSubscribe_RejectsSuspendedUser() {
        CustomUserDetails userDetails = CustomUserDetails.build(suspendedUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(userRepository.findById(11L)).thenReturn(Optional.of(suspendedUser));

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("SUBSCRIBE: Rejects subscription if user becomes suspended in DB after CONNECT")
    void testSubscribe_RejectsWhenSuspendedInDbAfterConnect() {
        // userDetails says active from CONNECT time
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // But in the DB, user was suspended!
        when(userRepository.findById(10L)).thenReturn(Optional.of(suspendedUser));

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("SEND: Allows active participant to send message")
    void testSend_AllowsActiveParticipant() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Conversation conv = new Conversation();
        conv.setId(100L);
        conv.setType(ConversationType.USER_TO_USER);

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserId(100L, 10L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("SEND: Rejects message send when user is suspended in DB")
    void testSend_RejectsSuspendedUser() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(userRepository.findById(10L)).thenReturn(Optional.of(suspendedUser));

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("suspended"));
    }

    @Test
    @DisplayName("SEND: Rejects message send when user is not participant in conversation")
    void testSend_RejectsNonParticipant() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat.send/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Conversation conv = new Conversation();
        conv.setId(100L);
        conv.setType(ConversationType.USER_TO_USER);

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));
        when(conversationRepository.findById(100L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserId(100L, 10L)).thenReturn(false);

        MessageDeliveryException ex = assertThrows(MessageDeliveryException.class, () ->
                interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("Forbidden"));
    }

    @Test
    @DisplayName("OUTBOUND MESSAGE: Allows delivery to active user")
    void testOutboundMessage_AllowsActiveRecipient() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeUser));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("OUTBOUND MESSAGE: Suppresses delivery (drops message) to suspended user")
    void testOutboundMessage_DropsSuspendedRecipient() {
        CustomUserDetails userDetails = CustomUserDetails.build(activeUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setDestination("/topic/conversations/100");
        accessor.setUser(auth);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(userRepository.findById(10L)).thenReturn(Optional.of(suspendedUser));

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNull(result, "Outbound message to suspended user must return null to drop delivery");
    }
}
