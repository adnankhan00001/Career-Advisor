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
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 4000, message = "Message exceeds maximum allowed length of 4000 characters")
    private String content;
}
