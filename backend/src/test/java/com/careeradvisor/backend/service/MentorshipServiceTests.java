package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.DuplicateResourceException;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.repository.MentorProfileRepository;
import com.careeradvisor.backend.repository.MentorshipRequestRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorshipServiceTests {

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ConversationRepository conversationRepository;

    private MentorshipCapacityConfigService capacityConfigService;
    private MentorProfileService mentorProfileService;
    private MentorshipRequestService mentorshipRequestService;

    private User studentUser;
    private User mentorUser;
    private MentorProfile approvedMentor;

    @BeforeEach
    void setUp() {
        capacityConfigService = new MentorshipCapacityConfigService(1, 10, 3);

        mentorProfileService = new MentorProfileService(
                mentorProfileRepository,
                mentorshipRequestRepository,
                userRepository,
                capacityConfigService
        );

        mentorshipRequestService = new MentorshipRequestService(
                mentorshipRequestRepository,
                mentorProfileRepository,
                userRepository,
                conversationService,
                conversationRepository
        );

        studentUser = User.builder()
                .id(101L)
                .name("Alice Student")
                .email("alice@student.com")
                .role(Role.USER)
                .build();

        mentorUser = User.builder()
                .id(202L)
                .name("Bob Mentor")
                .email("bob@mentor.com")
                .role(Role.USER)
                .build();

        approvedMentor = MentorProfile.builder()
                .id(1L)
                .user(mentorUser)
                .jobTitle("Senior Staff Engineer")
                .company("Netflix")
                .yearsOfExperience(10)
                .bio("Specialist in cloud architectures and Java Spring Boot")
                .expertiseAreas("Java, Spring Boot, Microservices")
                .maxMentees(3)
                .verificationStatus(MentorVerificationStatus.APPROVED)
                .isActive(true)
                .build();
    }

    // ================================================================
    // MENTOR PROFILE SERVICE TESTS
    // ================================================================

    @Test
    @DisplayName("Submit mentor application creates profile with APPLIED status and validated capacity")
    void testSubmitApplication_Success() {
        when(mentorProfileRepository.existsByUserId(studentUser.getId())).thenReturn(false);
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> {
            MentorProfile p = i.getArgument(0);
            p.setId(50L);
            return p;
        });

        MentorApplicationDto dto = MentorApplicationDto.builder()
                .jobTitle("Backend Engineer")
                .company("Acme Corp")
                .yearsOfExperience(4)
                .bio("Ready to help newcomers.")
                .expertiseAreas("Java, MySQL")
                .maxMentees(2)
                .linkedinUrl("https://linkedin.com/in/alice")
                .githubUrl("https://github.com/alice")
                .build();

        MentorProfileDto result = mentorProfileService.submitApplication(studentUser.getId(), dto);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("Backend Engineer", result.getJobTitle());
        assertEquals(MentorVerificationStatus.APPLIED, result.getVerificationStatus());
        assertEquals(2, result.getMaxMentees());
        assertTrue(result.isActive());
        verify(mentorProfileRepository, times(1)).save(any(MentorProfile.class));
    }

    @Test
    @DisplayName("Submit application throws DuplicateResourceException if profile already exists")
    void testSubmitApplication_DuplicateThrowsException() {
        when(mentorProfileRepository.existsByUserId(studentUser.getId())).thenReturn(true);

        MentorApplicationDto dto = MentorApplicationDto.builder()
                .jobTitle("Backend Engineer")
                .company("Acme Corp")
                .yearsOfExperience(4)
                .expertiseAreas("Java")
                .build();

        assertThrows(DuplicateResourceException.class, () ->
                mentorProfileService.submitApplication(studentUser.getId(), dto));
    }

    @Test
    @DisplayName("Submit application throws IllegalArgumentException for invalid URL format")
    void testSubmitApplication_InvalidUrl_ThrowsException() {
        when(mentorProfileRepository.existsByUserId(studentUser.getId())).thenReturn(false);
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));

        MentorApplicationDto dto = MentorApplicationDto.builder()
                .jobTitle("Backend Engineer")
                .company("Acme Corp")
                .yearsOfExperience(4)
                .expertiseAreas("Java")
                .linkedinUrl("invalid-url-without-http")
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                mentorProfileService.submitApplication(studentUser.getId(), dto));
    }

    @Test
    @DisplayName("Admin approval transitions profile to APPROVED and clears any rejection reason")
    void testAdminApproval_Success() {
        MentorProfile pending = MentorProfile.builder()
                .id(20L)
                .user(studentUser)
                .jobTitle("Lead Dev")
                .company("Meta")
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .rejectionReason("Prior issues")
                .isActive(true)
                .maxMentees(3)
                .build();

        when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(pending));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> i.getArgument(0));

        MentorProfileDto result = mentorProfileService.approveApplication(999L, 20L);

        assertEquals(MentorVerificationStatus.APPROVED, result.getVerificationStatus());
        assertNull(result.getRejectionReason());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("Admin approval prevents admin from approving their own application")
    void testAdminApproval_SelfApproval_ThrowsException() {
        MentorProfile adminProfile = MentorProfile.builder()
                .id(20L)
                .user(mentorUser)
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .build();

        when(mentorProfileRepository.findById(20L)).thenReturn(Optional.of(adminProfile));

        assertThrows(IllegalArgumentException.class, () ->
                mentorProfileService.approveApplication(mentorUser.getId(), 20L));
    }

    @Test
    @DisplayName("Admin reject and suspend transitions correctly")
    void testAdminRejectAndSuspend() {
        when(mentorProfileRepository.findById(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> i.getArgument(0));

        MentorProfileDto suspended = mentorProfileService.suspendMentor(approvedMentor.getId(), "Policy breach");
        assertEquals(MentorVerificationStatus.SUSPENDED, suspended.getVerificationStatus());
        assertFalse(suspended.isActive());
        assertEquals("Policy breach", suspended.getRejectionReason());

        MentorProfileDto rejected = mentorProfileService.rejectApplication(approvedMentor.getId(), "Incomplete profile");
        assertEquals(MentorVerificationStatus.REJECTED, rejected.getVerificationStatus());
        assertEquals("Incomplete profile", rejected.getRejectionReason());
    }

    @Test
    @DisplayName("Public directory filters only APPROVED and active mentors")
    void testGetApprovedMentors_ExcludesNonApproved() {
        MentorProfile suspended = MentorProfile.builder()
                .id(2L)
                .verificationStatus(MentorVerificationStatus.SUSPENDED)
                .isActive(false)
                .build();

        when(mentorProfileRepository.findByVerificationStatusAndIsActiveTrue(MentorVerificationStatus.APPROVED))
                .thenReturn(List.of(approvedMentor));

        List<MentorProfileDto> result = mentorProfileService.getApprovedMentors(null, null);

        assertEquals(1, result.size());
        assertEquals(approvedMentor.getId(), result.get(0).getId());
    }

    // ================================================================
    // MENTORSHIP REQUEST WORKFLOW TESTS
    // ================================================================

    @Test
    @DisplayName("Student creates mentorship request successfully")
    void testCreateRequest_Success() {
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(mentorProfileRepository.findById(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        when(mentorshipRequestRepository.countByMentorIdAndStatus(approvedMentor.getId(), MentorshipRequestStatus.ACCEPTED)).thenReturn(1L);
        when(mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(anyLong(), anyLong(), anyList())).thenReturn(false);
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(i -> {
            MentorshipRequest r = i.getArgument(0);
            r.setId(301L);
            return r;
        });

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto("Need career guidance on high-throughput distributed systems.");
        MentorshipRequestDto result = mentorshipRequestService.createRequest(studentUser.getId(), approvedMentor.getId(), dto);

        assertNotNull(result);
        assertEquals(301L, result.getId());
        assertEquals(MentorshipRequestStatus.PENDING, result.getStatus());
        assertEquals("Alice Student", result.getStudentName());
        assertEquals("Bob Mentor", result.getMentorName());
    }

    @Test
    @DisplayName("Self-mentorship request throws IllegalArgumentException")
    void testCreateRequest_SelfMentorship_ThrowsException() {
        when(userRepository.findById(mentorUser.getId())).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.findById(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto("Mentoring myself");

        assertThrows(IllegalArgumentException.class, () ->
                mentorshipRequestService.createRequest(mentorUser.getId(), approvedMentor.getId(), dto));
    }

    @Test
    @DisplayName("Requesting unapproved or inactive mentor throws IllegalStateException")
    void testCreateRequest_UnapprovedMentor_ThrowsException() {
        MentorProfile pendingMentor = MentorProfile.builder()
                .id(3L)
                .user(mentorUser)
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .isActive(true)
                .build();

        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(mentorProfileRepository.findById(pendingMentor.getId())).thenReturn(Optional.of(pendingMentor));

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto("Learn Spring Boot");

        assertThrows(IllegalStateException.class, () ->
                mentorshipRequestService.createRequest(studentUser.getId(), pendingMentor.getId(), dto));
    }

    @Test
    @DisplayName("Creating request when mentor is at full capacity throws IllegalStateException")
    void testCreateRequest_AtCapacity_ThrowsException() {
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(mentorProfileRepository.findById(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        // Mentor max is 3, currently has 3 active mentees
        when(mentorshipRequestRepository.countByMentorIdAndStatus(approvedMentor.getId(), MentorshipRequestStatus.ACCEPTED)).thenReturn(3L);

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto("Learn Spring Boot");

        assertThrows(IllegalStateException.class, () ->
                mentorshipRequestService.createRequest(studentUser.getId(), approvedMentor.getId(), dto));
    }

    @Test
    @DisplayName("Creating duplicate pending or active request throws DuplicateResourceException")
    void testCreateRequest_DuplicatePending_ThrowsException() {
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(mentorProfileRepository.findById(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        when(mentorshipRequestRepository.countByMentorIdAndStatus(approvedMentor.getId(), MentorshipRequestStatus.ACCEPTED)).thenReturn(1L);
        when(mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(eq(studentUser.getId()), eq(approvedMentor.getId()), anyList()))
                .thenReturn(true);

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto("Learn Spring Boot");

        assertThrows(DuplicateResourceException.class, () ->
                mentorshipRequestService.createRequest(studentUser.getId(), approvedMentor.getId(), dto));
    }

    // ================================================================
    // CONCURRENCY & MESSAGING INTEGRATION TESTS
    // ================================================================

    @Test
    @DisplayName("Mentor accepts request under pessimistic lock, creates conversation, and updates status to ACCEPTED")
    void testAcceptRequest_Success_WithPessimisticLockAndConversation() {
        MentorshipRequest pending = MentorshipRequest.builder()
                .id(500L)
                .student(studentUser)
                .mentor(approvedMentor)
                .goalDescription("Learn microservices")
                .status(MentorshipRequestStatus.PENDING)
                .build();

        when(mentorshipRequestRepository.findById(500L)).thenReturn(Optional.of(pending));
        // Verify pessimistic lock query is invoked
        when(mentorProfileRepository.findByIdWithLock(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        when(mentorshipRequestRepository.countByMentorIdAndStatus(approvedMentor.getId(), MentorshipRequestStatus.ACCEPTED)).thenReturn(1L);
        when(mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(eq(studentUser.getId()), eq(approvedMentor.getId()), anyList()))
                .thenReturn(false);

        Conversation mockConv = Conversation.builder()
                .id(990L)
                .title("Bob & Alice")
                .type(ConversationType.USER_TO_USER)
                .build();

        ConversationResponseDto convDto = ConversationResponseDto.builder()
                .id(990L)
                .title("Bob & Alice")
                .conversationType("USER_TO_USER")
                .build();

        when(conversationService.createUserToUserConversation(eq(mentorUser), eq(studentUser.getId()), anyString()))
                .thenReturn(convDto);
        when(conversationRepository.findById(990L)).thenReturn(Optional.of(mockConv));
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(i -> i.getArgument(0));

        MentorshipRequestDto result = mentorshipRequestService.acceptRequest(mentorUser.getId(), 500L, "Glad to mentor you!");

        assertNotNull(result);
        assertEquals(MentorshipRequestStatus.ACCEPTED, result.getStatus());
        assertEquals(990L, result.getConversationId());
        assertEquals("Glad to mentor you!", result.getResponseNote());

        verify(mentorProfileRepository, times(1)).findByIdWithLock(approvedMentor.getId());
        verify(conversationService, times(1)).createUserToUserConversation(eq(mentorUser), eq(studentUser.getId()), anyString());
    }

    @Test
    @DisplayName("Accept request fails if mentor hits capacity under lock")
    void testAcceptRequest_CapacityExceededUnderLock_ThrowsException() {
        MentorshipRequest pending = MentorshipRequest.builder()
                .id(500L)
                .student(studentUser)
                .mentor(approvedMentor)
                .status(MentorshipRequestStatus.PENDING)
                .build();

        when(mentorshipRequestRepository.findById(500L)).thenReturn(Optional.of(pending));
        when(mentorProfileRepository.findByIdWithLock(approvedMentor.getId())).thenReturn(Optional.of(approvedMentor));
        // Capacity reached
        when(mentorshipRequestRepository.countByMentorIdAndStatus(approvedMentor.getId(), MentorshipRequestStatus.ACCEPTED)).thenReturn(3L);

        assertThrows(IllegalStateException.class, () ->
                mentorshipRequestService.acceptRequest(mentorUser.getId(), 500L, "Note"));
    }

    @Test
    @DisplayName("Accept request fails with AccessDeniedException if caller is not the mentor")
    void testAcceptRequest_UnauthorizedCaller_ThrowsAccessDenied() {
        MentorshipRequest pending = MentorshipRequest.builder()
                .id(500L)
                .student(studentUser)
                .mentor(approvedMentor)
                .status(MentorshipRequestStatus.PENDING)
                .build();

        when(mentorshipRequestRepository.findById(500L)).thenReturn(Optional.of(pending));

        assertThrows(AccessDeniedException.class, () ->
                mentorshipRequestService.acceptRequest(999L, 500L, "Note"));
    }

    @Test
    @DisplayName("Decline and Cancel transition properly to DECLINED and CANCELLED")
    void testDeclineAndCancelTransitions() {
        MentorshipRequest pending = MentorshipRequest.builder()
                .id(501L)
                .student(studentUser)
                .mentor(approvedMentor)
                .status(MentorshipRequestStatus.PENDING)
                .build();

        when(mentorshipRequestRepository.findById(501L)).thenReturn(Optional.of(pending));
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(i -> i.getArgument(0));

        MentorshipRequestDto declined = mentorshipRequestService.declineRequest(mentorUser.getId(), 501L, "Schedule full");
        assertEquals(MentorshipRequestStatus.DECLINED, declined.getStatus());
        assertEquals("Schedule full", declined.getResponseNote());

        pending.setStatus(MentorshipRequestStatus.PENDING);
        MentorshipRequestDto cancelled = mentorshipRequestService.cancelRequest(studentUser.getId(), 501L);
        assertEquals(MentorshipRequestStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("Complete mentorship transitions to COMPLETED and preserves conversation")
    void testCompleteMentorship_PreservesConversation() {
        Conversation mockConv = Conversation.builder()
                .id(990L)
                .title("Bob & Alice")
                .type(ConversationType.USER_TO_USER)
                .build();

        MentorshipRequest active = MentorshipRequest.builder()
                .id(502L)
                .student(studentUser)
                .mentor(approvedMentor)
                .conversation(mockConv)
                .status(MentorshipRequestStatus.ACCEPTED)
                .build();

        when(mentorshipRequestRepository.findById(502L)).thenReturn(Optional.of(active));
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenAnswer(i -> i.getArgument(0));

        MentorshipRequestDto completed = mentorshipRequestService.completeMentorship(mentorUser.getId(), 502L, "Great progress!");

        assertEquals(MentorshipRequestStatus.COMPLETED, completed.getStatus());
        assertEquals(990L, completed.getConversationId(), "Conversation ID must remain preserved after completion");
    }

    @Test
    @DisplayName("Mentorship capacity config validates min, max, and default boundaries")
    void testCapacityConfig_BoundaryValidation() {
        assertEquals(1, capacityConfigService.getMinMentees());
        assertEquals(10, capacityConfigService.getMaxMentees());
        assertEquals(3, capacityConfigService.getDefaultMentees());

        // Valid update
        MentorshipCapacityConfigDto update = new MentorshipCapacityConfigDto(2, 8, 4);
        MentorshipCapacityConfigDto updated = capacityConfigService.updateConfig(update);
        assertEquals(2, updated.getMinMentees());
        assertEquals(8, updated.getMaxMentees());
        assertEquals(4, updated.getDefaultMentees());

        // Invalid update (max < min)
        assertThrows(IllegalArgumentException.class, () ->
                capacityConfigService.updateConfig(new MentorshipCapacityConfigDto(5, 3, 4)));

        // Resolving capacity
        assertEquals(4, capacityConfigService.validateAndResolveCapacity(null));
        assertEquals(6, capacityConfigService.validateAndResolveCapacity(6));
        assertThrows(IllegalArgumentException.class, () ->
                capacityConfigService.validateAndResolveCapacity(12));
    }
}
