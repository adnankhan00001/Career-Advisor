package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMentorshipServiceTests {

    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private AdminMentorshipService adminMentorshipService;

    private User student;
    private User mentorUser;
    private MentorProfile mentorProfile;
    private MentorshipRequest acceptedRequest;
    private MentorshipRequest pendingRequest;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(10L)
                .name("Alice Candidate")
                .email("alice@student.com")
                .careerGoal("Full Stack Engineer")
                .userLevel("Intermediate")
                .build();

        mentorUser = User.builder()
                .id(20L)
                .name("Bob Mentor")
                .email("bob@mentor.com")
                .build();

        mentorProfile = MentorProfile.builder()
                .id(30L)
                .user(mentorUser)
                .company("Acme Corp")
                .jobTitle("Principal Engineer")
                .maxMentees(5)
                .verificationStatus(MentorVerificationStatus.APPROVED)
                .isActive(true)
                .build();

        acceptedRequest = MentorshipRequest.builder()
                .id(100L)
                .student(student)
                .mentor(mentorProfile)
                .status(MentorshipRequestStatus.ACCEPTED)
                .goalDescription("Seeking career coaching")
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();

        pendingRequest = MentorshipRequest.builder()
                .id(101L)
                .student(student)
                .mentor(mentorProfile)
                .status(MentorshipRequestStatus.PENDING)
                .goalDescription("Review resume and system design")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("getMentorshipSummary computes accurate counts and active mentorships")
    void testGetMentorshipSummary() {
        when(mentorshipRequestRepository.count()).thenReturn(25L);
        List<Object[]> grouped = List.of(
                new Object[]{MentorshipRequestStatus.PENDING, 5L},
                new Object[]{MentorshipRequestStatus.ACCEPTED, 10L},
                new Object[]{MentorshipRequestStatus.COMPLETED, 7L},
                new Object[]{MentorshipRequestStatus.DECLINED, 2L},
                new Object[]{MentorshipRequestStatus.TERMINATED, 1L}
        );
        when(mentorshipRequestRepository.countGroupedByStatus()).thenReturn(grouped);

        MentorshipOversightSummaryDto summary = adminMentorshipService.getMentorshipSummary();

        assertNotNull(summary);
        assertEquals(25L, summary.getTotalRequests());
        assertEquals(5L, summary.getPendingRequests());
        assertEquals(10L, summary.getActiveMentorships());
        assertEquals(10L, summary.getAcceptedRequests());
        assertEquals(7L, summary.getCompletedRequests());
        assertEquals(2L, summary.getDeclinedRequests());
        assertEquals(1L, summary.getTerminatedRequests());
    }

    @Test
    @DisplayName("getMentorshipRequests returns mapped page with search and status filters")
    void testGetMentorshipRequests() {
        Conversation conv = Conversation.builder()
                .id(500L)
                .title("Coaching Session")
                .lastMessageAt(LocalDateTime.now().minusHours(2))
                .build();
        acceptedRequest.setConversation(conv);

        Page<MentorshipRequest> mockPage = new PageImpl<>(List.of(acceptedRequest), PageRequest.of(0, 10), 1);
        when(mentorshipRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        PageResponse<AdminMentorshipRequestDto> result = adminMentorshipService.getMentorshipRequests(
                0, 10, "createdAt,desc", "Alice", MentorshipRequestStatus.ACCEPTED, null, null
        );

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        AdminMentorshipRequestDto dto = result.getContent().get(0);
        assertEquals(100L, dto.getId());
        assertEquals("Alice Candidate", dto.getStudentName());
        assertEquals("Bob Mentor", dto.getMentorName());
        assertEquals(MentorshipRequestStatus.ACCEPTED, dto.getStatus());
        assertEquals(500L, dto.getConversationId());
        assertNotNull(dto.getLastActivityAt());
    }

    @Test
    @DisplayName("getMentorshipDetail returns rich detail DTO for existing request")
    void testGetMentorshipDetailSuccess() {
        Conversation conv = Conversation.builder()
                .id(500L)
                .title("Mentorship Chat")
                .lastMessageAt(LocalDateTime.now().minusHours(2))
                .build();
        acceptedRequest.setConversation(conv);

        when(mentorshipRequestRepository.findById(100L)).thenReturn(Optional.of(acceptedRequest));
        when(userSkillRepository.findByUser(student)).thenReturn(List.of(UserSkill.builder().skillName("Java").build()));
        when(chatMessageRepository.countByConversation(conv)).thenReturn(14L);
        when(mentorshipRequestRepository.countByMentorIdAndStatus(30L, MentorshipRequestStatus.ACCEPTED)).thenReturn(2L);

        AdminMentorshipDetailDto detail = adminMentorshipService.getMentorshipDetail(100L);

        assertNotNull(detail);
        assertEquals(100L, detail.getId());
        assertEquals("Alice Candidate", detail.getStudentName());
        assertEquals("Bob Mentor", detail.getMentorName());
        assertTrue(detail.getStudentSkills().contains("Java"));
        assertEquals(5, detail.getMentorMaxMentees());
        assertEquals(2L, detail.getMentorActiveMenteesCount());
        assertEquals(14L, detail.getMessageCount());
        assertEquals(500L, detail.getConversationId());
        assertEquals("Mentorship Chat", detail.getConversationTitle());
    }

    @Test
    @DisplayName("getMentorshipDetail throws ResourceNotFoundException for unknown ID")
    void testGetMentorshipDetailNotFound() {
        when(mentorshipRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminMentorshipService.getMentorshipDetail(999L)
        );
    }

    @Test
    @DisplayName("terminateMentorship successfully terminates ACCEPTED mentorship, sets audit reason, and frees capacity")
    void testTerminateMentorshipSuccessOnAccepted() {
        when(mentorshipRequestRepository.findById(100L)).thenReturn(Optional.of(acceptedRequest));
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("Inappropriate communication reported by candidate")
                .build();

        AdminMentorshipDetailDto result = adminMentorshipService.terminateMentorship(1L, 100L, req);

        assertNotNull(result);
        assertEquals(MentorshipRequestStatus.TERMINATED, result.getStatus());
        assertEquals(MentorshipRequestStatus.TERMINATED, acceptedRequest.getStatus());
        assertTrue(acceptedRequest.getResponseNote().contains("[ADMIN TERMINATED]: Inappropriate communication reported by candidate"));
        verify(mentorshipRequestRepository, times(1)).save(acceptedRequest);
    }

    @Test
    @DisplayName("terminateMentorship successfully terminates PENDING request")
    void testTerminateMentorshipSuccessOnPending() {
        when(mentorshipRequestRepository.findById(101L)).thenReturn(Optional.of(pendingRequest));
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("Spam duplicate request")
                .build();

        AdminMentorshipDetailDto result = adminMentorshipService.terminateMentorship(1L, 101L, req);

        assertNotNull(result);
        assertEquals(MentorshipRequestStatus.TERMINATED, result.getStatus());
        assertEquals(MentorshipRequestStatus.TERMINATED, pendingRequest.getStatus());
    }

    @Test
    @DisplayName("terminateMentorship throws IllegalStateException when terminating COMPLETED request")
    void testTerminateMentorshipThrowsOnCompleted() {
        MentorshipRequest completed = MentorshipRequest.builder()
                .id(102L)
                .status(MentorshipRequestStatus.COMPLETED)
                .build();
        when(mentorshipRequestRepository.findById(102L)).thenReturn(Optional.of(completed));

        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("Attempting termination on finished cycle")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminMentorshipService.terminateMentorship(1L, 102L, req)
        );
        assertTrue(ex.getMessage().contains("Cannot terminate a mentorship request that is already in state: COMPLETED"));
        verify(mentorshipRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("terminateMentorship throws IllegalArgumentException when reason is empty")
    void testTerminateMentorshipThrowsOnEmptyReason() {
        TerminateMentorshipRequest req = TerminateMentorshipRequest.builder()
                .reason("   ")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminMentorshipService.terminateMentorship(1L, 100L, req)
        );
        assertTrue(ex.getMessage().contains("A meaningful termination reason is mandatory"));
        verify(mentorshipRequestRepository, never()).save(any());
    }
}
