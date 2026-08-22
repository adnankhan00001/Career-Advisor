package com.careeradvisor.backend.service;

import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.model.UserSkill;
import com.careeradvisor.backend.repository.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSkillService {

    private final UserSkillRepository userSkillRepository;

    public UserSkillService(UserSkillRepository userSkillRepository) {
        this.userSkillRepository = userSkillRepository;
    }

    public List<String> getUserSkills(User user) {
        return userSkillRepository.findByUser(user)
                .stream()
                .map(UserSkill::getSkillName)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> addSkill(User user, String rawSkill) {
        String skill = rawSkill.trim();
        if (skill.isEmpty()) {
            return getUserSkills(user);
        }

        if (!userSkillRepository.existsByUserAndSkillNameIgnoreCase(user, skill)) {
            userSkillRepository.save(new UserSkill(user, skill));
        }

        return getUserSkills(user);
    }

    @Transactional
    public List<String> removeSkill(User user, String rawSkill) {
        String skill = rawSkill.trim();
        userSkillRepository.deleteByUserAndSkillNameIgnoreCase(user, skill);
        return getUserSkills(user);
    }
}
