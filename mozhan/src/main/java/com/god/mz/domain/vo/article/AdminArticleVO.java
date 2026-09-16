package com.god.mz.domain.vo.article;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.god.mz.common.enums.ArticleStatusEnum;
import com.god.mz.domain.vo.tag.TagVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章列表项。
 * <p>
 * 不复用 {@link ArticleVO}：那是前台列表的契约，改它会波及 mozhan-web。
 * </p>
 */
@Data
public class AdminArticleVO {
    private Long id;

    private String title;

    private String summary;

    private Long authorId;

    private String authorName;

    private Long categoryId;

    private String categoryName;

    /**
     * 标签列表。序列化成 JSON 后只保留 id/name（articleId 在后台列表里无意义）。
     */
    private List<TagVO> tags;

    private Long likeCount;

    private Long commentCount;

    /**
     * 枚举的 {@code @JsonValue} 打在中文 desc 上，所以这里出参是 "发布" / "草稿"。
     * 注意这只影响出参：作为查询条件传回来时走的是另一条转换链，见 AdminArticlePageQuery。
     */
    private ArticleStatusEnum status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
