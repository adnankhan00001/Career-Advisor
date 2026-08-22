package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.Resume;
import com.careeradvisor.backend.model.ResumeStatus;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeParserService resumeParserService;
    private final ResumeSkillExtractor skillExtractor;
    private final UserSkillService userSkillService;
    private final CareerService careerService;

    public ResumeService(ResumeRepository resumeRepository,
                         ResumeParserService resumeParserService,
                         ResumeSkillExtractor skillExtractor,
                         UserSkillService userSkillService,
                         CareerService careerService) {
        this.resumeRepository = resumeRepository;
        this.resumeParserService = resumeParserService;
        this.skillExtractor = skillExtractor;
        this.userSkillService = userSkillService;
        this.careerService = careerService;
    }

    @Transactional
    public ResumeAnalysisResponse uploadAndAnalyze(User user, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String fileType = file.getContentType();
        long fileSize = file.getSize();

        // 1. Extract Plain Text from PDF/DOCX
        String parsedText = resumeParserService.extractText(file);

        // 2. Extract Skills and Sections
        List<String> userSkills = userSkillService.getUserSkills(user);
        Set<String> userSkillSet = new HashSet<>(userSkills);

        List<ExtractedSkillDto> extractedSkills = skillExtractor.extractSkills(parsedText, userSkillSet);
        String summary = skillExtractor.extractSummary(parsedText);
        String email = skillExtractor.extractEmail(parsedText);
        String phone = skillExtractor.extractPhone(parsedText);
        List<String> education = skillExtractor.extractEducation(parsedText);
        List<String> experience = skillExtractor.extractExperience(parsedText);
        List<String> projects = skillExtractor.extractProjects(parsedText);

        List<String> skillNames = extractedSkills.stream()
                .map(ExtractedSkillDto::getSkillName)
                .collect(Collectors.toList());

        // 3. Persist Resume
        Resume resume = Resume.builder()
                .user(user)
                .originalFileName(fileName)
                .fileType(fileType != null ? fileType : "application/octet-stream")
                .fileSize(fileSize)
                .parsingStatus(ResumeStatus.COMPLETED)
                .parsedText(parsedText)
                .extractedSummary(summary)
                .extractedEmail(email)
                .extractedPhone(phone)
                .extractedEducation(String.join("\n", education))
                .extractedExperience(String.join("\n", experience))
                .extractedProjects(String.join("\n", projects))
                .extractedSkills(skillNames)
                .build();

        Resume saved = resumeRepository.save(resume);

        // 4. Compute Matches and Gaps
        return buildAnalysisResponse(saved, user, extractedSkills, education, experience, projects);
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> getUserResumes(User user) {
        return resumeRepository.findByUserOrderByUploadTimestampDesc(user)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getResumeAnalysis(User user, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        return buildAnalysisFromEntity(resume, user);
    }

    @Transactional(readOnly = true)
    public Optional<ResumeAnalysisResponse> getLatestAnalysis(User user) {
        return resumeRepository.findFirstByUserOrderByUploadTimestampDesc(user)
                .map(resume -> buildAnalysisFromEntity(resume, user));
    }

    @Transactional
    public void deleteResume(User user, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        resumeRepository.delete(resume);
    }

    @Transactional
    public SyncSkillsResponse syncSkills(User user, Long resumeId, List<String> skillsToSync) {
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        int addedCount = 0;
        List<String> currentSkills = userSkillService.getUserSkills(user);
        Set<String> currentSkillSet = currentSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());

        if (skillsToSync != null) {
            for (String skill : skillsToSync) {
                if (skill != null && !skill.trim().isEmpty() && !currentSkillSet.contains(skill.trim().toLowerCase())) {
                    userSkillService.addSkill(user, skill.trim());
                    addedCount++;
                    currentSkillSet.add(skill.trim().toLowerCase());
                }
            }
        }

        List<String> updatedSkills = userSkillService.getUserSkills(user);

        return SyncSkillsResponse.builder()
                .syncedCount(addedCount)
                .userSkills(updatedSkills)
                .message("Successfully synced " + addedCount + " skill(s) to your profile.")
                .build();
    }

    private ResumeAnalysisResponse buildAnalysisFromEntity(Resume resume, User user) {
        List<String> userSkills = userSkillService.getUserSkills(user);
        Set<String> userSkillSet = new HashSet<>(userSkills);

        List<ExtractedSkillDto> skillDtos = skillExtractor.extractSkills(resume.getParsedText(), userSkillSet);
        if (skillDtos.isEmpty() && resume.getExtractedSkills() != null) {
            skillDtos = resume.getExtractedSkills().stream()
                    .map(s -> ExtractedSkillDto.builder()
                            .skillName(s)
                            .category("Technical Skills")
                            .confidence(90)
                            .alreadyInProfile(userSkillSet.contains(s.toLowerCase()))
                            .build())
                    .collect(Collectors.toList());
        }

        List<String> education = resume.getExtractedEducation() != null && !resume.getExtractedEducation().isEmpty()
                ? Arrays.asList(resume.getExtractedEducation().split("\n"))
                : Collections.emptyList();

        List<String> experience = resume.getExtractedExperience() != null && !resume.getExtractedExperience().isEmpty()
                ? Arrays.asList(resume.getExtractedExperience().split("\n"))
                : Collections.emptyList();

        List<String> projects = resume.getExtractedProjects() != null && !resume.getExtractedProjects().isEmpty()
                ? Arrays.asList(resume.getExtractedProjects().split("\n"))
                : Collections.emptyList();

        return buildAnalysisResponse(resume, user, skillDtos, education, experience, projects);
    }

    private ResumeAnalysisResponse buildAnalysisResponse(Resume resume,
                                                         User user,
                                                         List<ExtractedSkillDto> extractedSkills,
                                                         List<String> education,
                                                         List<String> experience,
                                                         List<String> projects) {
        List<String> userSkills = userSkillService.getUserSkills(user);

        // Combined skills: user existing + resume detected
        Set<String> combinedSkills = new HashSet<>();
        for (String s : userSkills) combinedSkills.add(s.toLowerCase());
        for (ExtractedSkillDto dto : extractedSkills) combinedSkills.add(dto.getSkillName().toLowerCase());

        String targetGoal = user.getCareerGoal() != null && !user.getCareerGoal().trim().isEmpty()
                ? user.getCareerGoal() : "Java Backend Developer";

        List<CareerDto> allCareers = careerService.getAllCareers();

        // 1. Calculate Matched Careers based on Resume Skills
        List<CareerMatchDto> careerMatches = new ArrayList<>();
        for (CareerDto career : allCareers) {
            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (String req : career.getRequiredSkills()) {
                if (combinedSkills.contains(req.toLowerCase())) {
                    matched.add(req);
                } else {
                    missing.add(req);
                }
            }

            int total = career.getRequiredSkills().size();
            int matchScore = total > 0 ? Math.round(((float) matched.size() / total) * 100) : 0;
            boolean isTarget = career.getTitle().equalsIgnoreCase(targetGoal);

            careerMatches.add(CareerMatchDto.builder()
                    .title(career.getTitle())
                    .category(career.getCategory())
                    .matchScore(matchScore)
                    .skillScore(matchScore)
                    .goalScore(isTarget ? 100 : 50)
                    .quizScore(70)
                    .matchedSkills(matched)
                    .missingSkills(missing)
                    .targetGoal(isTarget)
                    .build());
        }

        careerMatches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));

        // 2. Calculate Target Career Skill Gap
        CareerDto targetCareer = allCareers.stream()
                .filter(c -> c.getTitle().equalsIgnoreCase(targetGoal))
                .findFirst()
                .orElse(allCareers.get(0));

        List<String> targetMatched = new ArrayList<>();
        List<String> targetMissing = new ArrayList<>();

        for (String req : targetCareer.getRequiredSkills()) {
            if (combinedSkills.contains(req.toLowerCase())) {
                targetMatched.add(req);
            } else {
                targetMissing.add(req);
            }
        }

        int targetTotal = targetCareer.getRequiredSkills().size();
        int targetMatchPct = targetTotal > 0 ? Math.round(((float) targetMatched.size() / targetTotal) * 100) : 0;
        List<String> highPriority = targetMissing.stream().limit(3).collect(Collectors.toList());

        SkillGapDto skillGap = SkillGapDto.builder()
                .targetCareer(targetCareer.getTitle())
                .matchPercentage(targetMatchPct)
                .acquiredSkills(targetMatched)
                .missingSkills(targetMissing)
                .highPriorityMissing(highPriority)
                .build();

        // 3. Recommended Actions
        List<RecommendationItemDto> recommendations = new ArrayList<>();
        if (!highPriority.isEmpty()) {
            recommendations.add(RecommendationItemDto.builder()
                    .id("resume-gap-1")
                    .priority("HIGH")
                    .type("SKILL_GAP")
                    .title("Bridge Missing Resume Skill: " + highPriority.get(0))
                    .description("Adding '" + highPriority.get(0) + "' increases your " + targetGoal + " match to " +
                            Math.min(100, targetMatchPct + 15) + "%.")
                    .actionUrl("/roadmap")
                    .actionText("Learn on Roadmap")
                    .score(targetMatchPct)
                    .build());
        }

        if (extractedSkills.stream().anyMatch(s -> !s.isAlreadyInProfile())) {
            recommendations.add(RecommendationItemDto.builder()
                    .id("resume-sync-1")
                    .priority("HIGH")
                    .type("RESUME_SYNC")
                    .title("Sync Detected Skills to Profile")
                    .description("Confirm detected resume skills to unlock updated assessment questions and coding practice.")
                    .actionUrl("/resume")
                    .actionText("Review Skills")
                    .score(extractedSkills.size())
                    .build());
        }

        return ResumeAnalysisResponse.builder()
                .resumeId(resume.getId())
                .fileName(resume.getOriginalFileName())
                .fileSize(resume.getFileSize())
                .fileType(resume.getFileType())
                .parsingStatus(resume.getParsingStatus())
                .uploadTimestamp(resume.getUploadTimestamp())
                .summary(resume.getExtractedSummary())
                .extractedEmail(resume.getExtractedEmail())
                .extractedPhone(resume.getExtractedPhone())
                .extractedSkills(extractedSkills)
                .extractedEducation(education)
                .extractedExperience(experience)
                .extractedProjects(projects)
                .matchedCareers(careerMatches)
                .skillGaps(skillGap)
                .recommendations(recommendations)
                .build();
    }

    private ResumeDto toDto(Resume resume) {
        return ResumeDto.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .uploadTimestamp(resume.getUploadTimestamp())
                .parsingStatus(resume.getParsingStatus())
                .skillsCount(resume.getExtractedSkills() != null ? resume.getExtractedSkills().size() : 0)
                .extractedSummary(resume.getExtractedSummary())
                .extractedEmail(resume.getExtractedEmail())
                .extractedPhone(resume.getExtractedPhone())
                .build();
    }
}
