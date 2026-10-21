package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminStatsOverviewDto;
import com.careeradvisor.backend.dto.AdminUserDetailDto;
import com.careeradvisor.backend.dto.AdminUserDto;
import com.careeradvisor.backend.dto.CategoryCountProjection;
import com.careeradvisor.backend.dto.PageResponse;
import com.careeradvisor.backend.dto.UpdateUserStatusRequest;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.MockInterview;
import com.careeradvisor.backend.model.Resume;
import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.AdminAuditAction;
import com.careeradvisor.backend.model.AdminAuditTargetType;
import com.careeradvisor.backend.model.UserStatus;
import com.careeradvisor.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private UserProblemProgressRepository problemProgressRepository;

    @Autowired
    private MockInterviewRepository mockInterviewRepository;

    @Autowired
    private UserSkillRepository userSkillRepository;

    @Autowired
    private UserRoadmapProgressRepository roadmapProgressRepository;

    @Autowired(required = false)
    private AdminAuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AdminStatsOverviewDto getStatsOverview() {
        long totalUsers = userRepository.count();
        long totalResumes = resumeRepository.count();
        long totalQuizAttempts = userRepository.countByLatestQuizScoreIsNotNull();
        long totalSolvedProblems = problemProgressRepository.countBySolvedTrue();
        long totalMockInterviews = mockInterviewRepository.count();
        long totalCompletedInterviews = mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED);

        Map<String, Long> careerGoalsDistribution = new LinkedHashMap<>();
        List<CategoryCountProjection> careerGoalList = userRepository.countGroupedByCareerGoal();
        if (careerGoalList != null) {
            for (CategoryCountProjection p : careerGoalList) {
                if (p != null && StringUtils.hasText(p.getCategory())) {
                    careerGoalsDistribution.put(p.getCategory().trim(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        Map<String, Long> userLevelDistribution = new LinkedHashMap<>();
        List<CategoryCountProjection> userLevelList = userRepository.countGroupedByUserLevel();
        if (userLevelList != null) {
            for (CategoryCountProjection p : userLevelList) {
                if (p != null && StringUtils.hasText(p.getCategory())) {
                    userLevelDistribution.put(p.getCategory().trim(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        long totalCareerGoals = careerGoalsDistribution.values().stream().mapToLong(Long::longValue).sum();

        return AdminStatsOverviewDto.builder()
                .totalUsers(totalUsers)
                .totalResumes(totalResumes)
                .totalQuizAttempts(totalQuizAttempts)
                .totalSolvedProblems(totalSolvedProblems)
                .totalMockInterviews(totalMockInterviews)
                .totalCompletedInterviews(totalCompletedInterviews)
                .totalCareerGoals(totalCareerGoals)
                .careerGoalsDistribution(careerGoalsDistribution)
                .userLevelDistribution(userLevelDistribution)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserDto> getUsers(int page, int size, String sort, String search, Role role, UserStatus status) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (StringUtils.hasText(sort)) {
            String[] parts = sort.split(",");
            String property = parts[0].trim();
            Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            if (Arrays.asList("id", "name", "email", "role", "status", "createdAt", "careerGoal", "userLevel").contains(property)) {
                sortObj = Sort.by(direction, property);
            }
        }

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sortObj);

        Specification<User> spec = Specification.where(null);
        if (StringUtils.hasText(search)) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("email")), term)
            ));
        }
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<User> users = userPage.getContent();
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        Map<Long, Long> skillCounts = new HashMap<>();
        Set<Long> usersWithResumes = new HashSet<>();
        Map<Long, Long> interviewCounts = new HashMap<>();
        Map<Long, Long> solvedCounts = new HashMap<>();

        if (!userIds.isEmpty()) {
            for (Object[] row : userSkillRepository.countSkillsByUserIds(userIds)) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    skillCounts.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }

            usersWithResumes.addAll(resumeRepository.findUserIdsWithResumes(userIds));

            for (Object[] row : mockInterviewRepository.countInterviewsByUserIds(userIds)) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    interviewCounts.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }

            for (Object[] row : problemProgressRepository.countSolvedProblemsByUserIds(userIds)) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    solvedCounts.put((Long) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        List<AdminUserDto> dtos = users.stream().map(u -> AdminUserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .status(u.getStatus() != null ? u.getStatus() : UserStatus.ACTIVE)
                .suspensionReason(u.getSuspensionReason())
                .suspendedAt(u.getSuspendedAt())
                .createdAt(u.getCreatedAt())
                .careerGoal(u.getCareerGoal())
                .userLevel(u.getUserLevel())
                .latestQuizScore(u.getLatestQuizScore())
                .skillCount(skillCounts.getOrDefault(u.getId(), 0L))
                .resumePresent(usersWithResumes.contains(u.getId()))
                .mockInterviewCount(interviewCounts.getOrDefault(u.getId(), 0L))
                .solvedProblemsCount(solvedCounts.getOrDefault(u.getId(), 0L))
                .build()
        ).collect(Collectors.toList());

        return PageResponse.of(userPage, dtos);
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> getAllUsers(String search) {
        return getUsers(0, 1000, "id,asc", search, null, null).getContent();
    }

    @Transactional
    public AdminUserDetailDto updateUserStatus(Long adminUserId, Long targetUserId, UpdateUserStatusRequest request) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (adminUserId != null && adminUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Administrators cannot modify their own account status.");
        }

        if (targetUser.getRole() == Role.ADMIN && request.getStatus() != UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot suspend or deactivate the last remaining active administrator.");
            }
        }

        UserStatus previousStatus = targetUser.getStatus() != null ? targetUser.getStatus() : UserStatus.ACTIVE;
        AdminAuditAction auditAction;
        String actionSummary;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("targetUserId", targetUserId);
        details.put("targetEmail", targetUser.getEmail());
        details.put("targetName", targetUser.getName());
        details.put("previousStatus", previousStatus.name());
        details.put("newStatus", request.getStatus().name());

        if (request.getStatus() == UserStatus.SUSPENDED) {
            if (!StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("A suspension reason is mandatory when suspending an account.");
            }
            targetUser.setStatus(UserStatus.SUSPENDED);
            targetUser.setSuspensionReason(request.getReason().trim());
            targetUser.setSuspendedAt(LocalDateTime.now());
            auditAction = AdminAuditAction.USER_SUSPENDED;
            actionSummary = "Suspended user account: " + targetUser.getEmail() + " (Reason: " + request.getReason().trim() + ")";
            details.put("reason", request.getReason().trim());
        } else if (request.getStatus() == UserStatus.ACTIVE) {
            targetUser.setStatus(UserStatus.ACTIVE);
            targetUser.setSuspensionReason(null);
            targetUser.setSuspendedAt(null);
            auditAction = AdminAuditAction.USER_REACTIVATED;
            actionSummary = "Reactivated user account: " + targetUser.getEmail();
        } else if (request.getStatus() == UserStatus.DEACTIVATED) {
            targetUser.setStatus(UserStatus.DEACTIVATED);
            if (StringUtils.hasText(request.getReason())) {
                targetUser.setSuspensionReason(request.getReason().trim());
                details.put("reason", request.getReason().trim());
            }
            auditAction = AdminAuditAction.USER_DEACTIVATED;
            actionSummary = "Deactivated user account: " + targetUser.getEmail();
        } else {
            throw new IllegalArgumentException("Unsupported user status: " + request.getStatus());
        }

        userRepository.save(targetUser);

        if (auditLogService != null) {
            auditLogService.recordAction(
                    adminUserId,
                    null,
                    null,
                    auditAction,
                    AdminAuditTargetType.USER,
                    targetUserId.toString(),
                    actionSummary,
                    details
            );
        }

        return getUserDetail(targetUserId);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<String> skills = userSkillRepository.findByUserOrderByAddedAtDesc(user).stream()
                .map(us -> us.getSkillName())
                .collect(Collectors.toList());

        List<Resume> resumes = resumeRepository.findByUserOrderByUploadTimestampDesc(user);
        boolean resumePresent = !resumes.isEmpty();
        String resumeFileName = resumePresent ? resumes.get(0).getOriginalFileName() : null;
        String resumeUploadTime = resumePresent && resumes.get(0).getUploadTimestamp() != null
                ? resumes.get(0).getUploadTimestamp().toString() : null;

        List<MockInterview> interviews = mockInterviewRepository.findByUserOrderByStartedAtDesc(user);
        long mockInterviewCount = interviews.size();
        OptionalDouble avgScore = interviews.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .mapToInt(MockInterview::getScore)
                .average();

        long solvedCount = problemProgressRepository.countByUserAndSolvedTrue(user);
        long completedStepsCount = roadmapProgressRepository.countByUser(user);

        return AdminUserDetailDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE)
                .suspensionReason(user.getSuspensionReason())
                .suspendedAt(user.getSuspendedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .careerGoal(user.getCareerGoal())
                .userLevel(user.getUserLevel())
                .latestQuizScore(user.getLatestQuizScore())
                .skills(skills)
                .resumePresent(resumePresent)
                .resumeFileName(resumeFileName)
                .resumeUploadTimestamp(resumeUploadTime)
                .mockInterviewCount(mockInterviewCount)
                .averageInterviewScore(avgScore.isPresent() ? Math.round(avgScore.getAsDouble() * 10.0) / 10.0 : null)
                .solvedProblemsCount(solvedCount)
                .completedRoadmapStepsCount(completedStepsCount)
                .build();
    }
}
