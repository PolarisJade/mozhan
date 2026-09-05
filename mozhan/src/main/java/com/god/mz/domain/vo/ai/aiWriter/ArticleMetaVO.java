package com.god.mz.domain.vo.ai.aiWriter;
import lombok.AllArgsConstructor; import lombok.Builder; import lombok.Data; import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleMetaVO {
    /**
     * 推荐标题
     */
    private String title;

    /**
     * 推荐摘要
     */
    private String summary;
}