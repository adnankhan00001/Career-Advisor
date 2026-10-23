package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CareerDto;
import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.dto.RecentActivityDto;
import com.careeradvisor.backend.dto.RoadmapSectionDto;
import com.careeradvisor.backend.model.InterviewStatus;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserRoadmapProgress;
import com.careeradvisor.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserProgressService {

    private final UserRoadmapProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final RoadmapService roadmapService;
    private final CareerService careerService;
    private final UserSkillService skillService;

    @Autowired(required = false)
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired(required = false)
    private MockInterviewRepository interviewRepository;

    @Autowired(required = false)
    private ResumeRepository resumeRepository;

    @Autowired(required = false)
    private MentorshipRequestRepository mentorshipRequestRepository;

    public UserProgressService(UserRoadmapProgressRepository progressRepository,
                               UserRepository userRepository,
                               RoadmapService roadmapService,
                               CareerService careerService,
                               UserSkillService skillService) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.roadmapService = roadmapService;
        this.careerService = careerService;
        this.skillService = skillService;
    }

    public List<String> getCompletedSteps(User user) {
        return progressRepository.findByUser(user)
                .stream()
                .map(UserRoadmapProgress::getStepTitle)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> toggleStep(User user, String stepTitle, String careerTitle) {
        String step = stepTitle.trim();
        String career = (careerTitle != null && !careerTitle.trim().isEmpty())
                ? careerTitle.trim()
                : (user.getCareerGoal() != null ? user.getCareerGoal() : null);

        if (career == null) {
            throw new IllegalArgumentException("Career track must be specified or set on user profile to toggle step progress");
        }

        Optional<UserRoadmapProgress> existing = progressRepository
                .findByUserAndCareerTitleAndStepTitle(user, career, step);

        if (existing.isPresent()) {
            progressRepository.delete(existing.get());
        } else {
            progressRepository.save(new UserRoadmapProgress(user, career, step));
        }

        return getCompletedSteps(user);
    }

    @Transactional
    public String updateCareerGoal(User user, String newGoal) {
        String goal = newGoal.trim();
        user.setCareerGoal(goal);
        userRepository.save(user);
        return goal;
    }

    @Transactional
    public void resetProgress(User user) {
        progressRepository.deleteByUser(user);
    }

    public ProgressSummaryResponse getProgressSummary(User user) {
        String careerGoal = user.getCareerGoal() != null && !user.getCareerGoal().trim().isEmpty()
                ? user.getCareerGoal().trim()
                : null;

        List<String> completedSteps = getCompletedSteps(user);
        List<String> skills = skillService.getUserSkills(user);

        if (careerGoal == null) {
            return ProgressSummaryResponse.builder()
                    .careerGoal(null)
                    .roadmapPercent(0)
                    .completedStepsCount(0)
                    .totalStepsCount(0)
                    .skillsCount(skills.size())
                    .skills(skills)
                    .completedSteps(completedSteps)
                    .userLevel(user.getUserLevel() != null ? user.getUserLevel() : "Beginner")
                    .latestQuizScore(user.getLatestQuizScore())
                    .nextTopicToLearn(null)
                    .skillMatchPercentage(0)
                    .build();
        }

        Set<String> completedSet = new HashSet<>(completedSteps);

        List<RoadmapSectionDto> sections = roadmapService.getRoadmapForCareer(careerGoal);
        int totalStepsInGoal = 0;
        int completedInGoal = 0;
        String nextTopic = null;

        for (RoadmapSectionDto section : sections) {
            for (String step : section.getSteps()) {
                totalStepsInGoal++;
                if (completedSet.contains(step)) {
                    completedInGoal++;
                } else if (nextTopic == null) {
                    nextTopic = step;
                }
            }
        }

        if (nextTopic == null) {
            nextTopic = (totalStepsInGoal > 0 && completedInGoal == totalStepsInGoal)
                    ? "All milestones completed!"
                    : "Getting Started with Core Concepts";
        }

        int roadmapPercent = totalStepsInGoal > 0
                ? Math.round(((float) completedInGoal / totalStepsInGoal) * 100)
                : 0;

        // Compute skill match percentage
        int matchPercentage = 0;
        Optional<CareerDto> careerOpt = careerService.getCareerByIdOrTitle(careerGoal);
        if (careerOpt.isPresent() && !careerOpt.get().getRequiredSkills().isEmpty()) {
            List<String> required = careerOpt.get().getRequiredSkills();
            Set<String> userSkillsNormalized = skills.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            long matchCount = required.stream()
                    .filter(req -> userSkillsNormalized.contains(req.toLowerCase()))
                    .count();

            matchPercentage = Math.round(((float) matchCount / required.size()) * 100);
        }

        return ProgressSummaryResponse.builder()
                .careerGoal(careerGoal)
                .roadmapPercent(roadmapPercent)
                .completedStepsCount(completedInGoal)
                .totalStepsCount(totalStepsInGoal)
                .skillsCount(skills.size())
                .skills(skills)
                .completedSteps(completedSteps)
                .userLevel(user.getUserLevel() != null ? user.getUserLevel() : "Intermediate")
                .latestQuizScore(user.getLatestQuizScore())
                .nextTopicToLearn(nextTopic)
                .skillMatchPercentage(matchPercentage)
                .build();
    }

    public List<RecentActivityDto> getRecentActivities(User user) {
        List<RecentActivityDto> activities = new ArrayList<>();

        // 1. Diagnostic Assessments
        if (quizAttemptRepository != null) {
            quizAttemptRepository.findByUserOrderByCreatedAtDesc(user).stream()
                    .limit(2)
                    .forEach(q -> activities.add(RecentActivityDto.builder()
                            .id("quiz-" + q.getId())
                            .activityType("ASSESSMENT")
                            .title("Completed Diagnostic Assessment")
                            .subtitle("Score: " + q.getPercentage() + "% (" + q.getScore() + "/" + q.getTotalQuestions() + ") • " + q.getLevel() + " Tier")
                            .timestamp(q.getCreatedAt())
                            .actionUrl("/quiz")
                            .status("COMPLETED")
                            .build()));
        }

        // 2. Technical Mock Interviews
        if (interviewRepository != null) {
            interviewRepository.findByUserOrderByStartedAtDesc(user).stream()
                    .limit(2)
                    .forEach(i -> activities.add(RecentActivityDto.builder()
                            .id("interview-" + i.getId())
                            .activityType("INTERVIEW")
                            .title("Mock Interview: " + (i.getCategory() != null ? i.getCategory().name() : "Technical"))
                            .subtitle(i.getStatus() == InterviewStatus.COMPLETED
                                    ? "Score: " + i.getScore() + "% • " + (i.getDifficulty() != null ? i.getDifficulty().name() : "Standard")
                                    : "Status: " + i.getStatus().name())
                            .timestamp(i.getCompletedAt() != null ? i.getCompletedAt() : i.getStartedAt())
                            .actionUrl("/mock-interview")
                            .status(i.getStatus().name())
                            .build()));
        }

        // 3. Resume Upload & Analysis
        if (resumeRepository != null) {
            resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user).ifPresent(r -> {
                activities.add(RecentActivityDto.builder()
                        .id("resume-" + r.getId())
                        .activityType("RESUME")
                        .title("Resume Analyzed: " + r.getOriginalFileName())
                        .subtitle("Extracted Skills & Career Alignment Verified")
                        .timestamp(r.getUploadTimestamp())
                        .actionUrl("/resume")
                        .status(r.getParsingStatus() != null ? r.getParsingStatus().name() : "COMPLETED")
                        .build());
            });
        }

        // 4. Completed Roadmap Milestones
        if (progressRepository != null) {
            progressRepository.findByUser(user).stream()
                    .filter(p -> p.getCompletedAt() != null)
                    .sorted(Comparator.comparing(UserRoadmapProgress::getCompletedAt).reversed())
                    .limit(2)
                    .forEach(p -> activities.add(RecentActivityDto.builder()
                            .id("roadmap-" + p.getId())
                            .activityType("ROADMAP")
                            .title("Mastered Milestone: " + p.getStepTitle())
                            .subtitle("Curriculum Track: " + p.getCareerTitle())
                            .timestamp(p.getCompletedAt())
                            .actionUrl("/roadmap")
                            .status("COMPLETED")
                            .build()));
        }

        // 5. Mentorship Interaction
        if (mentorshipRequestRepository != null) {
            mentorshipRequestRepository.findByStudentIdOrderByCreatedAtDesc(user.getId()).stream()
                    .limit(2)
                    .forEach(m -> {
                        String mentorName = (m.getMentor() != null && m.getMentor().getUser() != null)
                                ? m.getMentor().getUser().getName() : "Industry Mentor";
                        activities.add(RecentActivityDto.builder()
                                .id("mentor-req-" + m.getId())
                                .activityType("MENTORSHIP")
                                .title("Mentorship Request: " + mentorName)
                                .subtitle("Status: " + m.getStatus().name())
                                .timestamp(m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt())
                                .actionUrl("/mentorship")
                                .status(m.getStatus().name())
                                .build());
                    });
        }

        // Sort by timestamp descending and take top 4
        return activities.stream()
                .filter(a -> a.getTimestamp() != null)
                .sorted(Comparator.comparing(RecentActivityDto::getTimestamp).reversed())
                .limit(4)
                .collect(Collectors.toList());
    }
}
