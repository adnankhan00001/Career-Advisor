package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import java.util.stream.Collectors;

@Service
public class AdminMentorshipService {

    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserSkillRepository userSkillRepository;
    private final AdminAuditLogService auditLogService;

    public AdminMentorshipService(
            MentorshipRequestRepository mentorshipRequestRepository,
            MentorProfileRepository mentorProfileRepository,
            ChatMessageRepository chatMessageRepository,
            UserSkillRepository userSkillRepository) {
        this(mentorshipRequestRepository, mentorProfileRepository, chatMessageRepository, userSkillRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminMentorshipService(
            MentorshipRequestRepository mentorshipRequestRepository,
            MentorProfileRepository mentorProfileRepository,
            ChatMessageRepository chatMessageRepository,
            UserSkillRepository userSkillRepository,
            AdminAuditLogService auditLogService) {
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userSkillRepository = userSkillRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public MentorshipOversightSummaryDto getMentorshipSummary() {
        long totalRequests = mentorshipRequestRepository.count();
        Map<MentorshipRequestStatus, Long> counts = new HashMap<>();

        for (Object[] row : mentorshipRequestRepository.countGroupedByStatus()) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                MentorshipRequestStatus status = (MentorshipRequestStatus) row[0];
                long count = ((Number) row[1]).longValue();
                counts.put(status, count);
            }
        }

        long pending = counts.getOrDefault(MentorshipRequestStatus.PENDING, 0L);
        long accepted = counts.getOrDefault(MentorshipRequestStatus.ACCEPTED, 0L);
        long completed = counts.getOrDefault(MentorshipRequestStatus.COMPLETED, 0L);
        long declined = counts.getOrDefault(MentorshipRequestStatus.DECLINED, 0L);
        long cancelled = counts.getOrDefault(MentorshipRequestStatus.CANCELLED, 0L);
        long terminated = counts.getOrDefault(MentorshipRequestStatus.TERMINATED, 0L);

        return MentorshipOversightSummaryDto.builder()
                .totalRequests(totalRequests)
                .pendingRequests(pending)
                .acceptedRequests(accepted)
                .completedRequests(completed)
                .declinedRequests(declined)
                .cancelledRequests(cancelled)
                .terminatedRequests(terminated)
                .activeMentorships(accepted)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminMentorshipRequestDto> getMentorshipRequests(
            int page,
            int size,
            String sort,
            String search,
            MentorshipRequestStatus status,
            LocalDate fromDate,
            LocalDate toDate) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            if (Arrays.asList("id", "createdAt", "updatedAt", "status").contains(property)) {
                sortObj = Sort.by(direction, property);
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sortObj);

        Specification<MentorshipRequest> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("student").get("name")), term),
                    cb.like(cb.lower(root.get("student").get("email")), term),
                    cb.like(cb.lower(root.get("mentor").get("user").get("name")), term),
                    cb.like(cb.lower(root.get("mentor").get("company")), term),
                    cb.like(cb.lower(root.get("mentor").get("jobTitle")), term)
            ));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (fromDate != null) {
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
        }

        if (toDate != null) {
            LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
        }

        Page<MentorshipRequest> reqPage = mentorshipRequestRepository.findAll(spec, pageable);
        List<MentorshipRequest> requests = reqPage.getContent();

        List<AdminMentorshipRequestDto> dtos = requests.stream().map(r -> {
            LocalDateTime lastActivity = r.getUpdatedAt();
            if (r.getConversation() != null && r.getConversation().getLastMessageAt() != null) {
                lastActivity = r.getConversation().getLastMessageAt();
            }

            return AdminMentorshipRequestDto.builder()
                    .id(r.getId())
                    .studentId(r.getStudent() != null ? r.getStudent().getId() : null)
                    .studentName(r.getStudent() != null ? r.getStudent().getName() : null)
                    .studentEmail(r.getStudent() != null ? r.getStudent().getEmail() : null)
                    .studentCareerGoal(r.getStudent() != null ? r.getStudent().getCareerGoal() : null)
                    .mentorProfileId(r.getMentor() != null ? r.getMentor().getId() : null)
                    .mentorUserId(r.getMentor() != null && r.getMentor().getUser() != null ? r.getMentor().getUser().getId() : null)
                    .mentorName(r.getMentor() != null && r.getMentor().getUser() != null ? r.getMentor().getUser().getName() : null)
                    .mentorEmail(r.getMentor() != null && r.getMentor().getUser() != null ? r.getMentor().getUser().getEmail() : null)
                    .mentorCompany(r.getMentor() != null ? r.getMentor().getCompany() : null)
                    .mentorJobTitle(r.getMentor() != null ? r.getMentor().getJobTitle() : null)
                    .goalDescription(r.getGoalDescription())
                    .status(r.getStatus())
                    .conversationId(r.getConversation() != null ? r.getConversation().getId() : null)
                    .lastActivityAt(lastActivity)
                    .responseNote(r.getResponseNote())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.of(reqPage, dtos);
    }

    @Transactional(readOnly = true)
    public AdminMentorshipDetailDto getMentorshipDetail(Long id) {
        MentorshipRequest request = mentorshipRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + id));

        List<String> studentSkills = Collections.emptyList();
        if (request.getStudent() != null) {
            studentSkills = userSkillRepository.findByUser(request.getStudent())
                    .stream()
                    .map(UserSkill::getSkillName)
                    .collect(Collectors.toList());
        }

        long messageCount = 0;
        String conversationTitle = null;
        LocalDateTime lastActivity = request.getUpdatedAt();

        if (request.getConversation() != null) {
            conversationTitle = request.getConversation().getTitle();
            messageCount = chatMessageRepository.countByConversation(request.getConversation());
            if (request.getConversation().getLastMessageAt() != null) {
                lastActivity = request.getConversation().getLastMessageAt();
            }
        }

        long mentorActiveMentees = 0;
        if (request.getMentor() != null) {
            mentorActiveMentees = mentorshipRequestRepository.countByMentorIdAndStatus(
                    request.getMentor().getId(), MentorshipRequestStatus.ACCEPTED);
        }

        return AdminMentorshipDetailDto.builder()
                .id(request.getId())
                .studentId(request.getStudent() != null ? request.getStudent().getId() : null)
                .studentName(request.getStudent() != null ? request.getStudent().getName() : null)
                .studentEmail(request.getStudent() != null ? request.getStudent().getEmail() : null)
                .studentCareerGoal(request.getStudent() != null ? request.getStudent().getCareerGoal() : null)
                .studentUserLevel(request.getStudent() != null ? request.getStudent().getUserLevel() : null)
                .studentSkills(studentSkills)
                .mentorProfileId(request.getMentor() != null ? request.getMentor().getId() : null)
                .mentorUserId(request.getMentor() != null && request.getMentor().getUser() != null ? request.getMentor().getUser().getId() : null)
                .mentorName(request.getMentor() != null && request.getMentor().getUser() != null ? request.getMentor().getUser().getName() : null)
                .mentorEmail(request.getMentor() != null && request.getMentor().getUser() != null ? request.getMentor().getUser().getEmail() : null)
                .mentorCompany(request.getMentor() != null ? request.getMentor().getCompany() : null)
                .mentorJobTitle(request.getMentor() != null ? request.getMentor().getJobTitle() : null)
                .mentorYearsOfExperience(request.getMentor() != null ? request.getMentor().getYearsOfExperience() : null)
                .mentorBio(request.getMentor() != null ? request.getMentor().getBio() : null)
                .mentorExpertiseAreas(request.getMentor() != null ? request.getMentor().getExpertiseAreas() : null)
                .mentorMaxMentees(request.getMentor() != null ? request.getMentor().getMaxMentees() : 3)
                .mentorActiveMenteesCount(mentorActiveMentees)
                .goalDescription(request.getGoalDescription())
                .status(request.getStatus())
                .conversationId(request.getConversation() != null ? request.getConversation().getId() : null)
                .conversationTitle(conversationTitle)
                .messageCount(messageCount)
                .lastActivityAt(lastActivity)
                .responseNote(request.getResponseNote())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    @Transactional
    public AdminMentorshipDetailDto terminateMentorship(Long adminUserId, Long requestId, TerminateMentorshipRequest request) {
        if (!StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("A meaningful termination reason is mandatory.");
        }

        MentorshipRequest targetRequest = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship request not found with id: " + requestId));

        MentorshipRequestStatus currentStatus = targetRequest.getStatus();
        if (currentStatus == MentorshipRequestStatus.CANCELLED ||
            currentStatus == MentorshipRequestStatus.DECLINED ||
            currentStatus == MentorshipRequestStatus.COMPLETED ||
            currentStatus == MentorshipRequestStatus.TERMINATED) {
            throw new IllegalStateException("Cannot terminate a mentorship request that is already in state: " + currentStatus);
        }

        targetRequest.setStatus(MentorshipRequestStatus.TERMINATED);
        String auditPrefix = "[ADMIN TERMINATED]: ";
        String newNote = auditPrefix + request.getReason().trim();
        if (StringUtils.hasText(targetRequest.getResponseNote())) {
            newNote = targetRequest.getResponseNote() + " | " + newNote;
        }
        targetRequest.setResponseNote(newNote);

        mentorshipRequestRepository.save(targetRequest);

        String studentName = targetRequest.getStudent() != null ? targetRequest.getStudent().getName() : "Student";
        String mentorName = (targetRequest.getMentor() != null && targetRequest.getMentor().getUser() != null)
                ? targetRequest.getMentor().getUser().getName() : "Mentor";

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("mentorshipRequestId", requestId);
        details.put("studentId", targetRequest.getStudent() != null ? targetRequest.getStudent().getId() : null);
        details.put("studentName", studentName);
        details.put("mentorProfileId", targetRequest.getMentor() != null ? targetRequest.getMentor().getId() : null);
        details.put("mentorName", mentorName);
        details.put("previousStatus", currentStatus.name());
        details.put("resultingStatus", MentorshipRequestStatus.TERMINATED.name());
        details.put("reason", request.getReason().trim());

        if (auditLogService != null) {
            auditLogService.recordAction(
                    adminUserId,
                    null,
                    null,
                    AdminAuditAction.MENTORSHIP_TERMINATED,
                    AdminAuditTargetType.MENTORSHIP_REQUEST,
                    requestId.toString(),
                    "Administratively terminated mentorship request #" + requestId + " between " + studentName + " and " + mentorName + " (Reason: " + request.getReason().trim() + ")",
                    details
            );
        }

        return getMentorshipDetail(requestId);
    }
}
