package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipActionDto {

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;
}
