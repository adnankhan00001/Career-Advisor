package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CareerDto;
import com.careeradvisor.backend.dto.ProgressSummaryResponse;
import com.careeradvisor.backend.dto.RoadmapSectionDto;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserRoadmapProgress;
import com.careeradvisor.backend.repository.UserRoadmapProgressRepository;
import com.careeradvisor.backend.repository.UserRepository;
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
                : (user.getCareerGoal() != null ? user.getCareerGoal() : "Java Backend Developer");

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
                ? user.getCareerGoal()
                : "Java Backend Developer";

        List<String> completedSteps = getCompletedSteps(user);
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

        List<String> skills = skillService.getUserSkills(user);

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
}
