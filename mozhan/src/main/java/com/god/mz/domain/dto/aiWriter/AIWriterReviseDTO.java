package com.god.mz.domain.dto.aiWriter;
import lombok.AllArgsConstructor; import lombok.Builder; import lombok.Data; import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIWriterReviseDTO {
    /**
     * 编辑器当前正文（HTML）——方案A的权威上下文，每轮由前端携带
     */
    private String currentContent;

    /**
     * 本轮修改要求
     */
    private String instruction;
}