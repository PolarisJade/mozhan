package com.god.mz.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatDTO {

    /**
     * 用户的问题
     */
    private String question;
    /**
     * 会话id
     */
    private String sessionId;
}