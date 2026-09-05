package com.god.mz.domain.dto.aiWriter;
import lombok.AllArgsConstructor; import lombok.Builder; import lombok.Data; import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIWriterMetaDTO {
    /**
     * 文章正文（HTML 或纯文本）
     */
    private String content;
}