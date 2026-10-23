package com.careeradvisor.backend.service;

import com.careeradvisor.backend.ai.config.AiConfigProperties;
import com.careeradvisor.backend.dto.ChatMessageDto;
import com.careeradvisor.backend.dto.ConversationResponseDto;
import com.careeradvisor.backend.dto.UpdateSupportStatusRequest;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ChatMessageRepository;
import com.careeradvisor.backend.repository.ConversationParticipantRepository;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportInboxServiceTests {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ConversationParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RealTimeMessagingService messagingService;

    @Mock
    private PresenceService presenceService;

    @Mock
    private AiConfigProperties aiConfigProperties;

    @Mock
    private AdminAuditLogService auditLogService;

    @InjectMocks
    private ConversationService conversationService;

    private User adminUser;
    private User studentUser;
    private Conversation openSupportTicket;
    private Conversation resolvedSupportTicket;
    private Conversation peerConversation;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .name("Admin Boss")
                .email("admin@careeradvisor.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        studentUser = User.builder()
                .id(10L)
                .name("Charlie Student")
                .email("charlie@student.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        openSupportTicket = Conversation.builder()
                .id(101L)
                .owner(studentUser)
                .type(ConversationType.USER_TO_ADMIN)
                .title("Support: Resume Analyzer Issue")
                .archived(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .lastMessageAt(LocalDateTime.now().minusHours(2))
                .build();

        resolvedSupportTicket = Conversation.builder()
                .id(102L)
                .owner(studentUser)
                .type(ConversationType.USER_TO_ADMIN)
                .title("Support: Account Verification Request")
                .archived(true)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .lastMessageAt(LocalDateTime.now().minusDays(1))
                .build();

        peerConversation = Conversation.builder()
                .id(201L)
                .owner(studentUser)
                .type(ConversationType.USER_TO_USER)
                .title("Charlie & Bob")
                .archived(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("getAdminInbox returns all support tickets for admin")
    void testGetAdminInboxAll() {
        when(conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.USER_TO_ADMIN))
                .thenReturn(List.of(openSupportTicket, resolvedSupportTicket));
        when(chatMessageRepository.countByConversation(any(Conversation.class))).thenReturn(2L);
        when(participantRepository.findByConversation(any(Conversation.class))).thenReturn(Collections.emptyList());

        List<ConversationResponseDto> result = conversationService.getAdminInbox(adminUser, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getAdminInbox filters by OPEN status")
    void testGetAdminInboxFilterOpen() {
        when(conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.USER_TO_ADMIN))
                .thenReturn(List.of(openSupportTicket, resolvedSupportTicket));
        when(chatMessageRepository.countByConversation(any(Conversation.class))).thenReturn(2L);
        when(participantRepository.findByConversation(any(Conversation.class))).thenReturn(Collections.emptyList());

        List<ConversationResponseDto> result = conversationService.getAdminInbox(adminUser, null, "OPEN");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
        assertFalse(result.get(0).isArchived());
    }

    @Test
    @DisplayName("getAdminInbox filters by RESOLVED status")
    void testGetAdminInboxFilterResolved() {
        when(conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.USER_TO_ADMIN))
                .thenReturn(List.of(openSupportTicket, resolvedSupportTicket));
        when(chatMessageRepository.countByConversation(any(Conversation.class))).thenReturn(2L);
        when(participantRepository.findByConversation(any(Conversation.class))).thenReturn(Collections.emptyList());

        List<ConversationResponseDto> result = conversationService.getAdminInbox(adminUser, null, "RESOLVED");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(102L, result.get(0).getId());
        assertTrue(result.get(0).isArchived());
    }

    @Test
    @DisplayName("getAdminInbox filters by search term on requester name")
    void testGetAdminInboxSearch() {
        when(conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.USER_TO_ADMIN))
                .thenReturn(List.of(openSupportTicket, resolvedSupportTicket));
        when(chatMessageRepository.countByConversation(any(Conversation.class))).thenReturn(2L);
        when(participantRepository.findByConversation(any(Conversation.class))).thenReturn(Collections.emptyList());

        List<ConversationResponseDto> result = conversationService.getAdminInbox(adminUser, "Resume", "ALL");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
    }

    @Test
    @DisplayName("getAdminInbox rejects non-admin caller")
    void testGetAdminInboxRejectsNonAdmin() {
        assertThrows(ResourceNotFoundException.class, () ->
                conversationService.getAdminInbox(studentUser, null, null)
        );
    }

    @Test
    @DisplayName("updateSupportStatus successfully resolves open support ticket and logs audit")
    void testUpdateSupportStatusResolveSuccess() {
        when(conversationRepository.findById(101L)).thenReturn(Optional.of(openSupportTicket));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("RESOLVED")
                .note("Issue addressed with client")
                .build();

        ConversationResponseDto result = conversationService.updateSupportStatus(101L, adminUser, req);

        assertNotNull(result);
        assertTrue(openSupportTicket.isArchived());
        verify(conversationRepository, times(1)).save(openSupportTicket);
        verify(auditLogService, times(1)).recordAction(
                eq(1L), isNull(), isNull(),
                eq(AdminAuditAction.SUPPORT_TICKET_RESOLVED),
                eq(AdminAuditTargetType.SUPPORT_CONVERSATION),
                eq("101"),
                contains("Resolved support ticket #101"),
                anyMap()
        );
    }

    @Test
    @DisplayName("updateSupportStatus rejects resolving already resolved ticket")
    void testUpdateSupportStatusRejectAlreadyResolved() {
        when(conversationRepository.findById(102L)).thenReturn(Optional.of(resolvedSupportTicket));

        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("RESOLVED")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                conversationService.updateSupportStatus(102L, adminUser, req)
        );
        assertTrue(ex.getMessage().contains("already resolved"));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSupportStatus successfully reopens resolved support ticket and logs audit")
    void testUpdateSupportStatusReopenSuccess() {
        when(conversationRepository.findById(102L)).thenReturn(Optional.of(resolvedSupportTicket));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(i -> i.getArgument(0));

        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("OPEN")
                .note("User requested further clarification")
                .build();

        ConversationResponseDto result = conversationService.updateSupportStatus(102L, adminUser, req);

        assertNotNull(result);
        assertFalse(resolvedSupportTicket.isArchived());
        verify(conversationRepository, times(1)).save(resolvedSupportTicket);
        verify(auditLogService, times(1)).recordAction(
                eq(1L), isNull(), isNull(),
                eq(AdminAuditAction.SUPPORT_TICKET_REOPENED),
                eq(AdminAuditTargetType.SUPPORT_CONVERSATION),
                eq("102"),
                contains("Reopened support ticket #102"),
                anyMap()
        );
    }

    @Test
    @DisplayName("updateSupportStatus rejects reopening already open ticket")
    void testUpdateSupportStatusRejectAlreadyOpen() {
        when(conversationRepository.findById(101L)).thenReturn(Optional.of(openSupportTicket));

        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("OPEN")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                conversationService.updateSupportStatus(101L, adminUser, req)
        );
        assertTrue(ex.getMessage().contains("already open"));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSupportStatus rejects non-support conversation")
    void testUpdateSupportStatusRejectNonSupport() {
        when(conversationRepository.findById(201L)).thenReturn(Optional.of(peerConversation));

        UpdateSupportStatusRequest req = UpdateSupportStatusRequest.builder()
                .status("RESOLVED")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                conversationService.updateSupportStatus(201L, adminUser, req)
        );
        assertTrue(ex.getMessage().contains("is not a support ticket"));
    }

    @Test
    @DisplayName("sendHumanMessage records SUPPORT_REPLY_SENT audit log on admin reply")
    void testSendHumanMessageAuditLogOnAdminReply() {
        when(conversationRepository.findById(101L)).thenReturn(Optional.of(openSupportTicket));
        when(chatMessageRepository.countByConversation(openSupportTicket)).thenReturn(1L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> {
            ChatMessage m = i.getArgument(0);
            m.setId(999L);
            return m;
        });

        ChatMessageDto msg = conversationService.sendHumanMessage(101L, adminUser, "Hello Charlie, we are investigating your issue.");

        assertNotNull(msg);
        assertEquals(999L, msg.getId());
        assertEquals("ADMIN", msg.getSenderType());
        verify(auditLogService, times(1)).recordAction(
                eq(1L), isNull(), isNull(),
                eq(AdminAuditAction.SUPPORT_REPLY_SENT),
                eq(AdminAuditTargetType.SUPPORT_CONVERSATION),
                eq("101"),
                contains("Admin Boss sent reply in support ticket #101"),
                anyMap()
        );
    }

    @Test
    @DisplayName("sendHumanMessage rejects sending message to resolved/archived ticket")
    void testSendHumanMessageRejectsArchived() {
        when(conversationRepository.findById(102L)).thenReturn(Optional.of(resolvedSupportTicket));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                conversationService.sendHumanMessage(102L, adminUser, "Attempted reply")
        );
        assertTrue(ex.getMessage().contains("Cannot send message to an archived conversation"));
    }
}
