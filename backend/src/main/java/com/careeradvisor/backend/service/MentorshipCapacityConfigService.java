package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.MentorshipCapacityConfigDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MentorshipCapacityConfigService {

    private final AtomicInteger minMentees = new AtomicInteger(1);
    private final AtomicInteger maxMentees = new AtomicInteger(10);
    private final AtomicInteger defaultMentees = new AtomicInteger(3);

    public MentorshipCapacityConfigService(
            @Value("${app.mentorship.capacity.min-mentees:1}") int min,
            @Value("${app.mentorship.capacity.max-mentees:10}") int max,
            @Value("${app.mentorship.capacity.default-mentees:3}") int def) {
        this.minMentees.set(Math.max(1, min));
        this.maxMentees.set(Math.max(this.minMentees.get(), max));
        this.defaultMentees.set(Math.min(this.maxMentees.get(), Math.max(this.minMentees.get(), def)));
    }

    public int getMinMentees() {
        return minMentees.get();
    }

    public int getMaxMentees() {
        return maxMentees.get();
    }

    public int getDefaultMentees() {
        return defaultMentees.get();
    }

    public MentorshipCapacityConfigDto getConfig() {
        return MentorshipCapacityConfigDto.builder()
                .minMentees(minMentees.get())
                .maxMentees(maxMentees.get())
                .defaultMentees(defaultMentees.get())
                .build();
    }

    public synchronized MentorshipCapacityConfigDto updateConfig(MentorshipCapacityConfigDto dto) {
        if (dto.getMinMentees() == null || dto.getMinMentees() < 1) {
            throw new IllegalArgumentException("Minimum mentees must be at least 1");
        }
        if (dto.getMaxMentees() == null || dto.getMaxMentees() < dto.getMinMentees()) {
            throw new IllegalArgumentException("Maximum mentees cannot be less than minimum mentees (" + dto.getMinMentees() + ")");
        }
        int defaultVal = dto.getDefaultMentees() != null ? dto.getDefaultMentees() : dto.getMinMentees();
        if (defaultVal < dto.getMinMentees() || defaultVal > dto.getMaxMentees()) {
            throw new IllegalArgumentException("Default mentees must be between " + dto.getMinMentees() + " and " + dto.getMaxMentees());
        }

        minMentees.set(dto.getMinMentees());
        maxMentees.set(dto.getMaxMentees());
        defaultMentees.set(defaultVal);

        return getConfig();
    }

    public int validateAndResolveCapacity(Integer requestedCapacity) {
        if (requestedCapacity == null) {
            return defaultMentees.get();
        }
        int min = minMentees.get();
        int max = maxMentees.get();
        if (requestedCapacity < min || requestedCapacity > max) {
            throw new IllegalArgumentException("Mentee capacity must be between " + min + " and " + max + " mentees");
        }
        return requestedCapacity;
    }
}
