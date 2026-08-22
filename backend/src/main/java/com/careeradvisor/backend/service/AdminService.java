package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AdminStatsOverviewDto;
import com.careeradvisor.backend.dto.AdminUserDetailDto;
import com.careeradvisor.backend.dto.AdminUserDto;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.MockInterview;
import com.careeradvisor.backend.model.Resume;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Transactional(readOnly = true)
    public AdminStatsOverviewDto getStatsOverview() {
        long totalUsers = userRepository.count();
        long totalResumes = resumeRepository.count();
        long totalQuizAttempts = userRepository.countByLatestQuizScoreIsNotNull();
        long totalSolvedProblems = problemProgressRepository.countBySolvedTrue();
        long totalMockInterviews = mockInterviewRepository.count();
        long totalCompletedInterviews = mockInterviewRepository.countByStatus(InterviewStatus.COMPLETED);

        List<User> allUsers = userRepository.findAll();
        long totalCareerGoals = allUsers.stream()
                .filter(u -> StringUtils.hasText(u.getCareerGoal()))
                .count();

        Map<String, Long> careerGoalsDistribution = allUsers.stream()
                .filter(u -> StringUtils.hasText(u.getCareerGoal()))
                .collect(Collectors.groupingBy(User::getCareerGoal, Collectors.counting()));

        Map<String, Long> userLevelDistribution = allUsers.stream()
                .filter(u -> StringUtils.hasText(u.getUserLevel()))
                .collect(Collectors.groupingBy(User::getUserLevel, Collectors.counting()));

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
    public List<AdminUserDto> getAllUsers(String search) {
        List<User> users;
        if (StringUtils.hasText(search)) {
            String term = search.trim();
            users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term);
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(u -> {
            long skillCount = userSkillRepository.countByUser(u);
            boolean resumePresent = !resumeRepository.findByUserOrderByUploadTimestampDesc(u).isEmpty();
            long interviewCount = mockInterviewRepository.countByUser(u);
            long solvedCount = problemProgressRepository.countByUserAndSolvedTrue(u);

            return AdminUserDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .role(u.getRole())
                    .careerGoal(u.getCareerGoal())
                    .userLevel(u.getUserLevel())
                    .latestQuizScore(u.getLatestQuizScore())
                    .skillCount(skillCount)
                    .resumePresent(resumePresent)
                    .mockInterviewCount(interviewCount)
                    .solvedProblemsCount(solvedCount)
                    .build();
        }).collect(Collectors.toList());
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
