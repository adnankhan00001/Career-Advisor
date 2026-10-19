package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMentorshipRequestDto {

    @NotBlank(message = "Goal description is required")
    @Size(max = 2000, message = "Goal description cannot exceed 2000 characters")
    private String goalDescription;
}
