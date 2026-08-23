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
public class HumanMessageRequest {
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 4000, message = "Message content must not exceed 4000 characters")
    private String content;
}
