package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorApplicationDto;
import com.careeradvisor.backend.dto.MentorProfileDto;
import com.careeradvisor.backend.dto.MentorProfileUpdateDto;
import com.careeradvisor.backend.exception.DuplicateResourceException;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.MentorProfile;
import com.careeradvisor.backend.model.MentorVerificationStatus;
import com.careeradvisor.backend.model.MentorshipRequestStatus;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.MentorProfileRepository;
import com.careeradvisor.backend.repository.MentorshipRequestRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MentorProfileService {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final UserRepository userRepository;
    private final MentorshipCapacityConfigService capacityConfigService;

    public MentorProfileService(
            MentorProfileRepository mentorProfileRepository,
            MentorshipRequestRepository mentorshipRequestRepository,
            UserRepository userRepository,
            MentorshipCapacityConfigService capacityConfigService) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.userRepository = userRepository;
        this.capacityConfigService = capacityConfigService;
    }

    @Transactional
    public MentorProfileDto submitApplication(Long userId, MentorApplicationDto dto) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (mentorProfileRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("You have already submitted a mentor application or have an existing mentor profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        validateUrl(dto.getLinkedinUrl(), "LinkedIn");
        validateUrl(dto.getGithubUrl(), "GitHub");
        validateUrl(dto.getPortfolioUrl(), "Portfolio");

        int resolvedMaxMentees = capacityConfigService.validateAndResolveCapacity(dto.getMaxMentees());

        MentorProfile profile = MentorProfile.builder()
                .user(user)
                .jobTitle(dto.getJobTitle().trim())
                .company(dto.getCompany().trim())
                .yearsOfExperience(dto.getYearsOfExperience())
                .bio(dto.getBio() != null ? dto.getBio().trim() : null)
                .expertiseAreas(dto.getExpertiseAreas().trim())
                .maxMentees(resolvedMaxMentees)
                .linkedinUrl(dto.getLinkedinUrl() != null ? dto.getLinkedinUrl().trim() : null)
                .githubUrl(dto.getGithubUrl() != null ? dto.getGithubUrl().trim() : null)
                .portfolioUrl(dto.getPortfolioUrl() != null ? dto.getPortfolioUrl().trim() : null)
                .verificationStatus(MentorVerificationStatus.APPLIED)
                .isActive(true)
                .build();

        MentorProfile saved = mentorProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public MentorProfileDto getMyProfile(Long userId) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No mentor profile found for user"));
        return mapToDto(profile);
    }

    @Transactional
    public MentorProfileDto updateMyProfile(Long userId, MentorProfileUpdateDto dto) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No mentor profile found for user"));

        if (dto.getJobTitle() != null && !dto.getJobTitle().trim().isEmpty()) {
            profile.setJobTitle(dto.getJobTitle().trim());
        }
        if (dto.getCompany() != null && !dto.getCompany().trim().isEmpty()) {
            profile.setCompany(dto.getCompany().trim());
        }
        if (dto.getYearsOfExperience() != null) {
            if (dto.getYearsOfExperience() < 0) {
                throw new IllegalArgumentException("Years of experience cannot be negative");
            }
            profile.setYearsOfExperience(dto.getYearsOfExperience());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio().trim());
        }
        if (dto.getExpertiseAreas() != null && !dto.getExpertiseAreas().trim().isEmpty()) {
            profile.setExpertiseAreas(dto.getExpertiseAreas().trim());
        }
        if (dto.getMaxMentees() != null) {
            int validatedCapacity = capacityConfigService.validateAndResolveCapacity(dto.getMaxMentees());
            profile.setMaxMentees(validatedCapacity);
        }
        if (dto.getLinkedinUrl() != null) {
            validateUrl(dto.getLinkedinUrl(), "LinkedIn");
            profile.setLinkedinUrl(dto.getLinkedinUrl().trim());
        }
        if (dto.getGithubUrl() != null) {
            validateUrl(dto.getGithubUrl(), "GitHub");
            profile.setGithubUrl(dto.getGithubUrl().trim());
        }
        if (dto.getPortfolioUrl() != null) {
            validateUrl(dto.getPortfolioUrl(), "Portfolio");
            profile.setPortfolioUrl(dto.getPortfolioUrl().trim());
        }
        if (dto.getIsActive() != null) {
            profile.setActive(dto.getIsActive());
        }

        // Note: verificationStatus and rejectionReason are never updated by applicant
        MentorProfile saved = mentorProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MentorProfileDto> getApprovedMentors(String query, String expertise) {
        List<MentorProfile> mentors;
        if (query != null && !query.trim().isEmpty()) {
            mentors = mentorProfileRepository.searchApprovedMentors(MentorVerificationStatus.APPROVED, query.trim());
        } else {
            mentors = mentorProfileRepository.findByVerificationStatusAndIsActiveTrue(MentorVerificationStatus.APPROVED);
        }

        if (expertise != null && !expertise.trim().isEmpty()) {
            String lowerExpertise = expertise.trim().toLowerCase();
            mentors = mentors.stream()
                    .filter(m -> m.getExpertiseAreas() != null && m.getExpertiseAreas().toLowerCase().contains(lowerExpertise))
                    .collect(Collectors.toList());
        }

        return mentors.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MentorProfileDto getMentorById(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorId));

        if (profile.getVerificationStatus() != MentorVerificationStatus.APPROVED || !profile.isActive()) {
            throw new ResourceNotFoundException("Mentor profile is not active or available");
        }

        return mapToDto(profile);
    }

    // ================================================================
    // ADMINISTRATIVE OPERATIONS (ROLE_ADMIN ONLY)
    // ================================================================

    @Transactional(readOnly = true)
    public List<MentorProfileDto> getPendingApplications() {
        return mentorProfileRepository.findByVerificationStatus(MentorVerificationStatus.APPLIED)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MentorProfileDto> getAllMentorsForAdmin(MentorVerificationStatus status) {
        List<MentorProfile> profiles = status != null
                ? mentorProfileRepository.findByVerificationStatus(status)
                : mentorProfileRepository.findAll();
        return profiles.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MentorProfileDto getMentorByIdForAdmin(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorId));
        return mapToDto(profile);
    }

    @Transactional
    public MentorProfileDto approveApplication(Long adminUserId, Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorId));

        if (adminUserId != null && profile.getUser() != null && profile.getUser().getId().equals(adminUserId)) {
            throw new IllegalArgumentException("Admins cannot approve their own mentor application");
        }

        profile.setVerificationStatus(MentorVerificationStatus.APPROVED);
        profile.setActive(true);
        profile.setRejectionReason(null);

        MentorProfile saved = mentorProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional
    public MentorProfileDto rejectApplication(Long mentorId, String reason) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorId));

        profile.setVerificationStatus(MentorVerificationStatus.REJECTED);
        profile.setRejectionReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Application did not meet requirements");

        MentorProfile saved = mentorProfileRepository.save(profile);
        return mapToDto(saved);
    }

    @Transactional
    public MentorProfileDto suspendMentor(Long mentorId, String reason) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor profile not found with id: " + mentorId));

        profile.setVerificationStatus(MentorVerificationStatus.SUSPENDED);
        profile.setActive(false);
        profile.setRejectionReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Mentor profile suspended by administration");

        MentorProfile saved = mentorProfileRepository.save(profile);
        return mapToDto(saved);
    }

    public MentorProfileDto mapToDto(MentorProfile profile) {
        long activeCount = mentorshipRequestRepository.countByMentorIdAndStatus(profile.getId(), MentorshipRequestStatus.ACCEPTED);
        int max = profile.getMaxMentees() != null ? profile.getMaxMentees() : 3;
        long availableSlots = Math.max(0, max - activeCount);

        return MentorProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .name(profile.getUser() != null ? profile.getUser().getName() : null)
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .jobTitle(profile.getJobTitle())
                .company(profile.getCompany())
                .yearsOfExperience(profile.getYearsOfExperience())
                .bio(profile.getBio())
                .expertiseAreas(profile.getExpertiseAreas())
                .maxMentees(max)
                .activeMenteesCount(activeCount)
                .availableSlots(availableSlots)
                .verificationStatus(profile.getVerificationStatus())
                .isActive(profile.isActive())
                .rejectionReason(profile.getRejectionReason())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .portfolioUrl(profile.getPortfolioUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private void validateUrl(String url, String fieldName) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        String trimmed = url.trim().toLowerCase();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException(fieldName + " URL must start with http:// or https://");
        }
    }
}
