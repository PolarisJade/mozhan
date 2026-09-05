package com.god.mz.domain.dto.aiWriter;
import lombok.AllArgsConstructor; import lombok.Builder; import lombok.Data; import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIWriterOutlineDTO {
    /**
     * 文章主题
     */
    private String topic;

    /**
     * 写作要求（可选）
     */
    private String requirement;
}