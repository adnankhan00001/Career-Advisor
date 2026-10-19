package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.ConversationResponseDto;
import com.careeradvisor.backend.dto.CreateMentorshipRequestDto;
import com.careeradvisor.backend.dto.MentorshipRequestDto;
import com.careeradvisor.backend.exception.DuplicateResourceException;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.ConversationRepository;
import com.careeradvisor.backend.repository.MentorProfileRepository;
import com.careeradvisor.backend.repository.MentorshipRequestRepository;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.repository.UserSkillRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MentorshipRequestService {

    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final UserSkillRepository userSkillRepository;

    public MentorshipRequestService(
            MentorshipRequestRepository mentorshipRequestRepository,
            MentorProfileRepository mentorProfileRepository,
            UserRepository userRepository,
            ConversationService conversationService,
            ConversationRepository conversationRepository,
            UserSkillRepository userSkillRepository) {
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
        this.conversationRepository = conversationRepository;
        this.userSkillRepository = userSkillRepository;
    }

    @Transactional
    public MentorshipRequestDto createRequest(Long studentUserId, Long mentorProfileId, CreateMentorshipRequestDto dto) {
        if (studentUserId == null) {
            throw new IllegalArgumentException("Student user ID is required");
        }
        if (mentorProfileId == null) {
            throw new IllegalArgumentException("Mentor profile ID is required");
        }

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found with id: " + studentUserId));

        MentorProfile mentor = mentorProfileRepository.findById(mentorProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorProfileId));

        if (mentor.getUser() != null && mentor.getUser().getId().equals(studentUserId)) {
            throw new IllegalArgumentException("You cannot request mentorship from yourself");
        }

        if (mentor.getVerificationStatus() != MentorVerificationStatus.APPROVED || !mentor.isActive()) {
            throw new IllegalStateException("Mentor is not currently approved and available for mentorship requests");
        }

        long activeCount = mentorshipRequestRepository.countByMentorIdAndStatus(mentor.getId(), MentorshipRequestStatus.ACCEPTED);
        int maxMentees = mentor.getMaxMentees() != null ? mentor.getMaxMentees() : 3;
        if (activeCount >= maxMentees) {
            throw new IllegalStateException("Mentor has reached maximum active mentee capacity (" + maxMentees + ")");
        }

        boolean hasPending = mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(
                studentUserId, mentorProfileId, List.of(MentorshipRequestStatus.PENDING));
        if (hasPending) {
            throw new DuplicateResourceException("You already have a pending mentorship request with this mentor");
        }

        boolean hasActive = mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(
                studentUserId, mentorProfileId, List.of(MentorshipRequestStatus.ACCEPTED));
        if (hasActive) {
            throw new DuplicateResourceException("You already have an active mentorship relationship with this mentor");
        }

        MentorshipRequest request = MentorshipRequest.builder()
                .student(student)
                .mentor(mentor)
                .goalDescription(dto.getGoalDescription().trim())
                .status(MentorshipRequestStatus.PENDING)
                .build();

        MentorshipRequest saved = mentorshipRequestRepository.save(request);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MentorshipRequestDto> getSentRequests(Long studentUserId) {
        return mentorshipRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentUserId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MentorshipRequestDto> getReceivedRequests(Long mentorUserId) {
        MentorProfile mentor = mentorProfileRepository.findByUserId(mentorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You do not have an active mentor profile"));

        return mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MentorshipRequestDto acceptRequest(Long mentorUserId, Long requestId, String responseNote) {
        MentorshipRequest request = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + requestId));

        if (request.getMentor().getUser() == null || !request.getMentor().getUser().getId().equals(mentorUserId)) {
            throw new AccessDeniedException("You are not authorized to accept this mentorship request");
        }

        if (request.getStatus() != MentorshipRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be accepted. Current status: " + request.getStatus());
        }

        // Concurrency control: acquire pessimistic write lock on the mentor profile to prevent race conditions
        MentorProfile mentor = mentorProfileRepository.findByIdWithLock(request.getMentor().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found"));

        if (mentor.getVerificationStatus() != MentorVerificationStatus.APPROVED || !mentor.isActive()) {
            throw new IllegalStateException("Mentor profile is not active or approved");
        }

        long activeCount = mentorshipRequestRepository.countByMentorIdAndStatus(mentor.getId(), MentorshipRequestStatus.ACCEPTED);
        int maxMentees = mentor.getMaxMentees() != null ? mentor.getMaxMentees() : 3;
        if (activeCount >= maxMentees) {
            throw new IllegalStateException("Mentor has reached maximum active mentee capacity (" + maxMentees + ")");
        }

        boolean alreadyActive = mentorshipRequestRepository.existsByStudentIdAndMentorIdAndStatusIn(
                request.getStudent().getId(), mentor.getId(), List.of(MentorshipRequestStatus.ACCEPTED));
        if (alreadyActive) {
            throw new IllegalStateException("An active mentorship relationship already exists between this student and mentor");
        }

        // Messaging integration: create or reuse 1-on-1 direct conversation
        String initialMsg = "Mentorship request accepted! Welcome to the mentorship program.";
        if (responseNote != null && !responseNote.trim().isEmpty()) {
            initialMsg = initialMsg + " Note from mentor: " + responseNote.trim();
        }

        ConversationResponseDto convDto = conversationService.createUserToUserConversation(
                mentor.getUser(), request.getStudent().getId(), initialMsg);

        Conversation conversation = conversationRepository.findById(convDto.getId()).orElse(null);
        request.setConversation(conversation);
        request.setStatus(MentorshipRequestStatus.ACCEPTED);
        request.setResponseNote(responseNote != null ? responseNote.trim() : null);

        MentorshipRequest saved = mentorshipRequestRepository.save(request);
        return mapToDto(saved);
    }

    @Transactional
    public MentorshipRequestDto declineRequest(Long mentorUserId, Long requestId, String responseNote) {
        MentorshipRequest request = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + requestId));

        if (request.getMentor().getUser() == null || !request.getMentor().getUser().getId().equals(mentorUserId)) {
            throw new AccessDeniedException("You are not authorized to decline this mentorship request");
        }

        if (request.getStatus() != MentorshipRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be declined. Current status: " + request.getStatus());
        }

        request.setStatus(MentorshipRequestStatus.DECLINED);
        request.setResponseNote(responseNote != null ? responseNote.trim() : null);

        MentorshipRequest saved = mentorshipRequestRepository.save(request);
        return mapToDto(saved);
    }

    @Transactional
    public MentorshipRequestDto cancelRequest(Long studentUserId, Long requestId) {
        MentorshipRequest request = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + requestId));

        if (!request.getStudent().getId().equals(studentUserId)) {
            throw new AccessDeniedException("You are not authorized to cancel this mentorship request");
        }

        if (request.getStatus() != MentorshipRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be cancelled. Current status: " + request.getStatus());
        }

        request.setStatus(MentorshipRequestStatus.CANCELLED);
        MentorshipRequest saved = mentorshipRequestRepository.save(request);
        return mapToDto(saved);
    }

    @Transactional
    public MentorshipRequestDto completeMentorship(Long currentUserId, Long requestId, String note) {
        MentorshipRequest request = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + requestId));

        boolean isStudent = request.getStudent().getId().equals(currentUserId);
        boolean isMentor = request.getMentor().getUser() != null && request.getMentor().getUser().getId().equals(currentUserId);

        if (!isStudent && !isMentor) {
            throw new AccessDeniedException("You are not a participant in this mentorship relationship");
        }

        if (request.getStatus() != MentorshipRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Only active (ACCEPTED) mentorships can be marked as completed. Current status: " + request.getStatus());
        }

        request.setStatus(MentorshipRequestStatus.COMPLETED);
        if (note != null && !note.trim().isEmpty()) {
            request.setResponseNote((request.getResponseNote() != null ? request.getResponseNote() + " | " : "") + "Completion note: " + note.trim());
        }

        MentorshipRequest saved = mentorshipRequestRepository.save(request);
        return mapToDto(saved);
    }

    public MentorshipRequestDto mapToDto(MentorshipRequest r) {
        String careerGoal = null;
        String userLevel = null;
        List<String> studentSkills = Collections.emptyList();

        if (r.getStudent() != null) {
            careerGoal = r.getStudent().getCareerGoal();
            userLevel = r.getStudent().getUserLevel();
            if (userSkillRepository != null) {
                studentSkills = userSkillRepository.findByUser(r.getStudent())
                        .stream()
                        .map(UserSkill::getSkillName)
                        .collect(Collectors.toList());
            }
        }

        return MentorshipRequestDto.builder()
                .id(r.getId())
                .studentId(r.getStudent() != null ? r.getStudent().getId() : null)
                .studentName(r.getStudent() != null ? r.getStudent().getName() : null)
                .studentEmail(r.getStudent() != null ? r.getStudent().getEmail() : null)
                .studentCareerGoal(careerGoal)
                .studentUserLevel(userLevel)
                .studentSkills(studentSkills)
                .mentorProfileId(r.getMentor() != null ? r.getMentor().getId() : null)
                .mentorUserId(r.getMentor() != null && r.getMentor().getUser() != null ? r.getMentor().getUser().getId() : null)
                .mentorName(r.getMentor() != null && r.getMentor().getUser() != null ? r.getMentor().getUser().getName() : null)
                .mentorCompany(r.getMentor() != null ? r.getMentor().getCompany() : null)
                .mentorJobTitle(r.getMentor() != null ? r.getMentor().getJobTitle() : null)
                .goalDescription(r.getGoalDescription())
                .status(r.getStatus())
                .conversationId(r.getConversation() != null ? r.getConversation().getId() : null)
                .responseNote(r.getResponseNote())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
