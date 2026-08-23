package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {
    private Long id;
    private String name;
    private String careerGoal;
    private String userLevel;
    private Role role;
    private boolean online;
}
