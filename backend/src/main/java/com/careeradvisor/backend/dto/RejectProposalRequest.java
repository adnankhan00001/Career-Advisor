package com.careeradvisor.backend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectProposalRequest {

    private String reason;
}
