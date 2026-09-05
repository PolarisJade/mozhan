package com.god.mz.domain.dto.aiWriter;
import lombok.AllArgsConstructor; import lombok.Builder; import lombok.Data; import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIWriterArticleDTO {
    /**
     * 文章主题
     */
    private String topic;

    /**
     * 文章大纲（可选，来自上一步生成的大纲）
     */
    private String outline;

    /**
     * 写作要求（可选）
     */
    private String requirement;
}